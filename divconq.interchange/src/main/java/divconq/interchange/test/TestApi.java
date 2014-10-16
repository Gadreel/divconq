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
package divconq.interchange.test;

import java.util.Scanner;

import divconq.api.ApiSession;
import divconq.api.DumpCallback;
import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.hub.HubResources;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.log.DebugLevel;
import divconq.log.Logger;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;

public class TestApi {
	public static void main(String[] args) {
		HubResources resources = new HubResources();
		resources.setDebugLevel(DebugLevel.Info);
		OperationResult or = resources.init();
		
		if (or.hasErrors()) {
			Logger.error("Unable to continue, hub resources not properly configured");
			return;
		}
	
		Hub.instance.start(resources);
		
		/*
		final CountDownLatch online = new CountDownLatch(1);
		
		Hub.instance.listenOnline(new OperationCallback() {
			@Override
			public void callback() {
				online.countDown();
			}
		} );
		
		try {
			online.await(5, TimeUnit.SECONDS);
			//online.await();
		} 
		catch (InterruptedException e) {
		}
		
		if (online.getCount() > 0) {
			Logger.error("Unable to continue, hub not online");
			scan.close();
			return;
		}
		*/
		
		final Scanner scan = new Scanner(System.in);
		
		System.out.print("Domain (e.g. root): ");
		String domain = scan.nextLine();
		
		OperationContext.useNewRoot();
		
		ApiSession capi = ApiSession.createLocalSession(domain);
		
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   Basic API Menu");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  Sign In");
				System.out.println("2)  Sign Out");
				System.out.println("3)  Dump Context");
				System.out.println("4)  Echo");
				System.out.println("5)  Sign In as Guest");
				System.out.println("6)  Send to InBox");
				System.out.println("7)  Check InBox");
				System.out.println("8)  Upload Test");
				System.out.println("9)  Download Test");
				System.out.println("10)  Switch Domain");
				System.out.println("11)  User Functions");
				System.out.println("12)  Group Functions");

				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					running = false;
					break;
					
				case 1: {
						System.out.print("User: ");
						String user = scan.nextLine();
						
						System.out.print("Password: ");
						String pass = scan.nextLine();

						if (capi.startSession(user, pass))
							System.out.println("Success");
						else 
							System.out.println("Failed");
						
						break;
					}
				
					case 2: {						
						capi.stop();
						capi = ApiSession.createLocalSession("teton");
						
						break;
					}
					
					case 3: {
						System.out.println("Session Key: " + capi.getSessionKey());
						System.out.println("User: " + capi.getUser());
						
						break;
					}
					
					case 4: {
						Message msg = capi.sendMessage(new Message("Status", "Echo", "Test", "Hello from Bus: à¤ à¥€ á‰» âŒ† ï¨¬"));
						
						System.out.println("Messages: " + msg.getMessages());
						System.out.println("Result: " + msg.getCode());
						
						break;
					}
					
					case 5: {
						if (capi.startSession())
							System.out.println("Success");
						else 
							System.out.println("Failed");
						
						break;
					}
					
					case 6: {
						System.out.print("Session: ");
						String sessid = scan.nextLine();
						
						Message request = new Message("Session", "InBox", "Deliver", "Hello from somewhere!");
						request.setField("Tag", sessid + "_serv_feat_op_tag_abc");
						Message msg = capi.sendMessage(request);
						
						System.out.println("Messages: " + msg.getMessages());
						System.out.println("Result: " + msg.getCode());
						
						break;
					}
					
					case 7: {
						System.out.println("InBox: " + capi.checkInBox());
						break;
					}
					
					case 8: {
						/* TODO fix this, we need to request an upload
						long t = System.currentTimeMillis();
						
						OperationResult srcres = capi.openForUpload("awww");
						
						if (srcres.hasErrors()) {
							System.out.println("Set Source Error: " + srcres.getMessages());
							break;
						}
						
						OperationResult upres = capi.upload(new File("D:/dev/divconq/hub/ext/divconq.core.jar"));
						
						if (upres.hasErrors()) 
							System.out.println("Upload Error: " + upres.getMessages());
						else 
							System.out.println("Success: " + (System.currentTimeMillis() - t));
						*/
						
						break;
					}
					
					case 9: {
						/* TODO fix this, we need to request an upload
						long t = System.currentTimeMillis();
						
						OperationResult srcres = capi.openForDownload("awww");
						
						if (srcres.hasErrors()) {
							System.out.println("Set Destination Error: " + srcres.getMessages());
							break;
						}
						
						OperationResult upres = capi.download(new File("./files/temp/testdownload.txt"));
						
						if (upres.hasErrors()) 
							System.out.println("Download Error: " + upres.getMessages());
						else 
							System.out.println("Success: " + (System.currentTimeMillis() - t));
						*/
						
						break;
					}
					
					case 10 : {
						// TODO this is not really accurate, we want to change the domain remotely
						// using a call to session probably!
						System.out.print("Domain (e.g. root): ");
						domain = scan.nextLine();
						
						domain = Hub.instance.resolveDomainId(domain);
					
						// TODO capi.getSession().getUser().setDomainId(domain);
					}
					
					case 11:
						TestApi.userStuff(scan, capi);
						break;
						
					case 12:
						TestApi.groupStuff(scan, capi);
						break;
				}
			}
			catch(Exception x) {
				System.out.println("CLI error: " + x);
			}
		}		
		
		capi.stop();
		
		try {
			// try to let the session clean up before stopping
			Thread.sleep(1500);
		} 
		catch (InterruptedException e) {
		}
		
		Hub.instance.stop();
	}	
	
	static public void userStuff(Scanner in, ApiSession capi) {
		boolean running = true;
		
		while(running) {
			System.out.println();
			System.out.println("-----------------------------------------------");
			System.out.println("   User Menu");
			System.out.println("-----------------------------------------------");
			System.out.println("0)  Return to main menu");
			System.out.println("1)  Add User Account");
			System.out.println("2)  Update User Account");
			System.out.println("3)  View User Account");
			System.out.println("4)  Retire User Account");
			System.out.println("5)  Revive User Account");
			System.out.println("6)  List Users");
			System.out.println("7)  Request Password Assistance");
			System.out.println("8)  Lookup User Name");
			System.out.println("9)  Set Auth Tags");
			System.out.println("10) Add Auth Tags");
			System.out.println("11) Remove Auth Tags");

			String opt = in.nextLine();
			
			Long mopt = StringUtil.parseInt(opt);
			
			if (mopt == null)
				continue;
			
			switch (mopt.intValue()) {
			case 0:
				running = false;
				break;
				
			case 1: {
				RecordStruct req = new RecordStruct();
				
				System.out.print("User Name: ");
				String uname = in.nextLine();
				
				req.setField("UserName", uname);				
				
				System.out.print("First Name: ");
				String fname = in.nextLine();
				req.setField("FirstName", fname);
				
				System.out.print("Last Name: ");
				String lname = in.nextLine();
				req.setField("LastName", lname);
				
				System.out.print("Email: ");
				String email = in.nextLine();
				req.setField("Email", email);
				
				System.out.print("Password: ");
				String pass = in.nextLine();
				req.setField("Password", pass);
				
				System.out.print("Locale [optional]: ");
				String locale = in.nextLine();
				
				if (!locale.isEmpty())
					req.setField("Locale", locale);
				
				System.out.print("ISO TimeZone [optional]: ");
				String tzone = in.nextLine();
				
				if (!tzone.isEmpty())
					req.setField("Chronology", "/" + tzone);
				
				ListStruct atags = new ListStruct();
				
				System.out.println("Add Authorization Tags");
				System.out.println("(empty entry to finish)");
				
				while (true) {
					System.out.print("Tag (Admin, SysAdmin, etc): ");
					String tag = in.nextLine();
					
					if (StringUtil.isEmpty(tag))
						break;
					
					atags.addItem(tag);
				}
				
				if (atags.getSize() > 0)
					req.setField("AuthorizationTags", atags);				
				
				capi.sendMessage(new Message("dcCoreDataServices", "Users", "AddUser", req), new DumpCallback("AddUser"));

				break;
			}
			
			case 2: {
				RecordStruct req = new RecordStruct();
				
				System.out.print("User Id: ");
				String uid = in.nextLine();
				
				if (StringUtil.isEmpty(uid))
					break;
				
				req.setField("Id", uid);
				
				System.out.println();
				System.out.println("For any of the following prompts, entering a * means skip this value.");
				
				System.out.print("User Name: ");
				String uname = in.nextLine();
				
				if (!"*".equals(uname)) 
					req.setField("UserName", uname);
				
				System.out.print("First Name: ");
				String fname = in.nextLine();
				
				if (!"*".equals(fname))
					req.setField("FirstName", fname);
				
				System.out.print("Last Name: ");
				String lname = in.nextLine();
				
				if (!"*".equals(lname))
					req.setField("LastName", lname);
				
				System.out.print("Email: ");
				String email = in.nextLine();
				
				if (!"*".equals(email)) 
					req.setField("Email", email);
				
				System.out.print("Password: ");
				String pass = in.nextLine();
				
				if (!"*".equals(pass))
					req.setField("Password", pass);
				
				System.out.print("Locale: ");
				String locale = in.nextLine();
				
				if (!"*".equals(locale))
					req.setField("Locale", locale);
				
				System.out.print("ISO Time Zone: ");
				String tzone = in.nextLine();
				
				if (!"*".equals(tzone))
					req.setField("Chronology", StringUtil.isNotEmpty(tzone) ?  "/" + tzone : null);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Users", "UpdateUser", req), new DumpCallback("UpdateUser"));
				
				break;
			}
			
			case 3: {
				System.out.print("User Id: ");
				String uid = in.nextLine();
				
				if (StringUtil.isEmpty(uid))
					break;				

				RecordStruct req = new RecordStruct(new FieldStruct("Id", uid));
				
				capi.sendMessage(new Message("dcCoreDataServices", "Users", "LoadUser", req), new DumpCallback("LoadUser"));
				
				break;
			}
			
			case 4: {
				System.out.print("User Id: ");
				String uid = in.nextLine();
				
				if (StringUtil.isEmpty(uid))
					break;

				RecordStruct req = new RecordStruct(new FieldStruct("Id", uid));
				
				capi.sendMessage(new Message("dcCoreDataServices", "Users", "RetireUser", req), new DumpCallback("RetireUser"));
				
				break;
			}
			
			case 5: {
				System.out.print("User Id: ");
				String uid = in.nextLine();
				
				if (StringUtil.isEmpty(uid))
					break;

				RecordStruct req = new RecordStruct(new FieldStruct("Id", uid));
				
				capi.sendMessage(new Message("dcCoreDataServices", "Users", "ReviveUser", req), new DumpCallback("ReviveUser"));
				
				break;
			}
			
			case 6:	{
				capi.sendMessage(new Message("dcCoreDataServices", "Users", "ListUsers"), new DumpCallback("ListUsers"));
				
				break;
			}
			
			case 7: {
				System.out.print("User Name: ");
				String uname = in.nextLine();
				
				RecordStruct req = new RecordStruct(new FieldStruct("UserName", uname));
				
				capi.sendMessage(new Message("dcAuth", "Recovery", "InitiateUser", req), new DumpCallback("InitiateUser"));
				
				break;
			}
			
			case 8: {
				System.out.print("User Name: ");
				String uname = in.nextLine();
				
				if (StringUtil.isEmpty(uname))
					break;
				
				RecordStruct req = new RecordStruct(new FieldStruct("UserName", uname));
				
				capi.sendMessage(new Message("dcCoreDataServices", "Users", "UserNameLookup", req), new DumpCallback("UserNameLookup"));
				
				break;
			}
			
			case 9: {
				final ListStruct recs = new ListStruct();
				final ListStruct tags = new ListStruct();
				
				System.out.println("Users");
				
				while (true) {
					System.out.print("Id: ");
					String id = in.nextLine();
					
					if (StringUtil.isEmpty(id))
						break;
					
					recs.addItem(id);
				}								
				
				System.out.println("Authorization Tags (Admin, SysAdmin, PowerUser, etc)");
				
				while (true) {
					System.out.print("Tag: ");
					String tag = in.nextLine();
					
					if (StringUtil.isEmpty(tag))
						break;
					
					tags.addItem(tag);
				}				
				
				RecordStruct req = new RecordStruct(
						new FieldStruct("Users", recs),
						new FieldStruct("AuthorizationTags", tags)
				);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Users", "SetUserAuthTags", req), new DumpCallback("SetUserAuthTags"));
				
				break;
			}
			
			case 10: {
				final ListStruct recs = new ListStruct();
				final ListStruct tags = new ListStruct();
				
				System.out.println("Users");
				
				while (true) {
					System.out.print("Id: ");
					String id = in.nextLine();
					
					if (StringUtil.isEmpty(id))
						break;
					
					recs.addItem(id);
				}								
				
				System.out.println("Authorization Tags (Admin, SysAdmin, PowerUser, etc)");
				
				while (true) {
					System.out.print("Tag: ");
					String tag = in.nextLine();
					
					if (StringUtil.isEmpty(tag))
						break;
					
					tags.addItem(tag);
				}				
				
				RecordStruct req = new RecordStruct(
						new FieldStruct("Users", recs),
						new FieldStruct("AuthorizationTags", tags)
				);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Users", "AddUserAuthTags", req), new DumpCallback("AddUserAuthTags"));
				
				break;
			}
			
			case 11: {
				final ListStruct recs = new ListStruct();
				final ListStruct tags = new ListStruct();
				
				System.out.println("Users");
				
				while (true) {
					System.out.print("Id: ");
					String id = in.nextLine();
					
					if (StringUtil.isEmpty(id))
						break;
					
					recs.addItem(id);
				}								
				
				System.out.println("Authorization Tags (Admin, SysAdmin, PowerUser, etc)");
				
				while (true) {
					System.out.print("Tag: ");
					String tag = in.nextLine();
					
					if (StringUtil.isEmpty(tag))
						break;
					
					tags.addItem(tag);
				}				
				
				RecordStruct req = new RecordStruct(
						new FieldStruct("Users", recs),
						new FieldStruct("AuthorizationTags", tags)
				);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Users", "RemoveUserAuthTags", req), new DumpCallback("RemoveUserAuthTags"));
				
				break;
			}
			
			}
		}		
	}
	
	static public void groupStuff(Scanner in, ApiSession capi) {
		boolean running = true;
		
		while(running) {
			System.out.println();
			System.out.println("-----------------------------------------------");
			System.out.println("   Group Menu");
			System.out.println("-----------------------------------------------");
			System.out.println("0)  Return to main menu");
			System.out.println("1)  Add Group");
			System.out.println("2)  Update Group");
			System.out.println("3)  View Group");
			System.out.println("4)  Retire Group");
			System.out.println("5)  Revive Group");
			System.out.println("6)  List Groups");
			System.out.println("7)  Add User to Group");
			System.out.println("8)  Remove User from Group");
			System.out.println("9)  Set Auth Tags");

			String opt = in.nextLine();
			
			Long mopt = StringUtil.parseInt(opt);
			
			if (mopt == null)
				continue;
			
			switch (mopt.intValue()) {
			case 0:
				running = false;
				break;
				
			case 1: {
				RecordStruct req = new RecordStruct();
				
				System.out.print("Name: ");
				String uname = in.nextLine();
				
				req.setField("Name", uname);				
				
				ListStruct atags = new ListStruct();
				
				System.out.println("Add Authorization Tags");
				System.out.println("(empty entry to finish)");
				
				while (true) {
					System.out.print("Tag (Admin, SysAdmin, etc): ");
					String tag = in.nextLine();
					
					if (StringUtil.isEmpty(tag))
						break;
					
					atags.addItem(tag);
				}
				
				if (atags.getSize() > 0)
					req.setField("AuthorizationTags", atags);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "AddGroup", req), new DumpCallback("AddGroup"));

				break;
			}
			
			case 2: {
				RecordStruct req = new RecordStruct();
				
				System.out.print("Group Id: ");
				String gid = in.nextLine();
				
				if (StringUtil.isEmpty(gid))
					break;
				
				req.setField("Id", gid);

				System.out.println();
				System.out.println("For any of the following prompts, entering a * means skip this value.");
				
				System.out.print("Name: ");
				String name = in.nextLine();
				
				if (!"*".equals(name))
					req.setField("Name", name);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "UpdateGroup", req), new DumpCallback("UpdateGroup"));
				
				break;
			}
			
			case 3: {
				RecordStruct req = new RecordStruct();
				
				System.out.print("Group Id: ");
				String gid = in.nextLine();
				
				if (StringUtil.isEmpty(gid))
					break;
				
				req.setField("Id", gid);
								
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "LoadGroup", req), new DumpCallback("LoadGroup"));

				break;
			}
			
			case 4: {
				RecordStruct req = new RecordStruct();
				
				System.out.print("Group Id: ");
				String gid = in.nextLine();
				
				if (StringUtil.isEmpty(gid))
					break;
				
				req.setField("Id", gid);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "RetireGroup", req), new DumpCallback("RetireGroup"));
				break;
			}
			
			case 5: {
				RecordStruct req = new RecordStruct();
				
				System.out.print("Group Id: ");
				String gid = in.nextLine();
				
				if (StringUtil.isEmpty(gid))
					break;
				
				req.setField("Id", gid);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "ReviveGroup", req), new DumpCallback("ReviveGroup"));
				break;
			}
			
			case 6:	{
				
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "ListGroups"), new DumpCallback("ListGroups"));
				
				break;
			}
			
			case 7: {
				final ListStruct groups = new ListStruct();
				final ListStruct users = new ListStruct();
				
				System.out.println("Groups");
				
				while (true) {
					System.out.print("Id: ");
					String id = in.nextLine();
					
					if (StringUtil.isEmpty(id))
						break;
					
					groups.addItem(id);
				}								
				
				System.out.println("Users");
				
				while (true) {
					System.out.print("Id: ");
					String id = in.nextLine();
					
					if (StringUtil.isEmpty(id))
						break;
					
					users.addItem(id);
				}				
								
				RecordStruct req = new RecordStruct(
						new FieldStruct("Groups", groups),
						new FieldStruct("Users", users)
				);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "AddUsersToGroups", req), new DumpCallback("AddUsersToGroups"));
				
				break;
			}
			
			case 8: {
				final ListStruct groups = new ListStruct();
				final ListStruct users = new ListStruct();
				
				System.out.println("Groups");
				
				while (true) {
					System.out.print("Id: ");
					String id = in.nextLine();
					
					if (StringUtil.isEmpty(id))
						break;
					
					groups.addItem(id);
				}								
				
				System.out.println("Users");
				
				while (true) {
					System.out.print("Id: ");
					String id = in.nextLine();
					
					if (StringUtil.isEmpty(id))
						break;
					
					users.addItem(id);
				}				
								
				RecordStruct req = new RecordStruct(
						new FieldStruct("Groups", groups),
						new FieldStruct("Users", users)
				);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "RemoveUsersFromGroups", req), new DumpCallback("RemoveUsersFromGroups"));
				
				break;
			}
			
			case 9: {
				final ListStruct recs = new ListStruct();
				final ListStruct tags = new ListStruct();
				
				System.out.println("Groups");
				
				while (true) {
					System.out.print("Id: ");
					String id = in.nextLine();
					
					if (StringUtil.isEmpty(id))
						break;
					
					recs.addItem(id);
				}								
				
				System.out.println("Authorization Tags (Admin, SysAdmin, PowerUser, etc)");
				
				while (true) {
					System.out.print("Tag: ");
					String tag = in.nextLine();
					
					if (StringUtil.isEmpty(tag))
						break;
					
					tags.addItem(tag);
				}				
								
				RecordStruct req = new RecordStruct(
						new FieldStruct("Users", recs),
						new FieldStruct("AuthorizationTags", tags)
				);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "SetGroupAuthTags", req), new DumpCallback("SetGroupAuthTags"));
				
				break;
			}
			
			case 10: {
				final ListStruct recs = new ListStruct();
				final ListStruct tags = new ListStruct();
				
				System.out.println("Groups");
				
				while (true) {
					System.out.print("Id: ");
					String id = in.nextLine();
					
					if (StringUtil.isEmpty(id))
						break;
					
					recs.addItem(id);
				}								
				
				System.out.println("Authorization Tags (Admin, SysAdmin, PowerUser, etc)");
				
				while (true) {
					System.out.print("Tag: ");
					String tag = in.nextLine();
					
					if (StringUtil.isEmpty(tag))
						break;
					
					tags.addItem(tag);
				}				
				
				RecordStruct req = new RecordStruct(
						new FieldStruct("Groups", recs),
						new FieldStruct("AuthorizationTags", tags)
				);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "AddGroupAuthTags", req), new DumpCallback("AddGroupAuthTags"));
				
				break;
			}
			
			case 11: {
				final ListStruct recs = new ListStruct();
				final ListStruct tags = new ListStruct();
				
				System.out.println("Groups");
				
				while (true) {
					System.out.print("Id: ");
					String id = in.nextLine();
					
					if (StringUtil.isEmpty(id))
						break;
					
					recs.addItem(id);
				}								
				
				System.out.println("Authorization Tags (Admin, SysAdmin, PowerUser, etc)");
				
				while (true) {
					System.out.print("Tag: ");
					String tag = in.nextLine();
					
					if (StringUtil.isEmpty(tag))
						break;
					
					tags.addItem(tag);
				}				
				
				RecordStruct req = new RecordStruct(
						new FieldStruct("Groups", recs),
						new FieldStruct("AuthorizationTags", tags)
				);
				
				capi.sendMessage(new Message("dcCoreDataServices", "Groups", "RemoveGroupAuthTags", req), new DumpCallback("RemoveGroupAuthTags"));
				
				break;
			}
			
			}
		}		
	}
}
