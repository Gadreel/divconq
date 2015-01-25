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

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.filestore.CommonPath;
import divconq.filestore.IFileStoreFile;
import divconq.filestore.local.FileSystemDriver;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.hub.HubEvents;
import divconq.hub.IEventSubscriber;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.log.Logger;
import divconq.mod.ExtensionBase;
import divconq.session.Session;
import divconq.session.DataStreamChannel;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
import divconq.work.ScriptWork;
import divconq.work.Task;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class FileServerService extends ExtensionBase implements IService {
	protected FileSystemDriver fsd = new FileSystemDriver();
	
	protected Map<String, FileSystemDriver> domainFsd = new HashMap<>();
	
	protected Session channels = null;
	protected String bestEvidence = null;
	protected String minEvidence = null;
	
	@Override
	public void init(XElement config) {
		super.init(config);
		
		this.fsd.setRootFolder(".\temp");
		this.bestEvidence = "SHA256";
		this.minEvidence = "Size";
		
		if (config != null) {
			if (config.hasAttribute("FileStorePath")) {
				this.fsd.setRootFolder(config.getAttribute("FileStorePath"));
				
				// don't wait on this, it'll log correctly
				this.fsd.connect(null, new OperationCallback() {					
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

		// load domain related details only after we are noted to be running
		Hub.instance.subscribeToEvent(HubEvents.Running, new IEventSubscriber() {
			@Override
			public void eventFired(Object e) {
				for (DomainInfo domain : Hub.instance.getDomains()) {
					XElement dset = domain.getSettings();
					
					if (dset != null) {
						XElement fsset = dset.find("FileServer");
						
						if ((fsset != null) && fsset.hasAttribute("FileStorePath")) {
							FileSystemDriver dfsd = new FileSystemDriver();
							
							dfsd.setRootFolder(fsset.getAttribute("FileStorePath"));
							
							// don't wait on this, it'll log correctly
							dfsd.connect(null, new OperationCallback() {					
								@Override
								public void callback() {
									// NA
								}
							});
							
							FileServerService.this.domainFsd.put(domain.getId(), dfsd);
							
							// TODO support Best and Min Evidence at domain level
						}
					}
				}
			}
		});
		
		this.channels = Hub.instance.getSessions().createForService();
	}
	
	@Override
	public void start() {
		super.start();
		
		/*
		 *	Select: {
		 *		Path: "/User/karabiner",
		 *		Recursion: 999,     
		 *		Content: false
		 *	}
		 *
		 */

    	/*
        EventLoopGroup el = Hub.instance.getEventLoopGroup();
    	
        try {
            ServerBootstrap sb = new ServerBootstrap()
            	.group(el)
              	.channel(NioServerSocketChannel.class)
              	.handler(new ChannelInitializer<ServerSocketChannel>() {
                  @Override
                  public void initChannel(ServerSocketChannel ch) throws Exception {
                      //ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                  }
              	})
              	//.childOption(ChannelOption.AUTO_READ, false)
              	.childHandler(new ChannelInitializer<SocketChannel>() {
              		@Override
              		public void initChannel(SocketChannel ch) throws Exception {
              			CtpAdapter adapter = FileServerService.this.channels.allocateCtpAdapter();

              			adapter.setMapper(CtpfCommandMapper.instance);
              			
              			adapter.setHandler(new ICommandHandler() {			
                      		protected IFileSelector lastSelector = null;
                      		
              				@Override
              				public void handle(CtpCommand cmd, CtpAdapter adapter) {
              					//System.out.println("Server got command: " + cmd.getCmdCode());
              					
            					if ((cmd instanceof RequestCommand) && ((RequestCommand)cmd).isOp(CtpConstants.CTP_F_OP_SELECT)) {
            						FileSelection sel = new FileSelection()
            							.withInstructions(((RequestCommand)cmd).getBody().getFieldAsRecord("Select"));
            						
              						this.lastSelector = FileServerService.this.fsd.select(sel);

              						try {
              							RecordStruct res = new RecordStruct(
              								new FieldStruct("Messages", new ListStruct(
                  								new RecordStruct(
                      								new FieldStruct("Code", 1),
                      								new FieldStruct("Message", "failed to list!!"),
                      								new FieldStruct("Level", "Info"),		// Error to test error		
                      								new FieldStruct("Occurred", "20141118T063704Z")
                      							)
              								))              								
              							);
              							
              							ResponseCommand resp = new ResponseCommand();
              							resp.setBody(res);
              							
										adapter.sendCommand(resp);
	  								}
	  								catch (Exception x) {
	  									System.out.println("Ctp-F Server error: " + x);
	  								}
              					}
            					
            					if (cmd instanceof ResponseCommand) {
            			            //System.out.println("Ctp-F Server: Client ACK the final block!");
            					}
            					
            					if (cmd instanceof EngageCommand) {
            			            System.out.println("Ctp-F Server: Client sent engage");
            			            
									try {
										adapter.sendCommand(new ResponseCommand());
									} 
									catch (Exception x) {
	  									System.out.println("Ctp-F Server error: " + x);
									}
            					}
            					
            					/*
            					if ((cmd instanceof RequestCommand) && ((RequestCommand)cmd).isOp(CtpConstants.CTP_F_OP_INIT)) {
            			            System.out.println("Ctp-F Server: Client sent init");
            			            
									try {
										adapter.sendCommand(new ResponseCommand());
									} 
									catch (Exception x) {
	  									System.out.println("Ctp-F Server error: " + x);
									}
            					}
            					* /
              					
              					if ((cmd instanceof SimpleCommand) && (cmd.getCmdCode() == CtpConstants.CTP_F_CMD_STREAM_READ)) {
              						if (this.lastSelector != null) {
              							this.lastSelector.read(adapter);
              							// TODO we should probably handle final back here??
              						}
              						else {
              							System.out.println("Error - no select");
              							// TODO send error response...
              						}
              					}
              					
              					if (cmd instanceof BlockCommand) {
              						BlockCommand file = (BlockCommand) cmd;
              						
              						System.out.println("% " + file.getPath() + "     " + file.getSize()
              								+ "     " + (file.isFolder() ? "FOLDER" : "FILE"));
              						
              						adapter.read();
              					}
              					
              					if ((cmd instanceof SimpleCommand) && (cmd.getCmdCode() == CtpConstants.CTP_F_CMD_STREAM_WRITE)) {
            			            System.out.println("Ctp-F Server: Client sent WRITE");
            			            
									try {
										adapter.sendCommand(new ResponseCommand());
									} 
									catch (Exception x) {
	  									System.out.println("Ctp-F Server error: " + x);
									}
              					}
              					
              					// get more, unless a stream command - then let it handle automatically
              					if (!(cmd instanceof IStreamCommand)) {
              						//System.out.println("Server issues read!!");
              						adapter.read();
              					}
              					
              					cmd.release();
              				}
              				
              				@Override
              				public void close() {
              					System.out.println("Server Connection closed");
              				}
              			});
              			
              			//tunnel.read();
          			
          				ch.pipeline().addLast(
          						//new SslHandler(SslContextFactory.getServerEngine()),
          						
          						// TODO put Zlib encoding directly into CtpHandler so "read" works better
      	            		  //new JdkZlibEncoder(ZlibWrapper.ZLIB),
    	            		  //new JdkZlibDecoder(ZlibWrapper.ZLIB),	            		  
								//new LoggingHandler(LogLevel.INFO),
    	            		  
                                //new LoggingHandler(LogLevel.INFO),
          						new CtpHandler(adapter, true)
                		 );
          			    
          				/* server not responsible for keep alive
          			    Task alive = new Task().withWork(new IWork() {			
          					@Override
          					public void run(TaskRun trun) {
          			            //System.out.println("Ctp Client - Active: " + chx.isActive());
          			            
          			            try {
          			            	adapter.sendCommand(CtpCommand.ALIVE);
          						}
          						catch (Exception x) {
          							System.out.println("Ctp-F Client error: " + x);
          						}
          			            
          			            trun.complete();
          					}
          				});
          			    
          			    Hub.instance.getScheduler().runEvery(alive, 30);
          				* /
              		}
              	});

    	    // Start the server.
    	    sb.bind(8181).sync();
        } 
        catch (Exception x) {
        	System.out.println("FS Start Error: " + x);
        }
		*/
		
		/*
		System.out.println("Folder Listing: ");
		
		IFileSelector selector = this.fsd.select(
				new FileSelection()
					.withRelativeTo("/User")
					.withFileSet("/karabiner", 2)
					.withForListing(true)
		);
		
		selector.forEach(new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				System.out.println("Path: " + this.getResult().getPath());
				System.out.println("RelativePath: " + this.getResult().path().subpath(selector.selection().relativeTo()));
			}
		});
		
		System.out.println();
		System.out.println("Folder Detail: ");
		
		IFileSelector selector2 = this.fsd.select(
				new FileSelection()
					.withFileSet("/User/karabiner", 0)
		);
		
		selector2.forEach(new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				System.out.println("FileName: " + this.getResult().getName());
				System.out.println("Path: " + this.getResult().getPath());
				System.out.println("LastModified: " + this.getResult().getModification());
			}
		});
		*/
		
		//CommonPath path = new CommonPath("/User/karabiner");
		
		/*
		CommonPath path = new CommonPath("/User/Salt");
		
		this.fsd.getFolderListing(path, new FuncCallback<List<IFileStoreFile>>() {			
			@Override
			public void callback() {
				if (this.hasErrors()) {
					System.out.println("Folder Listing Failed: " + this.getMessage());
					return;					
				}
				
				for (IFileStoreFile file : this.getResult()) {
					System.out.println();
					System.out.println("FileName: " + file.getName());
					System.out.println("IsFolder: " + file.isFolder());
					System.out.println("LastModified: " + file.getModification());
					System.out.println("Size: " + file.getSize());
				}
				
				System.out.println();
			}
		});
		*/
	}
	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		// find the correct file store for this domain
		FileSystemDriver fs = this.domainFsd.get(request.getContext().getUserContext().getDomainId());
		
		if (fs == null)
			fs = this.fsd;
		
		if ("FileStore".equals(feature)) {
			if ("FileDetail".equals(op)) {
				this.handleFileDetail(request, fs);
				return;
			}
			
			if ("DeleteFile".equals(op)) {
				this.handleDeleteFile(request, fs);
				return;
			}
			
			if ("DeleteFolder".equals(op)) {
				this.handleDeleteFolder(request, fs);
				return;
			}
			
			if ("AddFolder".equals(op)) {
				this.handleAddFolder(request, fs);
				return;
			}
			
			if ("ListFiles".equals(op)) {
				this.handleListFiles(request, fs);
				return;
			}
			
			if ("StartUpload".equals(op)) {
				this.handleStartUpload(request, fs);
				return;
			}

			if ("FinishUpload".equals(op)) {
				this.handleFinishUpload(request, fs);
				return;
			}
			
			if ("StartDownload".equals(op)) {
				this.handleStartDownload(request, fs);
				return;
			}

			if ("FinishDownload".equals(op)) {
				this.handleFinishDownload(request, fs);
				return;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	public void handleFileDetail(final TaskRun request, FileSystemDriver fs) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
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
				fdata.setField("LastModified", fi.getModificationTime());
				fdata.setField("Size", fi.getSize());
				
				String meth = rec.getFieldAsString("Method");
				
				if (StringUtil.isEmpty(meth) || fi.isFolder()) {
					request.setResult(fdata);
					request.complete();
					return;
				}

				fi.hash(meth, new FuncCallback<String>() {						
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
	
	public void handleDeleteFile(TaskRun request, FileSystemDriver fs) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
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

				fi.remove(new OperationCallback() {					
					@Override
					public void callback() {
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleDeleteFolder(TaskRun request, FileSystemDriver fs) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = new CommonPath(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
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

				fi.remove(new OperationCallback() {					
					@Override
					public void callback() {
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleAddFolder(TaskRun request, FileSystemDriver fs) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = new CommonPath(fpath);
		
		fs.addFolder(path, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				request.complete();
			}
		});
	}
	
	public void handleListFiles(TaskRun request, FileSystemDriver fs) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = new CommonPath(fpath);
		
		fs.getFolderListing(path, new FuncCallback<List<IFileStoreFile>>() {			
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
					fdata.setField("LastModified", file.getModificationTime());
					fdata.setField("Size", file.getSize());
					
					files.addItem(fdata);
				}
				
				request.returnValue(files);
			}
		});
	}
	
	public void handleStartUpload(TaskRun request, FileSystemDriver fs) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
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
				this.getResult().openWrite(chan, new FuncCallback<RecordStruct>() {					
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
	
	public void handleFinishUpload(TaskRun request, FileSystemDriver fs) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		if ("Faliure".equals(rec.getFieldAsString("Status"))) {
			request.warn("File upload incomplete or corrupt: " + path);
			
			if (!rec.isFieldEmpty("Note"))
				request.warn("File upload note: " + rec.getFieldAsString("Note"));
			
			request.complete();
			return;
		}
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
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
				
				IFileStoreFile fresult =  this.getResult();
				
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
					
					if (!request.hasErrors())
						FileServerService.this.watch("Upload", fresult);
					
					if (!rec.isFieldEmpty("Note"))
						request.info("File upload note: " + rec.getFieldAsString("Note"));
					
					request.complete();
				};
				
				// TODO wait for file to flush out?
				if ("Size".equals(selEvidenceType)) {
					Long src = evidinfo.getFieldAsInteger("Size");
					long dest = fresult.getSize();
					boolean match = (src == dest);
					
					if (match)
						request.info("File sizes match");
					else
						request.error("File sizes do not match");
					
					afterVerify.accept(match);
				}
				else if (StringUtil.isNotEmpty(selEvidenceType)) {
					fresult.hash(selEvidenceType, new FuncCallback<String>() {						
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
	
	public Message handleStartDownload(TaskRun request, FileSystemDriver fs) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
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
				
				this.getResult().openRead(chan, new FuncCallback<RecordStruct>() {					
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
	
	public void handleFinishDownload(TaskRun request, FileSystemDriver fs) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = new CommonPath(fpath);
		
		if ("Faliure".equals(rec.getFieldAsString("Status"))) {
			request.warn("File download incomplete or corrupt: " + path);
			
			if (!rec.isFieldEmpty("Note"))
				request.warn("File download note: " + rec.getFieldAsString("Note"));
			
			request.complete();
			return;
		}
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
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
					this.getResult().hash(selEvidenceType, new FuncCallback<String>() {						
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
	
	public void watch(String op, IFileStoreFile file) {
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
	}
}
