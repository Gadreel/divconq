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

import divconq.bus.Message;
import divconq.log.DebugLevel;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.work.TaskRun;

/**
 * Provides the same function support as @see OperationResult, however used with
 * callbacks when the method called is asynchronous.
 * 
 * @author Andy
 *
 */
abstract public class WrappedOperationCallback extends OperationCallback {
	protected int state = 0;  // 1 = delivered, -1 = canceled
	protected OperationResult or = null;
	
	public WrappedOperationCallback(OperationResult or, DebugLevel loglevel) {
		super(loglevel);
		this.or = or;
	}
	
	public WrappedOperationCallback(OperationResult or) {
		super();
		this.or = or;
	}
	
	public WrappedOperationCallback(TaskRun run) {
		super(run);
		this.or = run;
	}
	
	@Override
	public void boundary(String... tags) {
		this.or.boundary(tags);
	}
	
	@Override
	public void copyMessages(OperationResult res) {
		this.or.copyMessages(res);
	}
	
	@Override
	public void copyMessages(RecordStruct rmsg) {
		this.or.copyMessages(rmsg);
	}
	
	@Override
	public void error(long code, String msg) {
		this.or.error(code, msg);
	}
	
	@Override
	public void errorTr(long code, Object... params) {
		this.or.errorTr(code, params);
	}
	
	@Override
	public void exit(long code, String msg) {
		this.or.exit(code, msg);
	}
	
	@Override
	public void exitTr(long code, Object... params) {
		this.or.exitTr(code, params);
	}
	
	@Override
	public long getAmountCompleted() {
		return this.or.getAmountCompleted();
	}
	
	@Override
	public long getCode() {
		return this.or.getCode();
	}
	
	@Override
	public long getCurrentStep() {
		return this.or.getCurrentStep();
	}
	
	@Override
	public String getCurrentStepName() {
		return this.or.getCurrentStepName();
	}
	
	@Override
	public long getLastActivity() {
		return this.or.getLastActivity();
	}
	
	@Override
	public void touch() {
		this.or.touch();
	}
	
	@Override
	public void warn(String msg) {
		this.or.warn(msg);
	}
	
	@Override
	public void trace(String msg) {
		this.or.trace(msg);
	}
	
	@Override
	public void debug(long code, String msg) {
		this.or.debug(code, msg);
	}
	
	@Override
	public void debug(String msg) {
		this.or.debug(msg);
	}
	
	@Override
	public void info(String msg) {
		this.or.info(msg);
	}
	
	@Override
	public void error(String msg) {
		this.or.error(msg);
	}
	
	@Override
	public void debugTr(long code, Object... params) {
		this.or.debugTr(code, params);
	}
	
	@Override
	public boolean hasCode(long code) {
		return this.or.hasCode(code);
	}
	
	@Override
	public OperationContext getContext() {
		return this.or.getContext();
	}
	
	@Override
	public DebugLevel getLogLevel() {
		return this.or.getLogLevel();
	}
	
	@Override
	public String getMessage() {
		return this.or.getMessage();
	}
	
	@Override
	public ListStruct getMessages() {
		return this.or.getMessages();
	}
	
	@Override
	public void addObserver(IOperationObserver arg0) {
		this.or.addObserver(arg0);
	}
	
	@Override
	public int countObservers() {
		return this.or.countObservers();
	}
	
	@Override
	public void deleteObserver(IOperationObserver o) {
		this.or.deleteObserver(o);
	}
	
	@Override
	public String getProgressMessage() {
		return this.or.getProgressMessage();
	}
	
	@Override
	public long getSteps() {
		return this.or.getSteps();
	}
	
	@Override
	public boolean hasErrors() {
		return this.or.hasErrors();
	}
	
	@Override
	public void info(long code, String msg) {
		this.or.info(code, msg);
	}
	
	@Override
	public void infoTr(long code, Object... params) {
		this.or.infoTr(code, params);
	}
	
	@Override
	public void log(DebugLevel lvl, long code, String msg) {
		this.or.log(lvl, code, msg);
	}
	
	@Override
	public void log(RecordStruct entry) {
		this.or.log(entry);
	}
	
	@Override
	public void logTr(DebugLevel lvl, long code, Object... params) {
		this.or.logTr(lvl, code, params);
	}
	
	@Override
	public void setAmountCompleted(int v) {
		this.or.setAmountCompleted(v);
	}
	
	@Override
	public void setContext(OperationContext v) {
		this.or.setContext(v);
	}
	
	@Override
	public void setCurrentStep(int step, String name) {
		this.or.setCurrentStep(step, name);
	}
	
	@Override
	public void setCurrentStepNameTr(int step, int code, Object... params) {
		this.or.setCurrentStepNameTr(step, code, params);
	}
	
	@Override
	public void nextStepTr(int code, Object... params) {
		this.or.nextStepTr(code, params);
	}
	
	@Override
	public void nextStep(String name) {
		this.or.nextStep(name);
	}
	
	@Override
	public void setLogLevel(DebugLevel v) {
		this.or.setLogLevel(v);
	}
	
	@Override
	public void setProgressMessage(String v) {
		this.or.setProgressMessage(v);
	}
	
	@Override
	public void setProgressMessageTr(int code, Object... params) {
		this.or.setProgressMessageTr(code, params);
	}
	
	@Override
	public void setSteps(int v) {
		this.or.setSteps(v);
	}
	
	@Override
	public Message toLogMessage() {
		return this.or.toLogMessage();
	}
	
	@Override
	public RecordStruct toRecord() {
		return this.or.toRecord();
	}
	
	@Override
	public String toString() {
		return this.or.toString();
	}
	
	@Override
	public void trace(long code, String msg) {
		this.or.trace(code, msg);
	}
	
	@Override
	public void traceTr(long code, Object... params) {
		this.or.traceTr(code, params);
	}
	
	@Override
	public void warn(long code, String msg) {
		this.or.warn(code, msg);
	}
	
	@Override
	public void warnTr(long code, Object... params) {
		this.or.warnTr(code, params);
	}
}
