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

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.log.Logger;
import divconq.session.Session;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;

// conforms to dcTaskInfo data type
public class Task {
	// for immediate use tasks only
	protected IWork work = null;
	protected List<ITaskObserver> observers = null;
	
	// used with immediate or queueable
	protected OperationContext context = null;	
	protected RecordStruct info = null;

	public Task() {
		this.info = new RecordStruct();
		this.context = OperationContext.get();
	}
	
	public Task(RecordStruct info) {
		this.info = info;
		
		if (!info.isFieldEmpty("Context"))
			this.context = OperationContext.allocate(info.getFieldAsRecord("Context"));
		else
			this.context = OperationContext.get();
	}

	public Task copy() {
		RecordStruct clone = (RecordStruct) this.info.deepCopyExclude("Context");
		
		Task t = new Task(clone);
		
		t.context = this.context;		// ok not to copy, it is immutable
		
		return t;
	}
	
	public RecordStruct freezeToRecord() {
		RecordStruct clone = (RecordStruct) this.info.deepCopy();
		
		clone.setField("Context", this.context.freezeToRecord());
		
		return clone;
	}
	
	public Task withWork(IWork work) {
		this.work = work;
		
		if (this.work != null)
			this.info.setField("WorkClassname", this.work.getClass().getCanonicalName());
		
		return this;
	}
	
	public Task withWork(Runnable work) {
		return this.withWork(new WorkAdapter(work));
	}
	
	public Task withWork(Class<? extends IWork> classref) {
		this.info.setField("WorkClassname", classref.getCanonicalName());
		return this;
	}
	
	// class name
	public Task withWork(String classname) {
		this.info.setField("WorkClassname", classname);
		return this;
	}
	
	public IWork getWork() {
		if (this.work == null) 
			this.work = (IWork) Hub.instance.getInstance(this.getWorkClassname());
		
		return this.work;
	}
	
	// won't create if not present
	public IWork getWorkInstance() {
		return this.work;
	}
	
	public String getWorkClassname() {
		return this.info.getFieldAsString("WorkClassname");
	}
	
	public Task withBucket(String v) {
		this.info.setField("Bucket", v);
		return this;
	}

	public String getBucket() {
		String name = this.info.getFieldAsString("Bucket");
		
		if (StringUtil.isEmpty(name))
			name = "Default";
		
		return name;
	}
	
	public Task withContext(OperationContext v) {
		this.context = v;
		return this;
	}
	
	public Task withCurrentContext() {
		this.context = OperationContext.get();
		return this;
	}
	
	public Task withRootContext() {
		this.context = OperationContext.allocateRoot();
		return this;
	}
	
	public Task withGuestContext() {
		this.context = OperationContext.allocateGuest();
		return this;
	}

	public OperationContext getContext() {
		return this.context;
	}
	
	public Task withObserver(ITaskObserver watcher) {
		if (this.observers == null)
			this.observers = new ArrayList<>();

		this.observers.add(watcher);
		
		if (watcher instanceof RecordStruct) {
			RecordStruct w = (RecordStruct)watcher;
			w.setField("_Classname", watcher.getClass().getCanonicalName());
			
			return this.withObserverRec(w);
		}
		
		return this.withObserver(watcher.getClass().getCanonicalName());
	}
	
	public Task withObserver(Class<? extends ITaskObserver> classref) {
		return this.withObserver(classref.getCanonicalName());
	}
	
	public Task withObserver(String classname) {
		return this.withObserverRec(new RecordStruct(
				new FieldStruct("_Classname", classname)
		));
	}
	
	public Task withObserverRec(RecordStruct observer) {
		ListStruct buildobservers = this.info.getFieldAsList("Observers"); 
		
		if (buildobservers == null) {
			buildobservers = new ListStruct();
			this.info.setField("Observers", buildobservers);
		}
		
		buildobservers.addItem(observer);
		
		return this;
	}
	
	public Task withDefaultLogger() {
		return this.withObserver("divconq.work.TaskLogger");
	}
	
	public List<ITaskObserver> getObservers() {
		if (this.observers != null)
			return this.observers;
		
		this.observers = new ArrayList<>();
		
		ListStruct buildobservers = this.info.getFieldAsList("Observers"); 
		
		if (buildobservers != null) {
			for (Struct s : buildobservers.getItems()) {
				RecordStruct orec = (RecordStruct) s;
				
				if (orec.isFieldEmpty("_Classname")) {
					Logger.warn("Missing observer classname (" + this.getId() + "): " + orec);
					continue;
				}
				
				ITaskObserver observer = (ITaskObserver) Hub.instance.getInstance(orec.getFieldAsString("_Classname").toString());
				
				if (observer instanceof RecordStruct)
					((RecordStruct)observer).copyFields(orec);
				
				this.observers.add(observer);
			}
		}
		
		return this.observers;
	}
	
	/*
	public String getObserverClassname() {
		return this.info.getFieldAsString("ObserverClassname");
	}
	
	public boolean hasObserver() {
		if (this.observer != null)
			return true;
		
		return !this.info.isFieldEmpty("ObserverClassname");
	}
	*/
	
	public Task withUsesTempFolder(boolean v) {
		this.info.setField("UsesTempFolder", v);
		return this;
	}
	
	public boolean isUsesTempFolder() {
		return this.info.getFieldAsBooleanOrFalse("UsesTempFolder");
	}
	
	public Task withId(String v) {
		this.info.setField("Id", v);
		return this;
	}
	
	public String getId() {
		return this.info.getFieldAsString("Id");
	}
	
	public Task withTitle(String v) {
		this.info.setField("Title", v);
		return this;
	}
	
	public String getTitle() {
		return this.info.getFieldAsString("Title");
	}
	
	public Task withStatus(String v) {
		this.info.setField("Status", v);
		return this;
	}
	
	public String getStatus() {
		return this.info.getFieldAsString("Status");
	}
	
	public Task withSquad(String v) {
		this.info.setField("Squad", v);
		return this;
	}
	
	public String getSquad() {
		return this.info.getFieldAsString("Squad");
	}
	
	public Task withHub(String v) {
		this.info.setField("HubId", v);
		return this;
	}
	
	public String getHub() {
		return this.info.getFieldAsString("HubId");
	}
	
	public Task withWorkId(String v) {
		this.info.setField("WorkId", v);
		return this;
	}
	
	public String getWorkId() {
		return this.info.getFieldAsString("WorkId");
	}
	
	public Task withAuditId(String v) {
		this.info.setField("AuditId", v);
		return this;
	}
	
	public String getAuditId() {
		return this.info.getFieldAsString("AuditId");
	}
	
	public boolean hasAuditId() {
		return !this.info.isFieldEmpty("AuditId");
	}
	
	public Task withCurrentTry(int v) {
		this.info.setField("CurrentTry", v);
		return this;
	}
	
	public int getCurrentTry() {
		return (int)this.info.getFieldAsInteger("CurrentTry", 0);
	}
	
	public void incCurrentTry() {
		int v = (int)this.info.getFieldAsInteger("CurrentTry", 0) + 1;
		this.info.setField("CurrentTry", v);
	}	
	
	public Task withMaxTries(int v) {
		this.info.setField("MaxTries", v);
		return this;
	}
	
	public int getMaxTries() {
		return (int)this.info.getFieldAsInteger("MaxTries", 1);
	}
	
	public Task withClaimedStamp(String v) {
		this.info.setField("ClaimedStamp", v);
		return this;
	}
	
	public String getClaimedStamp() {
		return this.info.getFieldAsString("ClaimedStamp");
	}
	
	public Task withAddStamp(DateTime v) {
		this.info.setField("AddStamp", v);
		return this;
	}
	
	public DateTime getAddStamp() {
		return this.info.getFieldAsDateTime("AddStamp");
	}
	
	// do not retry this task on the queue
	public boolean getFinalTry() {
		return (this.info.getFieldAsBooleanOrFalse("FinalTry") || (this.getCurrentTry() >= this.getMaxTries()));
	}
	
	// finish the current run, but go no further, don't try again even if Tries left  
	public Task withFinalTry(boolean v) {
		this.info.setField("FinalTry", v);
		return this;
	}
	
	
	public Task withSetTags(String... v) {
		this.info.setField("Tags", new ListStruct((Object[])v));
		return this;
	}
	
	public Task withSetTags(ListStruct v) {
		this.info.setField("Tags", v);
		return this;
	}
	
	public Task withAddTags(String... v) {
		if (this.info.isFieldEmpty("Tags"))
			this.info.setField("Tags", new ListStruct((Object[])v));
		else
			this.info.getFieldAsList("Tags").addItem((Object[])v);
		
		return this;
	}
	
	public Task withAddTags(ListStruct v) {
		if (this.info.isFieldEmpty("Tags"))
			this.info.setField("Tags", v);
		else
			this.info.getFieldAsList("Tags").addItem(v);
		
		return this;
	}
	
	public ListStruct getTags() {
		return this.info.getFieldAsList("Tags");
	}
	
	/**
	 * @param tags to search for with this task
	 * @return true if this task has one of the requested tags  
	 */
	public boolean isTagged(String... tags) {
		if (this.info.isFieldEmpty("Tags"))
			return false;
		
		for (Struct shas : this.info.getFieldAsList("Tags").getItems()) {
			String has = shas.toString();
			
			for (String wants : tags) {
				if (has.equals(wants))
					return true;
			}
		}
		
		return false;
	}
	
	public Task withParams(RecordStruct v) {
		this.info.setField("Params", v);
		return this;
	}
	
	public RecordStruct getParams() {
		return this.info.getFieldAsRecord("Params");
	}
	
	public Message getParamsAsMessage() {
		return (Message) this.info.getFieldAsRecord("Params");
	}
	
	public Task withExtra(RecordStruct v) {
		this.info.setField("Extra", v);
		return this;
	}
	
	public RecordStruct getExtra() {
		return this.info.getFieldAsRecord("Extra");
	}
	
	/**
	 * Timeout is when nothing happens for v minutes...see Overdue also
	 * 
	 * @param v
	 * @return
	 */
	public Task withTimeout(int v) {
		this.info.setField("Timeout", v);
		
		//if (v > this.getDeadline())
		//	this.withDeadline(v + 1);
		
		return this;
	}
	
	// in minutes
	public int getTimeout() {
		return (int) this.info.getFieldAsInteger("Timeout", 1);  
	}
	
	public int getTimeoutMS() {
		return (int) this.info.getFieldAsInteger("Timeout", 1)  * 60 * 1000; // convert to ms	
	}
	
	/**
	 * Deadline is v minutes until the task must complete, see Timeout also
	 * 
	 * @param v
	 * @return
	 */
	public Task withDeadline(int v) {
		this.info.setField("Deadline", v);
		return this;
	}
	
	// stalled even if still active, not getting anything done
	// in minutes
	public int getDeadline() {
		return (int) this.info.getFieldAsInteger("Deadline", 0); 
	}
	
	public int getDeadlineMS() {
		return (int) this.info.getFieldAsInteger("Deadline", 0)  * 60 * 1000; // convert to ms	
	}
	
	public OperationResult validate() {
		return this.info.validate("dcTaskInfo");
	}

	// happens after submit to pool or to queue
	public void prep() {
		if (this.info.isFieldEmpty("Title"))
			this.info.setField("Title", "[unnamed]");
		
		if (this.info.isFieldEmpty("Id"))
			this.info.setField("Id", Session.nextTaskId());
	}

	/* doesn't work with lambda's
	// this builder is going to be used with another task (repeat task) so cleanup
	public void reset() {
		// if we have the class name then start with a fresh instance each run
		if (!this.info.isFieldEmpty("WorkClassname"))
			this.work = null;
		
		// if we have the class name then start with a fresh instance each run
		if (!this.info.isFieldEmpty("ObserverClassname"))
			this.observers = null;
		
		this.info.removeField("FinalTry");
	}
	*/

	public boolean isFromWorkQueue() {
		return (StringUtil.isNotEmpty(this.getWorkId()));
	}

	@Override
	public String toString() {
		return this.getTitle() + " (" + this.getId() + ")";
	}

	public RecordStruct status() {
		return new RecordStruct( 
				this.info.getFieldStruct("WorkId"),
				new FieldStruct("TaskId", this.info.getField("Id")),
				this.info.getFieldStruct("Title"),
				new FieldStruct("MaxTry", this.getMaxTries()),
				new FieldStruct("Added", this.getAddStamp()),
				new FieldStruct("Try", this.getCurrentTry())
		);
	}
}
