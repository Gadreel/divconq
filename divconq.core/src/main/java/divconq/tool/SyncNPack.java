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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

import divconq.lang.op.FuncResult;
import divconq.util.HashUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class SyncNPack {
    static public void main(String[] args) {
		/*
		Path dpath1 = Paths.get("D:\\dev\\divconq\\builds\\ICANNWorkflowEngine\\lib\\abc.jar");
		Path dpath2 = Paths.get("D:\\dev\\divconq\\builds", "ICANNWorkflowEngine\\lib\\abc.jar");
		
		boolean exists1 = Files.exists(dpath1);
		
		System.out.println("1: " + exists1);
		
		boolean exists2 = Files.exists(dpath2);
		
		System.out.println("2: " + exists2);
		
		AttributeView bs = Files.getFileAttributeView(dpath1, BasicFileAttributeView.class);
		
		System.out.println("3: " + bs.name());
		
		System.exit(0);
		*/
		
		if (args.length == 0) {
			System.out.println("Settings folder parameter missing!");
			System.exit(1);
			return;
		}
		
		String setfolder = args[0];
		
		System.out.println("Settings folder: " + setfolder);
		
		File sfolder = new File(setfolder);
		
		FuncResult<XElement> config = XmlReader.loadFile(new File(sfolder, "config.xml"), true);
		
		if (config.hasErrors()) {
			System.out.println("Skipping config file because of parse errors: " + config.getMessage());
			return;
		}
		
		XElement croot = config.getResult();
		
		String orgfolder = croot.getAttribute("Origin");
		
		System.out.println("----------------------------------------------------");
		System.out.println("   Master Source: " + orgfolder);
		System.out.println("----------------------------------------------------");
		
		final Path orgpath = Paths.get(orgfolder);
		
		for (File f : sfolder.listFiles()) {
			if (!f.isFile() || !f.getName().endsWith(".xml") || "config.xml".equals(f.getName()))
				continue;
			
			FuncResult<XElement> settings = XmlReader.loadFile(f, true);
			
			if (settings.hasErrors()) {
				System.out.println("Skipping file because of parse errors: " + f.getName());
				continue;
			}
			
			XElement root = settings.getResult();
			
			System.out.println("----------------------------------------------------");
			System.out.println("   Starting Dest: " + root.getAttribute("Title"));
			System.out.println("----------------------------------------------------");
			
			String buildpath = root.getAttribute("Path");
			
			if (StringUtil.isEmpty(buildpath)) {
				System.out.println("Skipping file because missing build path: " + f.getName());
				continue;
			}
			
			final Path dpath = Paths.get(buildpath);
			
			// do file/folder sync
			
			XElement sync = root.find("Sync");
			
			if (sync != null) {
				for (final XElement op : sync.selectAll("*")) {
					if ("Folder".equals(op.getName())) {
						System.out.println("Sync folder: " + op.getAttribute("Path"));
						
						final Collection<XElement> exceptions = op.selectAll("Except");
						
						try {
							Files.walkFileTree(Paths.get(orgfolder, op.getAttribute("Path")), new SimpleFileVisitor<Path>() {
								@Override
								public FileVisitResult visitFile(Path sfile, BasicFileAttributes attrs) throws IOException {
									// check for exceptions, do not copy an exception
									for (XElement xel : exceptions) {
										if (sfile.endsWith(Paths.get(xel.getAttribute("File"))))
											return FileVisitResult.CONTINUE;
									}
									
									// calculate the destination path
									Path dfile = dpath.resolve(sfile.subpath(orgpath.getNameCount(), sfile.getNameCount()));
									
									boolean exists = Files.exists(dfile);
									
									if (!exists) {
										System.out.println("MISSING - copy over - Source file: " + sfile + " - Dest file: " + dfile);
									}
									else if (attrs.size() != Files.size(dfile)) {
										System.out.println("SIZE    - copy over - Source file: " + sfile + " - Dest file: " + dfile);
									}
									else {
										String h1 = HashUtil.getSha1(Files.newInputStream(sfile));
										String h2 = HashUtil.getSha1(Files.newInputStream(dfile));
										
										if (!h1.equals(h2)) 									
											System.out.println("HASH    - copy over - Source file: " + sfile + " - Dest file: " + dfile);
									}
									
									return FileVisitResult.CONTINUE;
								}
							});
						}
						catch (IOException x) {
							
						}
						
					}
					else if ("File".equals(op.getName())) {
						//System.out.println("Sync file: " + op.getAttribute("Path"));
					}
				}
			}
			
			// TODO pack
		}
	}
}
