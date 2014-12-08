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

import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationContext;
import divconq.struct.builder.ICompositeBuilder;

/**
 * Super class that helps manage results from dcDb.  See ObjectResult and
 * CustomResult for specifics.
 * 
 * @author Andy
 *
 */
abstract public class DatabaseResult extends FuncCallback<ICompositeBuilder> {
	/**
	 * Assign an object to manage the output stream from the database.
	 * 
	 * @param builder Stream or Struct builder 
	 */
	public DatabaseResult(ICompositeBuilder builder) {
		this.value = builder;		
	}
	
	public DatabaseResult(ICompositeBuilder builder, OperationContext ctx) {
		super(ctx);
		this.value = builder;		
	}
}
