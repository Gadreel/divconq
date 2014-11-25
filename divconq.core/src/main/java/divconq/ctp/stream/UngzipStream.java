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
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.compress.compressors.gzip.GzipUtils;

import divconq.ctp.f.FileDescriptor;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.script.StackEntry;
import divconq.util.FileUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class UngzipStream extends BaseStream implements IStreamSource {
	protected static final int FHCRC = 0x02;
    protected static final int FEXTRA = 0x04;
    protected static final int FNAME = 0x08;
    protected static final int FCOMMENT = 0x10;
    protected static final int FRESERVED = 0xE0;

    private enum GzipState {
        HEADER_START,
        FLG_READ,
        XLEN_READ,
        SKIP_FNAME,
        SKIP_COMMENT,
        PROCESS_FHCRC,
        PRROCESS_CONTENT,
        PROCESS_FOOTER,
        DONE
    }

    protected GzipState gzipState = GzipState.HEADER_START;
    protected int flags = -1;
    protected int xlen = -1;

	protected Inflater inflater = null;
	protected byte[] dictionary = null;
    protected CRC32 crc = new CRC32();
    
    protected List<ByteBuf> outlist = new ArrayList<>();
    
    protected String nameHint = null;
    //protected String ourpath = null;
    //protected long ourmod = 0;
    //protected byte ourperm = 0;
    protected FileDescriptor ufile = null;
    protected boolean eofsent = false;
    
    protected ByteBuf remnant = null;

    public UngzipStream() {
        this.inflater = new Inflater(true);
        this.crc = new CRC32();
    }

    public UngzipStream(byte[] dictionary) {
    	this();
        this.dictionary = dictionary;
    }
    
	@Override
	public void init(StackEntry stack, XElement el) {
		this.nameHint = stack.stringFromElement(el, "NameHint");
	}
    
	@Override
    public void close() {
		Inflater inf = this.inflater;
		
		if (inf != null)
			inf.end();
		
    	this.inflater = null;
    	
		ByteBuf rem = this.remnant;
		
		if (rem != null) {
			rem.release();
			
			this.remnant = null;
		}
    	
    	// not truly thread safe, consider
    	for (ByteBuf bb : this.outlist)
    		bb.release();
    	
    	this.outlist.clear();
    
    	super.close();
    }

	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileDescriptor file, ByteBuf data) {
    	if (file == FileDescriptor.FINAL) 
    		return this.downstream.handle(file, data);
    	
    	if (this.ufile == null)
    		this.initializeFileValues(file);
    	
    	// inflate the payload into 1 or more outgoing buffers set in a queue
    	ByteBuf in = data;
    	
		if (in != null) {
			ByteBuf rem = this.remnant;

			ByteBuf src = ((rem != null) && rem.isReadable()) 
					? Unpooled.copiedBuffer(rem, in)
					: in;

			this.inflate(src);
			
			// if there are any unread bytes here we need to store them and combine with the next "handle"
			// this would be rare since the header and footer are small, but it is possible and should be handled
			// file content has its own "in progress" buffer so no need to worry about that
			this.remnant = src.isReadable() ? src.copy() : null;		// TODO wrap or slice here? we need copy above
			
	        if (in != null)
	        	in.release();
	        
			if (rem != null) 
				rem.release();
			
			if(OperationContext.get().getTaskRun().isKilled())
				return ReturnOption.DONE;
		}
		
		// write all buffers in the queue
		while (this.outlist.size() > 0) {
			ReturnOption ret = this.nextMessage();
			
			if (ret != ReturnOption.CONTINUE)
				return ret;
		}
		
		// if we reached done and we wrote all the buffers, then send the EOF marker if not already
		if ((this.gzipState == GzipState.DONE) && !this.eofsent)
			return this.nextMessage();
		
		// otherwise we need more data
		return ReturnOption.CONTINUE;
    }
    
    // TODO return null if not EOF and if no bytes to send - there is no point in sending messages
    // downstream without content or EOF
    public ReturnOption nextMessage() {
    	ByteBuf out = null;
    	
		if (this.outlist.size() > 0) 
			out = this.outlist.remove(0);
		
		boolean eof = (this.outlist.size() == 0) && (this.gzipState == GzipState.DONE);
		
       	this.ufile.setEof(eof);
       	
       	// set it on but never off
       	if (eof)
       		this.eofsent = true;
        
        return this.downstream.handle(this.ufile, out);
    }
    
    public void initializeFileValues(FileDescriptor src) {
    	this.ufile = new FileDescriptor();
    	
		if (StringUtil.isNotEmpty(this.nameHint)) 
			this.ufile.setPath("/" +  this.nameHint);
		else if (src.hasPath()) 
			this.ufile.setPath("/" +  GzipUtils.getUncompressedFilename(src.path().getFileName()));
		else
			this.ufile.setPath("/" + FileUtil.randomFilename("bin"));    	
		
		this.ufile.setModTime(src.getModTime());
		this.ufile.setPermissions(src.getPermissions());
    }

    // return true when completely done
    protected void inflate(ByteBuf in) {
        switch (this.gzipState) {
        case HEADER_START:
            if (in.readableBytes() < 10) 
                return;
            
            // read magic numbers
            int magic0 = in.readByte();
            int magic1 = in.readByte();

            if (magic0 != 31) {
            	OperationContext.get().getTaskRun().kill("Input is not in the GZIP format");
                return;
            }
            
            this.crc.update(magic0);
            this.crc.update(magic1);

            int method = in.readUnsignedByte();
            
            if (method != Deflater.DEFLATED) {
            	OperationContext.get().getTaskRun().kill("Unsupported compression method " + method + " in the GZIP header");
            	return;
            }
            
            this.crc.update(method);

            this.flags = in.readUnsignedByte();
            this.crc.update(this.flags);

            if ((this.flags & FRESERVED) != 0) {
            	OperationContext.get().getTaskRun().kill("Reserved flags are set in the GZIP header");
                return;
            }
            
            // mtime (int)
            this.crc.update(in.readByte());
            this.crc.update(in.readByte());
            this.crc.update(in.readByte());
            this.crc.update(in.readByte());

            this.crc.update(in.readUnsignedByte()); // extra flags
            this.crc.update(in.readUnsignedByte()); // operating system

            this.gzipState = GzipState.FLG_READ;
        case FLG_READ:
            if ((this.flags & FEXTRA) != 0) {
                if (in.readableBytes() < 2) 
                    return;
                
                int xlen1 = in.readUnsignedByte();
                int xlen2 = in.readUnsignedByte();
                
                this.crc.update(xlen1);
                this.crc.update(xlen2);

                this.xlen |= xlen1 << 8 | xlen2;
            }
            
            this.gzipState = GzipState.XLEN_READ;
        case XLEN_READ:
            if (this.xlen != -1) {
                if (in.readableBytes() < xlen) 
                    return;
                
                byte[] xtra = new byte[xlen];
                in.readBytes(xtra);
                this.crc.update(xtra);
            }
            
            this.gzipState = GzipState.SKIP_FNAME;
        case SKIP_FNAME:
            if ((this.flags & FNAME) != 0) {
                boolean gotend = false;
                
                while (in.isReadable()) {
                    int b = in.readUnsignedByte();
                    this.crc.update(b);
                    
                    if (b == 0x00) {
                    	gotend = true;
                        break;
                    }
                } 
                
                if (!gotend)
                	return;
            }
            
            this.gzipState = GzipState.SKIP_COMMENT;
        case SKIP_COMMENT:
            if ((this.flags & FCOMMENT) != 0) {
                boolean gotend = false;
                
                while (in.isReadable()) {
                    int b = in.readUnsignedByte();
                    this.crc.update(b);
                    
                    if (b == 0x00) {
                    	gotend = true;
                        break;
                    }
                } 
                
                if (!gotend)
                	return;
            }
            
            this.gzipState = GzipState.PROCESS_FHCRC;
        case PROCESS_FHCRC:
            if ((this.flags & FHCRC) != 0) {
                if (in.readableBytes() < 4) 
                    return;
                
                long crcValue = 0;
                
                for (int i = 0; i < 4; ++i) 
                    crcValue |= (long) in.readUnsignedByte() << i * 8;

                long readCrc = crc.getValue();
                
                if (crcValue != readCrc) {
                	OperationContext.get().getTaskRun().kill("CRC value missmatch. Expected: " + crcValue + ", Got: " + readCrc);
                    return;
                }
            }
            
            this.crc.reset();
            
            this.gzipState = GzipState.PRROCESS_CONTENT;
        case PRROCESS_CONTENT:
            int readableBytes = in.readableBytes();
            
            if (readableBytes < 1)
            	return;
            
            if (in.hasArray()) {
            	this.inflater.setInput(in.array(), in.arrayOffset() + in.readerIndex(), readableBytes);
            } 
            else {
                byte[] array = new byte[readableBytes];
                in.getBytes(in.readerIndex(), array);
                this.inflater.setInput(array);
            }

            int maxOutputLength = this.inflater.getRemaining() << 1;
            ByteBuf decompressed = Hub.instance.getBufferAllocator().heapBuffer(maxOutputLength);
            
            boolean readFooter = false;
            byte[] outArray = decompressed.array();
            
            try {
                while (!this.inflater.needsInput()) {
                    int writerIndex = decompressed.writerIndex();
                    int outIndex = decompressed.arrayOffset() + writerIndex;
                    int length = decompressed.writableBytes();

                    if (length == 0) {
                        // completely filled the buffer allocate a new one and start to fill it
                    	this.outlist.add(decompressed);
                        decompressed = Hub.instance.getBufferAllocator().heapBuffer(maxOutputLength);
                        outArray = decompressed.array();
                        continue;
                    }

                    int outputLength = this.inflater.inflate(outArray, outIndex, length);
                    
                    if (outputLength > 0) {
                        decompressed.writerIndex(writerIndex + outputLength);
                        
                       	this.crc.update(outArray, outIndex, outputLength);
                    } 
                    else {
                        if (this.inflater.needsDictionary()) {
                            if (this.dictionary == null) {
                            	OperationContext.get().getTaskRun().kill("decompression failure, unable to set dictionary as non was specified");
                            	return;
                            }
                            
                            this.inflater.setDictionary(this.dictionary);
                        }
                    }

                    if (this.inflater.finished()) {
                        readFooter = true;
                        break;
                    }
                }

                in.skipBytes(readableBytes - this.inflater.getRemaining());
            } 
            catch (DataFormatException x) {
            	OperationContext.get().getTaskRun().kill("decompression failure: " + x);
            	return;
            } 
            finally {
                if (decompressed.isReadable()) {
                    this.outlist.add(decompressed);
                } 
                else {
                    decompressed.release();
                }
            }
        	
            if (!readFooter) 
            	break;
            
        	this.gzipState = GzipState.PROCESS_FOOTER;
        case PROCESS_FOOTER:
            if (in.readableBytes() < 8) 
                return;

            long crcValue = 0;
            
            for (int i = 0; i < 4; ++i) 
                crcValue |= (long) in.readUnsignedByte() << i * 8;
            
            long readCrc = this.crc.getValue();
            
            if (crcValue != readCrc) {
            	OperationContext.get().getTaskRun().kill("CRC value missmatch. Expected: " + crcValue + ", Got: " + readCrc);
            	return;
            }

            // read ISIZE and verify
            int dataLength = 0;
            
            for (int i = 0; i < 4; ++i) 
                dataLength |= in.readUnsignedByte() << i * 8;
            
            int readLength = this.inflater.getTotalOut();
            
            if (dataLength != readLength) {
            	OperationContext.get().getTaskRun().kill("Number of bytes mismatch. Expected: " + dataLength + ", Got: " + readLength);
            	return;
            }
            
        	this.gzipState = GzipState.DONE;
        case DONE:
        	break;
        }
    }

    // TODO if there is more from us then handle that before going upstream 
    
    @Override
    public void read() {
		// write all buffers in the queue
		while (this.outlist.size() > 0) {
			if (this.nextMessage() != ReturnOption.CONTINUE)
				return;
		}
		
		// if we reached done and we wrote all the buffers, then send the EOF marker if not already
		if ((this.gzipState == GzipState.DONE) && !this.eofsent) {
			if (this.nextMessage() != ReturnOption.CONTINUE)
				return;
		}
		
    	this.upstream.read();
    }	
}
