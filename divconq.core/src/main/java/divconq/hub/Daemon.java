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

import java.util.Scanner;

import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.log.DebugLevel;
import divconq.log.Logger;

public class Daemon implements org.apache.commons.daemon.Daemon {
	protected DaemonContext procCtx = null;

	public static void main(String[] args) {
		try {
			Daemon.startService(args);
			
			try (Scanner scan = new Scanner(System.in)) {
				System.out.println("Press enter to end Daemon");
				scan.nextLine();
			}
		}
		catch (Exception x) {
			
		}
		
		Daemon.stopService(args);
	}
	
	public static void startService(String[] args) {
		String deployment = (args.length > 0) ? args[0] : null;
		String squadid = (args.length > 1) ? args[1] : null;
		String hubid = (args.length > 2) ? args[2] : null;
		
		OperationContext.useHubContext();
		
		HubResources resources = new HubResources(deployment, squadid, null, hubid);

		resources.setDebugLevel(DebugLevel.Info);
		OperationResult or = resources.init();
		
		if (or.hasErrors()) {
			Logger.error("Unable to continue, hub resources not properly configured");
			return;
		}
	
		or = Hub.instance.start(resources);
		
		if (or.hasErrors()) {
			Logger.error("Unable to continue, hub not properly started, please see logs");
			Hub.instance.stop();
			System.exit(1);
			return;
		}
		
		Logger.info("Daemon started");
    }	
	
	public static void stopService(String[] args) {
		OperationContext.useHubContext();
		
		Logger.info("Daemon stopping");
		
		Hub.instance.stop();
	}
	
	@Override
	public void start() throws Exception {
		Daemon.startService(this.procCtx.getArguments());
	}

	@Override
	public void stop() throws Exception {
		Daemon.stopService(this.procCtx.getArguments());
	}

	@Override
	public void destroy() {
		this.procCtx = null;
	}

	@Override
	public void init(DaemonContext ctx) throws DaemonInitException, Exception {
		this.procCtx = ctx;
	}
}
