/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.db;

import divconq.bus.Message;
import divconq.lang.op.OperationContext;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ObjectBuilder;

/**
 * Although there are alternatives (see CustomResult) typically results from the
 * database will come in the form of a collection of java objects (a CompositeStruct).
 * This class provides a handy wrapper for this commonly used result type.
 * 
 * @author Andy
 *
 */
abstract public class ObjectResult extends DatabaseResult {
	/**
	 * @return the collection of java objects (a CompositeStruct) assembled from the database response
	 */
	public CompositeStruct getResultAsComposite() {
		return ((ObjectBuilder)this.value).getRoot();
	}

	/**
	 * @return the database result casted as RecordStruct
	 */
	public RecordStruct getResultAsRec() {
		return (RecordStruct) ((ObjectBuilder)this.value).getRoot();
	}

	/**
	 * @return the database result casted as ListStruct
	 */
	public ListStruct getResultAsList() {
		return (ListStruct) ((ObjectBuilder)this.value).getRoot();
	}

	/**
	 * Create a wrapper for database results that assembles those results into 
	 * a collection of java objects (a CompositeStruct).
	 */
	public ObjectResult() {
		super(new ObjectBuilder());
	}
	
	public ObjectResult(OperationContext ctx) {
		super(new ObjectBuilder(), ctx);
	}
	
	@Override
	public void callback() {
		this.process(this.getResultAsComposite());
	}
	
	abstract public void process(CompositeStruct result);
	
	@Override
	public Message toLogMessage() {
		Message msg = super.toLogMessage();
		
		if (this.value != null)
			msg.setField("Body", this.getResultAsComposite());
		
		return msg;
	}
}
