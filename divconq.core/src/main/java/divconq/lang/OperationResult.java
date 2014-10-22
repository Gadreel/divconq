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
package divconq.lang;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import divconq.bus.Message;
import divconq.locale.LocaleUtil;
import divconq.log.DebugLevel;
import divconq.log.Logger;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;

/**
 * Provides info about the success of a method call - like a boolean return - only with more info.
 * Check "hasErrors" to see if call failed.  Also there is an error code, and a log of messages.
 * That log of messages will also get written to the debug logger using the TaskContext of the caller.
 * It is also possible to track progress using a step count and/or a percent count.
 * 
 * @author Andy
 *
 */
public class OperationResult implements GroovyObject {
	// primary error code and message (info and warning not in here)
	protected long code = 0;		// error code, non-zero means error, only first error code is tracked 
	protected String message = null;		// error code, non-zero means error, first code is tracked 
	protected List<RecordStruct> messages = new ArrayList<RecordStruct>();
	
    // progress tracking
    protected int progTotalSteps = 0;
    protected int progCurrStep = 0;
    protected String progStepName = null;
    protected int progComplete = 0;	
    protected String progMessage = null;
    
    protected OperationContext opcontext = null;
    protected DebugLevel loglevel = null;
    
	protected List<IOperationObserver> observers = new CopyOnWriteArrayList<>();
    
    // this tracks time stamp of signs of life from the job writing to the log/progress tracks
    // volatile helps keep threads on same page - issue found in code testing and this MAY have helped 
    volatile protected long lastactivity = System.currentTimeMillis();
    
    /**
     * @param loglevel filter messages higher than this level, those that pass get sent to the Logger
     */
    public OperationResult(DebugLevel loglevel) {
    	this.loglevel = loglevel;
    	
    	if (OperationContext.hasContext())
    		this.opcontext = OperationContext.get();    	
    }
    
    public OperationResult(OperationContext ctx) {
    	this.opcontext = ctx;    	
		this.loglevel = this.opcontext.getLevel();
    }
    
    public OperationResult() {
    	if (OperationContext.hasContext()) {
    		this.opcontext = OperationContext.get();
    		this.loglevel = this.opcontext.getLevel();
    	}
    }
    
    public OperationContext getContext() {
		return this.opcontext;
	}
    
    public void touch() {
    	this.lastactivity = System.currentTimeMillis();
    }
    
    public long getLastActivity() {
		return this.lastactivity;
	}
    
	/**
	 * @return error or exit code if any, 0 otherwise
	 */
	public long getCode() {
		return this.code;
	}
	
	/**
	 * @return error or exit message, if any
	 */
	public String getMessage() {
		return this.message;
	}
	
	/**
	 * @return all messages logged with this call (and possibly sub calls made within the call)
	 */
	public ListStruct getMessages() {
		return new ListStruct(this.messages.toArray());
	}

	public void setContext(OperationContext v) {
		this.opcontext = v;
	}
	
	public void setLogLevel(DebugLevel v) {
		this.loglevel = v;
	}
	
	public DebugLevel getLogLevel() {
		if (this.loglevel != null)
			return this.loglevel;

		if (this.opcontext != null)
    		return this.opcontext.getLevel();
		
		return DebugLevel.None;
	}
	
	public void trace(String msg) {
		this.log(DebugLevel.Trace, 0, msg);
	}
	
	/**
	 * @param code code for message
	 * @param msg message
	 */
	public void trace(long code, String msg) {
		this.log(DebugLevel.Trace, code, msg);
	}
	
	public void debug(String msg) {
		this.log(DebugLevel.Debug, 0, msg);
	}
	
	/**
	 * @param code code for message
	 * @param msg message
	 */
	public void debug(long code, String msg) {
		this.log(DebugLevel.Debug, code, msg);
	}
	
	public void info(String msg) {		
		this.log(DebugLevel.Info, 0, msg);
	}
	
	/**
	 * @param code code for message
	 * @param msg message
	 */
	public void info(long code, String msg) {		
		this.log(DebugLevel.Info, code, msg);
	}
	
	public void warn(String msg) {
		this.log(DebugLevel.Warn, 2, msg);
	}
	
	/**
	 * @param code code for message
	 * @param msg message
	 */
	public void warn(long code, String msg) {
		this.log(DebugLevel.Warn, code, msg);
	}
	
	public void error(String msg) {
		this.log(DebugLevel.Error, 1, msg);
	}
	
	/**
	 * @param code code for message
	 * @param msg message
	 */
	public void error(long code, String msg) {
		this.log(DebugLevel.Error, code, msg);
	}
	
	/**
	 * Overrides any previous return codes and messages
	 * 
	 * @param code code for message
	 * @param msg message
	 */
	public void exit(long code, String msg) {
		this.code = code;
		this.message = msg;
		
		this.log(DebugLevel.Info, code, msg);
	}
	
	public void clearExitCode() {
		this.code = 0;
		this.message = null;
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void traceTr(long code, Object... params) {
		this.logTr(DebugLevel.Trace, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void debugTr(long code, Object... params) {
		this.logTr(DebugLevel.Debug, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void infoTr(long code, Object... params) {		
		this.logTr(DebugLevel.Info, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void warnTr(long code, Object... params) {
		this.logTr(DebugLevel.Warn, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void errorTr(long code, Object... params) {
		this.logTr(DebugLevel.Error, code, params);
	}
	
	/**
	 * Overrides any previous return codes and messages
	 * 
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void exitTr(long code, Object... params) {
		String msg = (this.opcontext != null) 
				? this.opcontext.tr("_code_" + code, params)
				: LocaleUtil.tr(LocaleUtil.getDefaultLocale(), "_code_" + code, params);;
		
		this.code = code;
		this.message = msg;
		
		this.logTr(DebugLevel.Info, code, params);
	}
	
	/**
	 * @param lvl level of message
	 * @param code for message
	 * @param msg text of message
	 */
	public void log(DebugLevel lvl, long code, String msg) {
		// must be some sort of message
		if (StringUtil.isEmpty(msg))
			return;
		
		// pass the message to logger 
		if (this.getLogLevel().getCode() >= lvl.getCode()) {
			RecordStruct entry = new RecordStruct(
					new FieldStruct("Occurred", new DateTime(DateTimeZone.UTC)),
					new FieldStruct("Level", lvl.toString()),
					new FieldStruct("Code", code),
					new FieldStruct("Message", msg)
			);
			
			if (code > 0)
				Logger.logWr((this.opcontext != null) ? this.opcontext.getOpId() : null, lvl, msg, "Code", code + "");
			else  
				Logger.logWr((this.opcontext != null) ? this.opcontext.getOpId() : null, lvl, msg);
			
			this.log(entry);
		}
	}
	
	// logging is hard on heap and GC - so only do it if necessary
	// not generally called by code, internal use mostly
	public void log(RecordStruct entry) {
		this.messages.add(entry);
		
		if ("Error".equals(entry.getFieldAsString("Level"))) {
			if (this.code == 0) {
				this.code = entry.getFieldAsInteger("Code");
				this.message = entry.getFieldAsString("Message");
			}
		}
		
		this.touch();
		
		for (IOperationObserver ob : this.observers)
			ob.log(this, entry);
	}
	
	/**
	 * @param lvl level of message
	 * @param code for message
	 * @param params parameters to the message string
	 */
	public void logTr(DebugLevel lvl, long code, Object... params) {
		// pass the code to logger 
		if (this.getLogLevel().getCode() >= lvl.getCode()) {
			String msg = (this.opcontext != null) 
					? this.opcontext.tr("_code_" + code, params)
					: LocaleUtil.tr(LocaleUtil.getDefaultLocale(), "_code_" + code, params);;
			
			RecordStruct entry = new RecordStruct(
					new FieldStruct("Occurred", new DateTime(DateTimeZone.UTC)),
					new FieldStruct("Level", lvl.toString()),
					new FieldStruct("Code", code),
					new FieldStruct("Message", msg)
			);
		
			Logger.logWr((this.opcontext != null) ? this.opcontext.getOpId() : null, lvl, code, params);
		
			this.log(entry);
		}
	}
    
    /**
     * Add a logging boundary, delineating a new section of work for this task
     * 
     * @param tags identity of this boundary
     */
    public void boundary(String... tags) {
		// pass the code to logger 
		if (this.getLogLevel().getCode() >= DebugLevel.Info.getCode())
			Logger.boundaryWr((this.opcontext != null) ? this.opcontext.getOpId() : null, tags);
		
		this.touch();
		
		for (IOperationObserver ob : this.observers)
			ob.boundary(this, tags);
    }

	/**
	 * @param res take the result and combine its messages with this result
	 */
	public void copyMessages(OperationResult res) {
		// if not in list, still copy top error message
		// copy code must come first so that log observers can clear the code if need be
		if (this.code == 0) {
			this.code = res.code;
			this.message = res.message;
		}
		
		for (RecordStruct msg :  res.messages) 
			this.log(msg);		
	}

	public void copyMessages(RecordStruct rmsg) {
		if (rmsg == null)
			return;
		
		ListStruct messages = rmsg.getFieldAsList("Messages");
		
		if (messages != null)
			for (Struct itm :  messages.getItems()) 
				this.log((RecordStruct)itm);
		
		// if not in list, still copy top error message
		if (this.code == 0) {
			this.code = rmsg.getFieldAsInteger("Result", 0);
			this.message = rmsg.getFieldAsString("Message");
		}
	}

	/**
	 * @return create a message for the bus that holds this result, useful for service results
	 */
	public Message toLogMessage() {
		Message m = new Message();
		
		m.setField("Result", this.code);
		
		if (StringUtil.isNotEmpty(this.message)) 
			m.setField("Message", this.message);
		
		m.setField("Messages", this.getMessages());		
		
		return m;
	}
	
	public RecordStruct toRecord() {
		RecordStruct m = new RecordStruct();
		
		m.setField("Result", this.code);
		
		if (StringUtil.isNotEmpty(this.message)) 
			m.setField("Message", this.message);
		
		m.setField("Messages", this.getMessages());		
		
		return m;
	}
	
	@Override
	public String toString() {
		return this.messages.toString(); 
	}

	/**
	 * @return true if any error code is present
	 */
	public boolean hasErrors() {
		return (this.code != 0);
	}

	/**
	 * @param code to search for
	 * @return true if an error code is present
	 */
	public boolean hasCode(long code) {
		if (this.code == code)
			return true;
		
		for (RecordStruct msg :  this.messages) 
			if (msg.getFieldAsInteger("Code") == code)
				return true;
		
		return false;
	}

	// progress methods
	
	/**
	 * @return units/percentage of task completed
	 */
	public int getAmountCompleted() {
		return this.progComplete; 
	}
	
	/**
	 * @param v units/percentage of task completed
	 */
	public void setAmountCompleted(int v) { 
		this.progComplete = v; 
		this.touch();
		
		for (IOperationObserver ob : this.observers)
			ob.amount(this, this.progComplete);
	}

	/**
	 * @return status message about task progress
	 */
	public String getProgressMessage() {
		return this.progMessage; 
	}
	
	/**
	 * @param v status message about task progress
	 */
	public void setProgressMessage(String v) { 
		this.progMessage = v; 
		this.touch();
		
		for (IOperationObserver ob : this.observers)
			ob.progress(this, this.progMessage);
	}
	
	/**
	 * @param code message translation code
	 * @param params for the message string
	 */
	public void setProgressMessageTr(int code, Object... params) { 
		String v = (this.opcontext != null) 
				? this.opcontext.tr("_code_" + code, params)
				: LocaleUtil.tr(LocaleUtil.getDefaultLocale(), "_code_" + code, params);;
				
		this.progMessage = v; 
		this.touch();
		
		for (IOperationObserver ob : this.observers)
			ob.progress(this, this.progMessage);
	}

	/**
	 * @return total steps for this specific task
	 */
	public long getSteps() { 
		return this.progTotalSteps; 
	}
	
	/**
	 * @param v total steps for this specific task
	 */
	public void setSteps(int v) { 
		this.progTotalSteps = v; 
	}

	/**
	 * @return current step within this specific task
	 */
	public long getCurrentStep() { 
		return this.progCurrStep; 
	}
	
	/**
	 * Set step name first, this triggers observers
	 * 
	 * @param step current step number within this specific task
	 * @param name current step name within this specific task
	 */
	public void setCurrentStep(int step, String name) { 
		this.progCurrStep = step; 
		this.progStepName = name; 
		this.touch();
		
		for (IOperationObserver ob : this.observers)
			ob.step(this, this.progCurrStep, this.progTotalSteps, this.progStepName);
	}
	
	/**
	 * Set step name first, this triggers observers
	 * 
	 * @param name current step name within this specific task
	 */
	public void nextStep(String name) { 
		this.progCurrStep++; 
		this.progStepName = name; 
		this.touch();
		
		for (IOperationObserver ob : this.observers)
			ob.step(this, this.progCurrStep, this.progTotalSteps, this.progStepName);
	}

	/**
	 * @return name of current step
	 */
	public String getCurrentStepName() { 
		return this.progStepName; 
	}
	
	/**
	 * @param step number of current step
	 * @param code message translation code
	 * @param params for the message string
	 */
	public void setCurrentStepNameTr(int step, int code, Object... params) {
		String name = (this.opcontext != null) 
				? this.opcontext.tr("_code_" + code, params)
				: LocaleUtil.tr(LocaleUtil.getDefaultLocale(), "_code_" + code, params);;
				
		this.progCurrStep = step; 
		this.progStepName = name; 
		this.touch();
		
		for (IOperationObserver ob : this.observers)
			ob.step(this, this.progCurrStep, this.progTotalSteps, this.progStepName);
	}    
	
	/**
	 * @param code message translation code
	 * @param params for the message string
	 */
	public void nextStepTr(int code, Object... params) {
		String name = (this.opcontext != null) 
				? this.opcontext.tr("_code_" + code, params)
				: LocaleUtil.tr(LocaleUtil.getDefaultLocale(), "_code_" + code, params);;
				
		this.progCurrStep++; 
		this.progStepName = name; 
		this.touch();
		
		for (IOperationObserver ob : this.observers)
			ob.step(this, this.progCurrStep, this.progTotalSteps, this.progStepName);
	}    
	
	public void addObserver(IOperationObserver oo) {
		// the idea is that we want to unwind the callbacks in LILO order
		if (!this.observers.contains(oo))
			this.observers.add(0, oo);
	}
	
	public int countObservers() {
		return this.observers.size();
	}
	
	public void deleteObserver(IOperationObserver o) {
		this.observers.remove(o);
	}
	
	@Override
    public Object getProperty(String name) { 
		/*
		Struct v = this.getField(name);
		
		if (v == null)
			return null;
		
		if (v instanceof CompositeStruct)
			return v;
		
		return ((ScalarStruct) v).getGenericValue();
		*/
		
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
}
