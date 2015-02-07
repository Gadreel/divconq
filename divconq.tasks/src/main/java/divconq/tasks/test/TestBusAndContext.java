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
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.work.IWork;
import divconq.work.TaskRun;
import divconq.xml.XAttribute;
import divconq.xml.XElement;

public class TestBusAndContext implements IWork {

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
		
		Schema s = new Schema("@Internal", Hub.instance.getSchema());
		
		s.loadSchema(myschema);
		
		if (testrun.hasErrors()) {
			testrun.error("Could not complete the test, bad schema!");
			testrun.complete();
			return;
		}		
		
		final OperationContext testctx = testrun.getContext();
		
		Hub.instance.getBus().getLocalHub().registerService(new IService() {		
			protected int callcnt = 0;
			
			@Override
			public String serviceName() {
				return "TestBusService";
			}
			
			@Override
			public void handle(TaskRun request) {
				this.callcnt++;
				
				// with services, log to the context via operation result
				request.info("Service got call!");
				
				OperationContext myctx = OperationContext.get();
				
				request.info("Service operation id is " + myctx.getOpId());
				//opresult.info("Service bucket is " + myctx.getBucket());
				request.info("Service is elevated " + myctx.isElevated());
				request.info("Service user is " + myctx.getUserContext().getUsername());
				
				if (myctx.getOpId().equals(testctx.getOpId()))
					request.info("Our operation id matches");
				else
					request.error("Our operation id does not match");
				
				//if (myctx.getBucket().equals(testctx.getBucket()))
				//	opresult.info("Our bucket matches");
				//else
				//	opresult.error("Our bucket does not match");
				
				// user context switch testing - become a root
				if (callcnt == 2) {
					// all we have to do is change the user context, bus will return the context
					OperationContext.use(myctx.toBuilder().withRootUserTemplate());
					request.info("Service user is now " + OperationContext.get().getUserContext().getUsername());
				}
				
				request.complete();
			}
		});
		
		testrun.info("Test operation id is " + testctx.getOpId());
		//testrun.info("Test bucket is " + testctx.getBucket());
		testrun.info("Test is elevated " + testctx.isElevated());
		testrun.info("Test user is " + testctx.getUserContext().getUsername());
		
		testrun.info("Now make the service call");
		
		int runcnt = 2;
		
		final CountDownCallback countdown = new CountDownCallback(runcnt, new OperationCallback() {			
			@Override
			public void callback() {
				testrun.info("TestBusAndConext finishing up.");
				
				OperationContext testctx = OperationContext.get();   // should be the same as testrun.getContext();
				
				testrun.info("Test operation id is " + testctx.getOpId());
				//testrun.info("Test bucket is " + testctx.getBucket());
				testrun.info("Test is elevated " + testctx.isElevated());
				testrun.info("Test user is " + testctx.getUserContext().getUsername());
				
				testrun.info("Here are all of the messages for run, including those from service: ");
				
				for (Struct m : testrun.getMessages().getItems()) {
					RecordStruct msg = (RecordStruct) m;
					
					testrun.info(msg.getFieldAsString("Code") + " - " + msg.getFieldAsString("Message"));
				}
				
				testrun.info("TestBusAndConext is cleaning up.");
				
				// cleanup by removing the schema and service
				
				Hub.instance.getBus().getLocalHub().removeService("TestBusService");
				
				Hub.instance.getSchema().removeService("TestBusService");
				
				// done
				
				if (testrun.hasErrors())
					testrun.info("TestBusAndConext completed with errors: " + testrun.getMessage());
				else
					testrun.info("TestBusAndConext completed successfully");
				
				testrun.complete();
			}
		});

		// send two messages, the second should have an altered user context
		for (int i = 0; i < runcnt; i++) {
			Message msg = new Message("TestBusService", "Test", "NoOp");
			
			Hub.instance.getBus().sendMessage(msg, new ServiceResult() {
				@Override
				public void callback() {
					// we now have a record of all the messages from the service
					testrun.info("TestBusAndConext context after call:.");
					
					OperationContext testctx = testrun.getContext();
					
					testrun.info("Test operation id is " + testctx.getOpId());
					//testrun.info("Test bucket is " + testctx.getBucket());
					testrun.info("Test is elevated " + testctx.isElevated());
					testrun.info("Test user is " + testctx.getUserContext().getUsername());
					
					countdown.countDown();
				}
			});		
			
			// wait a little before next call
			
			try {
				Thread.sleep(1000);
			} 
			catch (InterruptedException x) {
				testrun.complete();
				break;
			}
		}
	}

}
