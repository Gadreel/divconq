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
package divconq.ctp.stream;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.file.Paths;

import org.bouncycastle.openpgp.PGPException;

import divconq.ctp.f.FileDescriptor;
import divconq.lang.op.OperationContext;
import divconq.log.Logger;
import divconq.pgp.EncryptedFileStream;
import divconq.script.StackEntry;
import divconq.util.FileUtil;
import divconq.xml.XElement;

public class PgpEncryptStream extends BaseStream implements IStreamSource {
	protected EncryptedFileStream pgp = new EncryptedFileStream();
	protected boolean needInit = true;
	protected FileDescriptor efile = null;
    //protected String ourpath = null;
    //protected long ourmod = 0;
    //protected byte ourperm = 0;
	
    public PgpEncryptStream() {
    }
    
	@Override
	public void init(StackEntry stack, XElement el) {
		String keyPath = stack.stringFromElement(el, "Keyring");
		
		try {
			this.pgp.loadPublicKey(Paths.get(keyPath));
		} 
		catch (IOException x) {
			OperationContext.get().error("Unabled to read keyfile: " + x);
		} 
		catch (PGPException x) {
			OperationContext.get().error("Unabled to load keyfile: " + x);
		}
	}
    
	@Override
    public void close() {
		try {
			this.pgp.close();
		} 
		catch (PGPException x) {
			// it should already be closed, unless we got here by a task kill/cancel
			Logger.warn("Error closing PGP stream: " + x);
		}
    
    	super.close();
    }

	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileDescriptor file, ByteBuf data) {
    	if (file == FileDescriptor.FINAL) 
    		return this.downstream.handle(file, data);
    	
    	if (this.needInit) {
    		this.pgp.setFileName(file.path().getFileName());
    		
    		try {
    			this.pgp.init();
    		}
    		catch (Exception x) {
    			OperationContext.get().getTaskRun().kill("PGP init failed: " + x);
    			return ReturnOption.DONE;
    		}
    		
    		this.initializeFileValues(file);
    		
    		this.needInit = false;
    	}
    	
    	// inflate the payload into 1 or more outgoing buffers set in a queue
    	ByteBuf in = data;
    	
		if (in != null) {
			this.pgp.writeData(in);
			
        	in.release();
			
			if (OperationContext.get().getTaskRun().isKilled())
				return ReturnOption.DONE;
		}
		
		// write all buffers in the queue
        ByteBuf buf = this.pgp.nextReadyBuffer();
        
        while (buf != null) {
        	ReturnOption ret = this.nextMessage(buf);
			
			if (ret != ReturnOption.CONTINUE)
				return ret;
        	
        	buf = this.pgp.nextReadyBuffer();
        }
		
        if (file.isEof()) {
        	try {
				this.pgp.close();
			} 
        	catch (PGPException x) {
        		OperationContext.get().getTaskRun().kill("PGP close failed: " + x);
				return ReturnOption.DONE;
			}
        	
    		// write all buffers in the queue
            buf = this.pgp.nextReadyBuffer();
            
            while (buf != null) {
            	ReturnOption ret = this.nextMessage(buf);
    			
    			if (ret != ReturnOption.CONTINUE)
    				return ret;
            	
            	buf = this.pgp.nextReadyBuffer();
            }
            
            ReturnOption ret = this.lastMessage();
			
			if (ret != ReturnOption.CONTINUE)
				return ret;
        }
		
		// otherwise we need more data
		return ReturnOption.CONTINUE;
    }
    
    public ReturnOption nextMessage(ByteBuf out) {
        return this.downstream.handle(this.efile, out);
    }
    
    public ReturnOption lastMessage() {
    	this.efile.setEof(true);
        
        return this.downstream.handle(this.efile, null);
    }
    
    public void initializeFileValues(FileDescriptor src) {
    	this.efile = new FileDescriptor();
    	
		if (src.hasPath()) 
			this.efile.setPath(src.getPath().toString() + ".gpg");
		else
			this.efile.setPath("/" + FileUtil.randomFilename("bin") + ".gpg");    	
		
		this.efile.setModTime(src.getModTime());
		this.efile.setPermissions(src.getPermissions());
    }
    
    @Override
    public void read() {
		// write all buffers in the queue
        ByteBuf buf = this.pgp.nextReadyBuffer();
        
        while (buf != null) {
        	ReturnOption ret = this.nextMessage(buf);
			
			if (ret != ReturnOption.CONTINUE)
				return;
        	
        	buf = this.pgp.nextReadyBuffer();
        }
		
		// if we reached done and we wrote all the buffers, then send the EOF marker if not already
		if (this.pgp.isClosed()) {
			ReturnOption ret = this.lastMessage();
			
			if (ret != ReturnOption.CONTINUE)
				return;
		}
		
    	this.upstream.read();
    }	
}
