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
import divconq.work.TaskRun;

/**
 * Provides the same function support as @see OperationCallback, however allows for more
 * than a true/false result type.  A specific return type may be provided using generics.
 * 
 * @author Andy
 *
 * @param <T> the return type
 */
abstract public class FuncCallback<T> extends OperationCallback {
	protected T value = null;
	 
	public FuncCallback(DebugLevel loglevel) {
		super(loglevel);
	}
		 
	public FuncCallback() {
		super();
	}
		
	public FuncCallback(TimeoutPlan plan) {
		super(plan);
	}
	
	// timeout on regular schedule  
	public FuncCallback(TaskRun run) {
		super(run);
	}
	
	public FuncCallback(TaskRun run, TimeoutPlan plan) {
		super(run, plan);
	}
	  
	  /**
	 * @return function result if call was a success
	 */
	public T getResult() {
	    return this.value;
	}
		 
	  /**
	 * @param v result to use, set by the method called
	 */
	public void setResult(T v) {
	    this.value = v;
	}
	
	public Message toMessage() {
		Message msg = super.toLogMessage();
		
		if (this.value != null)
			msg.setField("Body", this.value);
		
		return msg;
	}
}
