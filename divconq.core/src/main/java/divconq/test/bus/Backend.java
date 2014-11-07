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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;

import divconq.api.ApiSession;
import divconq.api.DumpCallback;
import divconq.api.tasks.TaskFactory;
import divconq.bus.Message;
import divconq.bus.net.SocketInfo;
import divconq.hub.Foreground;
import divconq.hub.Hub;
import divconq.hub.ILocalCommandLine;
import divconq.interchange.CommonPath;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationObserver;
import divconq.util.StringUtil;
import divconq.work.Task;

/**
 *  ONLY works with local session, does not work with remote sessions
 */
public class Backend implements ILocalCommandLine {
	@Override
	public void run(final Scanner scan, final ApiSession api) {
		HashMap<String, SocketInfo> listeners = new HashMap<>();
		HashMap<String, SocketInfo> connectors = new HashMap<>();
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   dcBackend Utilities");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  Add Listener");
				System.out.println("2)  Remove Listener");
				System.out.println("3)  Bus Status");
				System.out.println("4)  Tickle Hub");
				System.out.println("5)  Echo Hub");
				System.out.println("6)  Add Connector");
				System.out.println("7)  Remove Connector");
				System.out.println("8)  Encrypt/Util");
				System.out.println("9)  Initiate Upload");
				System.out.println("10) Initiate Download");
				System.out.println("12) Verify Self");

				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					running = false;
					break;
					
				case 1: {
					System.out.println("Add port: ");
					String port = scan.nextLine();
					
					if (listeners.containsKey(port))
						System.out.println("already listening");
					else {
						SocketInfo info = SocketInfo.buildLoopback((int)StringUtil.parseInt(port, 0), true);
						
						if (info.getPort() == 0)
							System.out.println("bad port");
						else {
							Hub.instance.getBus().addListener(info);
							listeners.put(port, info);
							System.out.println("listener added");
						}
					}
					
			        break;
				}
				case 2: {
					System.out.println("Remove port: ");
					String port = scan.nextLine();
					
					if (!listeners.containsKey(port))
						System.out.println("not listening to that port");
					else {
						SocketInfo info = listeners.remove(port);
						
						if (info == null)
							System.out.println("bad port");
						else {
							Hub.instance.getBus().removeListener(info);
							System.out.println("listener removed");
						}
					}
					
			        break;
				}
				case 3: {
					Hub.instance.getBus().dumpInfo();					
					
					break;
				}
				
				case 4: {
					System.out.println("To Hub: ");
					String to = scan.nextLine();
					
					Message msg = new Message("Status", "Tickle", "Test");
					msg.setToHub(to);
					api.sendForgetMessage(msg);
					break;
				}
				
				case 5: {
					System.out.println("To Hub: ");
					String to = scan.nextLine();
					
					System.out.println("Message: ");
					String data = scan.nextLine();
					
					Message msg = new Message("Status", "Echo", "Test", data + " - av73Dw??gT80Hgt");
					msg.setToHub(to);
					api.sendMessage(msg, new DumpCallback("Echo"));
					break;
				}
				case 6: {
					System.out.println("Add hubid: ");
					String hubid = scan.nextLine();
					
					System.out.println("Add port: ");
					String port = scan.nextLine();
					
					if (connectors.containsKey(hubid))
						System.out.println("already connecting");
					else {
						SocketInfo info = SocketInfo.buildLoopback((int)StringUtil.parseInt(port, 0), true);
						info.setHubId(hubid);
						
						if ((info.getPort() == 0) || StringUtil.isEmpty(hubid))
							System.out.println("bad connector");
						else {
							Hub.instance.getBus().addConnector(info);
							connectors.put(hubid, info);
							System.out.println("connector added");
						}
					}
					
			        break;
				}
				case 7: {
					System.out.println("Remove hubid: ");
					String hubid = scan.nextLine();
					
					if (!connectors.containsKey(hubid))
						System.out.println("not connecting to that port");
					else {
						SocketInfo info = connectors.remove(hubid);
						
						if (info == null)
							System.out.println("bad connector");
						else {
							Hub.instance.getBus().removeConnector(info);
							System.out.println("connector removed");
						}
					}
					
			        break;
				}
				case 8: {
					System.out.println();
					
					Foreground.utilityMenu(scan);
					
					break;
				}
				
				case 9: {
					System.out.println("File Name: ");
					final String fname = "D:\\dev\\divconq\\hub\\lib\\guava-14.0-rc1.jar"; //scan.nextLine();
					
					System.out.println("Save Path ([enter] for root): ");
					final String spath = scan.nextLine();
			    	
			    	final Path src = Paths.get(fname);
			    	CommonPath dest = new CommonPath(spath + "/" + src.getFileName());
			    	
			    	// TODO name
			    	Task uploadtask = TaskFactory.createUploadTask(api, "x", src, dest, null, true);
			    	
			    	Hub.instance.getWorkPool().submit(uploadtask, new OperationObserver() {
						@Override
						public void completed(OperationContext or) {
							if (or.hasErrors())
								System.out.println("Upload failed!");
							else
								System.out.println("Upload worked!");
						}
					});
			    	
					break;
				}
				
				case 10: {
					/* TODO
					System.out.println("File Name: ");
					final String spath = scan.nextLine();
					
			    	final CommonPath src = new CommonPath(spath);
					
					System.out.println("Save Path: ");
					final Path dest = Paths.get(scan.nextLine(), src.getFileName());
			    	
					DataStreamApi dsapi = new DataStreamApi(api);

					dsapi.simpleDownloadAndVerify(src, dest, new OperationCallback() {
						// initializer is optional, it supports the progress bar, etc
						{
							this.addObserver(new Observer() {						
								@Override
								public void update(Observable or, Object area) {
									// TODO output progress
								}
							});
						}
						
						@Override
						public void callback() {
					    	if (this.hasErrors()) {
					    		System.out.println("Download Error: " + this.getMessage());
					    		return;
					    	}
							
					    	System.out.println("Download complete!");
						}
					});
			    	*/
					
					break;
				}
				
				
				case 11: {
					Message msg = new Message("Status", "Echo", "Test", "Test 1");
					Message rmsg = api.sendMessage(msg);
					
					System.out.println("Response 1: " + rmsg.getFieldAsString("Body"));
					
					msg = new Message("Status", "Echo", "Test", "Test 2");
					rmsg = api.sendMessage(msg);
					
					System.out.println("Response 2: " + rmsg.getFieldAsString("Body"));
					
					msg = new Message("Status", "Echo", "Test", "Test 3");
					rmsg = api.sendMessage(msg);
					
					System.out.println("Response 3: " + rmsg.getFieldAsString("Body"));
					
					msg = new Message("Status", "Echo", "Test", "Test 4");
					rmsg = api.sendMessage(msg);
					
					System.out.println("Response 4: " + rmsg.getFieldAsString("Body"));
					
					break;
				}
				
				case 12: {
					Message msg = new Message("Status", "Info", "Test");
					api.sendMessage(msg, new DumpCallback("Info"));
					break;
				}
				
				}
			}
			catch (Exception x) {
				System.out.println("Cli Error: " + x);
			}
		}
	}
}
