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
import divconq.filestore.CommonPath;
import divconq.hub.Hub;
import divconq.hub.ILocalCommandLine;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationObserver;
import divconq.util.StringUtil;
import divconq.work.Task;

/**
 *  ONLY works with local session, does not work with remote sessions
 */
public class Gateway implements ILocalCommandLine {
	@Override
	public void run(final Scanner scan, final ApiSession api) {
		HashMap<String, SocketInfo> listeners = new HashMap<>();
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   dcGateway Utilities");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  Add Listener");
				System.out.println("2)  Remove Listener");
				System.out.println("3)  Bus Status");
				System.out.println("4)  Tickle Hub");
				System.out.println("5)  Echo Hub");
				System.out.println("6)  Initiate Upload");
				System.out.println("7)  Initiate Download");

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
					
					Message msg = new Message("Status", "Echo", "Test", data);
					msg.setToHub(to);
					api.sendMessage(msg, new DumpCallback("Echo"));
					break;
				}
				
				case 6: {
					System.out.println("File Name: ");
					final String fname = scan.nextLine();
					
					System.out.println("Save Path ([enter] for root): ");
					final String spath = scan.nextLine();
			    	
			    	final Path src = Paths.get(fname);
			    	CommonPath dest = new CommonPath(spath + "/" + src.getFileName());
			    	
			    	// TODO name
			    	Task uploadtask = TaskFactory.createUploadTask(api, "x", src, dest, null, true);

			    	uploadtask.withObserver(new OperationObserver() {			
			    		@Override
			    		public void amount(OperationContext or, int v) {
							// TODO output upload progress
						}
					});
			    	
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
				
				case 7: {
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
				
				}
			}
			catch (Exception x) {
				System.out.println("Cli Error: " + x);
			}
		}
	}
}
