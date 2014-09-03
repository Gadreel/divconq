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
package divconq.tasks.cli;

import java.util.Scanner;

import divconq.api.ApiSession;
import divconq.api.DumpCallback;
import divconq.api.ServiceResult;
import divconq.bus.Message;
import divconq.hub.Foreground;
import divconq.hub.Hub;
import divconq.hub.ILocalCommandLine;
import divconq.lang.FuncResult;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.work.Task;

public class Client implements ILocalCommandLine {

	@Override
	public void run(Scanner scan, ApiSession api) {
		Message verres = api.sendMessage(new Message("Status", "Info", "Version"));	
		
		if (verres.hasErrors()) {
			System.out.println("Error connecting to Uploader: " + verres.getMessage());
			return;
		}
		
		System.out.println("dcTasks Version: " + verres.getFieldAsRecord("Body").getFieldAsString("Version"));
		
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   Test customer");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  Check AuthToken");
				System.out.println("3)  Local Utilities [ignore if not running WF foreground]");

				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					running = false;
					break;
					
				case 1: {
					RecordStruct body = new RecordStruct();
					
					System.out.println("AuthToken: ");
					body.setField("AuthToken", scan.nextLine());
					
					api.sendMessage(new Message("ftcNccUploader", "Deposits", "AuthCheck", body), new DumpCallback("Uploader Auth Check"));

					break;
				}	// end case 1
				case 3: {
					System.out.println("Note these utilities are only good from the main console,");
					System.out.println("if you are using a remote connection then the encryption will");
					System.out.println("not work as expected.  [we do not have access the master keys]");
					System.out.println();
					
					Foreground.utilityMenu(scan);
					
					break;
				}
				case 4: {
					// TODO cheating... use API
					
					/*
					RecordStruct info = new RecordStruct();
					
					info.setField("TaskIdentity", "BASHFull"); 
					info.setField("ClaimedStamp", "20140315T002400000Z");

					OperationResult res = Hub.instance.getWorkQueue().updateClaim(info);
					
					FuncResult<String> res = Hub.instance.getWorkQueue().reserveWork("ddd2");
					
					System.out.println("Res: " + res.getCode());
					
					if (res.hasErrors())
						break;
					
					String resstamp = res.getResult();
					
					FuncResult<Long> res2 = Hub.instance.getWorkQueue().addWork("ddd2", "test of ddd 2", "Defaul", null, null, "ClassName", "something", "else", "whatever", "ddd tag", null, null, 5, 20, resstamp);
					
					System.out.println("Res2: " + res2.getCode());
					*/
					
					Task task = new Task()
						.withId("carl1")
						.withTitle("Greeting Carl")
						.withParams(new RecordStruct(new FieldStruct("Greet", "Carl")))
						.withWork("divconq.tasks.test.GreetTask");
					
					Hub.instance.getWorkPool().submit(task);
					
					break;
				}
				case 5: {
					// TODO cheating... use API
					
					for (int i = 0; i < 10; i++) {
						Task task = new Task()
							.withId("carl" + i)
							.withTitle("Greeting Carl")
							.withBucket("Greet")
							.withRootContext()
							.withParams(new RecordStruct(new FieldStruct("Greet", "Carl")))
							.withWork("divconq.tasks.test.SlowGreetWork")
							.withMaxTries(1)
							.withTimeout(5);
						
						FuncResult<String> res = Hub.instance.getWorkQueue().submit(task);
						
						System.out.println("Work Id: " + res.getResult());
					}
					
					/*
					RecordStruct info = new RecordStruct();
					
					info.setField("TaskIdentity", "BASHFull"); 
					info.setField("ClaimedStamp", "20140315T002400000Z");

					OperationResult res = Hub.instance.getWorkQueue().updateClaim(info);
					FuncResult<Long> res2 = Hub.instance.getWorkQueue().addWork("ddd3", "test of ddd 3", "CharterPool", null, null, "ClassName", "something 2", "else", "whatever", "ddd tag", null, null, 5, 25, null);
					
					System.out.println("Res2: " + res2.getCode());
					*/
					
					break;
				}
				case 6: {
					api.sendMessage(
							new Message("Status", "Info", "HubStatus"), 
							new ServiceResult() {
								@Override
								public void callback() {
									if (this.hasErrors()) {
										System.out.println("errors: " + this.getMessages());
										return;
									}
									
									RecordStruct rec = this.getBodyAsRec();

									ListStruct sessions = rec.getFieldAsList("Queue");
									
									for (Struct s : sessions.getItems()) {
										RecordStruct sess = (RecordStruct)s;

										System.out.println(sess.getFieldAsString("TaskIdentity") + "  " + sess.getFieldAsString("Title")
												 + "  Added: " + sess.getFieldAsString("Added") + "  Try: " + sess.getFieldAsString("LastTry")
												 + " of " + sess.getFieldAsString("MaxTry")
										);

										System.out.println("     Claimed by: " + sess.getFieldAsString("ClaimedBy") + " at: " + sess.getFieldAsString("ClaimedAt")
										);
										
										ListStruct tasks = sess.getFieldAsList("Audit");
										
										for (Struct s2 : tasks.getItems()) {
											RecordStruct task = (RecordStruct)s2;
											
											System.out.println(
													"     TRY #" + task.getFieldAsString("Try")  
													+ " By: " + task.getFieldAsString("Hub") 
													+ "   Code: " + task.getFieldAsString("Code") 
													+ "   Msg: " + task.getFieldAsString("Message") 
											);
										}
										
										System.out.println(" ------------------------------------------- ");
									}
									
									System.out.println(" Workflow Idled: " + rec.getFieldAsBoolean("ServerIdled"));
								}
							}
					);	
					
					break;
				}
				case 7: {
					// TODO cheating... use API
					/*
					RecordStruct info = new RecordStruct();
					
					info.setField("WorkId", "1"); 

					FuncResult<RecordStruct> res = Hub.instance.getWorkQueue().loadWork(info);
					
					System.out.println("Res: " + res.getResult());
					*/
					
					/*  TODO move common tasks into API, include API project in tasks project path
					Task task = new Task()
						.withId("carl1")
						.withTitle("Greeting Carl")
						.withParams(new RecordStruct(new FieldStruct("Greet", "Carl")))
						.withWork(GreetWork.class);
					
					Hub.instance.getWorkPool().submit(task);
					*/
					
					break;
				}
				
				}	
			}
			catch(Exception x) {
				System.out.println("CLI error: " + x);
			}
		}		
	}
	
}
