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
package divconq.web.http;

import io.netty.buffer.ByteBuf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import divconq.bus.MessageUtil;
import divconq.bus.net.StreamMessage;
import divconq.http.multipart.AbstractHttpData;
import divconq.http.multipart.FileUpload;
import divconq.http.multipart.InterfaceHttpData;
import divconq.lang.op.OperationResult;
import divconq.session.DataStreamChannel;

/**
 */
public class StreamingHttpData extends AbstractHttpData implements FileUpload {
	protected int max = 0;
	protected int seq = 0;
	protected DataStreamChannel channel = null;
	protected String op = null;

    protected StreamingHttpData(DataStreamChannel dsc, String op, String name, Charset charset, long size) {
        super(name, charset, size);
        this.channel = dsc;
        this.op = op;
    }

    public void setChannel(DataStreamChannel v) {
		this.channel = v;
	}
    
    @Override
    public void setContent(ByteBuf buffer) throws IOException {
    	this.addContent(buffer, true);
    	
    	/*
    	// TODO review all this for functionality - may not be working as desired
        try {
			if (!this.channel.isKilled()) {
	            this.size = buffer.readableBytes();
	
	            // TODO set hint in netty as to where this buffer was handled and sent
	            //buffer.
	            
	            if (this.size > 0) { 
		    		StreamMessage b = new StreamMessage("Block", buffer); //Unpooled.copiedBuffer(buffer));
		    		b.setField("Sequence", this.seq);
		    		
		    		OperationResult or = this.channel.send(b);
		    		
		    		if (or.hasErrors()) {
		    			// kill channel/data stream if error
		    			this.channel.kill();
		    		//else	
		    			//buffer.retain();	// TODO where do we place this
		    		}
		    		
		    		this.seq++;
	            }
	
	            // this should always be Final right?  channel complete suggests so?
	            if ("Final".equals(this.op)) {
	            	this.channel.send(MessageUtil.streamFinal());
	    			//this.channel.complete();  // TODO decide if we really should complete here or with setCompleted
	            }
			}
			
			// block and upload are complete
            this.setCompleted();
        } 
        finally {
        	// don't release, use and release it elsewhere
            //buffer.release();
        }
        */
    }

    @Override
    public void addContent(ByteBuf buffer, boolean last) throws IOException {
        try {
			if (!this.channel.isClosed()) {
	            this.size = buffer.readableBytes();
	
	            // TODO set hint in netty as to where this buffer was handled and sent
	            
	            if (this.size > 0) { 
		    		StreamMessage b = new StreamMessage("Block", buffer);  
		    		b.setField("Sequence", this.seq);
		    		
		    		OperationResult or = this.channel.send(b);
		    		
		    		if (or.hasErrors()) {
		    			this.channel.abort();
		    		}
		    		
		    		this.seq++;
	            }
	
	            // if last buffer of last block then mark the upload as completed
	    		if (last) {
	    			if ("Final".equals(this.op))  
	    				this.channel.send(MessageUtil.streamFinal());
	    			else
	    				this.channel.getDriver().nextChunk();   
				}
			}
			
			// means this block is completed, not necessarily entire file uploaded
    		if (last) 
	            this.setCompleted();
        } 
        finally {
        	// don't release, use and release it elsewhere
            //buffer.release();
        }
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public FileUpload retain() {
    	return null;  //super.retain();
    }
    
    @Override
    public FileUpload retain(int increment) {
    	return null; //super.retain(increment);
    }
    
	@Override
	public void delete() {
	}

	@Override
	public byte[] get() throws IOException {
		return null;
	}

	@Override
	public ByteBuf getByteBuf() throws IOException {
		return null;
	}

	@Override
	public ByteBuf getChunk(int arg0) throws IOException {
		return null;
	}

	@Override
	public File getFile() throws IOException {
		return null;
	}

	@Override
	public String getString() throws IOException {
		return null;
	}

	@Override
	public String getString(Charset arg0) throws IOException {
		return null;
	}

	@Override
	public boolean renameTo(File arg0) throws IOException {
		return false;
	}

	@Override
	public void setContent(File arg0) throws IOException {
	}

	@Override
	public void setContent(InputStream arg0) throws IOException {
	}

	@Override
	public HttpDataType getHttpDataType() {
		return null;
	}

	@Override
	public int compareTo(InterfaceHttpData arg0) {
		return 0;
	}

	@Override
	public FileUpload copy() {
		return null;
	}

	@Override
	public FileUpload duplicate() {
		return null;
	}

	@Override
	public String getContentTransferEncoding() {
		return null;
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public String getFilename() {
		return null;
	}

	@Override
	public void setContentTransferEncoding(String arg0) {
	}

	@Override
	public void setContentType(String arg0) {
	}

	@Override
	public void setFilename(String arg0) {
	}
}
