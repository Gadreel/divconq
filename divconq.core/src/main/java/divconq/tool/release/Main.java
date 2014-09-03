package divconq.tool.release;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import divconq.api.ApiSession;
import divconq.hub.Foreground;
import divconq.hub.Hub;
import divconq.hub.ILocalCommandLine;
import divconq.lang.FuncResult;
import divconq.lang.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.FileUtil;
import divconq.util.IOUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class Main implements ILocalCommandLine {
	@Override
	public void run(Scanner scan, ApiSession api) {
		Path relpath = null;
		Path gitpath = null;
		
		XElement fldset = Hub.instance.getConfig().selectFirst("CommandLine/Settings");
		
		if (fldset != null) {
			relpath = Paths.get(fldset.getAttribute("ReleasePath"));			
			gitpath = Paths.get(fldset.getAttribute("GitPath"));
		}
		
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   Release Builder Menu");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				
				if (relpath != null)
					System.out.println("1)  Build release package from Settings File");
				
				System.out.println("2)  Build custom release package [under construction]");
				
				if (gitpath != null)
					System.out.println("5)  Copy Source to GitHub folder");

				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					running = false;
					break;
					
				case 1: {
					if (relpath == null) {
						System.out.println("Release path not defined");
						break;
					}
						
					FuncResult<XElement> xres = XmlReader.loadFile(relpath.resolve("release.xml"), false);
					
					if (xres.hasErrors()) {
						System.out.println("Release settings file is not present or has bad xml structure");
						break;
					}
					
					List<XElement> rellist = xres.getResult().selectAll("Release");
					
					System.out.println("Select a release to build");
					System.out.println("0) None");
					
					for (int i = 0; i < rellist.size(); i++)
						System.out.println((i+1) + ") " + rellist.get(i).getAttribute("Name"));
					
					System.out.println("Option #: ");
					opt = scan.nextLine();
					
					mopt = StringUtil.parseInt(opt);
					
					if (mopt == null)
						break;
					
					if (mopt < 0 || mopt > rellist.size()) {
						System.out.println("Invalid option");
						break;
					}
					
					if (mopt == 0) 
						break;
					
					XElement relchoice = rellist.get(mopt.intValue() - 1);
					
					boolean includeinstaller = Struct.objectToBooleanOrFalse(relchoice.getAttribute("IncludeInstaller"));
					String prinpackage = relchoice.getAttribute("PrincipalPackage");
					
					Set<String> instpkgs = new HashSet<>(); 
					instpkgs.add(prinpackage);
					
					if (includeinstaller)
						instpkgs.add("dc/dcInstall");
					
					Set<String> relopts = new HashSet<>(); 
					
					if (relchoice.hasAttribute("Options"))
						relopts.addAll(Arrays.asList(relchoice.getAttribute("Options").split(",")));
					
					relchoice.selectAll("Package").stream().forEach(pkg -> instpkgs.add(pkg.getAttribute("Name")));
					
					System.out.println("Selected packages: " + instpkgs);
					
					Path pkgspath = Paths.get("./packages");
					Map<String, XElement> availpackages = new HashMap<>();
					
					Files.walkFileTree(pkgspath, new SimpleFileVisitor<Path>() {
						public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws java.io.IOException {
							Path pkgdesc = path.resolve("package.xml");
							
							if (Files.exists(pkgdesc)) {
								
								FuncResult<XElement> xres = XmlReader.loadFile(path.resolve("package.xml"), false);
								
								if (xres.hasErrors()) 
									System.out.println("package.xml found, but not usable: " + path);
								else 
									availpackages.put(pkgspath.relativize(path).toString(), xres.getResult());
								
								return FileVisitResult.SKIP_SUBTREE; 
							}
							
							return FileVisitResult.CONTINUE; 
						}
					});
					
					System.out.println("Available packages: " + availpackages.keySet());
					
					AtomicBoolean errored = new AtomicBoolean(false); 
					String[] coreinst = instpkgs.toArray(new String[instpkgs.size()]);
					
					for (int i = 0; i < coreinst.length; i++)
						this.collectPackageDependencies(instpkgs, availpackages, relopts, coreinst[i], errored);

					if (errored.get()) {
						System.out.println("Error with package dependencies");
						break;
					}
					
					System.out.println("All release packages: " + instpkgs);
					
					XElement prindesc = availpackages.get(prinpackage);
					
					XElement prininst = prindesc.find("Install");
					
					if (prininst == null) {
						System.out.println("Principle package: " + prinpackage + " cannot be released directly, it must be part of another package.");
						break;
					}
					
					String relvers = prindesc.getAttribute("Version");
					
					System.out.println("Building release version " + relvers);
					
					if (prindesc.hasAttribute("LastVersion"))
						System.out.println("Previous release version " + prindesc.getAttribute("LastVersion"));
					
					String rname = relchoice.getAttribute("Name");
					Path destpath = relpath.resolve(rname + "/" + rname + "-" + relvers + "-bin.zip");
					
					if (Files.exists(destpath)) {
						System.out.println("Version " + relvers + " already exists, overwrite? (y/n): ");
						if (!scan.nextLine().toLowerCase().startsWith("y"))
							break;
						
						Files.delete(destpath);
					}
					
					System.out.println("Preparing zip files");
					
					Path tempfolder = FileUtil.allocateTempFolder2();
					
					ListStruct ignorepaths = new ListStruct();
					Set<String> nolongerdepends = new HashSet<>();
					Set<String> dependson = new HashSet<>();
					
					// put all the release files into a temp folder
					instpkgs.forEach(pname -> {
						availpackages.get(pname)
							.selectAll("DependsOn").stream()
							.filter(doel -> !doel.hasAttribute("Option") || relopts.contains(doel.getAttribute("Option")))
							.forEach(doel -> {
								// copy all libraries we rely on
								doel.selectAll("Library").forEach(libel -> {
									dependson.add(libel.getAttribute("File"));
									
									Path src = Paths.get("./lib/" + libel.getAttribute("File"));
									Path dest = tempfolder.resolve("lib/" + libel.getAttribute("File"));
									
									try {
										Files.createDirectories(dest.getParent());
										
										if (Files.notExists(dest))
											Files.copy(src, dest, StandardCopyOption.COPY_ATTRIBUTES);
									} 
									catch (Exception x) {
										errored.set(true);
										System.out.println("Unable to copy file: " + src);
									}
								});
								
								// copy all files we rely on
								doel.selectAll("File").forEach(libel -> {
									Path src = Paths.get("./" + libel.getAttribute("Path"));
									Path dest = tempfolder.resolve(libel.getAttribute("Path"));
									
									try {
										Files.createDirectories(dest.getParent());
										
										if (Files.notExists(dest))
											Files.copy(src, dest, StandardCopyOption.COPY_ATTRIBUTES);
									} 
									catch (Exception x) {
										errored.set(true);
										System.out.println("Unable to copy file: " + src);
									}
								});
								
								// copy all folders we rely on
								doel.selectAll("Folder").forEach(libel -> {
									Path src = Paths.get("./" + libel.getAttribute("Path"));
									Path dest = tempfolder.resolve(libel.getAttribute("Path"));
									
									try {
										Files.createDirectories(dest.getParent());
									} 
									catch (Exception x) {
										errored.set(true);
										System.out.println("Unable to copy file: " + src);
									}

									OperationResult cres = FileUtil.copyFileTree(src, dest);
									
									if (cres.hasErrors())
										errored.set(true);
								});
							});
							
						availpackages.get(pname)
							.selectAll("IgnorePaths/Ignore")
							.forEach(doel -> ignorepaths.addItem(doel.getAttribute("Path")));
						
						// NoLongerDependsOn functionally currently only applies to libraries
						availpackages.get(pname)
							.selectAll("NoLongerDependsOn/Library")
							.forEach(doel -> nolongerdepends.add(doel.getAttribute("File")));
						
						// copy the released packages folders
						Path src = Paths.get("./packages/" + pname);
						Path dest = tempfolder.resolve("packages/" + pname);
						
						try {
							Files.createDirectories(dest.getParent());
						} 
						catch (Exception x) {
							errored.set(true);
							System.out.println("Unable to copy file: " + src);
						}

						// we may wish to enhance filter to allow .JAR sometimes, but this is meant to prevent copying of packages/pname/lib/abc.lib.jar files 
						OperationResult cres = FileUtil.copyFileTree(src, dest, path -> !path.toString().endsWith(".jar"));
						
						if (cres.hasErrors())
							errored.set(true);
						
						// copy the released packages libraries
						Path libsrc = Paths.get("./packages/" + pname + "/lib");
						Path libdest = tempfolder.resolve("lib");
						
						if (Files.exists(libsrc)) {
							cres = FileUtil.copyFileTree(libsrc, libdest);
							
							if (cres.hasErrors())
								errored.set(true);
						}
					});

					if (errored.get()) {
						System.out.println("Error with assembling package");
						break;
					}
					
					// copy the principle config
					Path csrc = Paths.get("./packages/" + prinpackage + "/config");
					Path cdest = tempfolder.resolve("config/" + prinpackage);
					
					if (Files.exists(csrc)) {
						Files.createDirectories(cdest);
						
						OperationResult cres = FileUtil.copyFileTree(csrc, cdest);
						
						if (cres.hasErrors()) {
							System.out.println("Error with prepping config");
							break;
						}
					}

					// also copy installer config if being used
					if (includeinstaller) {
						csrc = Paths.get("./packages/dc/dcInstall/config");
						cdest = tempfolder.resolve("config/dcInstall");
						
						if (Files.exists(csrc)) {
							Files.createDirectories(cdest);
							
							OperationResult cres = FileUtil.copyFileTree(csrc, cdest);
							
							if (cres.hasErrors()) {
								System.out.println("Error with prepping install config");
								break;
							}
						}
					}
					
					// write out the deployed file
					RecordStruct deployed = new RecordStruct();
					
					deployed.setField("Version", relvers);
					deployed.setField("PackageFolder", relpath.resolve(rname));
					deployed.setField("PackagePrefix", rname);
					
					OperationResult d1res = IOUtil.saveEntireFile(tempfolder.resolve("config/deployed.json"), deployed.toPrettyString()); 
					
					if (d1res.hasErrors()) {
						System.out.println("Error with prepping deployed");
						break;
					}

					RecordStruct deployment = new RecordStruct();
					
					deployment.setField("Version", relvers);
					
					if (prindesc.hasAttribute("LastVersion"))
						deployment.setField("DependsOn", prindesc.getAttribute("LastVersion"));
					
					deployment.setField("UpdateMessage", "This update is complete, you may accept this update as runnable.");
					
					nolongerdepends.removeAll(dependson);
					
					ListStruct deletefiles = new ListStruct();
					
					nolongerdepends.forEach(fname -> deletefiles.addItem("lib/" + fname));
					
					deployment.setField("DeleteFiles", deletefiles);
					deployment.setField("IgnorePaths", ignorepaths);
					
					d1res = IOUtil.saveEntireFile(tempfolder.resolve("deployment.json"), deployment.toPrettyString()); 
					
					if (d1res.hasErrors()) {
						System.out.println("Error with prepping deployment");
						break;
					}

					// write env file
					d1res = IOUtil.saveEntireFile(tempfolder.resolve("env.bat"), "set mem=" + relchoice.getAttribute("Memory", "2048") + "\n"
							+ "SET project=" + prinpackage + "\n"
							+ "SET service=" + relchoice.getAttribute("Service", prinpackage) + "\n"
							+ "SET servicename=" + relchoice.getAttribute("ServiceName", prinpackage + " Service") + "\n");			
					
					if (d1res.hasErrors()) {
						System.out.println("Error with prepping env");
						break;
					}
					
					System.out.println("Packing Release file.");
					
					ZipArchiveOutputStream zipout = new ZipArchiveOutputStream(relpath.resolve(rname + "/" + rname + "-" + relvers + "-bin.zip").toFile()); 
					
			        try {
						Files.walkFileTree(tempfolder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
						        new SimpleFileVisitor<Path>() {
						            @Override
						            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						                throws IOException
						            {
										ZipArchiveEntry entry = new ZipArchiveEntry(tempfolder.relativize(file).toString());
										entry.setSize(Files.size(file));
										zipout.putArchiveEntry(entry);
										zipout.write(Files.readAllBytes(file));
										zipout.closeArchiveEntry();					
						                
						                return FileVisitResult.CONTINUE;
						            }
						        });
					} 
			        catch (IOException x) {
						System.out.println("Error building zip: " + x);
					}
					
					zipout.close();
					
					System.out.println("Release file written");
					
					FileUtil.deleteDirectory(tempfolder);
					
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
				
				case 5: {
					System.out.println("Cleaning folders");
					
					OperationResult or = FileUtil.deleteDirectory(gitpath.resolve("divconq.core/src/main/java"));
					
					if (or.hasErrors()) {
						System.out.println("Error deleting files");
						break;
					}
					
					or = FileUtil.deleteDirectory(gitpath.resolve("divconq.core/src/main/resources"));
					
					if (or.hasErrors()) {
						System.out.println("Error deleting files");
						break;
					}
					
					or = FileUtil.deleteDirectory(gitpath.resolve("divconq.interchange/src/main/java"));
					
					if (or.hasErrors()) {
						System.out.println("Error deleting files");
						break;
					}
					
					or = FileUtil.deleteDirectory(gitpath.resolve("divconq.tasks/src/main/java"));
					
					if (or.hasErrors()) {
						System.out.println("Error deleting files");
						break;
					}
					
					or = FileUtil.deleteDirectory(gitpath.resolve("divconq.tasks.api/src/main/java"));
					
					if (or.hasErrors()) {
						System.out.println("Error deleting files");
						break;
					}
					
					or = FileUtil.deleteDirectory(gitpath.resolve("divconq.web/src/main/java"));
					
					if (or.hasErrors()) {
						System.out.println("Error deleting files");
						break;
					}
					
					or = FileUtil.deleteDirectory(gitpath.resolve("packages"));
					
					if (or.hasErrors()) {
						System.out.println("Error deleting files");
						break;
					}
					
					System.out.println("Copying folders");
					
					System.out.println("Copy tree ./divconq.core/src");
					
					or = FileUtil.copyFileTree(Paths.get("./divconq.core/src/divconq"), gitpath.resolve("divconq.core/src/main/java/divconq"), new Predicate<Path>() {
						@Override
						public boolean test(Path file) {
							return file.getFileName().toString().endsWith(".java");
						}
					});
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					or = FileUtil.copyFileTree(Paths.get("./divconq.core/src/localize"), gitpath.resolve("divconq.core/src/main/resources/localize"), new Predicate<Path>() {
						@Override
						public boolean test(Path file) {
							return file.getFileName().toString().endsWith(".xml");
						}
					});
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./divconq.interchange/src");
					
					or = FileUtil.copyFileTree(Paths.get("./divconq.interchange/src"), gitpath.resolve("divconq.interchange/src/main/java"));					
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./divconq.tasks/src");
					
					or = FileUtil.copyFileTree(Paths.get("./divconq.tasks/src"), gitpath.resolve("divconq.tasks/src/main/java"));					
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./divconq.tasks.api/src");
					
					or = FileUtil.copyFileTree(Paths.get("./divconq.tasks.api/src"), gitpath.resolve("divconq.tasks.api/src/main/java"));					
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./divconq.web/src");
					
					or = FileUtil.copyFileTree(Paths.get("./divconq.web/src"), gitpath.resolve("divconq.web/src/main/java"));
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./packages/dcCore");
					
					or = FileUtil.copyFileTree(Paths.get("./packages/dcCore"), gitpath.resolve("packages/dcCore"));
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./packages/dcCorePublic");

					or = FileUtil.copyFileTree(Paths.get("./packages/dcCorePublic"), gitpath.resolve("packages/dcCorePublic"));
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./packages/dcInterchange");
					
					or = FileUtil.copyFileTree(Paths.get("./packages/dcInterchange"), gitpath.resolve("packages/dcInterchange"));
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./packages/dcTasks");
					
					or = FileUtil.copyFileTree(Paths.get("./packages/dcTasks"), gitpath.resolve("packages/dcTasks"));
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./packages/dcTasksApi");
					
					or = FileUtil.copyFileTree(Paths.get("./packages/dcTasksApi"), gitpath.resolve("packages/dcTasksApi"));
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./packages/dcTasksWeb");
					
					or = FileUtil.copyFileTree(Paths.get("./packages/dcTasksWeb"), gitpath.resolve("packages/dcTasksWeb"));
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./packages/dcTest");
					
					or = FileUtil.copyFileTree(Paths.get("./packages/dcTest"), gitpath.resolve("packages/dcTest"));
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copy tree ./packages/dcWeb");
					
					or = FileUtil.copyFileTree(Paths.get("./packages/dcWeb"), gitpath.resolve("packages/dcWeb"));
					
					if (or.hasErrors()) {
						System.out.println("Error copying files");
						break;
					}
					
					System.out.println("Copying files");
					
					Files.copy(Paths.get("./README.md"), gitpath.resolve("README.md"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
					Files.copy(Paths.get("./RELEASE_NOTES.md"), gitpath.resolve("RELEASE_NOTES.md"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
					Files.copy(Paths.get("./NOTICE.txt"), gitpath.resolve("NOTICE.txt"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
					Files.copy(Paths.get("./LICENSE.txt"), gitpath.resolve("LICENSE.txt"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
					
					System.out.println("Done");
					
					break;
				}
				}
			}
			catch(Exception x) {
				System.out.println("CLI error: " + x);
			}
		}		
	}

	protected void collectPackageDependencies(Set<String> instpkgs, Map<String, XElement> availpackages, Set<String> relopts, String pname, AtomicBoolean errored) {
		if (!availpackages.containsKey(pname)) {
			errored.set(true);
			System.out.println("Required Package not found: " + pname);
			return;
		}
		
		// filter DependsOn by Option
		for (XElement doel : availpackages.get(pname).selectAll("DependsOn")) {
			if (doel.hasAttribute("Option") && !relopts.contains(doel.getAttribute("Option")))
				continue;
			
			 // copy all libraries we rely on
			for (XElement pkg : doel.selectAll("Package")) {
				String doname = pkg.getAttribute("Name");
				instpkgs.add(doname);
				this.collectPackageDependencies(instpkgs, availpackages, relopts, doname, errored);
			}
		}

		return;
	}
}
