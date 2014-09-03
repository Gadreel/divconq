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
package divconq.tool;

import java.io.File;
import java.util.Scanner;

import divconq.hub.Clock;
import divconq.lang.FuncResult;
import divconq.lang.OperationResult;
import divconq.util.FileUtil;
import divconq.util.ISettingsObfuscator;
import divconq.util.StringUtil;
import divconq.xml.XAttribute;
import divconq.xml.XElement;
import divconq.xml.XmlReader;
import divconq.xml.XmlWriter;

/**
 * TODO out of date, replace
 * 
 * Utility to construct a Project from a local (developer) repository.  Allows the user
 * to pick the packages to use and to setup the database connection.  Should only be run 
 * by developer using a local repository, will not work otherwise.
 * 
 * A Project is required both by developers running in developer mode and by deployed 
 * production Hubs.  Projects hold configuration, server keys and other project specific
 * resources.
 * 
 * @author Andy
 *
 */
public class Configure {
	/*
	 * Install Developer
	 * 
	 * hub/
	 * 		bin/
	 * 			run.bat						[configure, sync, testdb or run which takes developers class name as 2nd argument]
	 * 			run.sh
	 * 
	 * 		lib/
	 * 			third party*.jar
	 * 			divconq.core.jar
	 * 
	 * 			licenses/
	 * 				*.txt
	 * 
	 * 		packages/
	 * 			dcCore/
	 * 				m/
	 * 					dcConn.m
	 * 					dcShema.m
	 * 					dcInstall.m
	 * 					dcUninstall.m
	 * 					dcStrUtil.m
	 * 					dcTimeUtil.m
	 * 				all/
	 * 					dictionary/
	 * 						dictionary.xml
	 * 					schema/
	 * 						schema.xml
	 * 					lib/
	 * 						divconq.core.jar
	 * 
	 * 			dcTest/
	 * 				m/
	 * 					dcTest.m
	 * 					dcToyTest.m
	 * 				all/
	 * 					dictionary/
	 * 						dictionary.xml
	 * 					schema/
	 * 						schema.xml
	 * 
	 * 		LICENSE.txt
	 * 		NOTICE.txt
	 * 		README.txt
	 * 
	 * 
	 * ---------------------------------------------------
	 * 
	 * Configure adds:
	 * 
	 * 		project/
	 * 			packages.xml				[if not present, prompt for packages to use]
	 * 
	 * 			private/
	 * 				config.xml				[if not present, create and init Id and Feed - prompt for database connection details]
	 * 
	 * 				resource/				[if using a ssh client key]
	 * 					[dbclientkey].pem
	 * 
	 * 			public/
	 * 				config.xml
	 * 
	 * 	TODO, copy libs, upload routines, start install - create repo
	 * 
	 * TODO local/remote repo - where to get files
	 * 
	 * TODO configure for deployment - not just devmode
	 * 
	 */
	
	/**
	 * Utility must be run from the application base directory (parent to "bin")
	 * 
	 * @param args ignored
	 */
	public static void main(String[] args) {
		File pkgs = new File("./packages");
		
		if (!pkgs.exists()) {
			System.out.println("This deployment does not have any local repository, unable to configure.");
			return;
		}
		
		Scanner scan = new Scanner(System.in);
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   DivConq Configure Menu");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  Install Project");
				System.out.println("2)  Configure Database");
				System.out.println("3)  Encrypt Setting");
				System.out.println("4)  Hash Value");

				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					running = false;
					break;
					
				case 1:
					Configure.install(scan);					
					break;
					
				case 2:
					Configure.db(scan);					
					break;
					
				case 3:
					Configure.encrypt(scan);					
					break;
					
				case 4:
					Configure.hash(scan);					
					break;
				}
						
			}
			catch(Exception x) {
				System.out.println("CLI error: " + x);
			}
		}		
		
	}

	/**
	 * Assemble the packages and config file.
	 * 
	 * @param scan user input scanner
	 */
	private static void install(Scanner scan) {
		File config = new File("./project/private/config.xml");
		
		if (config.exists()) {
			System.out.println("Installing will wipe out previous project, are you sure you want to continue?");
			System.out.print("[yes/no]: ");
			
			if (!"yes".equals(scan.nextLine().toLowerCase()))
				return;
		}
		
		File proj = new File("./project");
		
		FileUtil.deleteDirectory(proj.toPath());
		
		proj.mkdirs();
		
		System.out.println();
		System.out.println("Adding packages, type 'yes' for packages you wish to include: ");
				
		File pkgs = new File("./packages");
		XElement xpkgs = new XElement("Packages");
		
		// TODO order the packages, not just in order found
		for (File pkg : pkgs.listFiles()) {
			if (pkg.isDirectory() || pkg.getName().endsWith(".zip")) {
				String name = pkg.getName();
				
				if (name.endsWith(".zip"))
					name = name.substring(0, name.length() - 4);
				
				System.out.println("Found: " + name);
				System.out.print("[yes/no]: ");
				
				if ("yes".equals(scan.nextLine().toLowerCase()))
					xpkgs.add(new XElement("Package", new XAttribute("Name", name)));
			}
		}
		
		XmlWriter.writeToFile(xpkgs, "./project/packages.xml");
			
		File priv = new File("./project/private");
		priv.mkdirs();
		
		File pub = new File("./project/public");
		pub.mkdirs();

		XElement conf = new XElement("Config");		
		XElement clock = new XElement("Clock");
		
		// TODO prompt for custom "TimerClass" - add that class name to clock
		
		OperationResult or = new OperationResult();
		
		Clock tclock = new Clock();
		tclock.init(or, clock);
		
		// TODO check "or" result
		
		ISettingsObfuscator crypto1 = tclock.getObfuscator();
		
		crypto1.configure(clock);
		
		conf.add(clock);
		
		XmlWriter.writeToFile(conf, "./project/private/config.xml");
		XmlWriter.writeToFile(conf, "./project/public/config.xml");
		
		Configure.db(scan);
	}

	/**
	 * configure the database settings (only for private safety area)
	 * 
	 * @param scan user input scanner
	 */
	private static void db(Scanner scan) {
		File config = new File("./project/private/config.xml");
		
		if (!config.exists()) {
			System.out.println("Missing project config, please install first.");
			return;
		}
		
		FuncResult<XElement> xres = XmlReader.loadFile(config, false); 
		
		if (xres.hasErrors()) {
			System.out.println("Project config is not well formed Xml, please re-install.");
			System.out.println("Errors: " + xres.getMessages());
			return;
		}
		
		XElement conf = xres.getResult();
		
		XElement clock = conf.find("Clock");
		
		if ((clock == null) || !clock.hasAttribute("Id")) {
			System.out.println("Project config is invlaid, missing Clock, please re-install.");
			return;
		}		
		
		OperationResult or = new OperationResult();
		
		Clock tclock = new Clock();
		tclock.init(or, clock);
		
		// TODO check "or" errors
		
		ISettingsObfuscator crypto1 = tclock.getObfuscator();
		
		// remove old database settings - TODO support more than one connection and support editing instead of simple replace
		
		XElement xdb1 = conf.find("Database");

		if (xdb1 == null) {
			xdb1 = new XElement("Database");
			conf.add(xdb1);
		}
		
		XElement xdb = xdb1.find("Connect");

		if (xdb == null) {
			xdb = new XElement("Connect", new XAttribute("Method", "Ssh"));
			xdb1.add(xdb);
		}
		
		System.out.println();
		System.out.println("Enter database connection settings.");

		System.out.print("Host: ");
		String host = scan.nextLine();
		
		if (StringUtil.isEmpty(host))
			return;
		
		xdb.setAttribute("Host", host);
		
		System.out.print("Port [enter for 22]: ");
		String port = scan.nextLine();
		
		if (StringUtil.isNotEmpty(port)) 
			xdb.setAttribute("Port", port);
		
		System.out.print("User: ");
		String user = scan.nextLine();
		
		if (StringUtil.isEmpty(user))
			return;
		
		xdb.setAttribute("User", user);
		
		System.out.print("Name of ssh key file [leave blank if not using client ssh key]: ");
		String keyfilename = scan.nextLine();
		
		if (StringUtil.isNotEmpty(keyfilename)) {
			xdb.removeAttribute("Password");
			xdb.removeAttribute("Passphrase");
			
			if (!keyfilename.endsWith(".pem"))
				keyfilename = keyfilename + ".pem";
			
			System.out.println("Place the ssh key file in the correct project resource path.");
			System.out.println("./project/private/resource/" + keyfilename);
			
			while (true) {
				System.out.println("Press enter after placing the file and we'll confirm the file");
				scan.nextLine();
				
				File keyf = new File("./project/private/resource/" + keyfilename);
				
				if (keyf.exists())
					break;
			}

			System.out.println("Key file confirmed.");
			System.out.println();
			
			xdb.setAttribute("KeyFile", keyfilename);
			
			System.out.print("Client Key Passphrase [leave blank if none]: ");
			String passphrase = scan.nextLine();
			
			if (StringUtil.isNotEmpty(passphrase))
				xdb.setAttribute("Passphrase", crypto1.encryptStringToHex(passphrase));
		}
		else {
			xdb.removeAttribute("Passphrase");
			xdb.removeAttribute("KeyFile");
			
			System.out.print("Password: ");
			String password = scan.nextLine();
			
			if (StringUtil.isNotEmpty(password))
				xdb.setAttribute("Password", crypto1.encryptStringToHex(password));
		}
		
		XmlWriter.writeToFile(conf, "./project/private/config.xml");
	}	

	private static void encrypt(Scanner scan) {
		File config = new File("./project/private/config.xml");
		
		if (!config.exists()) {
			System.out.println("Missing project config, please install first.");
			return;
		}
		
		FuncResult<XElement> xres = XmlReader.loadFile(config, false); 
		
		if (xres.hasErrors()) {
			System.out.println("Project config is not well formed Xml, please re-install.");
			System.out.println("Errors: " + xres.getMessages());
			return;
		}
		
		XElement conf = xres.getResult();
		
		XElement clock = conf.find("Clock");
		
		if ((clock == null) || !clock.hasAttribute("Id")) {
			System.out.println("Project config is invlaid, missing Clock, please re-install.");
			return;
		}
		
		OperationResult or = new OperationResult();
		
		Clock tclock = new Clock();
		tclock.init(or, clock);

		// TODO check "or" results
		
		ISettingsObfuscator crypto1 = tclock.getObfuscator();

		System.out.print("Value: ");
		String password = scan.nextLine();
		
		System.out.println("Result: " + crypto1.encryptStringToHex(password));
	}	

	private static void hash(Scanner scan) {
		File config = new File("./project/private/config.xml");
		
		if (!config.exists()) {
			System.out.println("Missing project config, please install first.");
			return;
		}
		
		FuncResult<XElement> xres = XmlReader.loadFile(config, false); 
		
		if (xres.hasErrors()) {
			System.out.println("Project config is not well formed Xml, please re-install.");
			System.out.println("Errors: " + xres.getMessages());
			return;
		}
		
		XElement conf = xres.getResult();
		
		XElement clock = conf.find("Clock");
		
		if ((clock == null) || !clock.hasAttribute("Id")) {
			System.out.println("Project config is invlaid, missing Clock, please re-install.");
			return;
		}
		
		OperationResult or = new OperationResult();
		
		Clock tclock = new Clock();
		tclock.init(or, clock);

		// TODO check "or" results
		
		ISettingsObfuscator crypto1 = tclock.getObfuscator();

		System.out.print("Value: ");
		String password = scan.nextLine();
		
		System.out.println("Result: " + crypto1.hashStringToHex(password));
	}	
}
