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
package divconq.tasks.test;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.bus.ServiceResult;
import divconq.hub.Hub;
import divconq.lang.CountDownCallback;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.schema.Schema;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.work.IWork;
import divconq.work.TaskRun;
import divconq.xml.XAttribute;
import divconq.xml.XElement;

public class TestBusErrors implements IWork {

	@Override
	public void run(final TaskRun testrun) {
		
		XElement myschema = new XElement("Schema", 
				new XElement("Services", 
					new XElement("Service", 
						new XAttribute("Name", "TestBusService"),
						new XElement("Secure", 
								new XAttribute("Tags", "Guest,User"),
								new XElement("Feature", 
										new XAttribute("Name", "Test"),
										new XElement("Op", 
												new XAttribute("Name", "NoOp"),
												new XElement("Request", new XAttribute("Inherits", "RequestMessage")),
												new XElement("Response", new XAttribute("Inherits", "ResponseMessage"))
										)
								)
						)
					)
				)
		);
		
		Schema s = new Schema();
		s.manager = Hub.instance.getSchema();
		
		s.loadSchema(testrun, myschema);
		
		if (testrun.hasErrors()) {
			testrun.error("Could not complete the test, bad schema!");
			testrun.complete();
			return;
		}		
		
		final OperationContext testctx = testrun.getContext();
		
		Hub.instance.getBus().getLocalHub().registerService(new IService() {		
			@Override
			public String serviceName() {
				return "TestBusService";
			}
			
			@Override
			public void handle(TaskRun request) {
				OperationContext myctx = OperationContext.get();
				
				myctx.info("I got the call but I'm not going to reply -- timeout test");
				
				request.complete();
			}
		});
		
		testrun.info("Test operation id is " + testctx.getOpId());
		//testrun.info("Test bucket is " + testctx.getBucket());
		testrun.info("Test is elevated " + testctx.isElevated());
		testrun.info("Test user is " + testctx.getUserContext().getUserName());
		
		testrun.info("Now make the service call");
		
		int runcnt = 3;
		
		final CountDownCallback countdown = new CountDownCallback(runcnt, new OperationCallback() {			
			@Override
			public void callback() {
				testrun.info("TestBusErrors finishing up.");
				
				OperationContext testctx = OperationContext.get();   // should be the same as testrun.getContext();
				
				testrun.info("Test operation id is " + testctx.getOpId());
				//testrun.info("Test bucket is " + testctx.getBucket());
				testrun.info("Test is elevated " + testctx.isElevated());
				testrun.info("Test user is " + testctx.getUserContext().getUserName());
				
				testrun.info("TestBusErrors is cleaning up.");
				
				// cleanup by removing the schema and service
				
				Hub.instance.getBus().getLocalHub().removeService("TestBusService");
				
				Hub.instance.getSchema().getService().remove("TestBusService");
				
				// done
				
				if (testrun.hasErrors())
					testrun.info("TestBusErrors completed with errors: " + testrun.getMessage());
				else
					testrun.info("TestBusErrors completed successfully");
				
				testrun.complete();
			}
		});

		// send two messages, the second should have an altered user context
		
		// bad service name
		{
			Message msg = new Message("TestBusServiceXXX", "Test", "NoOp");
			
			Hub.instance.getBus().sendMessage(msg, new ServiceResult() {
				@Override
				public void callback() {
					testrun.info("Back from bad service call");

					if (this.getCode() != 221)
						testrun.error("Expected error 221, got: " + this.getCode());
					
					countdown.countDown();
				}
			});
		}

		// bad body
		{
			Message msg = new Message("TestBusService", "Test", "NoOp", new RecordStruct(new FieldStruct("Color", "Green")));
			
			Hub.instance.getBus().sendMessage(msg, new ServiceResult() {
				@Override
				public void callback() {
					testrun.info("Back from bad body call");

					if (this.getCode() != 419)
						testrun.error("Expected error 419, got: " + this.getCode());
					
					countdown.countDown();
				}
			});
		}

		// timeout
		{
			Message msg = new Message("TestBusService", "Test", "NoOp");
			
			Hub.instance.getBus().sendMessage(msg, new ServiceResult() {
				@Override
				public void callback() {
					testrun.info("Back from timeout call");
					
					if (this.getCode() != 218)
						testrun.error("Expected error 218, got: " + this.getCode());
					
					countdown.countDown();
				}
			});
		}
	}
}
