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
package divconq.test.bus;

import divconq.service.plugin.Operation;
import divconq.service.plugin.Request;
import divconq.service.plugin.Response;
import divconq.struct.Struct;
import divconq.work.TaskRun;

public class TestPluginService {
	
	@Operation()
	public void handleTest0(TaskRun run, Struct request) {
		
	}
	
	@Operation(
		description = "Example with no request type and no response type",
		tags = { "User", "Guest" },
		response = @Response(),
		request = @Request()
	)
	public void handleTest1(TaskRun run, Struct request) {
		
	}
	
	@Operation(
		descriptionCode = "1234",
		tags = { "User", "Guest" },
		response = @Response(type = "String"),
		request = @Request("xml")
	)
	public void handleTest2(TaskRun run, Struct request) {
		
	}
}
