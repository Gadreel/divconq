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

import java.io.FileInputStream;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import divconq.hub.Hub;
import divconq.hub.HubResources;
import divconq.interchange.FileSystemDriver;
import divconq.lang.FuncResult;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.log.DebugLevel;
import divconq.log.Logger;
import divconq.script.Activity;
import divconq.script.ActivityManager;
import divconq.script.ExecuteState;
import divconq.script.IInstructionCallback;
import divconq.script.Script;
import divconq.session.Session;
import divconq.struct.builder.JsonStreamBuilder;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class TestScript {
	public static void main(String[] args) {
		try {
			HubResources resources = new HubResources();
			resources.setDebugLevel(DebugLevel.Warn);
			OperationResult or = resources.init();
			
			if (or.hasErrors()) {
				Logger.error("Unable to continue, hub resources not properly initialized");
				return;
			}
			
			Hub.instance.start(resources);
			
			OperationContext.useNewRoot();
			
			Session session = Hub.instance.getSessions().create("hub:", null);
			String sid = session.getId();
			
			FileSystemDriver fs = new FileSystemDriver();
			fs.setField("RootFolder", ".");
			
			// TODO rework session.setMountedStore(fs);
			
			System.out.println("Started new session for SessionStore: " + sid);
			
			ActivityManager man = Hub.instance.getActivityManager();
			
			FuncResult<XElement> xres = XmlReader.parse(new FileInputStream("../docs/dcl/file5.dcs.xml"), true); 
			
			if (xres.hasErrors()) {
				System.out.println("Errors: " + xres.getMessages());
				
				Hub.instance.stop();				
				System.out.println("Stopped.");
				return;
			}
			
			XElement script = xres.getResult(); 

			Script srpt = new Script(man);
			OperationResult compilelog = srpt.compile(script);
			
			if (compilelog.hasErrors()) {
				System.out.println("Errors: " + compilelog);
				
				Hub.instance.stop();				
				System.out.println("Stopped.");
				return;
			}
			
			Activity act = new Activity(srpt, sid);
			
			try (Scanner scn = new Scanner(System.in)) {
				System.out.println("Type 'debug' to step through program.");
				
				String op = scn.nextLine();
				
				if (!"debug".equals(op)) {
					act.run();
					System.out.println("Script done. #" + act.getRuntime());
					
					Hub.instance.stop();
					System.out.println("Stopped.");
					
					return;
				}
			
				final Semaphore s = new Semaphore(0);
				
				final IInstructionCallback cb = new IInstructionCallback() {
					@Override
					public void resume() {
						s.release();
					}
				};
				
				while (act.getState() != ExecuteState.Exit) {
					act.runSingleInstruction(cb);
					
					s.acquire();
					
					System.out.println("------------------------------");
		    		
					act.getDebugInfo().toBuilder(new JsonStreamBuilder(System.out, true));	
					
					System.out.println("------------------------------");
					System.out.println("Press enter to continue.");
					
					scn.nextLine();
				}
				
				System.out.println("Script done.");
			}
		} 
		catch (Exception x) {
			System.out.println("Error in loop: " + x);
		}
		
		Hub.instance.stop();
		
		System.out.println("Stopped.");
	}
}
