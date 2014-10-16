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
package divconq.count;

import java.util.Observable;
import java.util.concurrent.locks.ReentrantLock;

import divconq.struct.RecordStruct;

public abstract class Counter extends Observable {
	protected String name = null;
	protected long lastactivity = System.currentTimeMillis();
	
	// current object may often be nothing, however, in the case of counting
	// say number of messages sent over the bus this may be useful
	// TODO this is not cool for GC
	protected Object currentObject = null;

	protected ReentrantLock valuelock = new ReentrantLock();
	
	public String getName() {
		return this.name;
	}
	
	/*
	public Object getCurrentObject() {
		return this.currentObject;
	}
	
	public void setCurrentObject(Object currentObject) {
		this.currentObject = null;   //currentObject;   TODO this is not cool for GC
	}
	*/
	
	public Counter(String name) {
		this.name = name;
	}
	
    @Override
    protected synchronized void setChanged() {
    	this.lastactivity = System.currentTimeMillis();
    	
    	super.setChanged();    	
		this.notifyObservers(this.currentObject);
    }
	
	abstract public Counter clone();
	abstract public RecordStruct toRecord(); 
	abstract public RecordStruct toCleanRecord();		// don't show object which may contain sensitive info 
	
	public void copyToClone(Counter clone) {
		clone.currentObject = this.currentObject;
		clone.lastactivity = this.lastactivity;
	}
	
	public void reset() {
		this.currentObject = null;
	}
	
	@Override
	public String toString() {
		return this.toRecord().toPrettyString();
	}
}
