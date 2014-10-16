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
import divconq.struct.Struct;

/**
 * Provides the same function support as @see OperationResult, however allows for more
 * than a true/false return type.  A specific return type may be provided using generics.
 * 
 * @author Andy
 *
 * @param <T> the return type
 */
public class FuncResult<T> extends OperationResult {
	  protected T value = null;
		 
	  public FuncResult(DebugLevel loglevel) {
		  super(loglevel);
	  }
	
	public FuncResult(OperationContext ctx) {
		super(ctx);
	}
	 
	  public FuncResult() {
		  super();
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
	
	public boolean isNotEmptyResult() {
		return !Struct.objectIsEmpty(this.value);
	}
	
	public boolean isEmptyResult() {
		return Struct.objectIsEmpty(this.value);
	}
	
	@Override
	public Message toLogMessage() {
		Message msg = super.toLogMessage();
		
		if (this.value != null)
			msg.setField("Body", this.value);
		
		return msg;
	}
}
