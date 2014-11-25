/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package divconq.interchange.sshd;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.common.util.IoUtils;
import org.apache.sshd.common.util.SelectorUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.common.file.FileSystemAware;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import divconq.hub.Hub;

/**
 * SFTP subsystem
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class SftpSubsystem implements Command, Runnable, SessionAware, FileSystemAware {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static class Factory implements NamedFactory<Command> {

        public Factory() {
        }

        public Command create() {
            return new SftpSubsystem();
        }

        public String getName() {
            return "sftp";
        }
    }

    /**
     * Properties key for the maximum of available open handles per session.
     */
    public static final String MAX_OPEN_HANDLES_PER_SESSION = "max-open-handles-per-session";

    // supporting SFTP v3 only
    // http://tools.ietf.org/wg/secsh/draft-ietf-secsh-filexfer/draft-ietf-secsh-filexfer-02.txt
    // compare versions http://www.greenend.org.uk/rjk/sftp/sftpversions.html
    public static final int LOWER_SFTP_IMPL = 3; // Working implementation from v3
    public static final int HIGHER_SFTP_IMPL = 3; //  .. up to
    public static final String ALL_SFTP_IMPL = "3";
    public static final int  MAX_PACKET_LENGTH = 1024 * 16;

    public static final int SSH_FXP_INIT =             1;
    public static final int SSH_FXP_VERSION =          2;
    public static final int SSH_FXP_OPEN =             3;
    public static final int SSH_FXP_CLOSE =            4;
    public static final int SSH_FXP_READ =             5;
    public static final int SSH_FXP_WRITE =            6;
    public static final int SSH_FXP_LSTAT =            7;
    public static final int SSH_FXP_FSTAT =            8;
    public static final int SSH_FXP_SETSTAT =          9;
    public static final int SSH_FXP_FSETSTAT =        10;
    public static final int SSH_FXP_OPENDIR =         11;
    public static final int SSH_FXP_READDIR =         12;
    public static final int SSH_FXP_REMOVE =          13;
    public static final int SSH_FXP_MKDIR =           14;
    public static final int SSH_FXP_RMDIR =           15;
    public static final int SSH_FXP_REALPATH =        16;
    public static final int SSH_FXP_STAT =            17;
    public static final int SSH_FXP_RENAME =          18;
    public static final int SSH_FXP_READLINK =        19;
    public static final int SSH_FXP_SYMLINK =         20;
    // v6
    //SSH_FXP_BLOCK              22
    //SSH_FXP_UNBLOCK            23
    
    public static final int SSH_FXP_STATUS =         101;
    public static final int SSH_FXP_HANDLE =         102;
    public static final int SSH_FXP_DATA =           103;
    public static final int SSH_FXP_NAME =           104;
    public static final int SSH_FXP_ATTRS =          105;
    public static final int SSH_FXP_EXTENDED =       200;
    public static final int SSH_FXP_EXTENDED_REPLY = 201;

    public static final int SSH_FX_OK =                0;
    public static final int SSH_FX_EOF =               1;
    public static final int SSH_FX_NO_SUCH_FILE =      2;
    public static final int SSH_FX_PERMISSION_DENIED = 3;
    public static final int SSH_FX_FAILURE =           4;
    public static final int SSH_FX_BAD_MESSAGE =       5;
    public static final int SSH_FX_NO_CONNECTION =     6;
    public static final int SSH_FX_CONNECTION_LOST =   7;
    public static final int SSH_FX_OP_UNSUPPORTED =    8;

    public static final int SSH_FILEXFER_ATTR_SIZE =        0x00000001;
    public static final int SSH_FILEXFER_ATTR_UIDGID =      0x00000002;
    public static final int SSH_FILEXFER_ATTR_PERMISSIONS = 0x00000004;
    public static final int SSH_FILEXFER_ATTR_ACMODTIME =   0x00000008; //v3 naming convention
    public static final int SSH_FILEXFER_ATTR_EXTENDED =    0x80000000;

    public static final int SSH_FXF_READ =   0x00000001;
    public static final int SSH_FXF_WRITE =  0x00000002;
    public static final int SSH_FXF_APPEND = 0x00000004;
    public static final int SSH_FXF_CREAT =  0x00000008;
    public static final int SSH_FXF_TRUNC =  0x00000010;
    public static final int SSH_FXF_EXCL =   0x00000020;		// must not already exist - exclusive

    public static final int S_IFMT =   0170000;  // bitmask for the file type bitfields
    public static final int S_IFSOCK = 0140000;  // socket
    public static final int S_IFLNK =  0120000;  // symbolic link
    public static final int S_IFREG =  0100000;  // regular file
    public static final int S_IFBLK =  0060000;  // block device
    public static final int S_IFDIR =  0040000;  // directory
    public static final int S_IFCHR =  0020000;  // character device
    public static final int S_IFIFO =  0010000;  // fifo
    public static final int S_ISUID =  0004000;  // set UID bit
    public static final int S_ISGID =  0002000;  // set GID bit
    public static final int S_ISVTX =  0001000;  // sticky bit
    public static final int S_IRUSR =  0000400;
    public static final int S_IWUSR =  0000200;
    public static final int S_IXUSR =  0000100;
    public static final int S_IRGRP =  0000040;
    public static final int S_IWGRP =  0000020;
    public static final int S_IXGRP =  0000010;
    public static final int S_IROTH =  0000004;
    public static final int S_IWOTH =  0000002;
    public static final int S_IXOTH =  0000001;


    private ExitCallback callback;
    private InputStream in;
    private OutputStream out;
    private ServerSession session;
    private boolean closed = false;

    private FileSystemView root;

    private int version;
    private Map<String, Handle> handles = new HashMap<String, Handle>();

    
    protected static abstract class Handle {
        SshFile file;

        public Handle(SshFile file) {
            this.file = file;
        }

        public SshFile getFile() {
            return file;
        }

        public void close() throws IOException {
            file.handleClose();
        }

    }

    protected static class DirectoryHandle extends Handle implements Iterator<SshFile> {
        boolean done;
        // the directory should be read once at "open directory"
        List<SshFile> fileList = null;
        int fileIndex;

        public DirectoryHandle(SshFile file) {
            super(file);
            fileList = file.listSshFiles();
            fileIndex = 0;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }

        public boolean hasNext() {
            return fileIndex < fileList.size();
        }

        public SshFile next() {
            SshFile f = fileList.get(fileIndex);
            fileIndex++;
            return f;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void clearFileList() {
            // allow the garbage collector to do the job
            fileList = null;
        }
    }

    protected static class FileHandle extends Handle {
        int flags;
        OutputStream output;
        long outputPos;
        InputStream input;
        long inputPos;
        long length;

        public FileHandle(SshFile sshFile, int flags) {
            super(sshFile);
            this.flags = flags;
        }

        public int read(byte[] data, long offset) throws IOException {
            if ((flags & SSH_FXF_READ) == 0) {
                throw new IOException("File has not been opened for reading");
            }
            if (input != null && offset >= length) {
                return -1;
            }
            if (input != null && offset != inputPos) {
                IoUtils.closeQuietly(input);
                input = null;
            }
            if (input == null) {
                input = file.createInputStream(offset);
                length = file.getSize();
                inputPos = offset;
            }
            if (offset >= length) {
                return -1;
            }
            int read = input.read(data);
            inputPos += read;
            return read;
        }

        public void write(byte[] data, long offset) throws IOException {
            if ((flags & SSH_FXF_WRITE) == 0) {
                throw new IOException("File has not been opened for writing");
            }
            if ((flags & SSH_FXF_APPEND) != 0) {
                offset = (output != null) ? outputPos : file.getSize();
            }
            if (output != null && offset != outputPos) {
                IoUtils.closeQuietly(output);
                output = null;
            }
            if (output == null) {
                outputPos = offset;
                output = file.createOutputStream(offset);
            }
            output.write(data);
            outputPos += data.length;
        }

        @Override
        public void close() throws IOException {
            IoUtils.closeQuietly(output, input);
            output = null;
            input = null;
            super.close();
        }
    }

    public SftpSubsystem() {}

    public void setSession(ServerSession session) {
        this.session = session;
    }

    public void setFileSystemView(FileSystemView view) {
        this.root = view;
    }

    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void setErrorStream(OutputStream err) {
        //this.err = err;
    }

    protected DataInputStream dis = null;
    protected DataOutputStream dos = null;
    //protected boolean needInput = true;
    
    public void start(Environment env) throws IOException {
        this.dis = new DataInputStream(this.in);
        this.dos = new DataOutputStream(this.out);
        //this.needInput = true;
        
		this.requestInput();
    }
    
    public void close() {
    	if (this.closed)
    		return;
    	
        if (this.dis != null) {
            try {
            	this.dis.close();
            } 
            catch (IOException ioe) {
            	this.log.error("Could not close DataInputStream", ioe);
            }
        }

        if (this.handles != null) {
            for (Map.Entry<String, Handle> entry : this.handles.entrySet()) {
                Handle handle = entry.getValue();
                
                try {
                    handle.close();
                } 
                catch (IOException ioe) {
                	this.log.error("Could not close an open file handle: " + entry.getKey(), ioe);
                }
            }
        }
        
        this.dis = null;
        
        System.out.println("Got close sftp: " + this.session.getId());
        
        this.closed = true;

        this.callback.onExit(0);
    }
    
    public void requestInput() {
        //Hub.instance.getEventLoopGroup().execute(this);
        
        Hub.instance.getEventLoopGroup().schedule(this, 10, TimeUnit.MILLISECONDS);
    }

    public void run() {
    	//System.out.println("check read ");
    	
        try {
        	int avail = dis.available();
        	
        	if (avail == -1) {
        		this.close();
        		return;
        	}
        	
        	if (avail == 0) {
    			this.requestInput();
        		return;
        	}
        	
        	if (avail < 9) {
                log.error("Exception caught in SFTP subsystem: Illegal Argument for command");
                this.close();
        			
        		return;
        	}
        	
            int length = dis.readInt();
            
            if (length < 5) 
                throw new IllegalArgumentException();
            
            Buffer buffer = new Buffer(length + 4);            
            buffer.putInt(length);            
            int nb = length;
            
            while (nb > 0) {
                int l = dis.read(buffer.array(), buffer.wpos(), nb);
                
                if (l < 0) 
                    throw new IllegalArgumentException();
                
                buffer.wpos(buffer.wpos() + l);                
                nb -= l;
            }
            
            this.process(buffer);
        } 
        catch (Throwable t) {
            if (!this.closed && !(t instanceof EOFException))  // Ignore han
                log.error("Exception caught in SFTP subsystem", t);
            
        	this.close();
        } 
    }

    protected void process(Buffer buffer) throws IOException {
        int length = buffer.getInt();
        int type = buffer.getByte();
        int id = buffer.getInt();
        
        switch (type) {
            case SSH_FXP_INIT: {
            	this.log.debug("Received SSH_FXP_INIT (version={})", this.version);
                
                System.out.println("Got init sftp message: " + this.session.getId());
                
                if (length != 5) 
                    throw new IllegalArgumentException();
                
                this.version = id;
                
                if (this.version >= LOWER_SFTP_IMPL) {
                	this.version = Math.min(version, HIGHER_SFTP_IMPL);
                	
                    buffer.clear();
                    buffer.putByte((byte) SSH_FXP_VERSION);
                    buffer.putInt(this.version);
                    
                    this.send(buffer);
                } 
                else {
                    // We only support version 3 (Version 1 and 2 are not common)
                	this.sendStatus(id, SSH_FX_OP_UNSUPPORTED, "SFTP server only support versions " + ALL_SFTP_IMPL);
                }
                
                this.requestInput();

                break;
            }
            case SSH_FXP_OPEN: {
                
                System.out.println("Got open sftp message: " + this.session.getId());
                
                if (this.session.getFactoryManager().getProperties() != null) {
                    String maxHandlesString = this.session.getFactoryManager().getProperties().get(MAX_OPEN_HANDLES_PER_SESSION);
                    
                    if (maxHandlesString != null) {
                        int maxHandleCount = Integer.parseInt(maxHandlesString);
                        
                        if (this.handles.size() > maxHandleCount) {
                        	this.sendStatus(id, SSH_FX_FAILURE, "Too many open handles");
                        	this.requestInput();
                            break;
                        }
                    }
                }

                String path = buffer.getString();
                int pflags = buffer.getInt();
                Map<SshFile.Attribute, Object> attrs = readAttrs(buffer);
                
                this.log.debug("Received SSH_FXP_OPEN (path={}, pflags={}, attrs={})", new Object[] { path, pflags, attrs });
                
                try {
                    SshFile file = resolveFile(path);
                    
                    if (file.doesExist()) {
                        if ((pflags & SSH_FXP_READ) != 0 && !file.isReadable()) {
                        	this.sendStatus(id, SSH_FX_PERMISSION_DENIED, "Can not read " + path);
                        	this.requestInput();
                            return;
                        }
                        
                        if ((pflags & SSH_FXP_WRITE) != 0 && !file.isWritable()) {
                        	this.sendStatus(id, SSH_FX_PERMISSION_DENIED, "Can not write " + path);
                        	this.requestInput();
                            return;
                        }
                        
                        if (((pflags & SSH_FXF_CREAT) != 0) && ((pflags & SSH_FXF_EXCL) != 0)) {
                        	this.sendStatus(id, SSH_FX_FAILURE, path);
                        	this.requestInput();
                            return;
                        }
                    } 
                    else {
                        if (((pflags & SSH_FXF_CREAT) != 0)) {
                            if (!file.isWritable()) {
                                sendStatus(id, SSH_FX_PERMISSION_DENIED, "Can not create " + path);
                            	this.requestInput();
                                return;
                            }
                            
                            if (!file.create()) {
                                sendStatus(id, SSH_FX_NO_SUCH_FILE, "No such file " + path);
                            	this.requestInput();
                                return;
                            }
                        } 
                        else {
                            sendStatus(id, SSH_FX_NO_SUCH_FILE, "No such file " + path);
                        	this.requestInput();
                            return;
                        }
                    }
                    
                    if ((pflags & SSH_FXF_TRUNC) != 0) {
                        if (!file.isWritable()) {
                            sendStatus(id, SSH_FX_PERMISSION_DENIED, "Can not truncate " + path);
                        	this.requestInput();
                            return;
                        }
                        
                        file.truncate();
                    }
                    
                    if ((pflags & SSH_FXF_CREAT) != 0) 
                        file.setAttributes(attrs);
                    
                    String handle = UUID.randomUUID().toString();
                    this.handles.put(handle, new FileHandle(file, pflags));                    
                    this.sendHandle(id, handle);
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage() == null ? "" : e.getMessage());
                }
                
            	this.requestInput();
            	
                break;
            }
            case SSH_FXP_CLOSE: {
                
                System.out.println("Got close sftp message: " + this.session.getId());
                
                String handle = buffer.getString();
                
                this.log.debug("Received SSH_FXP_CLOSE (handle={})", handle);
                
                try {
                    Handle h = this.handles.get(handle);
                    
                    if (h == null) {
                    	this.sendStatus(id, SSH_FX_FAILURE, handle, "");
                    } 
                    else {
                    	this.handles.remove(handle);
                        h.close();
                        
                        this.sendStatus(id, SSH_FX_OK, "", "");
                    }
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_READ: {
                String handle = buffer.getString();
                long offset = buffer.getLong();
                int len = buffer.getInt();
                
                this.log.debug("Received SSH_FXP_READ (handle={}, offset={}, length={})", new Object[] { handle, offset, len });
                
                try {
                    Handle p = this.handles.get(handle);
                    
                    if (!(p instanceof FileHandle)) {
                    	this.sendStatus(id, SSH_FX_FAILURE, handle);
                    } 
                    else {
                        FileHandle fh = (FileHandle) p;
                        
                        byte[] b = new byte[Math.min(len, Buffer.MAX_LEN)];
                        len = fh.read(b, offset);
                        
                        if (len >= 0) {
                            Buffer buf = new Buffer(len + 5);
                            buf.putByte((byte) SSH_FXP_DATA);
                            buf.putInt(id);
                            buf.putBytes(b, 0, len);
                            
                            this.send(buf);
                        } 
                        else {
                        	this.sendStatus(id, SSH_FX_EOF, "");
                        }
                    }
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_WRITE: {
                String handle = buffer.getString();
                long offset = buffer.getLong();
                byte[] data = buffer.getBytes();
                
                this.log.debug("Received SSH_FXP_WRITE (handle={}, offset={}, data=byte[{}])", new Object[] { handle, offset, data.length });
                
                try {
                    Handle p = this.handles.get(handle);
                    
                    if (!(p instanceof FileHandle)) {
                    	this.sendStatus(id, SSH_FX_FAILURE, handle);
                    } 
                    else {
                        FileHandle fh = (FileHandle) p;
                        fh.write(data, offset);
                        SshFile sshFile = fh.getFile();

                        sshFile.setLastModified(new Date().getTime());
                        
                        this.sendStatus(id, SSH_FX_OK, "");
                    }
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_STAT: {
                String path = buffer.getString();
                
                this.log.debug("Received SSH_FXP_STAT (path={})", path);
                
                try {
                    SshFile p = this.resolveFile(path);
                    this.sendAttrs(id, p, true);
                } 
                catch (FileNotFoundException e) {
                	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, e.getMessage());
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_LSTAT: {
                String path = buffer.getString();
                
                this.log.debug("Received SSH_FXP_LSTAT (path={})", path);
                
                try {
                    SshFile p = this.resolveFile(path);
                    
                    this.sendAttrs(id, p, false);
                } 
                catch (FileNotFoundException e) {
                	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, e.getMessage());
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_FSTAT: {
                String handle = buffer.getString();
                
                this.log.debug("Received SSH_FXP_FSTAT (handle={})", handle);
                
                try {
                    Handle p = this.handles.get(handle);
                    
                    if (p == null) {
                    	this.sendStatus(id, SSH_FX_FAILURE, handle);
                    } 
                    else {
                    	this.sendAttrs(id, p.getFile(), true);
                    }
                } 
                catch (FileNotFoundException e) {
                	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, e.getMessage());
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_SETSTAT: {
                String path = buffer.getString();
                
                Map<SshFile.Attribute, Object> attrs = this.readAttrs(buffer);
                
                this.log.debug("Received SSH_FXP_SETSTAT (path={}, attrs={})", path, attrs);
                
                try {
                    SshFile p = this.resolveFile(path);
                    p.setAttributes(attrs);
                    
                    this.sendStatus(id, SSH_FX_OK, "");
                } 
                catch (FileNotFoundException e) {
                	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, e.getMessage());
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                } 
                catch (UnsupportedOperationException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, "");
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_FSETSTAT: {
                String handle = buffer.getString();
                Map<SshFile.Attribute, Object> attrs = this.readAttrs(buffer);
                
                this.log.debug("Received SSH_FXP_FSETSTAT (handle={}, attrs={})", handle, attrs);
                
                try {
                    Handle p = this.handles.get(handle);
                    
                    if (p == null) {
                    	this.sendStatus(id, SSH_FX_FAILURE, handle);
                    } else {
                        p.getFile().setAttributes(attrs);
                        this.sendStatus(id, SSH_FX_OK, "");
                    }
                } 
                catch (FileNotFoundException e) {
                	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, e.getMessage());
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                } 
                catch (UnsupportedOperationException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_OPENDIR: {
                String path = buffer.getString();
                
                System.out.println("Got open dir sftp message: " + this.session.getId());
                
                this.log.debug("Received SSH_FXP_OPENDIR (path={})", path);
                
                try {
                    SshFile p = this.resolveFile(path);
                    
                    if (!p.doesExist()) {
                    	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, path);
                    } 
                    else if (!p.isDirectory()) {
                    	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, path);
                    } 
                    else if (!p.isReadable()) {
                    	this.sendStatus(id, SSH_FX_PERMISSION_DENIED, path);
                    } 
                    else {
                        String handle = UUID.randomUUID().toString();
                        this.handles.put(handle, new DirectoryHandle(p));
                        
                        this.sendHandle(id, handle);
                    }
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_READDIR: {
                String handle = buffer.getString();
                
                this.log.debug("Received SSH_FXP_READDIR (handle={})", handle);
                
                try {
                    Handle p = this.handles.get(handle);
                    
                    if (!(p instanceof DirectoryHandle)) {
                    	this.sendStatus(id, SSH_FX_FAILURE, handle);
                    } 
                    else if (((DirectoryHandle) p).isDone()) {
                    	this.sendStatus(id, SSH_FX_EOF, "", "");
                    } 
                    else if (!p.getFile().doesExist()) {
                    	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, p.getFile().getAbsolutePath());
                    } 
                    else if (!p.getFile().isDirectory()) {
                    	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, p.getFile().getAbsolutePath());
                    } 
                    else if (!p.getFile().isReadable()) {
                    	this.sendStatus(id, SSH_FX_PERMISSION_DENIED, p.getFile().getAbsolutePath());
                    } 
                    else {
                        DirectoryHandle dh = (DirectoryHandle) p;
                        
                        if (dh.hasNext()) {
                            // There is at least one file in the directory.
                            // Send only a few files at a time to not create packets of a too
                            // large size or have a timeout to occur.
                        	this.sendName(id, dh);
                            
                            if (!dh.hasNext()) {
                                // if no more files to send
                                dh.setDone(true);
                                dh.clearFileList();
                            }
                        } 
                        else {
                            // empty directory
                            dh.setDone(true);
                            dh.clearFileList();
                            
                            this.sendStatus(id, SSH_FX_EOF, "", "");
                        }
                    }
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_REMOVE: {
                String path = buffer.getString();
                
                this.log.debug("Received SSH_FXP_REMOVE (path={})", path);
                
                try {
                    SshFile p = this.resolveFile(path);
                    
                    if (!p.doesExist()) {
                    	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, p.getAbsolutePath());
                    } 
                    else if (p.isDirectory()) {
                    	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, p.getAbsolutePath());
                    } 
                    else if (!p.delete()) {
                    	this.sendStatus(id, SSH_FX_FAILURE, "Failed to delete file");
                    } 
                    else {
                    	this.sendStatus(id, SSH_FX_OK, "");
                    }
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_MKDIR: {
                String path = buffer.getString();
                Map<SshFile.Attribute, Object> attrs = readAttrs(buffer);

                this.log.debug("Received SSH_FXP_MKDIR (path={})", path);
                
                // attrs
                try {
                    SshFile p = this.resolveFile(path);
                    
                    if (p.doesExist()) {
                        if (p.isDirectory()) {
                        	this.sendStatus(id, SSH_FX_FAILURE, p.getAbsolutePath());
                        } 
                        else {
                        	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, p.getAbsolutePath());
                        }
                    } 
                    else if (!p.isWritable()) {
                    	this.sendStatus(id, SSH_FX_PERMISSION_DENIED, p.getAbsolutePath());
                    } 
                    else if (!p.mkdir()) {
                        throw new IOException("Error creating dir " + path);
                    } 
                    else {
                        p.setAttributes(attrs);
                        this.sendStatus(id, SSH_FX_OK, "");
                    }
                } catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_RMDIR: {
                String path = buffer.getString();
                
                this.log.debug("Received SSH_FXP_RMDIR (path={})", path);
                
                // attrs
                try {
                    SshFile p = this.resolveFile(path);
                    
                    if (p.isDirectory()) {
                        if (p.doesExist()) {
                            if (p.listSshFiles().size() == 0) {
                                if (p.delete()) {
                                	this.sendStatus(id, SSH_FX_OK, "");
                                } 
                                else {
                                	this.sendStatus(id, SSH_FX_FAILURE, "Unable to delete directory " + path);
                                }
                            } 
                            else {
                            	this.sendStatus(id, SSH_FX_FAILURE, path);
                            }
                        } 
                        else {
                        	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, path);
                        }
                    } 
                    else {
                    	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, p.getAbsolutePath());
                    }
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_REALPATH: {
                String path = buffer.getString();
                
                this.log.debug("Received SSH_FXP_REALPATH (path={})", path);
                
                if (path.trim().length() == 0) {
                    path = ".";
                }
                
                try {
                    SshFile p = resolveFile(path);
                    this.sendPath(id, p, false);
                } 
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                    this.sendStatus(id, SSH_FX_NO_SUCH_FILE, e.getMessage());
                } 
                catch (IOException e) {
                    e.printStackTrace();
                    this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_RENAME: {
                String oldPath = buffer.getString();
                String newPath = buffer.getString();
                
                this.log.debug("Received SSH_FXP_RENAME (oldPath={}, newPath={})", oldPath, newPath);
                
                try {
                    SshFile o = this.resolveFile(oldPath);
                    SshFile n = this.resolveFile(newPath);
                    
                    if (!o.doesExist()) {
                    	this.sendStatus(id, SSH_FX_NO_SUCH_FILE, o.getAbsolutePath());
                    } 
                    else if (n.doesExist()) {
                    	this.sendStatus(id, SSH_FX_FAILURE, n.getAbsolutePath());
                    } 
                    else if (!o.move(n)) {
                    	this.sendStatus(id, SSH_FX_FAILURE, "Failed to rename file");
                    } 
                    else {
                    	this.sendStatus(id, SSH_FX_OK, "");
                    }
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_READLINK: {
                String path = buffer.getString();
                
                this.log.debug("Received SSH_FXP_READLINK (path={})", path);
                
                try {
                    SshFile f = this.resolveFile(path);
                    String l = f.readSymbolicLink();
                    this.sendLink(id, l);
                } 
                catch (UnsupportedOperationException e) {
                	this.sendStatus(id, SSH_FX_OP_UNSUPPORTED, "Command " + type + " is unsupported or not implemented");
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            case SSH_FXP_SYMLINK: {
                String linkpath = buffer.getString();
                String targetpath = buffer.getString();
                
                this.log.debug("Received SSH_FXP_SYMLINK (linkpath={}, targetpath={})", linkpath, targetpath);
                
                try {
                    SshFile link = this.resolveFile(linkpath);
                    SshFile target = this.resolveFile(targetpath);
                    link.createSymbolicLink(target);
                    this.sendStatus(id, SSH_FX_OK, "");
                } 
                catch (UnsupportedOperationException e) {
                	this.sendStatus(id, SSH_FX_OP_UNSUPPORTED, "Command " + type + " is unsupported or not implemented");
                } 
                catch (IOException e) {
                	this.sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                
                this.requestInput();
                
                break;
            }
            default: {
            	this.log.error("Received: {}", type);
            	this.sendStatus(id, SSH_FX_OP_UNSUPPORTED, "Command " + type + " is unsupported or not implemented");
            	
            	this.requestInput();
            	
                break;
            }
        }
    }

    protected void sendHandle(int id, String handle) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_HANDLE);
        buffer.putInt(id);
        buffer.putString(handle);
        send(buffer);
    }

    protected void sendAttrs(int id, SshFile file, boolean followLinks) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_ATTRS);
        buffer.putInt(id);
        writeAttrs(buffer, file, followLinks);
        send(buffer);
    }

    protected void sendPath(int id, SshFile f) throws IOException {
        sendPath(id, f, true);
    }

    protected void sendPath(int id, SshFile f, boolean sendAttrs) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_NAME);
        buffer.putInt(id);
        buffer.putInt(1);
        //normalize the given path, use *nix style separator
        String normalizedPath = SelectorUtils.normalizePath(f.getAbsolutePath(), "/");
        if (normalizedPath.length() == 0) {
            normalizedPath = "/";
        }
        buffer.putString(normalizedPath);
        f = resolveFile(normalizedPath);
        if (f.getName().length() == 0) {
            f = resolveFile(".");
        }
        buffer.putString(getLongName(f, sendAttrs)); // Format specified in the specs
        buffer.putInt(0);
        send(buffer);
    }

    protected void sendLink(int id, String link) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_NAME);
        buffer.putInt(id);
        buffer.putInt(1);
        //normalize the given path, use *nix style separator
        buffer.putString(link);
        buffer.putString(link);
        buffer.putInt(0);
        send(buffer);
    }

    protected void sendName(int id, Iterator<SshFile> files) throws IOException {
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_NAME);
        buffer.putInt(id);
        int wpos = buffer.wpos();
        buffer.putInt(0);
        int nb = 0;
        while (files.hasNext() && buffer.wpos() < MAX_PACKET_LENGTH) {
            SshFile f = files.next();
            buffer.putString(f.getName());
            buffer.putString(getLongName(f)); // Format specified in the specs
            writeAttrs(buffer, f, false);
            nb++;
        }
        int oldpos = buffer.wpos();
        buffer.wpos(wpos);
        buffer.putInt(nb);
        buffer.wpos(oldpos);
        send(buffer);
    }

    private String getLongName(SshFile f) throws IOException {
        return getLongName(f, true);
    }

    private String getLongName(SshFile f, boolean sendAttrs) throws IOException {
        Map<SshFile.Attribute, Object> attributes;
        if (sendAttrs) {
            attributes = f.getAttributes(true);
        } else {
            attributes = new HashMap<SshFile.Attribute, Object>();
            attributes.put(SshFile.Attribute.Owner, "owner");
            attributes.put(SshFile.Attribute.Group, "group");
            attributes.put(SshFile.Attribute.Size, (long) 0);
            attributes.put(SshFile.Attribute.IsDirectory, false);
            attributes.put(SshFile.Attribute.IsSymbolicLink, false);
            attributes.put(SshFile.Attribute.IsRegularFile, false);
            attributes.put(SshFile.Attribute.Permissions, EnumSet.noneOf(SshFile.Permission.class));
            attributes.put(SshFile.Attribute.LastModifiedTime, (long) 0);
        }
        String username = (String) attributes.get(SshFile.Attribute.Owner);
        if (username.length() > 8) {
            username = username.substring(0, 8);
        } else {
            for (int i = username.length(); i < 8; i++) {
                username = username + " ";
            }
        }
        String group = (String) attributes.get(SshFile.Attribute.Group);
        if (group.length() > 8) {
            group = group.substring(0, 8);
        } else {
            for (int i = group.length(); i < 8; i++) {
                group = group + " ";
            }
        }

        long length = (Long) attributes.get(SshFile.Attribute.Size);
        String lengthString = String.format("%1$8s", length);

        boolean isDirectory = (Boolean) attributes.get(SshFile.Attribute.IsDirectory);
        boolean isLink = (Boolean) attributes.get(SshFile.Attribute.IsSymbolicLink);
        int perms = getPermissions(attributes);

        StringBuilder sb = new StringBuilder();
        sb.append(isDirectory ? "d" : isLink ? "l" : "-");
        sb.append((perms & S_IRUSR) != 0 ? "r" : "-");
        sb.append((perms & S_IWUSR) != 0 ? "w" : "-");
        sb.append((perms & S_IXUSR) != 0 ? "x" : "-");
        sb.append((perms & S_IRGRP) != 0 ? "r" : "-");
        sb.append((perms & S_IWGRP) != 0 ? "w" : "-");
        sb.append((perms & S_IXGRP) != 0 ? "x" : "-");
        sb.append((perms & S_IROTH) != 0 ? "r" : "-");
        sb.append((perms & S_IWOTH) != 0 ? "w" : "-");
        sb.append((perms & S_IXOTH) != 0 ? "x" : "-");
        sb.append(" ");
        sb.append("  1");
        sb.append(" ");
        sb.append(username);
        sb.append(" ");
        sb.append(group);
        sb.append(" ");
        sb.append(lengthString);
        sb.append(" ");
        sb.append(getUnixDate((Long) attributes.get(SshFile.Attribute.LastModifiedTime)));
        sb.append(" ");
        sb.append(f.getName());

        return sb.toString();
    }

    protected Map<SshFile.Attribute, Object> getPermissions(int perms) {
        Map<SshFile.Attribute, Object> attrs = new HashMap<SshFile.Attribute, Object>();
        if ((perms & S_IFMT) == S_IFREG) {
            attrs.put(SshFile.Attribute.IsRegularFile, Boolean.TRUE);
        }
        if ((perms & S_IFMT) == S_IFDIR) {
            attrs.put(SshFile.Attribute.IsDirectory, Boolean.TRUE);
        }
        if ((perms & S_IFMT) == S_IFLNK) {
            attrs.put(SshFile.Attribute.IsSymbolicLink, Boolean.TRUE);
        }
        EnumSet<SshFile.Permission> p = EnumSet.noneOf(SshFile.Permission.class);
        if ((perms & S_IRUSR) != 0) {
            p.add(SshFile.Permission.UserRead);
        }
        if ((perms & S_IWUSR) != 0) {
            p.add(SshFile.Permission.UserWrite);
        }
        if ((perms & S_IXUSR) != 0) {
            p.add(SshFile.Permission.UserExecute);
        }
        if ((perms & S_IRGRP) != 0) {
            p.add(SshFile.Permission.GroupRead);
        }
        if ((perms & S_IWGRP) != 0) {
            p.add(SshFile.Permission.GroupWrite);
        }
        if ((perms & S_IXGRP) != 0) {
            p.add(SshFile.Permission.GroupExecute);
        }
        if ((perms & S_IROTH) != 0) {
            p.add(SshFile.Permission.OthersRead);
        }
        if ((perms & S_IWOTH) != 0) {
            p.add(SshFile.Permission.OthersWrite);
        }
        if ((perms & S_IXOTH) != 0) {
            p.add(SshFile.Permission.OthersExecute);
        }
        attrs.put(SshFile.Attribute.Permissions, p);
        return attrs;
    }

    protected int getPermissions(Map<SshFile.Attribute, Object> attributes) {
        boolean isReg = (Boolean) attributes.get(SshFile.Attribute.IsRegularFile);
        boolean isDir = (Boolean) attributes.get(SshFile.Attribute.IsDirectory);
        boolean isLnk = (Boolean) attributes.get(SshFile.Attribute.IsSymbolicLink);
        int pf = 0;
        EnumSet<SshFile.Permission> perms = (EnumSet<SshFile.Permission>) attributes.get(SshFile.Attribute.Permissions);
        for (SshFile.Permission p : perms) {
            switch (p) {
                case UserRead:      pf |= S_IRUSR; break;
                case UserWrite:     pf |= S_IWUSR; break;
                case UserExecute:   pf |= S_IXUSR; break;
                case GroupRead:     pf |= S_IRGRP; break;
                case GroupWrite:    pf |= S_IWGRP; break;
                case GroupExecute:  pf |= S_IXGRP; break;
                case OthersRead:    pf |= S_IROTH; break;
                case OthersWrite:   pf |= S_IWOTH; break;
                case OthersExecute: pf |= S_IXOTH; break;
            }
        }
        pf |= isReg ? S_IFREG : 0;
        pf |= isDir ? S_IFDIR : 0;
        pf |= isLnk ? S_IFLNK : 0;
        return pf;
    }

    protected void writeAttrs(Buffer buffer, SshFile file, boolean followLinks) throws IOException {
        if (!file.doesExist()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        Map<SshFile.Attribute, Object> attributes = file.getAttributes(followLinks);
        boolean isReg = getBool((Boolean) attributes.get(SshFile.Attribute.IsRegularFile));
        boolean isDir = getBool((Boolean) attributes.get(SshFile.Attribute.IsDirectory));
        boolean isLnk = getBool((Boolean) attributes.get(SshFile.Attribute.IsSymbolicLink));
        int flags = 0;
        if ((isReg || isLnk) && attributes.containsKey(SshFile.Attribute.Size)) {
            flags |= SSH_FILEXFER_ATTR_SIZE;
        }
        if (attributes.containsKey(SshFile.Attribute.Uid) && attributes.containsKey(SshFile.Attribute.Gid)) {
            flags |= SSH_FILEXFER_ATTR_UIDGID;
        }
        if (attributes.containsKey(SshFile.Attribute.Permissions)) {
            flags |= SSH_FILEXFER_ATTR_PERMISSIONS;
        }
        if (attributes.containsKey(SshFile.Attribute.LastAccessTime) && attributes.containsKey(SshFile.Attribute.LastModifiedTime)) {
            flags |= SSH_FILEXFER_ATTR_ACMODTIME;
        }
        buffer.putInt(flags);
        if ((flags & SSH_FILEXFER_ATTR_SIZE) != 0) {
            buffer.putLong((Long) attributes.get(SshFile.Attribute.Size));
        }
        if ((flags & SSH_FILEXFER_ATTR_UIDGID) != 0) {
            buffer.putInt((Integer) attributes.get(SshFile.Attribute.Uid));
            buffer.putInt((Integer) attributes.get(SshFile.Attribute.Gid));
        }
        if ((flags & SSH_FILEXFER_ATTR_PERMISSIONS) != 0) {
            buffer.putInt(getPermissions(attributes));
        }
        if ((flags & SSH_FILEXFER_ATTR_ACMODTIME) != 0) {
            buffer.putInt(((Long) attributes.get(SshFile.Attribute.LastAccessTime)) / 1000);
            buffer.putInt(((Long) attributes.get(SshFile.Attribute.LastModifiedTime)) / 1000);
        }
    }

    protected boolean getBool(Boolean bool) {
        return bool != null && bool;
    }

    protected Map<SshFile.Attribute, Object> readAttrs(Buffer buffer) throws IOException {
        Map<SshFile.Attribute, Object> attrs = new HashMap<SshFile.Attribute, Object>();
        int flags = buffer.getInt();
        if ((flags & SSH_FILEXFER_ATTR_SIZE) != 0) {
            attrs.put(SshFile.Attribute.Size, buffer.getLong());
        }
        if ((flags & SSH_FILEXFER_ATTR_UIDGID) != 0) {
            attrs.put(SshFile.Attribute.Uid, buffer.getInt());
            attrs.put(SshFile.Attribute.Gid, buffer.getInt());
        }
        if ((flags & SSH_FILEXFER_ATTR_PERMISSIONS) != 0) {
            attrs.putAll(getPermissions(buffer.getInt()));
        }
        if ((flags & SSH_FILEXFER_ATTR_ACMODTIME) != 0) {
            attrs.put(SshFile.Attribute.LastAccessTime, ((long) buffer.getInt()) * 1000);
            attrs.put(SshFile.Attribute.LastModifiedTime, ((long) buffer.getInt()) * 1000);
        }
        return attrs;
    }

    protected void sendStatus(int id, int substatus, String msg) throws IOException {
        sendStatus(id, substatus, msg, "");
    }

    protected void sendStatus(int id, int substatus, String msg, String lang) throws IOException {
        log.debug("Send SSH_FXP_STATUS (substatus={}, msg={})", substatus, msg);
        Buffer buffer = new Buffer();
        buffer.putByte((byte) SSH_FXP_STATUS);
        buffer.putInt(id);
        buffer.putInt(substatus);
        buffer.putString(msg);
        buffer.putString(lang);
        send(buffer);
    }

    protected void send(Buffer buffer) throws IOException {
        this.dos.writeInt(buffer.available());
        this.dos.write(buffer.array(), buffer.rpos(), buffer.available());
        this.dos.flush();
    }

    public void destroy() {
    	this.close();
        //closed = true;
    }

    private SshFile resolveFile(String path) {
    	return this.root.getNormalizedView().getFile(path);
    }

    private final static String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May",
            "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    /**
     * Get unix style date string.
     */
    private final static String getUnixDate(long millis) {
        if (millis < 0) {
            return "------------";
        }

        StringBuffer sb = new StringBuffer(16);
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(millis);

        // month
        sb.append(MONTHS[cal.get(Calendar.MONTH)]);
        sb.append(' ');

        // day
        int day = cal.get(Calendar.DATE);
        if (day < 10) {
            sb.append(' ');
        }
        sb.append(day);
        sb.append(' ');

        long sixMonth = 15811200000L; // 183L * 24L * 60L * 60L * 1000L;
        long nowTime = System.currentTimeMillis();
        if (Math.abs(nowTime - millis) > sixMonth) {

            // year
            int year = cal.get(Calendar.YEAR);
            sb.append(' ');
            sb.append(year);
        } else {

            // hour
            int hh = cal.get(Calendar.HOUR_OF_DAY);
            if (hh < 10) {
                sb.append('0');
            }
            sb.append(hh);
            sb.append(':');

            // minute
            int mm = cal.get(Calendar.MINUTE);
            if (mm < 10) {
                sb.append('0');
            }
            sb.append(mm);
        }
        return sb.toString();
    }

}
