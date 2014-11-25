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

package divconq.interchange.sshd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sshd.common.file.SshFile;

import divconq.api.ApiSession;
import divconq.bus.Message;
import divconq.filestore.CommonPath;
import divconq.hub.Hub;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationCallback;
import divconq.log.Logger;
import divconq.struct.RecordStruct;
import divconq.work.IWork;
import divconq.work.TaskRun;

public class SshFileImpl implements SshFile {
    protected CommonPath filePath = null;
    protected FileSystemViewImpl fsView = null;

    protected SshFileImpl(FileSystemViewImpl fsview, CommonPath filePath) {
        if (fsview == null) 
            throw new IllegalArgumentException("fsview can not be null");
        
        if (filePath == null) 
            throw new IllegalArgumentException("fileName can not be null");
        
        this.fsView = fsview;
        this.filePath = filePath;
    }

    @Override
    public String getAbsolutePath() {
    	return this.filePath.toString();
    }
    
    @Override
    public String getName() {
        return this.filePath.getFileName();
    }

    @Override
    public String getOwner() {
        return null;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public boolean doesExist() {
        return false;
    }

    @Override
    public long getSize() {
    	System.out.println("get size called");
    	
        return 0;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public boolean setLastModified(long time) {
    	return false;
    }

    @Override
    public boolean isReadable() {
        return false;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isRemovable() {
    	return false;
    }

    @Override
    public SshFile getParentFile() {
        return this.fsView.getRoot();
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public boolean create() {
    	System.out.println("Create called");
    	
        return true;
    }

    // TODO support offset and such
    @Override
    public OutputStream createOutputStream(final long offset) throws IOException {
    	System.out.println("Create output stream called");
    	
    	final ApiSession api = this.fsView.getApi();
    	final Pipe p = Pipe.open();
    	
    	// TODO
    	final String token = "abc9";
    	
		RecordStruct rec = new RecordStruct();
		
		rec.setField("FileName", this.filePath.getFileName());
		rec.setField("FileSize", 0);
		
		rec.setField("AuthToken", token);
		
		Message msg = new Message("nccUploader", "Deposits", "StartUpload", rec);
		
		// TODO make sure this results in proper user context in local session
		// calling this puts the result into another thread
		api.establishDataStream("Uploading " + this.filePath, "Upload", msg, new FuncCallback<RecordStruct>() {
			@Override
			public void callback() {
				if (this.hasErrors()) { 
		    		Logger.error("Start Upload error: " + this.getMessage());
		    		
		    		try {
			    		// so SFTP knows not to continue
						p.sink().close();
					} 
		    		catch (IOException x) {
						System.out.println("Error closing sshd output stream");;
					}
		    		
		    		return;
				}

				System.out.println("Upload request good, sending stream - need new thread [for keep alive].");
				
				final RecordStruct sinfo = this.getResult();
		    	
				// TODO this means we are limited in number of uploads by size of WorkPool
				// TODO figure out how not to overload the pool
				Hub.instance.getWorkPool().submit(new IWork() {					
					@Override
					public void run(final TaskRun run) {
				    	api.sendStream(p.source(), 0, offset, sinfo.getFieldAsString("ChannelId"), new OperationCallback() {
							@Override
							public void callback() {
								if (this.hasErrors())
									System.out.println("Upload failed!");
								else
									System.out.println("Upload success!");
								
								run.complete();
							}
						});
					}
				});
			}
		});
    	
    	
    	return Channels.newOutputStream(p.sink());
    }

    @Override
    public void truncate() {
    	System.out.println("Truncate called");
    }

    @Override
    public boolean move(final SshFile dest) {
        return false;
    }

    @Override
    public boolean mkdir() {
        return false;
    }

    @Override
    public List<SshFile> listSshFiles() {
    	return null;
    }

    @Override
    public InputStream createInputStream(long offset) {
    	return null;
    }

    @Override
    public void handleClose() {
    	System.out.println("handle close called");
        // Noop
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof SshFileImpl) 
            return this.filePath.equals(((SshFileImpl)obj).filePath);

        return false;
    }
    
    @Override
    public String toString() {
        return this.filePath.toString();
    }

    @Override
    public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
        Map<Attribute, Object> map = new HashMap<Attribute, Object>();
        map.put(Attribute.Size, getSize());
        map.put(Attribute.IsDirectory, isDirectory());
        map.put(Attribute.IsRegularFile, isFile());
        map.put(Attribute.IsSymbolicLink, false);
        map.put(Attribute.LastModifiedTime, getLastModified());
        map.put(Attribute.LastAccessTime, getLastModified());
        map.put(Attribute.Owner, "ncc");
        map.put(Attribute.Group, "ncc");
        EnumSet<Permission> p = EnumSet.noneOf(Permission.class);
        if (isReadable()) {
            p.add(Permission.UserRead);
            p.add(Permission.GroupRead);
            p.add(Permission.OthersRead);
        }
        if (isWritable()) {
            p.add(Permission.UserWrite);
            p.add(Permission.GroupWrite);
            p.add(Permission.OthersWrite);
        }
        if (isExecutable()) {
            p.add(Permission.UserExecute);
            p.add(Permission.GroupExecute);
            p.add(Permission.OthersExecute);
        }
        map.put(Attribute.Permissions, p);
        return map;
    }

    @Override
    public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
    	System.out.println("set attributes called");
    	
        if (!attributes.isEmpty()) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
        return getAttributes(followLinks).get(attribute);
    }

    @Override
    public void setAttribute(Attribute attribute, Object value) throws IOException {
    	System.out.println("set attribute called");
    	
        Map<Attribute, Object> map = new HashMap<Attribute, Object>();
        map.put(attribute, value);
        setAttributes(map);
    }

    @Override
    public String readSymbolicLink() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createSymbolicLink(SshFile destination) throws IOException {
        throw new UnsupportedOperationException();
    }

	@Override
	public boolean isExecutable() {
		return false;
	}
}
