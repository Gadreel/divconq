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
package divconq.service.simple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Platform;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import divconq.bus.IService;
import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.hub.Hub;
import divconq.interchange.CommonPath;
import divconq.interchange.FileSystemDriver;
import divconq.interchange.FileSystemFile;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.log.Logger;
import divconq.mod.ExtensionBase;
import divconq.session.Session;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.work.ScriptWork;
import divconq.work.Task;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class FileWatcher extends ExtensionBase implements IService {
	//protected HashMap<WatchKey, XElement> watches = new HashMap<>();
	//protected HashMap<WatchKey, FileSystemDriver> stores = new HashMap<>();
	//protected boolean stopped = false;
	
	protected List<Integer> watchids = new ArrayList<>();
	
	@Override
	public void start() {
		XElement settings = this.getLoader().getSettings();
		
		if (settings != null) {
			try {
		        for (XElement watch : settings.selectAll("Watch")) {
		        	String path = watch.getAttribute("FilePath");
		        	
		        	if (StringUtil.isEmpty(path))
		        		continue;
		        	
			        Path dir = Paths.get(path);
			        
			        Files.createDirectories(dir);
			        
			        FileSystemDriver drv = new FileSystemDriver(dir);
			        
					try {
						int watchID = JNotify.addWatch(
								path, 
								JNotify.FILE_CREATED | JNotify.FILE_DELETED | JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED, 
								true, 
								new JNotifyListener() {
									public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
										this.trigger(JNotify.FILE_RENAMED, rootPath, newName, oldName);
									}
					
									public void fileModified(int wd, String rootPath, String name) {
										this.trigger(JNotify.FILE_MODIFIED, rootPath, name, null);
									}
					
									public void fileDeleted(int wd, String rootPath, String name) {
										this.trigger(JNotify.FILE_DELETED, rootPath, name, null);
									}
					
									public void fileCreated(int wd, String rootPath, String name) {
										this.trigger(JNotify.FILE_CREATED, rootPath, name, null);
									}
									
									public void trigger(int type, String rootPath, String name, String oldName) {
										// ignore mac hidden file
										if (Platform.isMac() && name.endsWith(".DS_Store"))
											return;
										
					                    String tasktag = "CreateTask";
					                    
					                    if (type == JNotify.FILE_MODIFIED)
					                    	tasktag = "ModifyTask";
					                    else if (type == JNotify.FILE_DELETED)
					                    	tasktag = "DeleteTask";
					                    else if (type == JNotify.FILE_RENAMED)
					                    	tasktag = "RenameTask";
					                    
					        			for (XElement task : watch.selectAll(tasktag)) {
					        				String id = task.getAttribute("Id");
					        				
					        				if (StringUtil.isEmpty(id))
					        					id = Session.nextTaskId();
					        				
					        				String title = task.getAttribute("Title");			        				
					        				String script = task.getAttribute("Script");			        				
					        				String params = task.selectFirstText("Params");
					        				RecordStruct prec = null;
					        				
					        				if (StringUtil.isNotEmpty(params)) {
					        					FuncResult<CompositeStruct> pres = CompositeParser.parseJson(params);
					        					
					        					if (pres.isNotEmptyResult())
					        						prec = (RecordStruct) pres.getResult();
					        				}
					        				
					        				if (prec == null) 
					        					prec = new RecordStruct();
					        				
									        FileSystemFile fh = new FileSystemFile(drv, new CommonPath("/" + name));
									        
									        prec.setField("File", fh);
					        				
					        				//prec.setField("FullPath", parent.resolve(fileName).toString());
					        				//prec.setField("RelativePath", fileName.toString());
					        				
					        				if (script.startsWith("$"))
					        					script = script.substring(1);
					        				
					        				Task t = new Task()
					        					.withId(id)
						    					.withTitle(title)
						    					.withParams(prec)
						    					.withRootContext();
						    				
						    				if (!ScriptWork.addScript(t, Paths.get(script))) {
						    					Logger.error("Unable to run script for file watcher: " + watch.getAttribute("FilePath"));
						    					continue;
						    				}
						    				
						    				Hub.instance.getWorkPool().submit(t);
					        			}
									}
								}
						);
						
						this.watchids.add(watchID);
					} 
					catch (Exception x) {
						OperationContext.get().error("Unable to add file watcher for " + dir + " - error: " +  x);
					}
		        }

		        /*
				ISystemWork watchwork = new ISystemWork() {
					protected boolean closed = false;
					
					@Override
					public void run(SysReporter reporter) {
						if (this.closed)
							return;
						
						if (FileWatcher.this.stopped) {
							try {
								watcher.close();
							} 
							catch (IOException x) {
							}
							
							this.closed = true;
							return;
						}				
						
		                WatchKey key = watcher.poll();
		
		                if (key == null)
		                    return;
		                
		                FileSystemDriver drv = FileWatcher.this.stores.get(key);
	                    XElement watch = FileWatcher.this.watches.get(key);
	                    //Path parent = (Path) key.watchable();
	                    
				        try {
			                for (WatchEvent<?> event : key.pollEvents()) {
			                    WatchEvent.Kind<?> kind = event.kind();
			                    Path fileName = (Path) event.context();
			                    
			                    System.out.println(kind.name() + ": " + fileName);
			                    
			                    String tasktag = "CreateTask";
			                    
			                    if (kind == ENTRY_MODIFY)
			                    	tasktag = "ModifyTask";
			                    else if (kind == ENTRY_DELETE)
			                    	tasktag = "DeleteTask";
			                    
			        			for (XElement task : watch.selectAll(tasktag)) {
			        				String id = task.getAttribute("Id");
			        				
			        				if (StringUtil.isEmpty(id))
			        					id = Session.nextTaskId();
			        				
			        				String title = task.getAttribute("Title");			        				
			        				String script = task.getAttribute("Script");			        				
			        				String params = task.selectFirstText("Params");
			        				RecordStruct prec = null;
			        				
			        				if (StringUtil.isNotEmpty(params)) {
			        					FuncResult<CompositeStruct> pres = CompositeParser.parseJson(params);
			        					
			        					if (pres.isNotEmptyResult())
			        						prec = (RecordStruct) pres.getResult();
			        				}
			        				
			        				if (prec == null) 
			        					prec = new RecordStruct();
			        				
							        FileSystemFile fh = new FileSystemFile(drv, new CommonPath("/" + fileName.toString()), false);
							        
							        prec.setField("File", fh);
			        				
			        				//prec.setField("FullPath", parent.resolve(fileName).toString());
			        				//prec.setField("RelativePath", fileName.toString());
			        				
			        				if (script.startsWith("$"))
			        					script = script.substring(1);
			        				
			        				Task t = new Task()
			        					.withId(id)
				    					.withTitle(title)
				    					.withParams(prec)
				    					.withRootContext();
				    				
				    				if (!ScriptWork.addScript(t, Paths.get(script))) {
				    					Logger.error("Unable to run script for file watcher: " + watch.getAttribute("FilePath"));
				    					continue;
				    				}
				    				
				    				Hub.instance.getWorkPool().submit(t);
			        			}
			                }
				        } 
				        catch (Exception ex) {
				            System.err.println(ex);
				        }
				        finally {
			                key.reset();
				        }
					}
					
					@Override
					public int period() {
						return 1;
					}
				};
		        
				Hub.instance.getClock().addSlowSystemWorker(watchwork);
				*/
	        } 
	        catch (IOException ex) {
	            System.err.println(ex);
	        }
		}
		
		super.start();
	}

	@Override
	public void stop() {
		for (int ids : this.watchids)
			try {
				JNotify.removeWatch(ids);
			} 
			catch (JNotifyException x) {
			}
		
		super.stop();
	}

	@Override
	public void handle(TaskRun request) {
		Message msg = MessageUtil.message(request);
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		/*
		
		if ("Manager".equals(feature)) {
			if ("LoadAll".equals(op)) {
				ListStruct names = new ListStruct("root", "localhost");
				
				ExtensionLoader el = this.getLoader(); 
				
				if (el != null) {
					XElement config = el.getSettings();
					
					if (config != null)
						for (XElement del : config.selectAll("Domain"))
							names.addItem(del.getAttribute("Name"));
				}
				
				request.setResult(new ListStruct(
						new RecordStruct(
								new FieldStruct("Id", "00000_000000000000001"),
								new FieldStruct("Title", "root"),
								new FieldStruct("Names", names)
						)
				));
				
				request.complete();
				return;
			}
		}
		*/
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
