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
package divconq.service;

import divconq.mod.ModuleBase;

// this class, being in an indirectly linked JAR, forces a new class loader to be used for all classes within this module 
public class ServiceModule extends ModuleBase  {

	@Override
	public void start() {
		// nothing to do, yet
	}

	@Override
	public void stop() {
		// nothing to do, yet
	}
}
