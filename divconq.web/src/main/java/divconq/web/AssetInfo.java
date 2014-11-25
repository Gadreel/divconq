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
package divconq.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedNioFile;
import divconq.filestore.CommonPath;
import divconq.io.ByteBufWriter;
import divconq.util.FileUtil;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;

public class AssetInfo {
	//protected InputStream content = null;   TODO for efficiency someday evaluate how to use chunkedinput for streaming
	
	protected ByteBufWriter buffer = null;
	
	protected ChunkedInput<HttpContent> chunks = null;
	protected long chunkSize = -1;
	
	protected CommonPath path= null;
	protected long when = 0;
	protected String mime = null; 
	protected boolean compressed = false;
	protected String attachmentName = null; 
	
	public CommonPath getPath() {
		return this.path;
	}

	public void setMime(String v) {
		this.mime = v;
	}

	public void setCompressed(boolean v) {
		this.compressed = v;
	}
	
	public boolean getCompressed() {
		return this.compressed;
	}

	public void setAttachmentName(String v) {
		this.attachmentName = v;
	}

	public String getAttachmentName() {
		return this.attachmentName;
	}
	
	public boolean isRegion() {
		return (this.chunks != null);
	}
	
	public ChunkedInput<HttpContent> getChunks() {
		return this.chunks;
	}
	
	public void setRegionSize(long regionSize) {
		this.chunkSize = regionSize;
	}
	
	public int getSize() {
		return this.isRegion() ? (int)this.chunkSize 
				: (this.buffer != null) ? this.buffer.readableBytes() 
				: -1;
	}
	
	public ByteBufWriter getBuffer() {
		return this.buffer;
	}
	
	public long getWhen() {
		return this.when;
	}
	
	public AssetInfo(WebContext ctx, CommonPath path, Path content, long when) {
		try {
			this.path = path;
			this.when = when;
			
			String fname = content.getFileName().toString();
			
			this.setMimeForFile(fname);
			
			if (fname.endsWith(".html")) {
				this.buffer = ByteBufWriter.createLargeHeap();
				
				Files.lines(content).forEach(line -> {
					this.buffer.writeLine(ctx.expandMacros(line));
				});
			}
			else {
				this.chunkSize = Files.size(content);
				this.chunks = new HttpChunkedInput(new ChunkedNioFile(content.toFile()));
			}
		} 
		catch (IOException x) {
			// TODO improve support
		}
	}
	
	public AssetInfo(CommonPath path, long when) {
		this.path = path;
		this.when = when;
	}
	
	public AssetInfo(CommonPath path, ByteBufWriter content, long when) {
		this.path = path;
		this.buffer = content;
		this.when = when;
	}
	
	public void setMimeForFile(String fname) {
		String ext = FileUtil.getFileExtension(fname);
		
		this.mime = MimeUtil.getMimeType(ext);
	}
	
	public String getMime() {
		if (StringUtil.isNotEmpty(this.mime))
			return this.mime;
		
		String extension = this.path.getFileExtension();
		return MimeUtil.getMimeType(extension);
	}
}
