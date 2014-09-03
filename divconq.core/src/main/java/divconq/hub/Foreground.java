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
package divconq.hub;

import java.io.Console;
import java.util.Scanner;

import org.joda.time.DateTime;

import divconq.api.ApiSession;
import divconq.bus.Bus;
import divconq.hub.Hub;
import divconq.hub.HubResources;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.log.DebugLevel;
import divconq.log.Logger;
import divconq.mod.ModuleLoader;
import divconq.util.StringUtil;
import divconq.work.TaskRun;
import divconq.work.WorkBucket;
import divconq.work.WorkPool;
import divconq.xml.XElement;

/*
 */
public class Foreground {
	public static void main(String[] args) {
		String deployment = (args.length > 0) ? args[0] : "default";
		String squadid = (args.length > 1) ? args[1] : "default";
		String hubid = (args.length > 2) ? args[2] : "00100";
		
		HubResources resources = new HubResources(deployment, hubid, squadid);

		resources.setDebugLevel(DebugLevel.Info);
		OperationResult or = resources.init();
		
		if (or.hasErrors()) {
			Logger.error("Unable to continue, hub resources not properly configured");
			return;
		}
		
		or = Hub.instance.start(resources);
		
		if (or.hasErrors()) {
			Logger.error("Unable to continue, hub resources not properly initialized");			
			Hub.instance.stop();
			return;
		}
		
		OperationContext.useNewRoot();
		
		final Scanner scan = new Scanner(System.in, "UTF-8");
		
		XElement croot = resources.getConfig();
		
		XElement cliel = croot.find("CommandLine");
		
		if ((cliel == null) || !cliel.hasAttribute("ClientClass")) {
			System.out.println("Missing CommadLine configuration");
		}
		else {
			ModuleLoader loader = new ModuleLoader(Hub.class.getClassLoader());
			loader.init(or, cliel);
			ILocalCommandLine cli = (ILocalCommandLine) loader.getInstance(cliel.getAttribute("ClientClass"));
			
			ApiSession capi = null;
			boolean auth = true;
			
			String mode = cliel.getAttribute("Mode", "root");
			String sess = cliel.getAttribute("Session");
			
			if ("root".equals(mode)) {
				if (StringUtil.isEmpty(sess))
					capi = ApiSession.createLocalSession(mode);
				else
					capi = ApiSession.createSessionFromConfig(sess);  //LocalSession("root");
				
				//System.out.print("Password: ");
				//String pass = scan.nextLine();
				
				Console cons = null;
				String pass = null; 
				char[] passwd = null;
				 
				if ((cons = System.console()) != null &&
				    (passwd = cons.readPassword("Password:")) != null) {
					pass = new String(passwd);
				}
				else {
					System.out.print("Password: ");
					pass = scan.nextLine();
				}
				
				//System.out.println("Entered: " + pass);
				
				if (StringUtil.isEmpty(pass) || "0".equals(pass))
					System.out.println("Stopping.");
				else if (capi.startSession("root", pass)) 
					cli.run(scan, capi);
				else
					System.out.println("Error logging in session");
			}
			else {
				while (true) {			
					System.out.print("Domain (e.g. root): ");
					String domain = scan.nextLine();
					
					if ("-".equals(domain)) {
						System.out.println("--------------------------------------------");
						continue;
					}
					
					if ("0".equals(domain)) {
						auth = false;
						break;
					}
					
					if ("*".equals(domain)) {
						capi = ApiSession.createSessionFromConfig(sess);  // LocalSession("root");
				
						if (capi.startSession("root", "A1s2d3f4"))
							break;
					}
					
					capi = ApiSession.createSessionFromConfig(sess);  //  LocalSession(domain);
					//capi = CoreApi.createSessionFromConfig(domain);
					
					System.out.print("UserName: ");
					String user = scan.nextLine();
					
					//System.out.print("Password: ");
					//String pass = scan.nextLine();
					
					Console cons = null;
					String pass = null; 
					char[] passwd = null;
					 
					if ((cons = System.console()) != null &&
					    (passwd = cons.readPassword("Password:")) != null) {
						pass = new String(passwd);
					}
					else {
						System.out.print("Password: ");
						pass = scan.nextLine();
					}
			
					if (capi.startSession(user, pass))
						break;
	
					System.out.println("Failed");
				}
	
				if (auth)
					cli.run(scan, capi);
			}

			if (capi != null)
				capi.stop();
			
			Hub.instance.stop();
		}
	}
		
	static public void utilityMenu(Scanner scan) { 	
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   Hub " + Hub.instance.getResources().getHubId() + " Utility Menu");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  Encrypt Setting");
				System.out.println("2)  Hash Setting");
				//System.out.println("3)  Decrypt Setting");
				System.out.println("4)  System Status");

				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					running = false;
					break;
				case 1: {
					System.out.println("Enter setting to encrypt:");
					String val = scan.nextLine();
					
					System.out.println("Result: "+ Hub.instance.getClock().getObfuscator().encryptStringToHex(val));
					break;
				}
				case 2: {
					System.out.println("Enter setting to hash:");
					String val = scan.nextLine();
					
					System.out.println("Result: "+ Hub.instance.getClock().getObfuscator().hashStringToHex(val));
					break;
				}
				case 793: {
					System.out.println("Enter setting to decrypt:");
					String val = scan.nextLine();
					
					System.out.println("Result: "+ Hub.instance.getClock().getObfuscator().decryptHexToString(val));
					break;
				}
				case 4: {
					Foreground.dumpStatus();
					break;
				}
				}
			}
			catch(Exception x) {
				System.out.println("CLI error: " + x);
			}
		}		
	}
	
	static public void dumpStatus() {
		WorkPool pool = Hub.instance.getWorkPool();
		
		System.out.println(" ------------------------------------------- ");
		//System.out.println("        Pool: " + pool.getName());
		//System.out.println("    Back Log: " + pool.backlog());
		
		//System.out.println("  Busy Level: " + pool.howBusy());
		
		System.out.println("   # Threads: " + pool.threadCount());
		System.out.println("   # Created: " + pool.threadsCreated());
		System.out.println("      # Hung: " + pool.threadsHung());
		System.out.println(" ------------------------------------------- ");
		
		for (WorkBucket bucket : pool.getBuckets()) {
			System.out.println(" Bucket:        " + bucket.getName());
			System.out.println(" - In Progress: " + bucket.inprogress());
			
			for (TaskRun task : bucket.tasksInProgress()) {
				System.out.println(" -- " + task.getTask().getId());
			}
		}
		
		SysReporter rep = Hub.instance.getClock().getSlowSysReporter();
		
		System.out.println(" Slow Sys Work Status: " + rep.getStatus() + " @ " + new DateTime(rep.getLast()));
		
		rep = Hub.instance.getClock().getFastSysReporter();
		
		System.out.println(" Fast Sys Work Status: " + rep.getStatus() + " @ " + new DateTime(rep.getLast()));
		
		Bus b = Hub.instance.getBus();
		
		b.dumpInfo();
	}
}
