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
package divconq.interchange;

public class ArchiveFile { /* extends RecordStruct implements IFileStoreFile {
	protected ArchiveDriver driver = null;
	protected ArchiveEntry file = null;
	protected FileInputStream input = null;
	protected long offset = 0;
		
	public ArchiveFile() {
		this.setType(Hub.instance.getSchema().getType("dciArchiveFile"));
	}

	public ArchiveFile(ArchiveDriver driver, ArchiveEntry file) {
		this();
		
		this.driver = driver;
		this.file = file;
		
		String name = file.getName();
		
		int pos = name.lastIndexOf('/');
		
		this.setField("Name", (pos == -1) ? name : name.substring(pos + 1));		
		this.setField("Size", file.getSize());
		this.setField("Modified", new DateTime(file.getLastModifiedDate()));
		
		// TODO enhance for "absolute"
		String cwd = driver.getFieldAsString("RootFolder");
		String fpath = file.getName();
		
		this.setField("Path", fpath.substring(cwd.length() + 1).replace('\\', '/'));
		this.setField("FullPath", fpath.replace('\\', '/'));
		this.setField("IsFolder", file.isDirectory());
	}
	
    public ArchiveFile(ArchiveDriver driver, RecordStruct rec) {
		this();
		
		this.driver = driver;
		
		((RecordStruct) this).copyFields(rec);
		
		String cwd = driver.getFieldAsString("RootFolder");
		
		this.driver.getEntry(cwd + "/" + this.getFieldAsString("Path"), new FuncCallback<ArchiveEntry>() {			
			@Override
			public void callback() {
				ArchiveFile.this.file = this.getResult();
				
				ArchiveFile.this.setField("Size", file.getSize());
				ArchiveFile.this.setField("Modified", new DateTime(file.getLastModifiedDate()));
				ArchiveFile.this.setField("FullPath", file.getName().replace('\\', '/'));
				ArchiveFile.this.setField("IsFolder", file.isDirectory());
			}
		});
	}

	@Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	ArchiveFile nn = (ArchiveFile)n;
		nn.driver = this.driver;
    }
    
	@Override
	public Struct deepCopy() {
		ArchiveFile cp = new ArchiveFile();
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	public void dispose() {
		// TODO support this!!!
		super.dispose();
	}
	
	@Override
	public void operation(final StackEntry stack, XElement code) {
		if ("Hash".equals(code.getName())) {
			String meth = stack.stringFromElement(code, "Method");
			
			// TODO support other methods
			if (StringUtil.isEmpty(meth) || !"Sha2".equals(meth)) {
				// TODO log
				stack.resume();
				return;
			}
			
	        final Struct var = stack.refFromElement(code, "Target");

			if (var instanceof ScalarStruct) { 				
				this.hash(meth, new FuncCallback<String>() {					
					@Override
					public void callback() {
						((ScalarStruct)var).adaptValue(this.getResult());
						stack.resume();
					}
				});
				
				return;
			}
			else {
				// TODO log
			}
			
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}

	@Override
	public void copyTo(final OutputStream out, final OperationCallback callback) {
		this.getInputStream(new FuncCallback<InputStream>() {			
			@Override
			public void callback() {
				// TODO check for errors
				
				InputStream in = this.getResult();
				
				try {
					CopyUtils.copy(in, out);
				} 
				catch (IOException x) {
					callback.error(1, "Unable to write file");		// TODO codes
				}
				finally {
		            IOUtils.closeQuietly(in);
		            IOUtils.closeQuietly(out);
				}
				
				callback.completed();
			}
		});
	}
	
	public class DestinationDriver implements IDestinationDriver {						
		protected FileOutputStream out = null;
		protected ISessionStream source = null;
		protected Session session = null;
		
		@Override
		public void setSource(ISessionStream v) {
			this.source = v;
		}
		
		public DestinationDriver(Session session) {
			this.session = session;
			
			/* TODO
			try {
				this.out = new FileOutputStream(ArchiveFile.this.file);
			} 
			catch (FileNotFoundException x) {
				// TODO 
				System.out.println("Error while creating consumer");
			}
			* /
		}

		@Override
		public void present(Block data) {
			try {
				if (data.isAbort()) {
					this.session.getAdapter().releaseSource();
					this.source = null;
					System.out.println("Error while uploading, aborted!");		// TODO
					this.out.close();
					return;
				}
				
				Memory mem = data.getData();
				
				if (mem != null)
					this.out.write(mem.toArray());

				if (data.isDone()) {
					this.out.flush();
					this.out.close();
				}
				
				/* TODO we need to restore this #NETTY
				Message rmsg = new Message();
				rmsg.setField("Op", data.isDone() ? "Done" : "Next");
				this.source.reaction(rmsg);		// give me more
				* /

				if (data.isDone()) {
					this.session.getAdapter().releaseSource();
					this.source = null;
				}
			} 
			catch (IOException x) {
				// TODO 
				System.out.println("Error while uploading");
			}
		}
	}	
	
	public class SourceDriver implements ISourceDriver {						
		protected FileInputStream in = null;
		protected ISessionStream dest = null;
		protected long offset = 0;
		
		@Override
		public void setDestination(ISessionStream v) {
			this.dest = v;
		}
		
		public SourceDriver(Session session) {
			/* TODO
			try {
				this.in = new FileInputStream(ArchiveFile.this.file);
			} 
			catch (FileNotFoundException x) {
				// TODO 
				System.out.println("Error while creating consumer");
			}
			* /
		}

		@Override
		public void collect(FuncCallback<Block> callback) {
			try {
				Block b = new Block();
				
				Memory mem = new Memory(16384);		// TODO config 
				b.setData(mem);
				b.setOffset(this.offset);

				int amt = mem.copyFromStream(this.in, 16384);

				if (amt < 16384) {
					b.setDone(true);
					this.in.close();
				}

				this.offset += amt;
				
				mem.setPosition(0);
				
				callback.setResult(b);
			} 
			catch (IOException x) {
				// TODO 
				System.out.println("Error while downloading");
			}
			
			callback.completed();
		}

		@Override
		public void abort() {
			try {
				this.in.close();
			} 
			catch (IOException x) {
				// TODO 
				System.out.println("Error while downloading");
			}
		}
	}

	@Override
	public void openRead(Session session, final FuncCallback<ISourceDriver> callback) {
		callback.setResult(new SourceDriver(session));
		callback.completed();
	}	

	@Override
	public void openWrite(Session session, final FuncCallback<IDestinationDriver> callback) {
		callback.setResult(new DestinationDriver(session));
		callback.completed();
	}

	@Override
	public void hash(String method, final FuncCallback<String> callback) {
		// TODO support something other than Sha2 - see params
		
		this.getInputStream(new FuncCallback<InputStream>() {			
			@Override
			public void callback() {
				callback.copyMessages(this);
				
				InputStream in = this.getResult();
				
				if (!callback.hasErrors())
					callback.setResult(HashUtil.getSha2(in));
				
				if (in != null)
					IOUtils.closeQuietly(in);
				
				callback.completed();
			}
		});
	}

	@Override
	public void getInputStream(final FuncCallback<InputStream> callback) {
		this.driver.archive.getInputStream(new FuncCallback<InputStream>() {				
			@Override
			public void callback() {
				// TODO dumping everything into memory not a good idea with big files - make those secure temp files
				
				try {
					ZipArchiveInputStream zin = new ZipArchiveInputStream(this.getResult());		// TODO not just ZIP
					
			        ArchiveEntry entry = zin.getNextEntry();
			        
			        String path = ArchiveFile.this.getFieldAsString("FullPath");
			        
			        while(entry != null) {
			        	String ename = entry.getName();
			        	
			        	if (ename.equals(path)) {
			        		int esize = (int) entry.getSize();
			        		
			        		if (esize > 0) {
			            		int eleft = esize;
			            		byte[] buff = new byte[esize];
			            		int offset = 0;
			            		
			            		// TODO sometimes it takes more than on read to get an entry - who knows why
			            		// anyway, there is probably an nicer way to do this (see also JarLibLoader)
			            		while (offset < esize) {
					            	int d = zin.read(buff, offset, eleft);
					            	offset += d;
					            	eleft -= d;
			            		}
			            		
			            		//Memory treem = new Memory(buff);
			            		//treem.setPosition(0);

			            		callback.setResult(new ByteArrayInputStream(buff));
			            		break;
			        		}
			        	}
			        	
			        	entry = zin.getNextEntry();
			        }	
				}
				catch (Exception x) {
					// TODO log
				}
				
				callback.completed();
			}
		});		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFullPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DateTime getMofificationTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isFolder() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void rename(String name, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setModificationTime(DateTime time, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getAttribute(String name, FuncCallback<Struct> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAttribute(String name, Struct value,
			OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}
	*/
}
