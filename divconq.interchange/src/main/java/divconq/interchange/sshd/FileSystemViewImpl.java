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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;

import divconq.api.ApiSession;
import divconq.filestore.CommonPath;

public class FileSystemViewImpl implements FileSystemView {
	protected SshFile root = null;
	protected ApiSession api = null;
	
	public SshFile getRoot() {
		return this.root;
	}
	
	public ApiSession getApi() {
		return this.api;
	}
	
    public FileSystemViewImpl(ApiSession api) {
    	this.root = new RootFile();
    	this.api = api;
    }

    /**
     * Get file object.
     */
    public SshFile getFile(String file) {
    	if ("/".equals(file) || ".".equals(file))
    		return this.root;
    	
    	CommonPath path = new CommonPath(file);

        return new SshFileImpl(this, path); 
    }

	@Override
	public SshFile getFile(SshFile dir, String file) {
    	CommonPath path = new CommonPath(dir + "/" + file);

        return new SshFileImpl(this, path); 
	}

	@Override
	public FileSystemView getNormalizedView() {
		return this;
	}

    public class RootFile implements SshFile {
    	@Override
        public String getAbsolutePath() {
            return "/";
        }
    	@Override
        public String getName() {
            return "/";
        }
    	@Override
        public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
            return null;
        }
    	@Override
        public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
            throw new UnsupportedOperationException();
        }
    	@Override
        public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
            return null;
        }
    	@Override
        public void setAttribute(Attribute attribute, Object value) throws IOException {
            throw new UnsupportedOperationException();
        }
    	@Override
        public String readSymbolicLink() throws IOException {
            return null;
        }
    	@Override
        public void createSymbolicLink(SshFile destination) throws IOException {
        }
    	@Override
        public String getOwner() {
            return null;
        }
    	@Override
        public boolean isDirectory() {
            return true;
        }
    	@Override
        public boolean isFile() {
            return false;
        }
    	@Override
        public boolean doesExist() {
            return true;
        }
    	@Override
        public boolean isReadable() {
            return true;
        }
    	@Override
        public boolean isWritable() {
            return false;
        }
    	@Override
        public boolean isExecutable() {
            return false;
        }
    	@Override
        public boolean isRemovable() {
            return false;
        }
    	@Override
        public SshFile getParentFile() {
            return null;
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
        public long getSize() {
            return 0;
        }
    	@Override
        public boolean mkdir() {
            return false;
        }
    	@Override
        public boolean delete() {
            return false;
        }
    	@Override
        public boolean create() throws IOException {
            return false;
        }
    	@Override
        public void truncate() throws IOException {
        }
    	@Override
        public boolean move(SshFile destination) {
            return false;
        }
    	@Override
        public List<SshFile> listSshFiles() {
            List<SshFile> list = new ArrayList<SshFile>();
            //new NccSshFile(this, path, fileObj)
            //list.add(createNativeSshFile(display, new File(roots.get(root)), userName));
            return list;
        }
    	@Override
        public OutputStream createOutputStream(long offset) throws IOException {
            return null;
        }
    	@Override
        public InputStream createInputStream(long offset) throws IOException {
            return null;
        }
    	@Override
        public void handleClose() throws IOException {
        }
    }
}
