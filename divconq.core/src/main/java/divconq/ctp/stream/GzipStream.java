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

import java.util.zip.CRC32;
import java.util.zip.Deflater;

import org.apache.commons.compress.compressors.gzip.GzipUtils;

import divconq.ctp.f.FileDescriptor;
import divconq.hub.Hub;
import divconq.script.StackEntry;
import divconq.util.FileUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class GzipStream extends BaseStream implements IStreamSource {
    protected static final byte[] gzipHeader = {0x1f, (byte) 0x8b, Deflater.DEFLATED, 0, 0, 0, 0, 0, 0, 0};
    
    protected int compressionLevel = 6;
    
    protected Deflater deflater = null;
    protected CRC32 crc = new CRC32();
    protected boolean writeHeader = true;
    
    protected String nameHint = null;
    protected String lastpath = null;
    
    public GzipStream() {
    }
    
    public GzipStream(int compressionLevel) {
    	this.compressionLevel = compressionLevel;
    }

	@Override
	public void init(StackEntry stack, XElement el) {
		this.nameHint = stack.stringFromElement(el, "NameHint");
	}

	@Override
    public void close() {
		//System.out.println("GZip killed");	// TODO
		
    	this.deflater = null;
    
    	super.close();
    }
    
	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileDescriptor file, ByteBuf data) {
    	if (file == FileDescriptor.FINAL) 
    		return this.downstream.handle(file, data);
    	
    	// we don't know what to do with a folder at this stage - gzip is for file content only
    	// folder scanning is upstream in the FileSourceStream and partners
    	if (file.isFolder())
    		return ReturnOption.CONTINUE;
    	
    	// init if not set for this round of processing 
    	if (this.deflater == null) {
            this.deflater = new Deflater(this.compressionLevel, true);
            this.crc.reset();
        	this.writeHeader = true;
    	}
        
    	ByteBuf in = data;
        ByteBuf out = null; 
    	
		if (in != null) {
	        byte[] inAry = in.array();
	
	        // always allow for a header (10) plus footer (8) plus extra (12)
	        // in addition to content
	        int sizeEstimate = (int) Math.ceil(in.readableBytes() * 1.001) + 30;
	        out = Hub.instance.getBufferAllocator().heapBuffer(sizeEstimate);
	        
	        if (this.writeHeader) {
	        	this.writeHeader = false;
	            out.writeBytes(gzipHeader);
	        } 
	
	        this.crc.update(inAry, in.arrayOffset(), in.writerIndex());
	
	        this.deflater.setInput(inAry, in.arrayOffset(), in.writerIndex());
	        
	        while (!this.deflater.needsInput()) 
	            deflate(out);
		}
		else
			out = Hub.instance.getBufferAllocator().heapBuffer(30);
		
		FileDescriptor blk = new FileDescriptor();
		
		if (StringUtil.isEmpty(this.lastpath)) {
			if (StringUtil.isNotEmpty(this.nameHint)) 
				this.lastpath = "/" +  this.nameHint;
			else if (file.getPath() != null) 
				this.lastpath = "/" +  GzipUtils.getCompressedFilename(file.path().getFileName());
			else
				this.lastpath = "/" + FileUtil.randomFilename("gz");
		}
		
		blk.setPath(this.lastpath);
		
		file.setModTime(System.currentTimeMillis());

        if (file.isEof()) {
	        this.deflater.finish();
	        
	        while (!this.deflater.finished()) 
	            deflate(out);
	    
	        int crcValue = (int) this.crc.getValue();
	        
	        out.writeByte(crcValue);
	        out.writeByte(crcValue >>> 8);
	        out.writeByte(crcValue >>> 16);
	        out.writeByte(crcValue >>> 24);
	        
	        int uncBytes = this.deflater.getTotalIn();
	        
	        out.writeByte(uncBytes);
	        out.writeByte(uncBytes >>> 8);
	        out.writeByte(uncBytes >>> 16);
	        out.writeByte(uncBytes >>> 24);
	
	        this.deflater.end();
	        this.deflater = null;		// cause a reset for next time we use stream
	        
        	blk.setEof(true);
        }
        
        if (in != null)
        	in.release();
        
       	return this.downstream.handle(blk, out);
    }

    protected void deflate(ByteBuf out) {
        int numBytes = 0;
        
        do {
        	byte[] o = out.array();
        	
            numBytes = this.deflater.deflate(o, out.arrayOffset() + out.writerIndex(), out.writableBytes(), Deflater.SYNC_FLUSH);
            
            out.writerIndex(out.writerIndex() + numBytes);
        } while (numBytes > 0);
    }
    
    @Override
    public void read() {
    	this.upstream.read();
    }
}
