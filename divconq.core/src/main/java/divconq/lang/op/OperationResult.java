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
package divconq.lang.op;

import divconq.bus.Message;
import divconq.log.DebugLevel;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;

/**
 * Provides info about the success of a method call - like a boolean return - only with more info.
 * Check "hasErrors" to see if call failed.  Also there is an error code, and a log of messages.
 * That log of messages will also get written to the debug logger using the TaskContext of the caller.
 * It is also possible to track progress using a step count and/or a percent count.
 * 
 * @author Andy
 *
 */
public class OperationResult {  //implements GroovyObject {  TODO provide middle party to manage groovy access
	protected OperationContext opcontext = null;
    
	protected int msgStart = 0;		// start of messages
	protected int msgEnd = -1;		// all messages
    
	public int getMsgStart() {
		return this.msgStart;
	}
	
	public int getMsgEnd() {
		return this.msgEnd;
	}
	
    public OperationResult() {
    	this(OperationContext.get());
    }
    
    public OperationResult(OperationContext ctx) {
    	this.opcontext = (ctx != null) ? ctx : OperationContext.allocateGuest();
    	
    	this.msgStart = this.opcontext.logMarker();
    }
    
    public void markStart() {
    	this.msgStart = this.opcontext.logMarker();
    }
    
    public void markEnd() {
    	this.msgEnd = this.opcontext.logMarker();	// end is exclusive, so size is right
    }
    
    public OperationContext getContext() {
		return this.opcontext;
	}
    
	/**
	 * @return error or exit code if any, 0 otherwise
	 */
	public long getCode() {
		RecordStruct entry = this.opcontext.findExitEntry(this.msgStart, this.msgEnd);
		
		if (entry != null)
			return entry.getFieldAsInteger("Code", 0);
		
		return 0;
	}
	
	/**
	 * @return error or exit message, if any
	 */
	public String getMessage() {
		RecordStruct entry = this.opcontext.findExitEntry(this.msgStart, this.msgEnd);
		
		if (entry != null)
			return entry.getFieldAsString("Message");
		
		return null;
	}
	
	/**
	 * @return all messages logged with this call (and possibly sub calls made within the call)
	 */
	public ListStruct getMessages() {
		return this.opcontext.getMessages(this.msgStart, this.msgEnd);
	}
    
    public void touch() {
    	this.opcontext.touch();
    }
    
    public long getLastActivity() {
		return this.opcontext.getLastActivity();
	}
	
	public void trace(String msg, String... tags) {
		this.opcontext.trace(msg, tags);
	}
	
	public void trace(long code, String msg, String... tags) {
		this.opcontext.trace(code, msg, tags);
	}
	
	public void debug(String msg, String... tags) {
		this.opcontext.debug(msg, tags);
	}
	
	public void debug(long code, String msg, String... tags) {
		this.opcontext.debug(code, msg, tags);
	}
	
	public void info(String msg, String... tags) {		
		this.opcontext.info(msg, tags);
	}
	
	public void info(long code, String msg, String... tags) {
		this.opcontext.info(code, msg, tags);
	}
	
	public void warn(String msg, String... tags) {
		this.opcontext.warn(msg, tags);
	}
	
	public void warn(long code, String msg, String... tags) {
		this.opcontext.warn(code, msg, tags);
	}
	
	public void error(String msg, String... tags) {
		this.opcontext.error(msg, tags);
	}
	
	public void error(long code, String msg, String... tags) {
		this.opcontext.error(code, msg, tags);
	}
	
	/**
	 * Overrides any previous return codes and messages
	 * 
	 * @param code code for message
	 * @param msg message
	 */
	public void exit(long code, String msg) {
		this.opcontext.exit(code, msg);
	}
	
	public void clearExitCode() {
		this.opcontext.clearExitCode();
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void traceTr(long code, Object... params) {
		this.opcontext.traceTr(code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void debugTr(long code, Object... params) {
		this.opcontext.debugTr(code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void infoTr(long code, Object... params) {
		this.opcontext.infoTr(code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void warnTr(long code, Object... params) {
		this.opcontext.warnTr(code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void errorTr(long code, Object... params) {
		this.opcontext.errorTr(code, params);
	}
	
	/**
	 * Overrides any previous return codes and messages
	 * 
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void exitTr(long code, Object... params) {
		this.opcontext.exitTr(code, params);
	}
    
    /**
     * Add a logging boundary, delineating a new section of work for this task
     * 
     * @param tags identity of this boundary
     */
    public void boundary(String... tags) {
    	this.opcontext.boundary(tags);
    }    

	/**
	 * @return create a message for the bus that holds this result, useful for service results
	 */
	public Message toLogMessage() {
		Message m = new Message();
		
		m.setField("Messages", this.opcontext.getMessages(this.msgStart, this.msgEnd));		
		
		return m;
	}
	
	@Override
	public String toString() {
		return this.getMessages().toString(); 
	}

	public boolean isLevel(DebugLevel debug) {
		return this.opcontext.isLevel(debug);
	}

	/**
	 * @return true if a relevant error code is present
	 */
	public boolean hasErrors() {
		return (this.getCode() != 0);
	}

	/**
	 * @param code to search for
	 * @return true if an error code is present
	 */
	public boolean hasCode(long code) {
		return this.opcontext.hasCode(code, this.msgStart, this.msgEnd);
	}
	
	/*
	@Override
    public Object getProperty(String name) { 
		/*
		Struct v = this.getField(name);
		
		if (v == null)
			return null;
		
		if (v instanceof CompositeStruct)
			return v;
		
		return ((ScalarStruct) v).getGenericValue();
		* /
		
		return null;
    }
    
	@Override
    public void setProperty(String name, Object value) { 
		//this.setField(name, value);
    }

	// generate only on request
	protected transient MetaClass metaClass = null;

	@Override
	public void setMetaClass(MetaClass v) {
		this.metaClass = v;
	}
	
	@Override
	public MetaClass getMetaClass() {
        if (this.metaClass == null) 
        	this.metaClass = InvokerHelper.getMetaClass(getClass());
        
        return this.metaClass;
	}

	@Override
	public Object invokeMethod(String name, Object arg1) {
		// is really an object array
		Object[] args = (Object[])arg1;
		
		if (args.length > 0)
			System.out.println("G1: " + name + " - " + args[0]);
		else
			System.out.println("G1: " + name);
		
		return null;
	}
	*/
}
