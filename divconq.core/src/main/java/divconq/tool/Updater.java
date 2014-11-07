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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.HashUtil;
import divconq.util.IOUtil;
import divconq.util.StringUtil;

public class Updater {
	public static Path DEPLOYED_PATH = Paths.get("./config/deployed.json");
	
	public static void main(String[] args) {
		boolean updated = Updater.tryUpdate();
		
		while (updated) {
			System.out.println();
			System.out.println("Checking for additional updates");
			System.out.println();
			
			updated = Updater.tryUpdate();
		}
		
		System.out.println("Updates completed!");
	}
	
	static public FuncResult<RecordStruct> loadDeployed() {
		FuncResult<RecordStruct> res = new FuncResult<>();
		
		FuncResult<CompositeStruct> pres = CompositeParser.parseJson(DEPLOYED_PATH);
		
		if (!pres.hasErrors())
			res.setResult((RecordStruct) pres.getResult());
		
		return res;
	}
	
	static public OperationResult saveDeployed(RecordStruct deployed) {
		OperationResult res = new OperationResult();
		IOUtil.saveEntireFile(Updater.DEPLOYED_PATH, deployed.toPrettyString()); 
		return res;
	}
	
	static public boolean tryUpdate() {
		@SuppressWarnings("resource")
		final Scanner scan = new Scanner(System.in);
		
		FuncResult<RecordStruct> ldres = Updater.loadDeployed();
		
		if (ldres.hasErrors()) {
			System.out.println("Error reading deployed.json file: " + ldres.getMessage());
			return false;
		}
		
		RecordStruct deployed = ldres.getResult();
		
		String ver = deployed.getFieldAsString("Version");
		String packfolder = deployed.getFieldAsString("PackageFolder");
		String packprefix = deployed.getFieldAsString("PackagePrefix");
		
		if (StringUtil.isEmpty(ver) || StringUtil.isEmpty(packfolder)) {
			System.out.println("Error reading deployed.json file: Missing Version or PackageFolder");
			return false;
		}
		
		if (StringUtil.isEmpty(packprefix))
			packprefix = "DivConq";
		
		System.out.println("Current Version: " + ver);
		
		Path packpath = Paths.get(packfolder);
		
		if (!Files.exists(packpath) || !Files.isDirectory(packpath)) {
			System.out.println("Error reading PackageFolder - it may not exist or is not a folder.");
			return false;
		}
		
		File pp = packpath.toFile();
		RecordStruct deployment = null;
		File matchpack = null;
		
		for (File f : pp.listFiles()) {
			if (!f.getName().startsWith(packprefix + "-") || !f.getName().endsWith("-bin.zip"))
				continue;
			
			System.out.println("Checking: " + f.getName());
			
			// if not a match before, clear this
			deployment = null;
			
			try {
				ZipFile zf = new ZipFile(f);
				
				Enumeration<ZipArchiveEntry> entries = zf.getEntries();
				
				while (entries.hasMoreElements()) {
					ZipArchiveEntry entry = entries.nextElement();
					
					if (entry.getName().equals("deployment.json")) {
						//System.out.println("crc: " + entry.getCrc());
						
						FuncResult<CompositeStruct> pres = CompositeParser.parseJson(zf.getInputStream(entry));
						
						if (pres.hasErrors()) {
							System.out.println("Error reading deployment.json file");
							break;
						}
						
						deployment = (RecordStruct) pres.getResult();
						
						break;
					}
				}
				
				zf.close();
			}
			catch (IOException x) {
				System.out.println("Error reading deployment.json file: " + x);
			}
			
			if (deployment != null) {
				String fndver = deployment.getFieldAsString("Version");
				String fnddependson = deployment.getFieldAsString("DependsOn");
				
				if (ver.equals(fnddependson)) {
					System.out.println("Found update: " + fndver);
					matchpack = f;
					break;
				}
			}
		}
		
		if ((matchpack == null) || (deployment == null)) {
			System.out.println("No updates found!");
			return false;
		}
		
		String fndver = deployment.getFieldAsString("Version");
		String umsg = deployment.getFieldAsString("UpdateMessage");
		
		if (StringUtil.isNotEmpty(umsg)) {
			System.out.println("========================================================================");
			System.out.println(umsg);
			System.out.println("========================================================================");
		}

		System.out.println();
		System.out.println("Do you want to install?  (y/n)");
		System.out.println();
		
		String p = scan.nextLine().toLowerCase();
		
		if (!p.equals("y"))
			return false;
		
		System.out.println();
		
		System.out.println("Intalling: " + fndver);

		Set<String> ignorepaths = new HashSet<>();
		
		ListStruct iplist = deployment.getFieldAsList("IgnorePaths");
		
		if (iplist != null) {
			for (Struct df : iplist.getItems()) 
				ignorepaths.add(df.toString());
		}
		
		ListStruct dflist = deployment.getFieldAsList("DeleteFiles");
		
		// deleting
		if (dflist != null) {
			for (Struct df : dflist.getItems()) {
				Path delpath = Paths.get(".", df.toString());
				
				if (Files.exists(delpath)) {
					System.out.println("Deleting: " + delpath.toAbsolutePath());
					
					try {
						Files.delete(delpath);
					} 
					catch (IOException x) {
						System.out.println("Unable to Delete: " + x);
					}
				}
			}
		}
		
		// copying updates
		
		System.out.println("Checking for updated files: ");
		
		try {
			@SuppressWarnings("resource")
			ZipFile zf = new ZipFile(matchpack);
			
			Enumeration<ZipArchiveEntry> entries = zf.getEntries();
			
			while (entries.hasMoreElements()) {
				ZipArchiveEntry entry = entries.nextElement();
				
				String entryname = entry.getName().replace('\\', '/');
				boolean xfnd = false;
				
				for (String exculde : ignorepaths) 
					if (entryname.startsWith(exculde)) {
						xfnd = true;
						break;
					}
				
				if (xfnd)
					continue;
				
				System.out.print(".");
				
				Path localpath = Paths.get(".", entryname);

				if (entry.isDirectory()) {
					if (!Files.exists(localpath)) 
						Files.createDirectories(localpath);
				}
				else {
					boolean hashmatch = false;
					
					if (Files.exists(localpath)) {
						String local = null;
						String update = null;
						
						try (InputStream lin = Files.newInputStream(localpath)) {
							local = HashUtil.getMd5(lin);
						}
						
						
						try (InputStream uin = zf.getInputStream(entry)) {
							update = HashUtil.getMd5(uin);
						}
						
						hashmatch = (StringUtil.isNotEmpty(local) && StringUtil.isNotEmpty(update) && local.equals(update));
					}
					
					if (!hashmatch) {
						System.out.print("[" + entryname + "]");
						
						try (InputStream uin = zf.getInputStream(entry)) {
							Files.createDirectories(localpath.getParent());
							
							Files.copy(uin, localpath, StandardCopyOption.REPLACE_EXISTING);
						}
						catch (Exception x) {
							System.out.println("Error updating: " + entryname + " - " + x);
							return false;
						}
					}
				}
			}
			
			zf.close();
		}
		catch (IOException x) {
			System.out.println("Error reading update package: " + x);
		}

		// updating local config
		deployed.setField("Version", fndver);
		
		OperationResult svres = Updater.saveDeployed(deployed);

		if (svres.hasErrors()) {
			System.out.println("Intalled: " + fndver + " but could not update deployed.json.  Repair the file before continuing.\nError: " + svres.getMessage());
			return false;
		}
		
		System.out.println("Intalled: " + fndver);
		
		return true;
	}
}
