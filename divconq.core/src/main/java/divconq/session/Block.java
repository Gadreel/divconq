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
package divconq.session;

import divconq.lang.Memory;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;

// TODO add facilities to read chunks from this block if it is too big
// say the upload provided at 128KB block but we want to pass 42KB blocks
// within our fabric - what happens is we ask for a block size around 42KB with a max of 48KB
// then block responds that we can do this with 43KB (3 transfers) which optimizes over 4 transfers 
public class Block {
	protected long offset = 0;
	protected boolean done = false;
	protected boolean abort = false;
	protected Memory chunk = null;  
	
	public Block() {
	}
	
	public Block(RecordStruct rec) {
    	if (rec.hasField("Data")) {
    		this.chunk = rec.getFieldAsBinary("Data");
    		this.chunk.setPosition(0);
    	}
    	
		//System.out.println("session got block: " + block);
    	
    	if (rec.hasField("Offset"))
    		this.offset = rec.getFieldAsInteger("Offset");
    	
    	if (rec.hasField("Done"))
    		this.done = rec.getFieldAsBoolean("Done");
    	
    	if (rec.hasField("Abort"))
    		this.abort = rec.getFieldAsBoolean("Abort");
	}

	public RecordStruct toRecord() {
		return new RecordStruct(
				new FieldStruct("Data", this.chunk),
				new FieldStruct("Offset", this.offset),
				new FieldStruct("Done", this.done),
				new FieldStruct("Abort", this.abort)
		);
	}
	
	public void setData(Memory v) {
		this.chunk = v;
	}
	
	public Memory getData() {
		return this.chunk;
	}
	
	public void setDone(boolean v) {
		this.done = v;
	}
	
	public boolean isDone() {
		return this.done;
	}
	
	public void setAbort(boolean v) {
		this.abort = v;
	}
	
	public boolean isAbort() {
		return this.abort;
	}
	
	public void setOffset(long v) {
		this.offset = v;
	}
	
	public long getOffset() {
		return this.offset;
	}
	
	public int getSize() {
		return this.chunk.getLength();
	}
}
