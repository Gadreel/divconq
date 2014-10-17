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
package divconq.script;

import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;

public class StackCallEntry extends StackEntry {
	protected StackEntry currEntry = null;
    
    public StackEntry getChild() {
        return this.currEntry; 
    }
    
    public void setChild(StackEntry v) { 
    	this.currEntry = v; 
    }
    
	public StackCallEntry(Activity act, StackEntry parent, Instruction inst) {
		super(act, parent, inst);
	}
    
	@Override
    public void debugStack(ListStruct dumpList) {
    	RecordStruct dumpRec = new RecordStruct();
    	dumpList.addItem(dumpRec);
    	
    	this.collectDebugRecord(dumpRec);
    	RecordStruct subRec = this.inst.collectDebugRecord(this, dumpRec);

    	if (subRec != null)
    		dumpList.addItem(subRec);
    	
        if (this.currEntry != null)
        	this.currEntry.debugStack(dumpList);
    }
	
	@Override
	public StackEntry getExecutingStack() {
		return (this.currEntry != null) ? this.currEntry : this;
	}
}
