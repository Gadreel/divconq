package divconq.ctp.f;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import divconq.ctp.CtpAdapter;
import divconq.ctp.CtpClient;
import divconq.ctp.CtpCommand;
import divconq.ctp.CtpConstants;
import divconq.ctp.cmd.RequestCommand;
import divconq.ctp.cmd.SimpleCommand;
import divconq.ctp.cmd.ProgressCommand;
import divconq.ctp.stream.CtpStreamDest;
import divconq.ctp.stream.CtpStreamSource;
import divconq.ctp.stream.FileSourceStream;
import divconq.ctp.stream.FolderDumpStream;
import divconq.ctp.stream.IStream;
import divconq.ctp.stream.IStreamDest;
import divconq.ctp.stream.NullStream;
import divconq.ctp.stream.StreamUtil;
import divconq.filestore.local.FileSystemDriver;
import divconq.filestore.select.FileSelection;
import divconq.filestore.select.FileSelectionMode;
import divconq.hub.Hub;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.IOperationObserver;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationObserver;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskRun;

/*
Title: Download Flow
Participant Client Code
Participant File Store A
Participant Pipe A
Participant Handler A
Participant Adapter A
Participant Server
Client Code->Adapter A: select
Adapter A->Server: SELECT
Server->Adapter A: SUCCESS
Adapter A->Handler A: SUCCESS
Handler A->Client Code: success
Client Code->Handler A: set pipe
Client Code->Adapter A: read
Adapter A->Server: READ
Server->Adapter A: BLOCK
Adapter A->Handler A: BLOCK
Handler A->Pipe A: BLOCK
Pipe A->File Store A: BLOCK
File Store A->Pipe A: request
Pipe A->Handler A: request
Handler A->Adapter A: read
Adapter A->Server: READ
Server->Adapter A: BLOCK*\netc...
Server->Adapter A: FINAL
Adapter A->Handler A: FINAL
Handler A->Client Code: final
Client Code->Handler A: close pipe
Client Code->Adapter A: success
Adapter A->Server: SUCCESS
 * 
 */

public class CtpFClient extends CtpClient {
	protected TaskRun currTask = null;
	protected CtpStreamSource readStream = null;
	
	// when using this be sure to issue "read()" in callback
	public void selectFiles(FileSelection sel, FuncCallback<RecordStruct> cb) {
		RequestCommand cmd = new RequestCommand();
		
		RecordStruct params = new RecordStruct(
				new FieldStruct("Op", CtpConstants.CTP_F_OP_SELECT),
				new FieldStruct("Select", sel.toInstructions())
		);
		
		cmd.setBody(params);
		
		try {
			this.adapter.sendCommand(cmd, cb);
		} 
		catch (Exception x) {
			System.out.println("ctp client send error: " + x);
		}
	}
	
	public void handle(CtpCommand cmd, CtpAdapter adapter) throws Exception {
		OperationContext ctx = adapter.getContext();
		
		ctx.touch();
		
		//System.out.println("Client got command: " + cmd.getCmdCode());
		
		if (cmd instanceof ProgressCommand) {
			if (ctx != null) {
				// put the call back into the work pool, don't tie up the IO thread 
				Task t = new Task()
					.withContext(ctx.subContext())
					.withWork(new IWork() {
						@Override
						public void run(TaskRun trun) {
							trun.getContext().setAmountCompleted(((ProgressCommand)cmd).getAmount());
							
							adapter.read();
							
							trun.complete();
						}
					});
				
				Hub.instance.getWorkPool().submit(t);
			}
			
			return;
		}
		
		if (cmd.getCmdCode() == CtpConstants.CTP_F_CMD_STREAM_FINAL) {
			//System.out.println("Client got Final!");
			TaskRun brun = this.currTask;
			CtpStreamSource src = this.readStream;
			
			if (brun == null) {
				System.out.println("Client READ error, missing task!!!!! " + ((BlockCommand) cmd).getPath());
			}
			else if (src == null) {
				System.out.println("Client READ error, missing src!!!!! " + ((BlockCommand) cmd).getPath());
			}
			else {
				src.setFinal();
				
				brun.resume();
			}			
			
			return;
		}
		
		if (cmd instanceof BlockCommand) {
			//System.out.println("Client got block: " + ((BlockCommand) cmd).getPath());
			//System.out.println("Client got block - is folder: " + ((BlockCommand) cmd).isFolder());
			//System.out.println("Client got block - size: " + ((BlockCommand) cmd).getSize());
			
			TaskRun brun = this.currTask;
			CtpStreamSource src = this.readStream;
			
			if (brun == null) {
				System.out.println("Client READ error, missing task!!!!! " + ((BlockCommand) cmd).getPath());
			}
			else if (src == null) {
				System.out.println("Client READ error, missing src!!!!! " + ((BlockCommand) cmd).getPath());
			}
			else {
				src.addNext((BlockCommand)cmd);
				
				brun.resume();
				
				/*
			Task t = new Task()
				.withRootContext()		// TODO use session context
				.withWork(new IWork() {											
					@Override
					public void run(TaskRun trun) {
						if (((BlockCommand) cmd).getData() == null)
							System.out.println("Client processed empty block");
						else
							System.out.println("Client processed block: " + ((BlockCommand) cmd).getData().readableBytes());
    					
						adapter.read();
      					
						cmd.release();
						
						trun.complete();
					}
				});
				*/
			}
			
			return;
		}
		
		// get more, unless a stream command - then let it handle automatically
		/*
		if (!(cmd instanceof IStreamCommand)) {
			adapter.read();
			
			cmd.release();
		}
		*/
	}
	
	public void writeFrom(OperationCallback cb, IStream... strm) {
		OperationContext ctx = this.adapter.getContext();

		ctx.setAmountCompleted(0);
		
		Task t = new Task()
			.withSubContext()
			.withTitle("Stream Write");
		
		@SuppressWarnings("resource")
		IStreamDest writeStream = new CtpStreamDest(this.adapter, t.getContext());
		
		t.withObserver(new OperationObserver() {
			@Override
			public void completed(OperationContext or) {
				//System.out.println("READ server is complete!!");
				//System.out.println("Context 3: " + OperationContext.get().getOpId());
				
				/*
				try {
					CtpFClient.this.adapter.sendCommand(new ResponseCommand());
				} 
				catch (Exception x) {
					System.out.println("Unable to send SUCCESS for READ: " + x);
				}
				*/
				
				cb.callback();
			}
		});
		
		IStream[] tstrm = new IStream[strm.length + 1];
		
		for (int i = 0; i < strm.length; i++)
			tstrm[i] = strm[i];
		
		tstrm[strm.length] = writeStream;
		
		//System.out.println("strm 0: " + tstrm[0]);
		//System.out.println("strm 1: " + tstrm[1]);
		
		TaskRun trun = StreamUtil.composeStream(t, tstrm); 
		
		this.currTask = trun;
		
		trun.resume();

		// we can read while writing
		this.adapter.read();
	}
	
	public void readTo(OperationCallback cb, IStream... strm) {
		//System.out.println("Context 2: " + OperationContext.get().getOpId());
		
		OperationContext ctx = this.adapter.getContext();

		ctx.setAmountCompleted(0);
		
		this.readStream = new CtpStreamSource(this.adapter);
		
		Task t = new Task()
			.withSubContext()
			.withTitle("Stream Read");
		
		t.withObserver(new OperationObserver() {
			@Override
			public void completed(OperationContext or) {
				//System.out.println("WRITE client is complete!!");
				//System.out.println("Context 3: " + OperationContext.get().getOpId());
				

				/*
				try {
					CtpFClient.this.adapter.sendCommand(new ResponseCommand());
				} 
				catch (Exception x) {
					System.out.println("Unable to send SUCCESS for READ: " + x);
				}
				*/
				
				cb.callback();
				
				adapter.read();
			}
		});
		
		IStream[] tstrm = new IStream[strm.length + 1];
		
		tstrm[0] = this.readStream;
		
		for (int i = 1; i < tstrm.length; i++)
			tstrm[i] = strm[i - 1];
		
		//System.out.println("strm 0: " + tstrm[0]);
		//System.out.println("strm 1: " + tstrm[1]);
		
		TaskRun trun = StreamUtil.composeStream(t, tstrm); 
		
		this.currTask = trun;
		
		trun.resume();
	}
	
	@Override
	public void connect(String host, int port, OperationCallback connCallback) {
		//System.out.println("Context 1: " + OperationContext.get().getOpId());
		
		super.connect(host, port, new OperationCallback() {			
			@Override
			public void callback() {
				adapter.setMapper(CtpfCommandMapper.instance);
			    
				/* TODO ???
			    CtpFClient.this.sendCommand(new InitCommand(), new FuncCallback<RecordStruct>() {					
					@Override
					public void callback() {
					    
					    System.out.println("Ctp F Client - Init complete");
					    
					    adapter.read();
					    
					    connCallback.complete();
					}
				});
				
				*/
				
			    System.out.println("Ctp F Client - Init complete");
			    
			    adapter.read();
			    
			    connCallback.complete();
			}
		});
	}
	
	public void folderListing(String path) {
        FileSelection sel = new FileSelection()
	    	.withMode(FileSelectionMode.Listing)
	    	.withFileSet(path)
	    	.withAttrs(CtpConstants.CTP_F_ATTR_PREFERED);
	
	    try {
	    	this.selectFiles(sel, new FuncCallback<RecordStruct>() {							
				@Override
				public void callback() {
					//System.out.println("Context 4: " + OperationContext.get().getOpId());
					if (this.hasErrors()) {
						System.out.println("Failed Selection: " + this.getMessage());
						
						adapter.read();		// TODO or close?
						return;
					}
					
					try {
						//ctpf.sendCommand(new ReadCommand());
		            	CtpFClient.this.readTo(new OperationCallback() {										
							@Override
							public void callback() {
								//System.out.println("Context 5: " + OperationContext.get().getOpId());
								
								System.out.println("Directory listing completed!");
							}
						},
						new FolderDumpStream());
					} 
					catch (Exception x) {
						System.out.println("Ctp-F Client cb error: " + x);
					}
					
					adapter.read();		// get that folder listing
				}
			});
		}
		catch (Exception x) {
			System.out.println("Ctp-F Client error: " + x);
		}
	}
	
	@Override
	public void close() {
		System.out.println("Client Connection closed");
	}
	
	static public void utilityMenu(Scanner scan) {
		OperationContext ctx = OperationContext.get();
		
		IOperationObserver oo = new OperationObserver() {
			@Override
			public void amount(OperationContext ctx, int v) {
				System.out.println("Progress: " + v);
			}
		};
		
		ctx.addObserver(oo);
		
		CtpFClient ctpf = new CtpFClient();

		CountDownLatch cdl = new CountDownLatch(1);
		
		ctpf.connect("localhost", 8181, new OperationCallback() {				
			@Override
			public void callback() {
				cdl.countDown();
			}
		});
		
		try {
			cdl.await();
		} 
		catch (InterruptedException x) {
			System.out.println("Error connecting: " + x);
			return;
		}
		
		boolean running = true;
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   Hub " + Hub.instance.getResources().getHubId() + " Utility Menu");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  ls on karabiner");
				System.out.println("2)  Folder Listing");
				System.out.println("3)  Test Download");
				System.out.println("4)  Test Upload");
	
				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0:
					ctpf.sendCommand(SimpleCommand.EXIT_SIGN_OUT);
					running = false;
					break;
				case 1: {
					ctpf.folderListing("/User/karabiner");
					break;
				}
				case 2: {
					System.out.println("Folder to list: ");
					ctpf.folderListing(scan.nextLine());
					break;
				}
				case 3: {
		            FileSelection sel = new FileSelection()
		            	.withMode(FileSelectionMode.Expanded)
		            	.withFileSet("/User/Salt/long score 3.txt.out")
		            	.withAttrs(CtpConstants.CTP_F_ATTR_PREFERED, CtpConstants.CTP_F_ATTR_DATA);
		
		            try {
		            	ctpf.selectFiles(sel, 
		            			new FuncCallback<RecordStruct>() {							
				    				@Override
				    				public void callback() {
				    					if (this.hasErrors()) {
				    						System.out.println("Failed Selection: " + this.getMessage());
				    						
				    						ctpf.read();		// TODO or close?
				    						return;
				    					}
				    					
				    					//System.out.println("Context 9: " + OperationContext.get().getOpId());
				    					
				    					try {
				    		            	ctpf.readTo(new OperationCallback() {										
				    							@Override
				    							public void callback() {
				    								//System.out.println("Context 10: " + OperationContext.get().getOpId());
				    								
				    								System.out.println("Download completed!");
				    							}
				    						},
				    						new NullStream());
				    					} 
				    					catch (Exception x) {
				    						System.out.println("Ctp-F Client cb error: " + x);
				    					}
				    					
				    					ctpf.read();		// get that folder listing
				    				}
				    			});
					}
					catch (Exception x) {
						System.out.println("Ctp-F Client error: " + x);
					}
	            
					break;
				}
				case 4: {
					@SuppressWarnings("resource")
					FileSystemDriver fsd = new FileSystemDriver();
					fsd.setRootFolder("/Work/Temp/Dest/x");
					
					FileSelection sel = new FileSelection()
						.withFileSet("/cc-logo-sm.png");
					
					ctpf.writeFrom(new OperationCallback() {										
							@Override
							public void callback() {
								//System.out.println("Context 10: " + OperationContext.get().getOpId());
								
								System.out.println("Upload completed!");
							}
						},
						new FileSourceStream(fsd.select(sel)));
					
					break;
				}
				}
			}
			catch(Exception x) {
				System.out.println("CLI error: " + x);
			}
		}
		
		ctx.removeObserver(oo);
	}

}
