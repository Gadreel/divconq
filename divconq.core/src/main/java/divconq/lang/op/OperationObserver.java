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

import java.lang.ref.WeakReference;

import divconq.struct.RecordStruct;

// just toss out the events, useful only for subclassing
// this is durable for use with Queue but remember only fields are saved in database
// and you should implement deepCopy
abstract public class OperationObserver extends RecordStruct implements IOperationObserver {
	protected WeakReference<OperationContext> ctxref = null;
    
    public OperationObserver() {
    	this(OperationContext.get());
    }
    
    public OperationObserver(OperationContext ctx) {
    	this.ctxref = new WeakReference<>((ctx != null) ? ctx : OperationContext.get());
    }
	
	/* do deepCopy in subclasses
	@Override
	public Struct deepCopy() {
		OperationObserver cp = new OperationObserver();
		this.doCopy(cp);
		return cp;
	}
	*/
	
    // fire is in the context that OO originated in, not in 
	@Override
	public void fireEvent(OperationEvent event, OperationContext target, Object detail) {
		// be sure we restore the context
		OperationContext ctx = OperationContext.get();
		
		try {
			OperationContext tempctx = this.ctxref.get();
			
			if (tempctx != null) {
				OperationContext.set(tempctx);
			
				if (event == OperationEvents.LOG)
					this.log(target, (RecordStruct) detail);
				else if (event == OperationEvents.PROGRESS) {
					if (detail == OperationEvents.PROGRESS_AMOUNT)
						this.amount(target, target.getAmountCompleted());
					else if (detail == OperationEvents.PROGRESS_STEP)
						this.step(target, target.getCurrentStep(), target.getSteps(), target.getCurrentStepName());
					else
						this.progress(target, target.getProgressMessage());
				}
				else if (event == OperationEvents.COMPLETED)
					this.completed(target);
				else if (event == OperationEvents.PREP_TASK)
					this.prep(target);
				else if (event == OperationEvents.START_TASK)
					this.start(target);
				else if (event == OperationEvents.STOP_TASK)
					this.stop(target);
			}
			
			// TODO else log?
		}
		finally {
			OperationContext.set(ctx);
		}
	}
	
	public void log(OperationContext ctx, RecordStruct entry) {
	}
	
	public void step(OperationContext ctx, int num, int of, String name){
	}
	
	public void progress(OperationContext ctx, String msg){
	}
	
	public void amount(OperationContext ctx, int v){
	}

	public void completed(OperationContext ctx) {
	}
	
	public void prep(OperationContext ctx) {
	}
	
	public void start(OperationContext ctx) {
	}
	
	public void stop(OperationContext ctx) {
	}
}
