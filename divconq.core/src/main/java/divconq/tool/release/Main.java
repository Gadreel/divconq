package divconq.tool.release;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
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
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

import divconq.api.ApiSession;
import divconq.hub.Foreground;
import divconq.hub.Hub;
import divconq.hub.ILocalCommandLine;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
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
		Path wikigitpath = null;
		
		XElement fldset = Hub.instance.getConfig().selectFirst("CommandLine/Settings");
		
		if (fldset != null) {
			relpath = Paths.get(fldset.getAttribute("ReleasePath"));			
			gitpath = Paths.get(fldset.getAttribute("GitPath"));
			wikigitpath = Paths.get(fldset.getAttribute("WikiGitPath"));
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
				
				System.out.println("4)  Pack the .jar files");
				
				if (gitpath != null)
					System.out.println("5)  Copy Source to GitHub folder");
				
				System.out.println("6)  Update AWWW");

				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					running = false;
					break;
					
				case 1: {
					ReleasesHelper releases = new ReleasesHelper();
					
					if (!releases.init(relpath))
						break;
					
					System.out.println("Select a release to build");
					System.out.println("0) None");
					
					List<String> rnames = releases.names();
					
					for (int i = 0; i < rnames.size(); i++)
						System.out.println((i+1) + ") " + rnames.get(i));
					
					System.out.println("Option #: ");
					opt = scan.nextLine();
					
					mopt = StringUtil.parseInt(opt);
					
					if ((mopt == null) || (mopt == 0))
						break;
					
					XElement relchoice = releases.get(mopt.intValue() - 1);
					
					if (relchoice == null) {
						System.out.println("Invalid option");
						break;
					}
					
					PackagesHelper availpackages = new PackagesHelper();
					availpackages.init();
					
					InstallHelper inst = new InstallHelper();
					if (!inst.init(availpackages, relchoice))
						break;
					
					XElement prindesc = availpackages.get(inst.prinpackage);
					
					XElement prininst = prindesc.find("Install");
					
					if (prininst == null) {
						System.out.println("Principle package: " + inst.prinpackagenm + " cannot be released directly, it must be part of another package.");
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
					
					AtomicBoolean errored = new AtomicBoolean();
					Path tempfolder = FileUtil.allocateTempFolder2();
					
					ListStruct ignorepaths = new ListStruct();
					Set<String> nolongerdepends = new HashSet<>();
					Set<String> dependson = new HashSet<>();
					
					// put all the release files into a temp folder
					inst.instpkgs.forEach(pname -> {
						availpackages.get(pname)
							.selectAll("DependsOn").stream()
							.filter(doel -> !doel.hasAttribute("Option") || inst.relopts.contains(doel.getAttribute("Option")))
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
					Path csrc = Paths.get("./packages/" + inst.prinpackage + "/config");
					Path cdest = tempfolder.resolve("config/" + inst.prinpackagenm);
					
					if (Files.exists(csrc)) {
						Files.createDirectories(cdest);
						
						OperationResult cres = FileUtil.copyFileTree(csrc, cdest);
						
						if (cres.hasErrors()) {
							System.out.println("Error with prepping config");
							break;
						}
					}
					
					boolean configpassed = true;
					
					// copy packages with config = true
					for (XElement pkg : relchoice.selectAll("Package")) {
						if (!"true".equals(pkg.getAttribute("Config")))
							break;
						
						String pname = pkg.getAttribute("Name");
						
						int pspos = pname.lastIndexOf('/');
						String pnm = (pspos != -1) ? pname.substring(pspos + 1) : pname;
						
						csrc = Paths.get("./packages/" + pname + "/config");
						cdest = tempfolder.resolve("config/" + pnm);
						
						if (Files.exists(csrc)) {
							Files.createDirectories(cdest);
							
							OperationResult cres = FileUtil.copyFileTree(csrc, cdest);
							
							if (cres.hasErrors()) {
								System.out.println("Error with prepping extra config");
								configpassed = false;
								break;
							}
						}
					}
					
					if (!configpassed)
						break;

					// also copy installer config if being used
					if (inst.includeinstaller) {
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
					d1res = IOUtil.saveEntireFile(tempfolder.resolve("env.bat"), 
							"set mem=" + relchoice.getAttribute("Memory", "2048") + "\r\n"
							+ "SET project=" + inst.prinpackagenm + "\r\n"
							+ "SET service=" + relchoice.getAttribute("Service", inst.prinpackagenm) + "\r\n"
							+ "SET servicename=" + relchoice.getAttribute("ServiceName", inst.prinpackagenm + " Service") + "\r\n");			
					
					if (d1res.hasErrors()) {
						System.out.println("Error with prepping env");
						break;
					}
					
					System.out.println("Packing Release file.");
					
					Path relbin = relpath.resolve(rname + "/" + rname + "-" + relvers + "-bin.zip");
					
					if (Files.notExists(relbin.getParent()))
						Files.createDirectories(relbin.getParent());
					
					ZipArchiveOutputStream zipout = new ZipArchiveOutputStream(relbin.toFile()); 
					
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
				
				case 4: {
					System.out.println("Packing jar library files.");
					
					String[] packlist = new String[] {	
							"divconq.core", "divconq.interchange", "divconq.web",
							"divconq.tasks", "divconq.tasks.api", "ncc.uploader.api", 
							"ncc.uploader.core", "ncc.workflow", "sd.core", "dcraft.dca" };
					
					String[] packnames = new String[] {	
							"dcCore", "dcInterchange", "dcWeb", 
							"dcTasks", "dcTasksApi", "nccUploaderApi", 
							"nccUploader", "nccWorkflow", "sd/sdBackend", "dca/dcaCms" };
					
					for (int i = 0; i < packlist.length; i++) {
						String lib = packlist[i];
						String pname = packnames[i]; 
						
						Path relbin = Paths.get("./ext/" + lib + ".jar");
						Path srcbin = Paths.get("./" + lib + "/bin");
						Path packbin = Paths.get("./packages/" + pname + "/lib/" + lib + ".jar");
						
						if (Files.notExists(relbin.getParent()))
							Files.createDirectories(relbin.getParent());
						
						Files.deleteIfExists(relbin);
						
						ZipArchiveOutputStream zipout = new ZipArchiveOutputStream(relbin.toFile()); 
						
				        try {
							Files.walkFileTree(srcbin, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
							        new SimpleFileVisitor<Path>() {
							            @Override
							            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
							                throws IOException
							            {
											ZipArchiveEntry entry = new ZipArchiveEntry(srcbin.relativize(file).toString());
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
						
						Files.copy(relbin, packbin, StandardCopyOption.REPLACE_EXISTING);
					}
					
					System.out.println("Done");
					
					break;
				}
				
				case 5: {
					System.out.println("Copying Source Files");
					
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
					
					or = FileUtil.deleteDirectoryContent(wikigitpath, ".git");
					
					if (or.hasErrors()) {
						System.out.println("Error deleting wiki files");
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
					
					or = FileUtil.copyFileTree(Paths.get("./divconq.core/src/org"), gitpath.resolve("divconq.core/src/main/java/org"), new Predicate<Path>() {
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
					
					System.out.println("Copy tree ./divconq.wiki/public");
					
					or = FileUtil.copyFileTree(Paths.get("./divconq.wiki/public"), wikigitpath);
					
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
				case 6: {
					System.out.println("Are you sure you want to update AWWW Server? (y/n): ");
					if (!scan.nextLine().toLowerCase().startsWith("y"))
						break;
					
					ReleasesHelper releases = new ReleasesHelper();
					if (!releases.init(relpath))						
						break;
					
					XElement relchoice = releases.get("AWWWServer");
					
					if (relchoice == null) {
						System.out.println("Invalid option");
						break;
					}
					
					PackagesHelper availpackages = new PackagesHelper();
					availpackages.init();
					
					InstallHelper inst = new InstallHelper();
					if (!inst.init(availpackages, relchoice))
						break;
					
					ServerHelper ssh = new ServerHelper();
					if (!ssh.init(relchoice.find("SSH")))
						break;
					
					ChannelSftp sftp = null;

					try {			
						Channel channel = ssh.session().openChannel("sftp");
						channel.connect();
						sftp = (ChannelSftp) channel;
						
						// go to routines folder
						sftp.cd("/usr/local/bin/dc/AWWWServer");
						
				        FileRepositoryBuilder builder = new FileRepositoryBuilder();
				        
				        Repository repository = builder
				        		.setGitDir(new File(".git"))
				                .findGitDir() // scan up the file system tree
				                .build();
				        
				        String lastsync = releases.getData("AWWWServer").getFieldAsString("LastCommitSync");
				        
				        RevWalk rw = new RevWalk(repository);
				        ObjectId head1 = repository.resolve(Constants.HEAD);
				        RevCommit commit1 = rw.parseCommit(head1);
				        
				        releases.getData("AWWWServer").setField("LastCommitSync", head1.name());
				        
				        ObjectId rev2 = repository.resolve(lastsync);
				        RevCommit parent = rw.parseCommit(rev2);
				        //RevCommit parent2 = rw.parseCommit(parent.getParent(0).getId());
				        
				        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
				        df.setRepository(repository);
				        df.setDiffComparator(RawTextComparator.DEFAULT);
				        df.setDetectRenames(true);
				        
				        // list oldest first or change types are all wrong!!
				        List<DiffEntry> diffs = df.scan(parent.getTree(), commit1.getTree());
				        
				        for (DiffEntry diff : diffs) {
				        	String gnpath = diff.getNewPath();
				        	String gopath = diff.getOldPath();
				        	
				        	Path npath = Paths.get("./" + gnpath);
				        	Path opath = Paths.get("./" + gopath);
				        	
				        	if (diff.getChangeType() == ChangeType.DELETE) {
					        	if (inst.containsPathExtended(opath)) {
					        		System.out.println("- " + diff.getChangeType().name() + " - " + opath);

					        		try {
						        		sftp.rm(opath.toString());
						        		System.out.println("deleted!!");
					        		}
									catch (SftpException x) {
										System.out.println(" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
										System.out.println("Sftp Error: " + x);
										System.out.println(" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
									} 
					        	}
					        	else {
					        		System.out.println("/ " + diff.getChangeType().name() + " - " + gopath + " !!!!!!!!!!!!!!!!!!!!!!!!!");
					        	}
				        	}
				        	else if ((diff.getChangeType() == ChangeType.ADD) || (diff.getChangeType() == ChangeType.MODIFY) || (diff.getChangeType() == ChangeType.COPY)) {
					        	if (inst.containsPathExtended(npath)) {
					        		System.out.println("+ " + diff.getChangeType().name() + " - " + npath);
					        		
					        		try {
										ssh.makeDirSftp(sftp, npath.getParent());
											
						        		sftp.put(npath.toString(), npath.toString(), ChannelSftp.OVERWRITE);
						        		sftp.chmod(npath.endsWith(".sh") ? 484 : 420, npath.toString());		// 644 octal = 420 dec, 744 octal = 484 dec
						        		System.out.println("uploaded!!");
					        		}
									catch (SftpException x) {
										System.out.println(" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
										System.out.println("Sftp Error: " + x);
										System.out.println(" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
									} 
					        	}
					        	else {
					        		System.out.println("> " + diff.getChangeType().name() + " - " + gnpath + " !!!!!!!!!!!!!!!!!!!!!!!!!");
					        	}
				        	}
				        	else if (diff.getChangeType() == ChangeType.RENAME) {
				        		// remove the old
					        	if (inst.containsPathExtended(opath)) {
					        		System.out.println("- " + diff.getChangeType().name() + " - " + opath);
					        		
					        		try {
						        		sftp.rm(opath.toString());
						        		System.out.println("deleted!!");
					        		}
									catch (SftpException x) {
										System.out.println(" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
										System.out.println("Sftp Error: " + x);
										System.out.println(" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
									} 
					        	}
					        	else {
					        		System.out.println("/ " + diff.getChangeType().name() + " - " + gopath + " !!!!!!!!!!!!!!!!!!!!!!!!!");
					        	}
					        	
					        	// add the new path
					        	if (inst.containsPathExtended(npath)) {
					        		System.out.println("+ " + diff.getChangeType().name() + " - " + npath);
					        		
					        		try {
										ssh.makeDirSftp(sftp, npath.getParent());
											
						        		sftp.put(npath.toString(), npath.toString(), ChannelSftp.OVERWRITE);
						        		sftp.chmod(npath.endsWith(".sh") ? 484 : 420, npath.toString());		// 644 octal = 420 dec, 744 octal = 484 dec
						        		System.out.println("uploaded!!");
					        		}
									catch (SftpException x) {
										System.out.println(" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
										System.out.println("Sftp Error: " + x);
										System.out.println(" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
									} 
					        	}
					        	else {
					        		System.out.println("> " + diff.getChangeType().name() + " - " + gnpath + " !!!!!!!!!!!!!!!!!!!!!!!!!");
					        	}
				        	}
				        	else {
				        		System.out.println("??????????????????????????????????????????????????????????");
				        		System.out.println(": " + diff.getChangeType().name() + " - " + gnpath + " ?????????????????????????");
				        		System.out.println("??????????????????????????????????????????????????????????");
				        	}
				        }
				        
				        rw.dispose();
				        
				        repository.close();
				        
				        releases.saveData();
					} 
					catch (JSchException x) {
						System.out.println("Sftp Error: " + x);
					}
					finally {			
						if (sftp.isConnected())
							sftp.exit();
						
						ssh.close();
					}
					
					
					break;
				}
				
				case 7: {
					Path sfolder = Paths.get("/Work/Projects/awww-current/dairy-graze/poly");
					Path dfolder = Paths.get("/Work/Projects/awww-current/dairy-graze/poly-js");
					
					Files.list(sfolder).forEach(file -> {
						String fname = file.getFileName().toString();

						if (!fname.endsWith(".xml"))
							return;
						
						FuncResult<XElement> lres = XmlReader.loadFile(file, false);
						
						if (lres.isEmptyResult()) {
							System.out.println("Unable to parse: " + file);
							return;
						}
						
						String zc = fname.substring(5, 8);
						String code = "zipsData['" + zc + "'] = ";
						XElement root = lres.getResult();
						
/*
<polyline1 lng="-90.620897" lat="45.377447"/>
<polyline1 lng="-90.619327" lat="45.3805"/>

					[-71.196845,41.67757],[-71.120168,41.496831],[-71.317338,41.474923],[-71.196845,41.67757]
 */
						ListStruct center = new ListStruct();
						ListStruct cords = new ListStruct();
						ListStruct currentPoly = null;
						//String currentName = null;
						
						for (XElement child : root.selectAll("*")) {
							String cname = child.getName();
							
							if (cname.startsWith("marker")) {
								// not always accurate
								if (center.isEmpty())
									center.addItem(Struct.objectToDecimal(child.getAttribute("lng")), Struct.objectToDecimal(child.getAttribute("lat")));
								
								currentPoly = new ListStruct();
								cords.addItem(new ListStruct(currentPoly));
								
								continue;
							}
							
							/*
							if (cname.startsWith("info")) {
								System.out.println("areas: " + child.getAttribute("areas"));
								continue;
							}
							*/
							
							if (!cname.startsWith("polyline"))
								continue;
							
							if (currentPoly == null) {
							//if (!cname.equals(currentName)) {
							//if (currentName == null) {
							//	currentName = cname;
								
							//	System.out.println("new poly: " + cname);
								
								currentPoly = new ListStruct();
								cords.addItem(new ListStruct(currentPoly));
							}
							
							currentPoly.addItem(new ListStruct(Struct.objectToDecimal(child.getAttribute("lng")), Struct.objectToDecimal(child.getAttribute("lat"))));
						}
						
						RecordStruct feat = new RecordStruct()
							.withField("type", "Feature")
							.withField("id", "zip" + zc)
							.withField("properties", 
								new RecordStruct()
									.withField("name", "Prefix " + zc)
									.withField("alias", zc)
							)
							.withField("geometry", 
									new RecordStruct()
										.withField("type", "MultiPolygon")
										.withField("coordinates", cords)
							);
								
						RecordStruct entry = new RecordStruct()
							.withField("code", zc)
							.withField("geo", feat)
							.withField("center", center);
						
						IOUtil.saveEntireFile2(dfolder.resolve("us-zips-" + zc + ".js"), code + entry.toPrettyString() + ";");
					});
					
					break;
				}
				
				}
			}
			catch(Exception x) {
				System.out.println("CLI error: " + x);
			}
		}		
	}

	public class ServerHelper {
		protected JSch jsch = new JSch();
		protected Session session = null;
		
		public boolean init(XElement connconfig) {
			try {
				String hostname = connconfig.getAttribute("Host");
				String username = connconfig.getAttribute("User");
		    	String password = connconfig.getAttribute("Password");
				String keyfile = connconfig.getAttribute("KeyFile");
		    	String passphrase = connconfig.getAttribute("Passphrase");
				
				int port = (int) StringUtil.parseInt(connconfig.getAttribute("Port"), 22);
	
		    	if (StringUtil.isNotEmpty(password))
		    		password = Hub.instance.getClock().getObfuscator().decryptHexToString(password).toString();
		    	
		    	String passwordx = password;
		    	
		    	if (StringUtil.isNotEmpty(passphrase))
		    		passphrase = Hub.instance.getClock().getObfuscator().decryptHexToString(passphrase).toString();
				
				if (StringUtil.isNotEmpty(keyfile)) 
					this.jsch.addIdentity(keyfile, passphrase);
				
				this.session = this.jsch.getSession(username, hostname, port);
				
		    	if (StringUtil.isNotEmpty(password))
		    		this.session.setPassword(password);
	
				this.session.setUserInfo(new UserInfo() {
					@Override
					public void showMessage(String message) {
						System.out.println("SSH session message: " + message);
					}
	
					@Override
					public boolean promptYesNo(String message) {
						return true;
					}
	
					@Override
					public boolean promptPassword(String message) {
						return false;
					}
	
					@Override
					public boolean promptPassphrase(String message) {
						return false;
					}
	
					@Override
					public String getPassword() {
						return passwordx;
					}
	
					@Override
					public String getPassphrase() {
						return null;
					}
				});
	
				this.session.connect(30000); // making a connection with timeout.
				this.session.setTimeout(20000);   // 20 second read timeout
			} 
			catch (Exception x) {
				System.out.println("Error initializing SSH session: " + x);
				return false;
			}
			
			return true;
		}

		public void close() {
			this.session.disconnect();
		}
		
		public Session session() {
			return this.session;
		}
		
		// intended to have a ./ before path 
		public boolean makeDirSftp(ChannelSftp sftp, Path path) {
			System.out.println("mkdir: " + path +  "  ------   " + path.getNameCount());
			
			// path "." should be there
			if (path.getNameCount() < 2)
				return true;
			
			//System.out.println("checking");
			
			try {
			    sftp.stat(path.toString());
			    return true;		// path is there 
			} 
			catch (Exception x) {
			}
			
			this.makeDirSftp(sftp, path.getParent());
			
			try {
				sftp.mkdir(path.toString());
        		sftp.chmod(493, path.toString());		// 755 octal = 493 dec
			} 
			catch (Exception x) {
				System.out.println("Failed to create directory: " + x);
				return false;
			}
			
			return true;
		}
	}
	
	public class ReleasesHelper {
		protected List<XElement> rellist = null;
		protected RecordStruct reldata = null;
		protected Path cspath = null;
		
		public List<String> names() {
			List<String> names = new ArrayList<String>();
			
			for (int i = 0; i < rellist.size(); i++)
				names.add(rellist.get(i).getAttribute("Name"));
			
			return names;
		}
		
		public void saveData() {
			IOUtil.saveEntireFile2(cspath, this.reldata.toPrettyString());
		}

		public XElement get(int i) {
			return this.rellist.get(i);
		}
		
		public XElement get(String name) {
			for (int i = 0; i < rellist.size(); i++)
				if (rellist.get(i).getAttribute("Name").equals(name))
					return rellist.get(i);
			
			return null;
		}
		
		public RecordStruct getData(String name) {
			return this.reldata.getFieldAsRecord(name);
		}
		
		public boolean init(Path relpath) {
			if (relpath == null) {
				System.out.println("Release path not defined");
				return false;
			}
				
			FuncResult<XElement> xres = XmlReader.loadFile(relpath.resolve("release.xml"), false);
			
			if (xres.hasErrors()) {
				System.out.println("Release settings file is not present or has bad xml structure");
				return false;
			}
			
			this.rellist = xres.getResult().selectAll("Release");
			
			this.cspath = relpath.resolve("release-data.json");

			if (Files.exists(cspath)) {
				FuncResult<CharSequence> res = IOUtil.readEntireFile(cspath);
				
				if (res.isEmptyResult()) {
					System.out.println("Release data unreadable");
					return false;
				}
				
				this.reldata = Struct.objectToRecord(res.getResult());
			}
			
			return true;
		}		
	}
	
	public class InstallHelper {
		protected XElement relchoice = null;
		protected PackagesHelper availpackages = null;
		protected String prinpackage = null;
		protected String prinpackagenm = null;
		protected Set<String> instpkgs = new HashSet<>(); 
		protected Set<String> relopts = new HashSet<>(); 
		protected boolean includeinstaller = false;
		
		public boolean init(PackagesHelper availpackages, XElement relchoice) {
			this.relchoice = relchoice;
			this.availpackages = availpackages;
			
			this.includeinstaller = Struct.objectToBooleanOrFalse(relchoice.getAttribute("IncludeInstaller"));
			this.prinpackage = relchoice.getAttribute("PrincipalPackage");
			
			int pspos = prinpackage.lastIndexOf('/');
			this.prinpackagenm = (pspos != -1) ? prinpackage.substring(pspos + 1) : prinpackage;
			
			instpkgs.add(prinpackage);
			
			if (includeinstaller)
				instpkgs.add("dc/dcInstall");
			
			if (relchoice.hasAttribute("Options"))
				relopts.addAll(Arrays.asList(relchoice.getAttribute("Options").split(",")));
			
			relchoice.selectAll("Package").stream().forEach(pkg -> instpkgs.add(pkg.getAttribute("Name")));
			
			System.out.println("Selected packages: " + instpkgs);
			
			String[] coreinst = instpkgs.toArray(new String[instpkgs.size()]);
			
			for (int i = 0; i < coreinst.length; i++) {
				if (!availpackages.collectPackageDependencies(this, coreinst[i])) {
					System.out.println("Error with package dependencies");
					return false;
				}
			}
			
			System.out.println("All release packages: " + instpkgs);
			return true;
		}
		
		// does the official release contain this...or do the domains
		public boolean containsPathExtended(Path npath) {
			if (npath.getName(1).toString().equals("public")) {
				if (npath.getNameCount() < 4)
					return false;
				
				String alias = npath.getName(3).toString();
				
				return relchoice.selectAll("Domain").stream().anyMatch(domain -> alias.equals(domain.getAttribute("Alias")));
			}
			
			return this.containsPath(npath);
		}

		// does the official release contain this...
		public boolean containsPath(Path npath) {
			AtomicBoolean fnd = new AtomicBoolean();
			
			String p = npath.toString().substring(2);
			
			this.instpkgs.forEach(pname -> {
				if (p.startsWith("packages/" + pname))
					fnd.set(true);
				
				availpackages.get(pname)
					.selectAll("DependsOn").stream()
					.filter(doel -> !doel.hasAttribute("Option") || this.relopts.contains(doel.getAttribute("Option")))
					.forEach(doel -> {
						// copy all libraries we rely on
						// TODO consider lib handling
						//doel.selectAll("Library").forEach(libel -> {
						//	
						//	Path src = Paths.get("./lib/" + libel.getAttribute("File"));
						//});
						
						// copy all files we rely on
						doel.selectAll("File").forEach(libel -> {
							if (p.equals(libel.getAttribute("Path")))
									fnd.set(true);;
						});
						
						// copy all folders we rely on
						doel.selectAll("Folder").forEach(libel -> {
							if (p.startsWith(libel.getAttribute("Path")))
								fnd.set(true);;
						});
					});
				
				// copy the released packages libraries
				// TODO handle package libs in main lib?
				//Path libsrc = Paths.get("./packages/" + pname + "/lib");
			});
			
			return fnd.get();
		}
		
		public boolean hasOption(String opt) {
			return relopts.contains(opt);
		}
		
		public void addPackage(String name) {
			this.instpkgs.add(name);
		}
	}
	
	public class PackagesHelper {
		protected Path pkgspath = Paths.get("./packages");
		protected Map<String, XElement> availpackages = new HashMap<>();
		
		public void init() throws Exception {
			Files.walkFileTree(pkgspath, new SimpleFileVisitor<Path>() {
				public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws java.io.IOException {
					Path pkgdesc = path.resolve("package.xml");
					
					if (Files.exists(pkgdesc)) {
						
						FuncResult<XElement> xres = XmlReader.loadFile(path.resolve("package.xml"), false);
						
						if (xres.hasErrors()) 
							System.out.println("package.xml found, but not usable: " + path);
						else 
							availpackages.put(pkgspath.relativize(path).toString().replace('\\', '/'), xres.getResult());
						
						return FileVisitResult.SKIP_SUBTREE; 
					}
					
					return FileVisitResult.CONTINUE; 
				}
			});
			
			System.out.println("Available packages: " + availpackages.keySet());
		}
		
		public XElement get(String pname) {
			return this.availpackages.get(pname);
		}
		
		// return true on success
		public boolean collectPackageDependencies(InstallHelper inst, String pname) {
			if (!availpackages.containsKey(pname)) {
				System.out.println("Required Package not found: " + pname);
				return false;
			}
			
			// filter DependsOn by Option
			for (XElement doel : availpackages.get(pname).selectAll("DependsOn")) {
				if (doel.hasAttribute("Option") && !inst.hasOption(doel.getAttribute("Option")))
					continue;
				
				 // copy all libraries we rely on
				for (XElement pkg : doel.selectAll("Package")) {
					String doname = pkg.getAttribute("Name");
					inst.addPackage(doname);
					if (!this.collectPackageDependencies(inst, doname))
						return false;
				}
			}

			return true;
		}
	}
}
