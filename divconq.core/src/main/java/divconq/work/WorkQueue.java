/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.work;

import divconq.hub.Hub;
import divconq.hub.HubState;
import divconq.hub.ISystemWork;
import divconq.hub.SysReporter;
import divconq.lang.FuncResult;
import divconq.lang.OperationResult;
import divconq.log.Logger;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

// longer term jobs than work pool - recoverable, retry, debugable, etc
public class WorkQueue implements IQueueDriver, IQueueAlerter {
	protected IQueueDriver impl = null;
	protected IQueueAlerter alerter = null;
	
	@Override
	public void init(OperationResult or, XElement config) {
		if (config == null)		// no error, it is ok to have a hub without a work queue 
			return;
		
		// setup the provider of the work queue
		String classname = config.getAttribute("InterfaceClass");
		
		if (StringUtil.isEmpty(classname)) {
			or.errorTr(173);
			return;
		}
		
		Object impl =  Hub.instance.getInstance(classname);		
		
		if ((impl == null) || !(impl instanceof IQueueDriver)) {
			or.errorTr(174, classname);
			return;
		}
		
		this.impl = (IQueueDriver)impl;
		this.impl.init(or, config);
		
		// setup the class to handle alerts
		classname = config.getAttribute("AlertClass");
		
		if (StringUtil.isNotEmpty(classname)) {
			impl =  Hub.instance.getInstance(classname);		
			
			if ((impl == null) || !(impl instanceof IQueueAlerter)) {
				or.errorTr(180, classname);
				return;
			}
			
			this.alerter = (IQueueAlerter)impl;
			this.alerter.init(or, config);
		}
		
		ISystemWork queuechecker = new ISystemWork() {
			@Override
			public void run(SysReporter reporter) {
				reporter.setStatus("Reviewing bucket work queues");
				
				if (Hub.instance.getState() != HubState.Running) 		// only grab work when running
					return;
				
				for (WorkBucket pool : Hub.instance.getWorkPool().getBuckets()) {
					if (!pool.getAutomaticQueueLoader())
						continue;
					
					int howmany = pool.availCount();    
					
					if (howmany < 1)
						continue;
					
					FuncResult<ListStruct> matches = WorkQueue.this.impl.findPotentialClaims(pool.getName(), howmany);
					
					if (matches.hasErrors()) {
						Logger.warn(matches.getMessage());
						continue;
					}
					
					ListStruct rs = matches.getResult();
					
					//System.out.print(rs.getSize() + "");
					
					for (Struct match : rs.getItems()) {
						RecordStruct rec = (RecordStruct)match;
						
						FuncResult<RecordStruct> claimop = WorkQueue.this.impl.makeClaim(rec);
						
						// ignore errors, typically means someone else got to it first
						if (claimop.hasErrors()) 
							continue;

						// replace
						rec = claimop.getResult();
						
						FuncResult<Task> loadop = WorkQueue.this.impl.loadWork(rec);
						
						// enough. should be logged, skip
						if (loadop.hasErrors())
							continue;
						
						// TODO fix dcQueue feature DCTASKLOG so we get the full builder object
						Task info = loadop.getResult();
						
						// TODO collect task objects here and watch lastActivity to update the claim
						// when updating claims, also routinely check for and update the logs in the db server?
						
						// TODO if being debugged put in session
						//Hub.instance.getSessions().createForSingleTaskAndDie(info);
						
						Hub.instance.getWorkPool().submit(info);
					}
				}
				
				reporter.setStatus("After bucket work queues");
			}

			@Override
			public int period() {
				// every 2 seconds to check for new tasks to claim - TODO config
				return 2;
			}
		};
		
		Hub.instance.getClock().addSlowSystemWorker(queuechecker);		
	}

	@Override
	public void start(OperationResult or) {
		if (this.impl != null)
			this.impl.start(or);
	}

	@Override
	public void stop(OperationResult or) {
		if (this.impl != null)
			this.impl.stop(or);
	}

	@Override
	public FuncResult<ListStruct> findPotentialClaims(String pool, int howmanymax) {
		if (this.impl != null)
			return this.impl.findPotentialClaims(pool, howmanymax);
		
		FuncResult<ListStruct> or = new FuncResult<ListStruct>();
		or.errorTr(172);
		return or;
	}
	
	@Override
	public FuncResult<RecordStruct> makeClaim(RecordStruct info) {
		if (this.impl != null)
			return this.impl.makeClaim(info);

		FuncResult<RecordStruct> or = new FuncResult<RecordStruct>();
		or.errorTr(172);
		return or;
	}
	
	@Override
	public OperationResult updateClaim(Task info) {
		if (this.impl != null)
			return this.impl.updateClaim(info);

		OperationResult or = new OperationResult();
		or.errorTr(172);
		return or;
	}
	
	public FuncResult<String> reserveUniqueAndSubmit(Task task) {
		FuncResult<String> cres = this.reserveUniqueWork(task.getId());
		
		if (cres.hasErrors())
			return cres;
		
		// if empty then assume someone else reserved it so skip (return "all is ok")
		if (cres.isEmptyResult())
			return new FuncResult<>();
		
		// we must have a claim, which means no one else can take it 
		task.withClaimedStamp(cres.getResult());
		return this.submit(task);
	}

	@Override
	public FuncResult<String> reserveUniqueWork(String taskidentity) {
		if (this.impl != null)
			return this.impl.reserveUniqueWork(taskidentity);

		FuncResult<String> or = new FuncResult<String>();
		or.errorTr(172);
		return or;
	}
	
	public FuncResult<String> reserveCurrentAndSubmit(Task task) {
		FuncResult<String> cres = this.reserveCurrentWork(task.getId());
		
		if (cres.hasErrors())
			return cres;
		
		// if empty then assume someone else reserved it so skip (return "all is ok")
		if (cres.isEmptyResult())
			return new FuncResult<>();
		
		// we must have a claim, which means no one else can take it 
		task.withClaimedStamp(cres.getResult());
		return this.submit(task);
	}

	@Override
	public FuncResult<String> reserveCurrentWork(String taskidentity) {
		if (this.impl != null)
			return this.impl.reserveCurrentWork(taskidentity);

		FuncResult<String> or = new FuncResult<String>();
		or.errorTr(172);
		return or;
	}

	@Override
	public FuncResult<String> submit(Task info) {
		info.prep();
		
		if (this.impl != null)
			return this.impl.submit(info);
		
		FuncResult<String> or = new FuncResult<>();
		or.errorTr(172);
		return or;
	}

	@Override
	public FuncResult<String> startWork(String workid) {
		if (this.impl != null)
			return this.impl.startWork(workid);
		
		FuncResult<String> or = new FuncResult<>();
		or.errorTr(172);
		return or;
	}

	public FuncResult<Task> loadWork(RecordStruct info) {
		if (this.impl != null)
			return this.impl.loadWork(info);
		
		FuncResult<Task> or = new FuncResult<>();
		or.errorTr(172);
		return or;
	}
	
	public OperationResult failWork(TaskRun task)  {
		task.getTask().withStatus("Failed");
		
		if (this.impl != null)
			return this.impl.endWork(task);
		
		OperationResult or = new OperationResult();
		or.errorTr(172);
		return or;
	}

	public OperationResult completeWork(TaskRun task) {
		// if work is complete, it is the final try
		task.getTask()
			.withFinalTry(true)
			.withStatus("Completed");
		
		if (this.impl != null)
			return this.impl.endWork(task);

		OperationResult or = new OperationResult();
		or.errorTr(172);
		return or;
	}

	@Override
	public OperationResult endWork(TaskRun task) {
		if (this.impl != null)
			return this.impl.endWork(task);
		
		OperationResult or = new OperationResult();
		or.errorTr(172);
		return or;
	}

	@Override
	public OperationResult trackWork(TaskRun task, boolean ended) {
		if (this.impl != null)
			return this.impl.trackWork(task, ended);
		
		OperationResult or = new OperationResult();
		or.errorTr(172);
		return or;
	}

	@Override
	public void sendAlert(long code, Object... params) {
		if (this.alerter != null)
			this.alerter.sendAlert(code, params);
	}

	@Override
	public ListStruct list() {
		if (this.impl != null)
			return this.impl.list();
		
		return null;
	}

	@Override
	public RecordStruct status(String taskid, String workid) {
		if (this.impl != null)
			return this.impl.status(taskid, workid);
		
		return null;
	}
}
