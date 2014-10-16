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
package divconq.test;

import divconq.bus.IService;
import divconq.work.TaskRun;

public class TestService implements IService {
	@Override
	public String serviceName() {
		return "dcTestService";
	}

	@Override
	public void handle(TaskRun request) {
		request.complete();
	}

}
