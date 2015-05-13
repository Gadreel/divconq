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

/**
 * when writing a stored procedure and then calling another SP inside it, you may want the output of that to be 
 * seamlessly integrated with your own result - if so this is the result handler you want
 * 
 * @author Andy
 *
 */
abstract public class WrappedResult extends DatabaseResult {
	public WrappedResult(DatabaseResult res) {
		super(res.getResult());			
	}
	
	@Override
	public void callback() {
		this.process();
	}
	
	abstract public void process();
}
