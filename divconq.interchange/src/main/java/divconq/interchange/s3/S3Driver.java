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
package divconq.interchange.s3;

public class S3Driver { /*extends RecordStruct implements IFileStoreDriver {
	protected AmazonS3Client s3 = null;
	protected IMimeProvider mimes = null;
	
	@Override
	public void setMimeProvider(IMimeProvider v) {
		this.mimes = v;
	}
	
	public S3Driver() {
		this.setField("Scanner", new S3Scanner(this));
		this.setField("RootFolder", ".");
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    }
    
	@Override
	public Struct deepCopy() {
		S3Driver cp = new S3Driver();
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	public void dispose() {
		this.close(null);
		
		super.dispose();
	}

	/*
	@Override
	public void toBuilder(ICompositeBuilder builder) throws BuilderStateException {
		builder.startRecord();
		
		for (FieldStruct f : this.fields.values()) 
			f.toBuilder(builder);
		
		// TODO add in FS specific fields
		
		builder.endRecord();
	}
	
	@Override
	public Struct select(PathPart... path) {
		if (path.length > 0) {
			PathPart part = path[0];
			
			if (part.isField()) {			
				String fld = part.getField();
				
				if ("Scanner".equals(fld))
					return this.search;
			}			
		}
		
		return super.select(path);
	}
	* /
	
	@Override
	public void connect(RecordStruct params, OperationCallback callback) {		
		this.copyFields(params);
		
		// TODO clean and start new this.channels.dispose()
		
        this.s3 = new AmazonS3Client(new AWSCredentials() {			
			@Override
			public String getAWSSecretKey() {
				return S3Driver.this.getFieldAsString("AwsSecretKey");
			}
			
			@Override
			public String getAWSAccessKeyId() {
				return S3Driver.this.getFieldAsString("AwsAccessKey");
			}
		});
		
		System.out.println("cwd: " + this.getFieldAsString("RootFolder"));
		
		/*
        for (Bucket bucket : this.s3.listBuckets()) 
            System.out.println(" - " + bucket.getName());
        
        System.out.println();
        
        System.out.println("Listing objects");
        
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                .withBucketName("chron-present"));
        
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            System.out.println(" - " + objectSummary.getKey() + "  " +
                               "(size = " + objectSummary.getSize() + ")");
        }
        
        System.out.println();   
		* /
		
		if (callback == null)
			return;
		
		callback.completed();
	}
	
	@Override
	public void close(OperationCallback callback) {
		if (callback == null)
			return;
		
		callback.completed();
	}
	
	@Override
	public void operation(final StackEntry stack, final XElement codeEl) {
		if ("Connect".equals(codeEl.getName())) {
			this.connect(null, new OperationCallback() {				
				@Override
				public void callback() {
					stack.resume();
				}
			});

			return;
		}
		
		if ("Close".equals(codeEl.getName())) {
			this.close(new OperationCallback() {				
				@Override
				public void callback() {
					stack.resume();
				}
			});

			return;
		}
		
		if ("Put".equals(codeEl.getName())) {
			
			// TODO integrate with put method below
			
			Struct src = stack.refFromElement(codeEl, "Source");

			if (!(src instanceof IFileStoreFile) && ! (src instanceof RecordStruct)) {
				// TODO log wrong type
				stack.resume();
				return;
			}
			
			boolean relative = stack.boolFromElement(codeEl, "Relative", true);
			
			this.put((IFileStoreFile)src, relative, new FuncCallback<IFileStoreFile>() {				
				@Override
				public void callback() {
					// TODO check errors
					
			        String handle = stack.stringFromElement(codeEl, "Handle");

					if (handle != null) 
			            stack.addVariable(handle, (Struct) this.getResult());
					
					stack.resume();
				}
			});
			
			return;
		}
		
		
		if ("PutAll".equals(codeEl.getName())) {
			
			// TODO integrate with put method below
			
			Struct src = stack.refFromElement(codeEl, "Source");

			if (src == null) {
				// TODO log missing
				stack.resume();
				return;
			}

			if (!(src instanceof IItemCollection)) {
				// TODO log wrong type
				stack.resume();
				return;
			}
			
			boolean relative = stack.boolFromElement(codeEl, "Relative", true);
			
			this.putAll((IItemCollection)src, relative, new OperationCallback() {				
				@Override
				public void callback() {
					// TODO check errors
					System.out.println("done");
					
					stack.resume();
				}
			});
			
			return;
		}
		
		/*
		if ("GetInfo".equals(codeEl.getName())) {
			String path = stack.stringFromElement(codeEl, "Path");

			if (StringUtil.isEmpty(path)) {
				// TODO log missing
				stack.resume();
				return;
			}
			
	        String handle = stack.stringFromElement(codeEl, "Handle");

			if (handle != null) 
	            stack.addVariable(handle, new SftpFile(SftpDriver.this, new RecordStruct(new FieldStruct("Path", path))));
			
			stack.resume();
			return;
		}
		
		if ("Put".equals(codeEl.getName())) {
			Struct src = stack.refFromElement(codeEl, "Source");

			if (src == null) {
				// TODO log missing
				stack.resume();
				return;
			}

			if (!(src instanceof IFileStoreFile) && ! (src instanceof RecordStruct)) {
				// TODO log wrong type
				stack.resume();
				return;
			}
			
			RecordStruct rsrc = (RecordStruct)src;
			final IFileStoreFile ssrc = (IFileStoreFile)src;
			boolean relative = stack.boolFromElement(codeEl, "Relative", true);
			
			String cwd = this.getFieldAsString("RootFolder");
			
			String dfilepath = cwd + "/" + (relative ? rsrc.getFieldAsString("Path") : rsrc.getFieldAsString("Name"));
			
			System.out.println("copied to: " + dfilepath);
			
			final File dest = new File(dfilepath);
			dest.getParentFile().mkdirs();
			
			try {
				final FileOutputStream out = new FileOutputStream(dest);
				
				ssrc.copyTo(out, new OperationCallback() {				
					@Override
					public void callback() {
						// TODO improve, check abort, etc
						
						try {
							out.close();
						} 
						catch (IOException x) {
						}
						
				        String handle = stack.stringFromElement(codeEl, "Handle");

						if (handle != null) 
				            stack.addVariable(handle, new SftpFile(SftpDriver.this, dest));
						
						stack.resume();
					}
				});
				
				return;
			}
			catch (Exception x) {
				// TODO
				//ssrc.abort();
			}			
			
			stack.resume();
			return;
		}
		*/
		
		/*
		if ("ChangeDirectory".equals(code.getName())) {
			String path = stack.stringFromElement(code, "Path");
			
			if (StringUtil.isEmpty(path)) {
				// TODO log
				stack.resume();
				return;
			}
			
			this.cwd = new File(path);
			
			stack.resume();
			return;
		}
		
		if ("ScanFilter".equals(code.getName())) {
			String path = stack.stringFromElement(code, "Path");
			
			...
			
			if (StringUtil.isEmpty(path)) {
				// TODO log
				stack.resume();
				return;
			}
			
			this.cwd = new File(path);
			
			stack.resume();
			return;
		}
		* /
		
		//System.out.println("fs operation: " + code);
		
		super.operation(stack, codeEl);
	}

	@Override
	public IFileStoreFile getFile(RecordStruct file) {
		return new S3File(this, file);
	}

	@Override
	public void getFileDetail(String path, FuncCallback<IFileStoreFile> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getRootFolder(FuncCallback<String> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRootFolder(String path, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addFolder(String path, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeFolder(String path, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void queryFeatures(FuncCallback<RecordStruct> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void customCommand(RecordStruct params, FuncCallback<RecordStruct> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IFileStoreScanner getScanner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getFolderListing(String path, FuncCallback<List<IFileStoreFile>> callback) {
		// TODO Auto-generated method stub
		
		//this.s3.listObjects(new ListObjectsRequest(bucketName, prefix, marker, delimiter, maxKeys));
	}

	@Override
	public void getFolderListing2(String path, FuncCallback<ListStruct> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void put(final IFileStoreFile source, boolean relative, final FuncCallback<IFileStoreFile> callback) {
		if (source == null) {
			// TODO log missing
			callback.completed();
			return;
		}

		if (! (source instanceof RecordStruct)) {
			// TODO log wrong type
			callback.completed();
			return;
		}
		
		final RecordStruct rsrc = (RecordStruct)source;
		
		String cwd = this.getFieldAsString("RootFolder");
		
		final String dfilepath = cwd + "/" + (relative ? rsrc.getFieldAsString("Path") : rsrc.getFieldAsString("Name"));
		
		source.getInputStream(new FuncCallback<InputStream>() {			
			@Override
			public void callback() {
				// TODO check errors
				
				final InputStream in = this.getResult();
				
				ObjectMetadata md = new ObjectMetadata();
				md.setContentLength(source.getSize());   //  rsrc.getFieldAsInteger("Size"));
				// TODO other meta data too?
				
				// TODO missing stuff!!!
				
		        S3Driver.this.s3.putObject(new PutObjectRequest(S3Driver.this.getFieldAsString("Bucket"), dfilepath, in, md));
		        
				callback.setResult(new S3File(S3Driver.this, rsrc));
				callback.callback();

		        /*
				S3Driver.this.getChannel(new FuncCallback<ChannelSftp>() {						
					@Override
					public void callback() {
						ChannelSftp channel = this.getResult();
						
						try {
							System.out.println("copied to sftp: " + dfilepath);
							
							// TODO mkdirs
							// dest.getParentFile().mkdirs();
							
							channel.mkdir(dfileparent);
							
							channel.put(in, dfilepath);
							
							@SuppressWarnings("rawtypes")
							Vector flist = channel.ls(dfilepath);
							
							if (flist.size() < 1) {
								// TODO log
							}
							else {	
								LsEntry entry = (LsEntry)flist.get(0);					
								callback.setResult(new S3File(S3Driver.this, entry, dfilepath));
							}
							
							callback.completed();
						}
						catch (Exception x) {
							// TODO
							//ssrc.abort();
							
							callback.completed();
						}			
						finally {
							S3Driver.this.releaseChannel(channel);
						}
					}
				});
				* /
			}
		});
	}

	@Override
	public void put(InputStream in, long size, final IFileStoreFile dest, boolean relative, final OperationCallback callback) {
		this.put(in, size, dest, relative, null, callback);
	}
	
	public void put(InputStream in, long size, final IFileStoreFile dest, boolean relative, String attachmentname, final OperationCallback callback) {
		if (in == null) {
			// TODO log missing
			callback.completed();
			return;
		}
		
		if (dest == null) {
			// TODO log missing
			callback.completed();
			return;
		}

		if (! (dest instanceof RecordStruct)) {
			// TODO log wrong type
			callback.completed();
			return;
		}
		
		final RecordStruct rsrc = (RecordStruct)dest;
		
		String cwd = this.getFieldAsString("RootFolder");
		int pos = cwd.indexOf('/', 1);
		String bucket = cwd.substring(1, pos);
		cwd = cwd.substring(pos + 1);
		
		final String dfilepath = cwd + "/" + (relative ? rsrc.getFieldAsString("Path") : rsrc.getFieldAsString("Name"));
		
		// TODO check that path does not dip below working directory via ..
		
		File temp = null;
		
		// TODO error checking
		try {
			// if size is not known then save to temp file first
			if (size == -1) {
				temp = File.createTempFile(UUID.randomUUID().toString(), null);
				FileOutputStream fos = new FileOutputStream(temp);
				
				IOUtils.copy(in, fos);
				
	            IOUtils.closeQuietly(fos);
	            
	            temp.deleteOnExit();
	            
	            in = new FileInputStream(temp);
	            size = temp.length();
			}
			
			ObjectMetadata md = new ObjectMetadata();
			//md.addUserMetadata("Modified", value);
			md.setContentLength(size);
			// TODO store other attributes too?
			
			if (this.mimes != null)
				md.setContentType(this.mimes.getMimeForExt(FilenameUtils.getExtension(dfilepath)));

			if (StringUtil.isNotEmpty(attachmentname))
				md.setContentDisposition("attachment; filename=\"" + NetUtil.urlEncodeUTF8(attachmentname) + "\"");
			
	        this.s3.putObject(new PutObjectRequest(bucket, dfilepath, in, md));
	        
	        // TODO change before open source
	        
	        // TODO get permissions from file
	        AccessControlList acl = new AccessControlList();
	        acl.setOwner(new Owner("151f1537f17624dca8ad737bf316bfd5f15bede15d10b9ffcf6073d8c8419025", "LightOfGadrel"));
	        acl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
	        
	        this.s3.setObjectAcl(bucket, dfilepath, acl);
	        
	        if (temp != null)
	        	temp.delete();
		}
		catch (Exception x) {
			// TODO log error
			System.out.println("Error uploading to S3: " + x);
		}
		
		callback.completed();
	}

	@Override
	public void putAll(IItemCollection files, boolean relative, OperationCallback callback) {
		// TODO Auto-generated method stub
		
	}
	*/
}
