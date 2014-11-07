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
package divconq.api.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.multipart.AbstractHttpData;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import divconq.lang.op.OperationResult;

public class UploadStream extends AbstractHttpData implements FileUpload {
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String NAME = "name";
    public static final String FILENAME = "filename";
    public static final String FORM_DATA = "form-data";
    public static final String ATTACHMENT = "attachment";

    protected String filename = null;
    protected String contentType = null;
    protected String contentTransferEncoding = null;
    protected OperationResult ops = null;
    
    protected ReadableByteChannel in = null;

    public UploadStream(ReadableByteChannel in, String name, String filename, String contentType,
            String contentTransferEncoding, Charset charset, long size, OperationResult ops) 
    {    	
        super(name, charset, size);
        
        this.setFilename(filename);
        this.setContentType(contentType);
        this.setContentTransferEncoding(contentTransferEncoding);
        
        this.in = in;
        this.ops = ops;
    }

    @Override
    public HttpDataType getHttpDataType() {
        return HttpDataType.FileUpload;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public void setFilename(String filename) {
        if (filename == null) 
            throw new NullPointerException("filename");
        
        this.filename = filename;
    }

    @Override
    public void setContentType(String contentType) {
        if (contentType == null) 
            throw new NullPointerException("contentType");
        
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public String getContentTransferEncoding() {
        return this.contentTransferEncoding;
    }

    @Override
    public void setContentTransferEncoding(String contentTransferEncoding) {
        this.contentTransferEncoding = contentTransferEncoding;
    }

    @Override
    public String toString() {
        return UploadStream.CONTENT_DISPOSITION + ": " +
        		UploadStream.FORM_DATA + "; " + UploadStream.NAME + "=\"" + getName() +
                "\"; " + UploadStream.FILENAME + "=\"" + this.filename + "\"\r\n" +
                HttpHeaders.Names.CONTENT_TYPE + ": " + this.contentType +
                (getCharset() != null? "; " + HttpHeaders.Values.CHARSET + '=' + getCharset() + "\r\n" : "\r\n") +
                HttpHeaders.Names.CONTENT_LENGTH + ": " + length() + "\r\n" +
                "Completed: " + isCompleted();
    }

    @Override
    public FileUpload retain(int increment) {
        super.retain(increment);
        return this;
    }

    @Override
    public FileUpload retain() {
        super.retain();
        return this;
    }

	@Override
	public void setContent(ByteBuf buffer) throws IOException {
	}

	@Override
	public void addContent(ByteBuf buffer, boolean last) throws IOException {
	}

	@Override
	public void setContent(File file) throws IOException {
	}

	@Override
	public void setContent(InputStream inputStream) throws IOException {
	}

	@Override
	public void delete() {
	}

	@Override
	public byte[] get() throws IOException {
		// only handle chunked
		return null;
	}

	@Override
	public ByteBuf getByteBuf() throws IOException {
		// only handle chunked
		return null;
	}

	@Override
	public ByteBuf getChunk(int length) throws IOException {
        if (this.in == null || length == 0) 
            return Unpooled.EMPTY_BUFFER;
        
        // indicate that we are keeping busy and not hung
        this.ops.touch();
        
        //System.out.println("Get last activity after touch: " + this.ops.getLastActivity());
        
        int read = 0;
        
        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        
        while (read < length) {
            int readnow = this.in.read(byteBuffer);
            
            if (readnow == -1) {
            	this.in.close();
                this.in = null;
                break;
            } 
            else {
                read += readnow;
            }
        }
        
        if (read == 0) 
            return Unpooled.EMPTY_BUFFER;
        
        byteBuffer.flip();
        ByteBuf buffer = Unpooled.wrappedBuffer(byteBuffer);
        buffer.readerIndex(0);
        buffer.writerIndex(read);
        return buffer;
	}

	@Override
	public String getString() throws IOException {
        return this.getString(HttpConstants.DEFAULT_CHARSET);
	}

	@Override
	public String getString(Charset encoding) throws IOException {
        return "N/A";
	}

	@Override
	public boolean renameTo(File dest) throws IOException {
		return false;
	}

	@Override
	public boolean isInMemory() {
		return false;
	}

	@Override
	public File getFile() throws IOException {
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
}
