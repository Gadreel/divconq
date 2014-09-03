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
package divconq.mod;

import divconq.lang.OperationResult;
import divconq.xml.XElement;

abstract public class ModuleBase implements IModule {
	protected long starttime = System.currentTimeMillis();
	protected ModuleLoader loader = null;
	protected XElement config = null;
	
	@Override
	public void setLoader(ModuleLoader v) {
		this.loader = v;
	}
	
	@Override
	public ModuleLoader getLoader() {
		return this.loader;
	}
	
	@Override
	public long startTime() {
		return this.starttime;
	}

	@Override
	public void init(OperationResult log, XElement config) {
		this.config = config;
	}
}
