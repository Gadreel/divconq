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
package divconq.schema;

import divconq.db.ICollector;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.util.StringUtil;

public class DbCollector {
	public String name = null;
	public String execute = null;
	public String[] securityTags = null;
	public ICollector sp = null;
	
	public ICollector getCollector() {
		if (this.sp != null)
			return this.sp;
		
		// composer should be stateless, save effort by putting 1 instance inside DbComposer and reusing it
		if (StringUtil.isNotEmpty(this.execute)) 
			this.sp = (ICollector) Hub.instance.getInstance(this.execute);
		
		if (this.sp == null)
			OperationContext.get().error("Collector " + this.name + " failed to create.");
		
		return this.sp;
	}
}