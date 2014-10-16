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

import java.util.List;
import java.util.function.Consumer;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.hub.Hub;
import divconq.interchange.CommonPath;
import divconq.interchange.FileSystemDriver;
import divconq.interchange.IFileStoreFile;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.lang.WrappedFuncCallback;
import divconq.lang.WrappedOperationCallback;
import divconq.mod.ExtensionBase;
import divconq.session.Session;
import divconq.session.DataStreamChannel;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class FileServerService extends ExtensionBase implements IService {
	protected FileSystemDriver fsd = new FileSystemDriver();
	protected Session channels = null;
	protected String bestEvidence = null;
	protected String minEvidence = null;
	
	@Override
	public void init(OperationResult log, XElement config) {
		super.init(log, config);
		
		this.fsd.setRootFolder(".\temp");
		this.bestEvidence = "SHA256";
		this.minEvidence = "Size";
		
		if (config != null) {
			if (config.hasAttribute("FileStorePath")) {
				this.fsd.setRootFolder(config.getAttribute("FileStorePath"));
				
				// don't wait on this, it'll log correctly
				this.fsd.connect(null, new WrappedOperationCallback(log) {					
					@Override
					public void callback() {
						// NA
					}
				});
			}
			
			if (config.hasAttribute("BestEvidence")) 
				this.bestEvidence = config.getAttribute("BestEvidence");
			
			if (config.hasAttribute("MinimumEvidence")) 
				this.minEvidence = config.getAttribute("MinimumEvidence");
		}
		
		this.channels = Hub.instance.getSessions().createForService();
	}
	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("FileStore".equals(feature)) {
			if ("FileDetail".equals(op)) {
				this.handleFileDetail(request);
				return;
			}
			
			if ("DeleteFile".equals(op)) {
				this.handleDeleteFile(request);
				return;
			}
			
			if ("DeleteFolder".equals(op)) {
				this.handleDeleteFolder(request);
				return;
			}
			
			if ("AddFolder".equals(op)) {
				this.handleAddFolder(request);
				return;
			}
			
			if ("ListFiles".equals(op)) {
				this.handleListFiles(request);
				return;
			}
			
			if ("StartUpload".equals(op)) {
				this.handleStartUpload(request);
				return;
			}

			if ("FinishUpload".equals(op)) {
				this.handleFinishUpload(request);
				return;
			}
			
			if ("StartDownload".equals(op)) {
				this.handleStartDownload(request);
				return;
			}

			if ("FinishDownload".equals(op)) {
				this.handleFinishDownload(request);
				return;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	public void handleFileDetail(final TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		this.fsd.getFileDetail(path, new WrappedFuncCallback<IFileStoreFile>(request) {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					request.error("File does not exist");
					request.complete();
					return;
				}
				
				final RecordStruct fdata = new RecordStruct();
				
				fdata.setField("FileName", fi.getName());
				fdata.setField("IsFolder", fi.isFolder());
				fdata.setField("LastModified", fi.getMofificationTime());
				fdata.setField("Size", fi.getSize());
				
				String meth = rec.getFieldAsString("Method");
				
				if (StringUtil.isEmpty(meth) || fi.isFolder()) {
					request.setResult(fdata);
					request.complete();
					return;
				}

				fi.hash(meth, new WrappedFuncCallback<String>(request) {						
					@Override
					public void callback() {
						if (!request.hasErrors()) {
							fdata.setField("Hash", this.getResult());
							request.setResult(fdata);
						}
						
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleDeleteFile(final TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		this.fsd.getFileDetail(path, new WrappedFuncCallback<IFileStoreFile>(request) {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					request.complete();
					return;
				}
				
				if (fi.isFolder()) {
					request.error("Path is folder, use DeleteFolder to remove a folder");
					request.complete();
					return;
				}

				fi.remove(new WrappedOperationCallback(request) {					
					@Override
					public void callback() {
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleDeleteFolder(final TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = new CommonPath(fpath);
		
		this.fsd.getFileDetail(path, new WrappedFuncCallback<IFileStoreFile>(request) {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					request.complete();
					return;
				}
				
				if (!fi.isFolder()) {
					request.error("Path is not folder, use DeleteFile to remove a file");
					request.complete();
					return;
				}

				fi.remove(new WrappedOperationCallback(request) {					
					@Override
					public void callback() {
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleAddFolder(final TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = new CommonPath(fpath);
		
		this.fsd.addFolder(path, new WrappedOperationCallback(request) {
			@Override
			public void callback() {
				request.complete();
			}
		});
	}
	
	public void handleListFiles(final TaskRun request) {
		final RecordStruct rec = MessageUtil.bodyAsRecord(request);
		final String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = new CommonPath(fpath);
		
		this.fsd.getFolderListing(path, new WrappedFuncCallback<List<IFileStoreFile>>(request) {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;					
				}
				
				ListStruct files = new ListStruct();
				
				for (IFileStoreFile file : this.getResult()) {
					RecordStruct fdata = new RecordStruct();
					
					fdata.setField("FileName", file.getName());
					fdata.setField("IsFolder", file.isFolder());
					fdata.setField("LastModified", file.getMofificationTime());
					fdata.setField("Size", file.getSize());
					
					files.addItem(fdata);
				}
				
				request.returnValue(files);
			}
		});
	}
	
	public void handleStartUpload(final TaskRun request) {
		final RecordStruct rec = MessageUtil.bodyAsRecord(request);
		final String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		this.fsd.getFileDetail(path, new WrappedFuncCallback<IFileStoreFile>(request) {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				boolean forceover = rec.getFieldAsBooleanOrFalse("ForceOverwrite");
				boolean resume = !forceover && this.getResult().exists();

				// define channel binding
				RecordStruct binding = new RecordStruct(
						new FieldStruct("FilePath", path),
						new FieldStruct("FileSize", rec.getFieldAsInteger("FileSize")),
						new FieldStruct("Hub", OperationContext.get().getSessionId().substring(0, 5)),
						new FieldStruct("Session", OperationContext.get().getSessionId()),
						new FieldStruct("Channel", rec.getFieldAsString("Channel")),
						new FieldStruct("Append", resume)
				);
				
				final DataStreamChannel chan = new DataStreamChannel(FileServerService.this.channels.getId(), "Uploading " + fpath, binding);
				
				if (rec.hasField("Params"))
					chan.setParams(rec.getField("Params"));
				
				// apply the channel to a write stream from selected file
				this.getResult().openWrite(chan, new WrappedFuncCallback<RecordStruct>(request) {					
					@Override
					public void callback() {
						if (!request.hasErrors()) {
							// add the channel only after we know it is open
							FileServerService.this.channels.addChannel(chan);	
							
							RecordStruct res = this.getResult();
							
							res.setField("BestEvidence", FileServerService.this.bestEvidence);		
							res.setField("MinimumEvidence", FileServerService.this.minEvidence);
							
							// get the binding info to return
							request.setResult(res);
						}
						
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleFinishUpload(final TaskRun request) {
		final RecordStruct rec = MessageUtil.bodyAsRecord(request);
		final String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		if ("Faliure".equals(rec.getFieldAsString("Status"))) {
			request.warn("File upload incomplete or corrupt: " + path);
			
			if (!rec.isFieldEmpty("Note"))
				request.warn("File upload note: " + rec.getFieldAsString("Note"));
			
			request.complete();
			return;
		}
		
		this.fsd.getFileDetail(path, new WrappedFuncCallback<IFileStoreFile>(request) {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				RecordStruct evidinfo = rec.getFieldAsRecord("Evidence");
				
				String evidenceType = null;

				// pick best evidence if available, we really don't care if higher is available
				if (!evidinfo.isFieldEmpty(FileServerService.this.bestEvidence)) { 
					evidenceType = FileServerService.this.bestEvidence;
				}
				// else pick the highest available evidence given
				else {
					for (FieldStruct fld : evidinfo.getFields()) 
						evidenceType = FileServerService.this.maxEvidence(fld.getName(), evidenceType);
				}

				final String selEvidenceType = evidenceType;
				
				final Consumer<Boolean> afterVerify = (pass) -> {
					if (pass) {
						if (FileServerService.this.isSufficentEvidence(FileServerService.this.bestEvidence, selEvidenceType))
							request.info("Verified best evidence for upload: " + path);
						else if (FileServerService.this.isSufficentEvidence(FileServerService.this.minEvidence, selEvidenceType))
							request.info("Verified minimum evidence for upload: " + path);
						else
							request.error("Verified evidence for upload, however evidence is insuffcient: " + path);
					}
					else {
						request.error("File upload incomplete or corrupt: " + path);
					}
					
					if (!rec.isFieldEmpty("Note"))
						request.info("File upload note: " + rec.getFieldAsString("Note"));
					
					request.complete();
				};
				
				// TODO wait for file to flush out?
				if ("Size".equals(selEvidenceType)) {
					Long src = evidinfo.getFieldAsInteger("Size");
					long dest = this.getResult().getSize();
					boolean match = (src == dest);
					
					if (match)
						request.info("File sizes match");
					else
						request.error("File sizes do not match");
					
					afterVerify.accept(match);
				}
				else if (StringUtil.isNotEmpty(selEvidenceType)) {
					this.getResult().hash(selEvidenceType, new WrappedFuncCallback<String>(request) {						
						@Override
						public void callback() {
							if (request.hasErrors()) {
								afterVerify.accept(false);
							}
							else {
								String src = evidinfo.getFieldAsString(selEvidenceType);
								String dest = this.getResult();
								boolean match = (src.equals(dest));
								
								if (match)
									request.info("File hashes match (" + selEvidenceType + ")");
								else
									request.error("File hashes do not match (" + selEvidenceType + ")");
								
								afterVerify.accept(match);
							}
						}
					});
				}
				else {
					request.error("Missing any form of evidence, supply at least size");
					
					afterVerify.accept(false);
				}
			}
		});
	}
	
	public Message handleStartDownload(final TaskRun request) {
		final RecordStruct rec = MessageUtil.bodyAsRecord(request);
		final String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		this.fsd.getFileDetail(path, new WrappedFuncCallback<IFileStoreFile>(request) {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}

				// define channel binding
				RecordStruct binding = new RecordStruct(
						new FieldStruct("FilePath", path),
						new FieldStruct("Offset", rec.getFieldAsInteger("Offset")),
						new FieldStruct("Hub", OperationContext.get().getSessionId().substring(0, 5)),
						new FieldStruct("Session", OperationContext.get().getSessionId()),
						new FieldStruct("Channel", rec.getFieldAsString("Channel"))
				);
				
				final DataStreamChannel chan = new DataStreamChannel(FileServerService.this.channels.getId(), "Downloading " + fpath, binding);
				
				if (rec.hasField("Params"))
					chan.setParams(rec.getField("Params"));
				
				this.getResult().openRead(chan, new WrappedFuncCallback<RecordStruct>(request) {					
					@Override
					public void callback() {
						if (!request.hasErrors()) {
							// add the channel only after we know it is open
							FileServerService.this.channels.addChannel(chan);		
							
							RecordStruct res = this.getResult();
							
							// always return path - if token was used to get file then here is the first chance to know the path/file we are collecting
							res.setField("FilePath", path);
							res.setField("Mime", MimeUtil.getMimeType(path));
							
							res.setField("BestEvidence", FileServerService.this.bestEvidence);		
							res.setField("MinimumEvidence", FileServerService.this.minEvidence);
							
							// get the binding info to return
							request.setResult(res);
						}
						
						request.complete();
					}
				});
			}
		});
		
		return null;
	}
	
	public void handleFinishDownload(final TaskRun request) {
		final RecordStruct rec = MessageUtil.bodyAsRecord(request);
		final String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		if ("Faliure".equals(rec.getFieldAsString("Status"))) {
			request.warn("File download incomplete or corrupt: " + path);
			
			if (!rec.isFieldEmpty("Note"))
				request.warn("File download note: " + rec.getFieldAsString("Note"));
			
			request.complete();
			return;
		}
		
		this.fsd.getFileDetail(path, new WrappedFuncCallback<IFileStoreFile>(request) {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				RecordStruct evidinfo = rec.getFieldAsRecord("Evidence");
				
				String evidenceType = null;

				// pick best evidence if available, we really don't care if higher is available
				if (!evidinfo.isFieldEmpty(FileServerService.this.bestEvidence)) { 
					evidenceType = FileServerService.this.bestEvidence;
				}
				// else pick the highest available evidence given
				else {
					for (FieldStruct fld : evidinfo.getFields()) 
						evidenceType = FileServerService.this.maxEvidence(fld.getName(), evidenceType);
				}

				final String selEvidenceType = evidenceType;
				
				final Consumer<Boolean> afterVerify = (pass) -> {
					if (pass) {
						if (FileServerService.this.isSufficentEvidence(FileServerService.this.bestEvidence, selEvidenceType))
							request.info("Verified best evidence for download: " + path);
						else if (FileServerService.this.isSufficentEvidence(FileServerService.this.minEvidence, selEvidenceType))
							request.info("Verified minimum evidence for download: " + path);
						else
							request.error("Verified evidence for download, however evidence is insuffcient: " + path);
					}
					else {
						request.error("File download incomplete or corrupt: " + path);
					}
					
					if (!rec.isFieldEmpty("Note"))
						request.info("File download note: " + rec.getFieldAsString("Note"));
					
					request.complete();
				};
				
				if ("Size".equals(selEvidenceType)) {
					Long src = evidinfo.getFieldAsInteger("Size");
					long dest = this.getResult().getSize();
					boolean match = (src == dest);
					
					if (match)
						request.info("File sizes match");
					else
						request.error("File sizes do not match");
					
					afterVerify.accept(match);
				}
				else if (StringUtil.isNotEmpty(selEvidenceType)) {
					this.getResult().hash(selEvidenceType, new WrappedFuncCallback<String>(request) {						
						@Override
						public void callback() {
							if (request.hasErrors()) {
								afterVerify.accept(false);
							}
							else {
								String src = evidinfo.getFieldAsString(selEvidenceType);
								String dest = this.getResult();
								boolean match = (src.equals(dest));
								
								if (match)
									request.info("File hashes match (" + selEvidenceType + ")");
								else
									request.error("File hashes do not match (" + selEvidenceType + ")");
								
								afterVerify.accept(match);
							}
						}
					});
				}
				else {
					request.error("Missing any form of evidence, supply at least size");
					
					afterVerify.accept(false);
				}
			}
		});
	}
	
	public boolean isSufficentEvidence(String lookingfor, String got) {
		if ("Size".equals(lookingfor)) 
			return ("Size".equals(got)  || "MD5".equals(got) || "SHA128".equals(got) || "SHA256".equals(got) || "SHA512".equals(got));
		
		if ("MD5".equals(lookingfor)) 
			return ("MD5".equals(got) || "SHA128".equals(got) || "SHA256".equals(got) || "SHA512".equals(got));
		
		if ("SHA128".equals(lookingfor)) 
			return ("SHA128".equals(got) || "SHA256".equals(got) || "SHA512".equals(got));
		
		if ("SHA256".equals(lookingfor)) 
			return ("SHA256".equals(got) || "SHA512".equals(got));
		
		if ("SHA512".equals(lookingfor)) 
			return ("SHA512".equals(got));
		
		return false;
	}
	
	public String maxEvidence(String lhs, String rhs) {
		if ("Size".equals(lhs) && ("MD5".equals(rhs) || "SHA128".equals(rhs) || "SHA256".equals(rhs) || "SHA512".equals(rhs)))
			return rhs;
		
		if ("MD5".equals(lhs) && ("SHA128".equals(rhs) || "SHA256".equals(rhs) || "SHA512".equals(rhs)))
			return rhs;
		
		if ("SHA128".equals(lhs) && ("SHA256".equals(rhs) || "SHA512".equals(rhs)))
			return rhs;
		
		if ("SHA256".equals(lhs) && "SHA512".equals(rhs))
			return rhs;
		
		return lhs;
	}
}
