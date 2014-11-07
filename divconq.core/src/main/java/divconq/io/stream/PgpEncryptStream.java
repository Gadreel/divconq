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
package divconq.io.stream;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.file.Paths;

import org.bouncycastle.openpgp.PGPException;

import divconq.lang.op.OperationContext;
import divconq.log.Logger;
import divconq.pgp.EncryptedFileStream;
import divconq.script.StackEntry;
import divconq.util.FileUtil;
import divconq.xml.XElement;

public class PgpEncryptStream extends BaseStream implements IStreamSource {
	protected EncryptedFileStream pgp = new EncryptedFileStream();
	protected boolean needInit = true;
    protected String ourpath = null;
    protected long ourmod = 0;
    protected byte ourperm = 0;
	
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
    public HandleReturn handle(StreamMessage msg) {
    	if (msg == StreamMessage.FINAL) 
    		return this.downstream.handle(msg);
    	
    	if (this.needInit) {
    		this.pgp.setFileName(msg.getPath().getFileName());
    		
    		try {
    			this.pgp.init();
    		}
    		catch (Exception x) {
    			OperationContext.get().getTaskRun().kill("PGP init failed: " + x);
    			return HandleReturn.DONE;
    		}
    		
    		this.initializeFileValues(msg);
    		
    		this.needInit = false;
    	}
    	
    	// inflate the payload into 1 or more outgoing buffers set in a queue
    	ByteBuf in = msg.getPayload();
    	
		if (in != null) {
			this.pgp.writeData(in);
			
        	in.release();
			
			if (OperationContext.get().getTaskRun().isKilled())
				return HandleReturn.DONE;
		}
		
		// write all buffers in the queue
        ByteBuf buf = this.pgp.nextReadyBuffer();
        
        while (buf != null) {
			HandleReturn ret = this.downstream.handle(this.nextMessage(buf));
			
			if (ret != HandleReturn.CONTINUE)
				return ret;
        	
        	buf = this.pgp.nextReadyBuffer();
        }
		
        if (msg.isEof()) {
        	try {
				this.pgp.close();
			} 
        	catch (PGPException x) {
        		OperationContext.get().getTaskRun().kill("PGP close failed: " + x);
				return HandleReturn.DONE;
			}
        	
    		// write all buffers in the queue
            buf = this.pgp.nextReadyBuffer();
            
            while (buf != null) {
    			HandleReturn ret = this.downstream.handle(this.nextMessage(buf));
    			
    			if (ret != HandleReturn.CONTINUE)
    				return ret;
            	
            	buf = this.pgp.nextReadyBuffer();
            }
            
			HandleReturn ret = this.downstream.handle(this.lastMessage());
			
			if (ret != HandleReturn.CONTINUE)
				return ret;
        }
		
		// otherwise we need more data
		return HandleReturn.CONTINUE;
    }
    
    public StreamMessage nextMessage(ByteBuf out) {
		// create the output message
		StreamMessage blk = new StreamMessage();
		
		blk.setPath(this.ourpath);
		blk.setModified(this.ourmod);
		blk.setPermission(this.ourperm);
		
		// TODO blk.setPayloadoffset(v);		
		blk.setPayload(out);
        
        return blk;
    }
    
    public StreamMessage lastMessage() {
		// create the output message
		StreamMessage blk = new StreamMessage();
		
		blk.setPath(this.ourpath);
		blk.setModified(this.ourmod);
		blk.setPermission(this.ourperm);
		
       	blk.setEof(true);
        
        return blk;
    }
    
    public void initializeFileValues(StreamMessage src) {
		if (src.getPath() != null) 
			this.ourpath = src.getPath().toString() + ".gpg";
		else
			this.ourpath = "/" + FileUtil.randomFilename("bin") + ".gpg";    	
		
		this.ourmod = src.getModified();
		this.ourperm = src.getPermission();
    }
    
    @Override
    public void request() {
		// write all buffers in the queue
        ByteBuf buf = this.pgp.nextReadyBuffer();
        
        while (buf != null) {
			HandleReturn ret = this.downstream.handle(this.nextMessage(buf));
			
			if (ret != HandleReturn.CONTINUE)
				return;
        	
        	buf = this.pgp.nextReadyBuffer();
        }
		
		// if we reached done and we wrote all the buffers, then send the EOF marker if not already
		if (this.pgp.isClosed()) {
			HandleReturn ret = this.downstream.handle(this.lastMessage());
			
			if (ret != HandleReturn.CONTINUE)
				return;
		}
		
    	this.upstream.request();
    }	
}
