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

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.joda.time.DateTime;

import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.lang.IOperationLogger;
import divconq.lang.IOperationObserver;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.FileUtil;
import divconq.util.StringUtil;

/**
 * Do not run same task object in parallel 
 * 
 */
public class TaskRun extends FuncResult<Struct> implements Runnable {
	protected Task task = null;
	
	protected long started = -1;  
	protected long lastclaimed = -1; 
	protected int slot = 0;
	
	protected boolean completed = false;
	protected boolean killed = false;	
	
	protected final Lock completionlock = new ReentrantLock();
	
	protected Set<AutoCloseable> closeables = new HashSet<>();
	
	public boolean hasStarted() {
		return (this.started > -1);
	}
	
	// don't alter this after submitting to work pool, this is for view only as submit
	public Task getTask() {
		return this.task;
	}
	
	public TaskRun() {
		this.task = new Task(); 
	}
	
	public TaskRun(Task info) {
		this.task = info;
	}

	public void prep() {
		// if we are resuming, leave the rest alone
		if (this.started != -1)
			return;
		
		this.task.prep();
		
		this.setContext(this.task.getContext());

		// add any new observers
		for (IOperationObserver ob : this.task.getObservers())
			this.addObserver(ob);
		
		// loop task observers from Task as well as added at run time
		for (IOperationObserver cb : this.observers) {
			try {
				if (cb instanceof ITaskObserver)
					((ITaskObserver)cb).prep(this);			
			} 
			catch (Exception x) {
				this.error("Error notifying completing task: " + x);
			}
			
			// they might change context on us, return context
			OperationContext.set(this.opcontext);
		}
	}
	
	public boolean isComplete() {
		return this.completed;
	}
	
	public boolean isKilled() {
		return this.killed;
	}

	// must report if timed out, even if completed - otherwise Worker thread might lock forever if WorkBucket kills us first
	public boolean isHung() {
		return this.isInactive() || this.isOverdue();
	}
	
	public boolean isInactive() {	
		long timeout = this.task.getTimeoutMS();
        
        //System.out.println("Get last activity in active test: " + this.getLastActivity());
		
		// has activity been quiet for longer than timeout?  
		if ((timeout > 0) && (this.getLastActivity() < (System.currentTimeMillis() - timeout)))
				return true;
		
		return false;
	}
	
	// only become overdue after it has started
	public boolean isOverdue() {	
		long deadline = this.task.getDeadlineMS();
		
		// has activity been working too long?
		if ((this.started != -1) && (deadline > 0) && (this.started < (System.currentTimeMillis() - deadline)))
				return true;
		
		return false;
	}
	
	// if task has been doing work but not fast enough we may need to renew/review claim
	// will not work if you use less than 2 minutes for timeout
	public void reviewClaim() {
		// if not started, if completed or if hung then nothing to review
		if ((this.started == -1) || this.completed || this.isHung())
			return;

		// once every 5 seconds we can renew a claim  (might cause problems if run log is huge and we are tracking work back to the database)
		if (this.lastclaimed >= (System.currentTimeMillis() - 5000))
			return;
		
		// otherwise there has been activity recently enough to warrant and update
		this.updateClaim();
	}
	
	// return true if claimed or completed - false if canceled or timed out
	public boolean updateClaim() {
		if (this.task.isFromWorkQueue()) { 
			// an incomplete load from work queue - edge error condition
			if (!this.task.hasAuditId()) {
				this.errorTr(191, this.task.getId());
				return false;
			}
			
			// get the logs up to date as much as possible
			OperationResult res1 = Hub.instance.getWorkQueue().trackWork(this, false);		// TODO add param for update claim?  review this
	
			this.copyMessages(res1);
			
			if (res1.hasErrors()) {
				this.errorTr(191, this.task.getId());
				return false;
			}
			
			// try to extend our claim
			OperationResult res2 = Hub.instance.getWorkQueue().updateClaim(this.task);
	
			this.copyMessages(res2);
			
			if (res2.hasErrors()) {
				this.errorTr(191, this.task.getId());
				return false;
			}
		}
		
		this.lastclaimed = System.currentTimeMillis();		
		
		return true;
	}
	
	public void run() {
		try {
			OperationContext.set(this.opcontext);
			
			if (this.started == -1) {
				
				if (this.task.isFromWorkQueue())
					this.infoTr(153, this.task.getId());
				else
					this.traceTr(153, this.task.getId());
					
				this.traceTr(144, Hub.instance.getWorkPool().getBucketOrDefault(this));
				
				// if this is a queue task then mark it started
				if (this.task.isFromWorkQueue()) {
					FuncResult<String> k = Hub.instance.getWorkQueue().startWork(this.task.getWorkId());
					
					if (k.hasErrors()) {
						// TODO replace with hub events
						Hub.instance.getWorkQueue().sendAlert(179, this.task.getId(), k.getMessage());
						
						this.errorTr(179, this.task.getId(), k.getMessage());
						this.complete();
						return;
					}
					
					this.task.incCurrentTry();
					this.task.withAuditId(k.getResult());
				}
				
				RecordStruct params = this.task.getParams();
				
				if (params == null) {
					params = new RecordStruct();
					this.task.withParams(params);
				}
				
				// use temp folder unless skip flag 
				if (this.task.isUsesTempFolder()) {
					try {
						File tempFolder = FileUtil.allocateTempFolder();
						
						// needs to be canonical for log filtering
						params.setField("_TempFolder", tempFolder.getCanonicalPath());
					}
					catch (Exception x) {
						this.errorTr(215, this, x);
						this.complete();
						return;
					}
				}
				
				// the official "logger" (the first) is available via the _Logger special var
				for (IOperationObserver cb : this.observers) {
					if (cb instanceof IOperationLogger) {
						params.setField("_Logger", cb);
						break;
					}
				}
				
				this.started = this.lastclaimed = System.currentTimeMillis();
				
				// task start before work
				for (IOperationObserver cb : this.observers) {
					try {
						if (cb instanceof ITaskObserver)
							((ITaskObserver)cb).start(this);			
					} 
					catch (Exception x) {
						this.error("Error notifying start task: " + x);
					}
					
					// they might change context on us, return context
					OperationContext.set(this.opcontext);
				}
			}
			
			//  TODO review info feature DCTASKLOG in NCC
			// task might need some way to refer to info structures
			//params.setField("_Info", this.info.info);
			
			IWork work = this.task.getWork();
			
			if (work == null) {
				this.errorTr(217, this);
				this.complete();
				return;
			}
			
			work.run(this);
			
			if (work instanceof ISynchronousWork)
				this.complete();
		}
		catch (Exception x) {
			this.errorTr(155, this.task.getId(), x);
			
			IWork work = this.task.getWorkInstance();
			
			if (work != null)			
				System.out.println("Work pool caught exception: " + work.getClass());
			
			System.out.println("Stack Trace: ");
			x.printStackTrace();
			
			this.complete();
		}
		finally {
			OperationContext.clear();
		}
	}		

	// when task enters an async callback that is not through OperationCallback or subclasses (e.g. a Java executor)
	// then call this to get the task back in context
	//
	// caution - before making this do anything more than resume context, keep in mind that the dcBus
	//           restores a task only by setting context, if this needs to do more then so does the bus  (try not to) 
	public void thawContext() {
		OperationContext.set(this.opcontext);
	}
	
	public void resume() {
		Hub.instance.getWorkPool().submit(this);		
	}
	
	public void kill(String msg) {
		this.completionlock.lock();
		
		try {			
			this.error(msg);
			
			this.kill();
		}
		finally {
			this.completionlock.unlock();
		}
	}
	
	/**
	 * @param code code for message
	 * @param msg message
	 */
	public void kill(long code, String msg) {
		this.completionlock.lock();
		
		try {			
			this.error(code, msg);
			
			this.kill();
		}
		finally {
			this.completionlock.unlock();
		}
	}
	
	public void exitTr(long code, Object... params) {
		this.completionlock.lock();
		
		try {			
			this.infoTr(code, params);
		}
		finally {
			this.completionlock.unlock();
		}
	}
	
	public void kill() {
		this.completionlock.lock();
		
		try {			
			if (this.completed)
				return;
			
			OperationContext.set(this.opcontext);
			
			// collect inactive before error logging, logging updates the activity
			boolean inactive = this.isInactive();
			
			this.errorTr(196, this.task);
			
			if (this.isOverdue())
				this.errorTr(222, this.task);
			else if (inactive)	
				this.errorTr(223, this.task);
			
			this.killed = true;
			
			IWork work = this.task.getWorkInstance();
			
			if ((work != null) && (work instanceof ISmartWork))
				try {			
					((ISmartWork)work).cancel(this);
				}
				catch (Exception x) {
					this.error("Error canceling task: " + x);
				}
			
			this.complete();
		}
		finally {
			this.completionlock.unlock();
		}
	}
	
	public void complete() {
		// make sure we complete in the correct context (only worker should call this method)
		OperationContext.set(this.opcontext);
		
		this.completionlock.lock();
		
		try {			
			// don't complete twice (but in try so we unlock)
			if (this.completed)
				return;
			
			this.completed = true;
			
			IWork work = this.task.getWorkInstance();
			
			if ((work != null) && (work instanceof ISmartWork))
				try {			
					((ISmartWork)work).completed(this);
				}
				catch (Exception x) {
					this.error("Error completing task: " + x);
				}
			
			// task observers could log still - so before close log
			for (IOperationObserver cb : this.observers) {
				try {
					if (cb instanceof ITaskObserver)
						((ITaskObserver)cb).completed(this);			
				} 
				catch (Exception x) {
					this.error("Error notifying completing task: " + x);
				}
				
				// they might change context on us, return context
				OperationContext.set(this.opcontext);
			}
			
			// task observers stop can/should no longer log
			for (IOperationObserver cb : this.observers) {
				try {
					if (cb instanceof ITaskObserver)
						((ITaskObserver)cb).stop(this);			
				} 
				catch (Exception x) {
					this.error("Error notifying completing task: " + x);
				}
				
				// they might change context on us, return context
				OperationContext.set(this.opcontext);
			}
			
			// if this is a queue task then end it - only if we got an audit it though
			// TODO refine this - if we have a task id but not an audit id we should cleanup the queue...
			// TODO what should we do if not started - (this.started == -1)
			if (this.task.isFromWorkQueue() && this.task.hasAuditId()) {				
				// don't go forward if this no longer holds a claim
				if (!this.updateClaim()) 
					// record only that we ended but not a status or a queue change
					Hub.instance.getWorkQueue().trackWork(this, true);
				else if (this.hasErrors()) 
					// record failure if errors
					Hub.instance.getWorkQueue().failWork(this);
				else 
					// otherwise record completed
					Hub.instance.getWorkQueue().completeWork(this);
			}
			
			// don't remove temp folder till after record to queue in case the logger needs the folder to read log content from
			RecordStruct params = this.task.getParams();
			
			if (params != null) {
				String tempFolder = params.getFieldAsString("_TempFolder");
				
				if (StringUtil.isNotEmpty(tempFolder))
					FileUtil.deleteDirectory(Paths.get(tempFolder));
			}			
			
			for (AutoCloseable ac : this.closeables)
				try {
					ac.close();
				} 
				catch (Exception x) {
					// ???
				}
			
			this.closeables.clear();
			
			// TODO what should we do if not started - (this.started == -1)
			if (this.task.isFromWorkQueue())
				this.infoTr(154, this.getCode());
			else
				this.traceTr(154, this.getCode());
			
			// mark task completed
			Hub.instance.getWorkPool().complete(this);
		}
		finally {
			this.completionlock.unlock();
		}
	}

	@Override
	public String toString() {
		return this.task.getTitle() + " (" + this.task.getId() + ")";
	}
	
	@Override
	public void addObserver(IOperationObserver oo) {
		if (oo instanceof ITaskObserver) {
			// protect with lock so we can ensure completed fires either now (if task is done)
			// or later if it is not
			this.completionlock.lock();
	
			super.addObserver(oo);
			
			try {
				if (this.completed) 
					((ITaskObserver)oo).completed(this);
			}
			finally {
				this.completionlock.unlock();
			}
		}
		else
			super.addObserver(oo);		
	}
	
	public RecordStruct toStatusReport() {
		RecordStruct rec = new RecordStruct();
		
		rec.setField("Id", this.task.getId());
		rec.setField("Title", this.task.getTitle());
		
		rec.setField("Tags", this.task.getTags());
		
		rec.setField("Completed", this.completed);
		
		// TODO started, last touched/action, code, message, finished...
		
		return rec;
	}
	
	@Override
	public int hashCode() {
		return this.task.getTitle().hashCode();
	}
	
	/*
	 * For scripting calls - set the return value (convert to struct if not already) then call complete all at once
	 * @param v
	 */
	public void returnValue(Object v) {
		this.value = Struct.objectToStruct(v);
		this.complete();
	}
	
	public void returnEmpty() {
		this.complete();
	}	
	
	@Override
	public Object invokeMethod(String name, Object arg1) {
		// is really an object array
		Object[] args = (Object[])arg1;
		
		if ("return".equals(name)) {
			if (args.length > 0)
				this.returnValue(args[0]);
			else
				this.returnEmpty();
			
			return null;
		}
		
		return super.invokeMethod(name, arg1);
	}
	
	public String getLog() {
		// the first task observer that implements log builder gets to return the log 
		for (IOperationObserver cb : this.observers) {
			try {
				if (cb instanceof IOperationLogger) 
					return ((IOperationLogger)cb).logToString();	
			} 
			catch (Exception x) {
				this.error("Error getting log: " + x);
			}
			finally {
				// they might change context on us, return context
				OperationContext.set(this.opcontext);
			}
		}
				
		// TODO reformat these as log entries not as JSON
		return this.messages.toString();
	}

	public RecordStruct status() {
		RecordStruct status = this.task.status();
		
		// TODO some of this may need review
		status.setField("Status", this.completed ? "Completed" : "Running");			
		status.setField("Start", new DateTime(this.started)); 
		status.setField("End", null); 
		status.setField("Hub", OperationContext.getHubId());
		
		status.setField("Code", this.getCode());
		status.setField("Message", this.getMessage()); 
		status.setField("Log", this.getLog());
		status.setField("Progress", this.getProgressMessage()); 
		status.setField("StepName", this.getCurrentStepName()); 
		status.setField("Completed", this.getAmountCompleted()); 
		status.setField("Step", this.getCurrentStep()); 
		status.setField("Steps", this.getSteps());
		
		return status;
	}

	public void addCloseable(AutoCloseable v) {
		this.closeables.add(v);
	}
}
