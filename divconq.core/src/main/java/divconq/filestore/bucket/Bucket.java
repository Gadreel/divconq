package divconq.filestore.bucket;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import divconq.filestore.CommonPath;
import divconq.filestore.IFileStoreDriver;
import divconq.filestore.IFileStoreFile;
import divconq.filestore.local.FileSystemDriver;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.io.LocalFileStore;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.session.DataStreamChannel;
import divconq.session.Session;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class Bucket {
	protected IFileStoreDriver fsd = null;	
	protected Session channels = null;
	protected String bestEvidence = null;
	protected String minEvidence = null;
	
	protected GroovyObject script = null;
	
	// return true if executed something
	public boolean tryExecuteMethod(String name, Object... params) {
		if (this.script == null) 
			return false;
		
		Method runmeth = null;
		
		for (Method m : this.script.getClass().getMethods()) {
			if (!m.getName().equals(name))
				continue;
			
			runmeth = m;
			break;
		}
		
		if (runmeth == null) 
			return false;
		
		try {
			this.script.invokeMethod(name, params);
			
			return true;
		}
		catch (Exception x) {
			OperationContext.get().error("Unable to execute watcher script!");
			OperationContext.get().error("Error: " + x);
		}		
		
		return false;
	}
	
	public void init(DomainInfo di, XElement bel, OperationCallback cb) {
		String bname = bel.getAttribute("Name");
		Path bpath = di.resolvePath("buckets").resolve(bname + ".groovy");
		
		if (Files.exists(bpath)) {
			// TODO Auto-generated method stub
			try (GroovyClassLoader loader = new GroovyClassLoader()) {
				Path dpath = di.resolvePath("glib");
				
				//System.out.println("dpath: " + dpath);
				
				if (Files.exists(dpath))
					loader.addClasspath(dpath.toString());
				
				Class<?> groovyClass = loader.parseClass(bpath.toFile());
				
				this.script = (GroovyObject) groovyClass.newInstance();
				
				this.tryExecuteMethod("Init", new Object[] { di });
			}
			catch (Exception x) {
				OperationContext.get().error("Unable to prepare bucket script: " + bpath);
				OperationContext.get().error("Error: " + x);
			}
		}
		
		// TODO enhance, someday this doesn't have to be a local FS
		this.fsd = new FileSystemDriver();
		
		String root = ".\temp";
		
		// TODO load from settings
		this.bestEvidence = "SHA256";
		this.minEvidence = "Size";
		
		this.channels = Hub.instance.getSessions().createForService();
		
		LocalFileStore pubfs = Hub.instance.getPublicFileStore();
		
		if (pubfs != null) 
			root = di.resolvePath(bel.getAttribute("RootFolder", root)).toAbsolutePath().normalize().toString();

		RecordStruct cparams = new RecordStruct().withField("RootFolder", root);
		
		// don't wait on this, it'll log correctly
		this.fsd.connect(cparams, new OperationCallback() {			
			@Override
			public void callback() {
				// TODO check success and set Bucket flag if no connection  

				if (cb != null)
					cb.complete();
			}
		});		
	}
	
	public IFileStoreDriver getFileStore() {
		return this.fsd;
	}
	
	/*
	 * ================ programming points ==================
	 */
	
	// feedback
	// - a file  (file returned may have IsReadable and IsWritable set to indicate permissions for current context)
	// - log errors
	public void mapRequest(RecordStruct data, FuncCallback<IFileStoreFile> fcb) {
		if (this.tryExecuteMethod("MapRequest", this, data, fcb))
			return;
		
		String path = data.getFieldAsString("Path");
		
		this.fsd.getFileDetail(new CommonPath(path), fcb);
	}

	// feedback
	// - lister is optional - it can filter entries and embellish entries
	// - log errors
	// - provide Extra response
	public void getLister(IFileStoreFile fi, RecordStruct data, FuncCallback<BucketLister> fcb) {
		// TODO
	}

	// feedback
	// - replace a file with another 
	// - log errors
	// - provide Extra response
	public void beforeStartDownload(IFileStoreFile fi, RecordStruct data, RecordStruct extra, FuncCallback<IFileStoreFile> fcb) {
		if (this.tryExecuteMethod("BeforeStartDownload", this, fi, data, extra, fcb))
			return;
		
		fcb.setResult(fi);
		fcb.complete();
	}

	// feedback
	// - log errors
	// - provide Extra response
	public void afterStartDownload(IFileStoreFile fi, RecordStruct data, RecordStruct resp, OperationCallback cb) {
		if (this.tryExecuteMethod("AfterStartDownload", this, fi, data, resp, cb))
			return;
		
		cb.complete();
	}

	// feedback
	// - log errors
	// - provide Extra response
	public void finishDownload(IFileStoreFile fi, RecordStruct data, RecordStruct extra, boolean pass, String evidenceUsed, OperationCallback cb) {
		if (this.tryExecuteMethod("FinishDownload", this, fi, data, extra, pass, evidenceUsed, cb))
			return;
		
		cb.complete();
	}

	// feedback
	// - replace a file with another 
	// - log errors
	// - provide Extra response
	public void beforeStartUpload(IFileStoreFile fi, RecordStruct data, RecordStruct extra, FuncCallback<IFileStoreFile> fcb) {
		if (this.tryExecuteMethod("BeforeStartUpload", this, fi, data, extra, fcb))
			return;
		
		fcb.setResult(fi);
		fcb.complete();
	}

	// feedback
	// - log errors
	// - provide Extra response
	public void afterStartUpload(IFileStoreFile fi, RecordStruct data, RecordStruct resp, OperationCallback cb) {
		if (this.tryExecuteMethod("AfterStartUpload", this, fi, data, resp, cb))
			return;
		
		cb.complete();
	}

	// feedback
	// - log errors
	// - provide Extra response
	public void finishUpload(IFileStoreFile fi, RecordStruct data, RecordStruct extra, boolean pass, String evidenceUsed, OperationCallback cb) {
		if (this.tryExecuteMethod("FinishUpload", this, fi, data, extra, pass, evidenceUsed, cb))
			return;
		
		cb.complete();
	}

	// feedback
	// - log errors
	// - provide Extra response
	public void beforeRemove(IFileStoreFile fi, RecordStruct data, RecordStruct extra, OperationCallback cb) {
		if (this.tryExecuteMethod("BeforeRemove", this, fi, data, extra, cb))
			return;
		
		cb.complete();
	}

	// feedback
	// - log errors
	// - provide Extra response
	public void afterRemove(IFileStoreFile fi, RecordStruct data, RecordStruct extra, OperationCallback cb) {
		if (this.tryExecuteMethod("AfterRemove", this, fi, data, extra, cb))
			return;
		
		cb.complete();
	}
	
	/*
	 * ================ features ==================
	 */

	public void handleFileDetail(RecordStruct request, FuncCallback<RecordStruct> fcb) {
		this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					fcb.complete();
					return;
				}
				
				if (this.isEmptyResult()) {
					fcb.error("Your request appears valid but does not map to a file.  Unable to complete.");
					fcb.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					fcb.error("File does not exist");
					fcb.complete();
					return;
				}
				
				RecordStruct fdata = new RecordStruct();
				
				fdata.setField("FileName", fi.getName());
				fdata.setField("IsFolder", fi.isFolder());
				fdata.setField("LastModified", fi.getModificationTime());
				fdata.setField("Size", fi.getSize());
				
				String meth = request.getFieldAsString("Method");
				
				if (StringUtil.isEmpty(meth) || fi.isFolder()) {
					fcb.setResult(fdata);
					fcb.complete();
					return;
				}
		
				fi.hash(meth, new FuncCallback<String>() {						
					@Override
					public void callback() {
						if (!fcb.hasErrors()) {
							fdata.setField("Hash", this.getResult());
							fcb.setResult(fdata);
						}
						
						fcb.complete();
					}
				});
			}
		});
	}
	
	public void handleDeleteFile(RecordStruct request, FuncCallback<RecordStruct> fcb) {
		this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					fcb.complete();
					return;
				}
				
				if (this.isEmptyResult()) {
					fcb.error("Your request appears valid but does not map to a file.  Unable to complete.");
					fcb.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				RecordStruct extra = new RecordStruct();
				fcb.setResult(extra);
				
				Bucket.this.beforeRemove(fi, request, extra, new OperationCallback() {
					@Override
					public void callback() {
						if (this.hasErrors() || !fi.exists()) {
							fcb.complete();
							return;
						}

						fi.remove(new OperationCallback() {					
							@Override
							public void callback() {
								Bucket.this.afterRemove(fi, request, extra, fcb);
							}
						});
					}
				});
			}
		});
	}
	
	public void handleAddFolder(RecordStruct request, FuncCallback<RecordStruct> fcb) {
		this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					fcb.complete();
					return;
				}
				
				if (this.isEmptyResult()) {
					fcb.error("Your request appears valid but does not map to a file.  Unable to complete.");
					fcb.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (fi.exists() && fi.isFolder()) {
					fcb.complete();
					return;
				}
				
				if (fi.exists() && !fi.isFolder()) {
					fcb.error("Path already maps to a file, unable to create folder");
					fcb.complete();
					return;
				}

				Bucket.this.fsd.addFolder(fi.path(), new FuncCallback<IFileStoreFile>() {
					@Override
					public void callback() {
						fcb.complete();
					}
				});
			}
		});
	}
	
	public void handleListFiles(RecordStruct request, FuncCallback<ListStruct> fcb) {
		this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					fcb.complete();
					return;
				}
				
				if (this.isEmptyResult()) {
					fcb.error("Your request appears valid but does not map to a file.  Unable to complete.");
					fcb.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					fcb.complete();
					return;
				}

				Bucket.this.fsd.getFolderListing(fi.path(), new FuncCallback<List<IFileStoreFile>>() {
					@Override
					public void callback() {
						if (this.hasErrors()) {
							fcb.complete();
							return;					
						}
						
						boolean showHidden = fcb.getContext().getUserContext().isTagged("Admin");
						
						ListStruct files = new ListStruct();
						
						for (IFileStoreFile file : this.getResult()) {
							if (!showHidden && file.getName().startsWith("."))		 
								continue;
							
							RecordStruct fdata = new RecordStruct();
							
							fdata.setField("FileName", file.getName());
							fdata.setField("IsFolder", file.isFolder());
							fdata.setField("LastModified", file.getModificationTime());
							fdata.setField("Size", file.getSize());
							
							// TODO embellish with Extra
							
							files.addItem(fdata);
						}
						
						fcb.setResult(files);
						fcb.complete();
					}
				});
			}
		});
	}
	
	public void handleCustom(RecordStruct request, FuncCallback<RecordStruct> fcb) {
		RecordStruct extra = new RecordStruct();
		fcb.setResult(extra);
		
		if (this.tryExecuteMethod("Custom", this, request, extra, fcb))
			return;
		
		fcb.setResult(extra);
		fcb.complete();
	}
	
	public void handleStartUpload(RecordStruct request, FuncCallback<RecordStruct> fcb) {
		this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					fcb.complete();
					return;
				}
				
				if (this.isEmptyResult()) {
					fcb.error("Your request appears valid but does not map to a file.  Unable to complete.");
					fcb.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				RecordStruct extra = new RecordStruct();
				//fcb.setResult(extra);
				
				Bucket.this.beforeStartUpload(fi, request, extra, new FuncCallback<IFileStoreFile>() {
					@Override
					public void callback() {
						if (this.hasErrors()) {
							fcb.complete();
							return;
						}
						
						IFileStoreFile fi = this.getResult();
						
						boolean forceover = request.getFieldAsBooleanOrFalse("ForceOverwrite");
						boolean resume = !forceover && fi.exists();

						// define channel binding
						RecordStruct binding = new RecordStruct(
								new FieldStruct("FilePath", fi.getPath()),
								new FieldStruct("FileSize", request.getFieldAsInteger("FileSize")),
								new FieldStruct("Hub", OperationContext.get().getSessionId().substring(0, 5)),
								new FieldStruct("Session", OperationContext.get().getSessionId()),
								new FieldStruct("Channel", request.getFieldAsString("Channel")),
								new FieldStruct("Append", resume)
						);
						
						final DataStreamChannel chan = new DataStreamChannel(Bucket.this.channels.getId(), "Uploading " + fi.getPath(), binding);
						
						if (request.hasField("Params"))
							chan.setParams(request.getField("Params"));
						
						// apply the channel to a write stream from selected file
						fi.openWrite(chan, new FuncCallback<RecordStruct>() {					
							@Override
							public void callback() {
								if (!fcb.hasErrors()) {
									// add the channel only after we know it is open
									Bucket.this.channels.addChannel(chan);	
									
									RecordStruct res = this.getResult();
									
									res.setField("BestEvidence", Bucket.this.bestEvidence);		
									res.setField("MinimumEvidence", Bucket.this.minEvidence);
									res.setField("Extra", extra);
									
									// get the binding info to return
									fcb.setResult(res);
									
									Bucket.this.afterStartUpload(fi, request, res, fcb);
									return;
								}
								
								fcb.complete();
							}
						});
					}
				});
			}
		});
	}
	
	public void handleFinishUpload(RecordStruct request, FuncCallback<RecordStruct> fcb) {
		this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					fcb.complete();
					return;
				}
				
				if (this.isEmptyResult()) {
					fcb.error("Your request appears valid but does not map to a file.  Unable to complete.");
					fcb.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				RecordStruct extra = new RecordStruct();
				fcb.setResult(extra);
				
				if ("Faliure".equals(request.getFieldAsString("Status"))) {
					fcb.warn("File upload incomplete or corrupt: " + fi.getPath());
					
					if (!request.isFieldEmpty("Note"))
						fcb.warn("File upload note: " + request.getFieldAsString("Note"));
					
					fcb.complete();
					return;
				}
				
				RecordStruct evidinfo = request.getFieldAsRecord("Evidence");
				
				String evidenceType = null;

				// pick best evidence if available, we really don't care if higher is available
				if (!evidinfo.isFieldEmpty(Bucket.this.bestEvidence)) { 
					evidenceType = Bucket.this.bestEvidence;
				}
				// else pick the highest available evidence given
				else {
					for (FieldStruct fld : evidinfo.getFields()) 
						evidenceType = BucketUtil.maxEvidence(fld.getName(), evidenceType);
				}

				String selEvidenceType = evidenceType;
				
				Consumer<Boolean> afterVerify = (pass) -> {
					if (pass) {
						if (BucketUtil.isSufficentEvidence(Bucket.this.bestEvidence, selEvidenceType))
							fcb.info("Verified best evidence for upload: " + fi.getPath());
						else if (BucketUtil.isSufficentEvidence(Bucket.this.minEvidence, selEvidenceType))
							fcb.info("Verified minimum evidence for upload: " + fi.getPath());
						else
							fcb.error("Verified evidence for upload, however evidence is insuffcient: " + fi.getPath());
					}
					else {
						fcb.error("File upload incomplete or corrupt: " + fi.getPath());
					}
					
					if (!fcb.hasErrors())
						Bucket.this.watch("Upload", fi);
					
					if (!request.isFieldEmpty("Note"))
						fcb.info("File upload note: " + request.getFieldAsString("Note"));
					
					Bucket.this.finishUpload(fi, request, extra, pass, selEvidenceType, fcb);
				};
				
				if ("Size".equals(selEvidenceType)) {
					Long src = evidinfo.getFieldAsInteger("Size");
					long dest = fi.getSize();
					boolean match = (src == dest);
					
					if (match)
						fcb.info("File sizes match");
					else
						fcb.error("File sizes do not match");
					
					afterVerify.accept(match);
				}
				else if (StringUtil.isNotEmpty(selEvidenceType)) {
					fi.hash(selEvidenceType, new FuncCallback<String>() {						
						@Override
						public void callback() {
							if (fcb.hasErrors()) {
								afterVerify.accept(false);
							}
							else {
								String src = evidinfo.getFieldAsString(selEvidenceType);
								String dest = this.getResult();
								boolean match = (src.equals(dest));
								
								if (match)
									fcb.info("File hashes match (" + selEvidenceType + ")");
								else
									fcb.error("File hashes do not match (" + selEvidenceType + ")");
								
								afterVerify.accept(match);
							}
						}
					});
				}
				else {
					fcb.error("Missing any form of evidence, supply at least size");
					
					afterVerify.accept(false);
				}
			}
		});
	}
	
	public void handleStartDownload(RecordStruct request, FuncCallback<RecordStruct> fcb) {
		this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					fcb.complete();
					return;
				}
				
				if (this.isEmptyResult()) {
					fcb.error("Your request appears valid but does not map to a file.  Unable to complete.");
					fcb.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				RecordStruct extra = new RecordStruct();
				
				Bucket.this.beforeStartDownload(fi, request, extra, new FuncCallback<IFileStoreFile>() {					
					@Override
					public void callback() {
						if (this.hasErrors()) {
							fcb.complete();
							return;
						}
						
						IFileStoreFile fi = this.getResult();
						
						// define channel binding
						RecordStruct binding = new RecordStruct(
								new FieldStruct("FilePath", fi.getPath()),
								new FieldStruct("Offset", request.getFieldAsInteger("Offset")),
								new FieldStruct("Hub", OperationContext.get().getSessionId().substring(0, 5)),
								new FieldStruct("Session", OperationContext.get().getSessionId()),
								new FieldStruct("Channel", request.getFieldAsString("Channel"))
						);
						
						DataStreamChannel chan = new DataStreamChannel(Bucket.this.channels.getId(), "Downloading " + fi.getPath(), binding);
						
						if (request.hasField("Params"))
							chan.setParams(request.getField("Params"));
						
						fi.openRead(chan, new FuncCallback<RecordStruct>() {					
							@Override
							public void callback() {
								if (!fcb.hasErrors()) {
									// add the channel only after we know it is open
									Bucket.this.channels.addChannel(chan);		
									
									RecordStruct res = this.getResult();
									
									// always return path - if token was used to get file then here is the first chance to know the path/file we are collecting
									res.setField("FilePath", fi.getPath());
									res.setField("Mime", MimeUtil.getMimeType(fi.getPath()));
									
									res.setField("BestEvidence", Bucket.this.bestEvidence);		
									res.setField("MinimumEvidence", Bucket.this.minEvidence);
									
									res.setField("Extra", extra);
									
									fcb.setResult(res);
									
									// get the binding info to return
									Bucket.this.afterStartDownload(fi, request, res, fcb);
									return;
								}
								
								fcb.complete();
							}
						});				
					}
				});
			}
		});
	}
	
	public void handleFinishDownload(RecordStruct request, FuncCallback<RecordStruct> fcb) {
		this.mapRequest(request, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				if (this.hasErrors()) {
					fcb.complete();
					return;
				}
				
				if (this.isEmptyResult()) {
					fcb.error("Your request appears valid but does not map to a file.  Unable to complete.");
					fcb.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				RecordStruct extra = new RecordStruct();
				fcb.setResult(extra);
				
				if ("Faliure".equals(request.getFieldAsString("Status"))) {
					fcb.warn("File download incomplete or corrupt: " + fi.getPath());
					
					if (!request.isFieldEmpty("Note"))
						fcb.warn("File download note: " + request.getFieldAsString("Note"));
					
					fcb.complete();
					return;
				}
				
				RecordStruct evidinfo = request.getFieldAsRecord("Evidence");
				
				String evidenceType = null;

				// pick best evidence if available, we really don't care if higher is available
				if (!evidinfo.isFieldEmpty(Bucket.this.bestEvidence)) { 
					evidenceType = Bucket.this.bestEvidence;
				}
				// else pick the highest available evidence given
				else {
					for (FieldStruct fld : evidinfo.getFields()) 
						evidenceType = BucketUtil.maxEvidence(fld.getName(), evidenceType);
				}

				String selEvidenceType = evidenceType;
				
				final Consumer<Boolean> afterVerify = (pass) -> {
					if (pass) {
						if (BucketUtil.isSufficentEvidence(Bucket.this.bestEvidence, selEvidenceType))
							fcb.info("Verified best evidence for download: " + fi.getPath());
						else if (BucketUtil.isSufficentEvidence(Bucket.this.minEvidence, selEvidenceType))
							fcb.info("Verified minimum evidence for download: " + fi.getPath());
						else
							fcb.error("Verified evidence for download, however evidence is insuffcient: " + fi.getPath());
					}
					else {
						fcb.error("File download incomplete or corrupt: " + fi.getPath());
					}
					
					if (!request.isFieldEmpty("Note"))
						fcb.info("File download note: " + request.getFieldAsString("Note"));
					
					//fcb.complete();
					Bucket.this.finishDownload(fi, request, extra, pass, selEvidenceType, fcb);
				};
				
				if ("Size".equals(selEvidenceType)) {
					Long src = evidinfo.getFieldAsInteger("Size");
					long dest = fi.getSize();
					boolean match = (src == dest);
					
					if (match)
						fcb.info("File sizes match");
					else
						fcb.error("File sizes do not match");
					
					afterVerify.accept(match);
				}
				else if (StringUtil.isNotEmpty(selEvidenceType)) {
					fi.hash(selEvidenceType, new FuncCallback<String>() {						
						@Override
						public void callback() {
							if (fcb.hasErrors()) {
								afterVerify.accept(false);
							}
							else {
								String src = evidinfo.getFieldAsString(selEvidenceType);
								String dest = this.getResult();
								boolean match = (src.equals(dest));
								
								if (match)
									fcb.info("File hashes match (" + selEvidenceType + ")");
								else
									fcb.error("File hashes do not match (" + selEvidenceType + ")");
								
								afterVerify.accept(match);
							}
						}
					});
				}
				else {
					fcb.error("Missing any form of evidence, supply at least size");
					
					afterVerify.accept(false);
				}
			}
		});
	}
	
	public void watch(String op, IFileStoreFile file) {
		/* TODO review
		XElement settings = this.getLoader().getSettings();
		
		if (settings != null) {
	        for (XElement watch : settings.selectAll("Watch")) {
	        	String wpath = watch.getAttribute("FilePath");
	        	
	        	// if we are filtering on path make sure the path is a parent of the triggered path
	        	if (StringUtil.isNotEmpty(wpath)) {
	        		CommonPath wp = new CommonPath(wpath);
	        		
	        		if (!wp.isParent(file.path()))
	        			continue;
	        	}
        	
                String tasktag = op + "Task";
                
    			for (XElement task : watch.selectAll(tasktag)) {
    				String id = task.getAttribute("Id");
    				
    				if (StringUtil.isEmpty(id))
    					id = Task.nextTaskId();
    				
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
    				
			        prec.setField("File", file);
    				
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
		*/
	}
}
