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
package divconq.session.component;

import java.util.concurrent.locks.ReentrantLock;

import divconq.lang.op.OperationResult;
import divconq.session.IComponent;
import divconq.session.Session;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;

public class PolledProgressTracker implements IComponent {
	protected String name = null;
	protected Session session = null;
	protected String status = null;
	protected long step = 0;
	protected long totalSteps = 0;
	protected Struct extra = null;
	protected boolean done = false;
	protected ReentrantLock lock = new ReentrantLock();
	
	@Override
	public void start(String name, Session session, RecordStruct msg, OperationResult errs) {
		this.name = name;		
		this.session = session;		
		this.collectStatus(msg);		
	}

	protected void collectStatus(RecordStruct msg){
		//System.out.println("Status updated: " + msg);
		
		if (!msg.isFieldEmpty("Status")) 
			this.status = msg.getFieldAsString("Status");
		
		if (!msg.isFieldEmpty("TotalSteps")) 
			this.totalSteps = msg.getFieldAsInteger("TotalSteps");
		
		if (msg.hasField("Extra"))
			this.extra = msg.getFieldAsStruct("Extra");
		
		if (!msg.isFieldEmpty("Step")) 
			this.step = msg.getFieldAsInteger("Step");
		
		if (msg.hasField("Done"))
			this.done = msg.getFieldAsBoolean("Done");
	}
	
	@Override
	public void call(RecordStruct msg, OperationResult errs) {		
		String action = msg.getFieldAsString("Action");

		this.lock.lock();
		
		try {
			if ("Update".equals(action)) {
				this.collectStatus(msg);
				return;
			}
			else if ("Poll".equals(action)) {
				RecordStruct rmsg = new RecordStruct();
				
				rmsg.setField("Name", this.name);
				rmsg.setField("Status", this.status);
				rmsg.setField("Step", this.step);
				rmsg.setField("TotalSteps", this.totalSteps);
				rmsg.setField("Extra", this.extra);
				rmsg.setField("Done", this.done);
				
				//System.out.println("Status polled: " + rmsg);
				
				//return rmsg;
				return;
			}
		}
		finally {
			this.lock.unlock();
		}
		
		try {
			errs.error(1, "Action not allowed: " + action);		// TODO better codes
		}
		catch (Exception x) {			
		}
	}

	@Override
	public void end(RecordStruct msg, OperationResult errs) {		
	}
}
