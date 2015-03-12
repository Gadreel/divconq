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
package divconq.web.asset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedNioFile;
import divconq.filestore.CommonPath;
import divconq.io.ByteBufWriter;
import divconq.util.FileUtil;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
import divconq.web.WebContext;

public class AssetInfo {
	//protected InputStream content = null;   TODO for efficiency someday evaluate how to use chunked for streaming
	
	protected ByteBufWriter buffer = null;
	
	protected long chunkSize = -1;
	
	protected CommonPath path= null;
	protected Path fpath = null;
	protected boolean region = false;
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
		return this.region;
	}
	
	public ChunkedInput<HttpContent> getChunks() {
		try {
			return new HttpChunkedInput(new ChunkedNioFile(this.fpath.toFile()));
		} 
		catch (IOException x) {
			// TODO improve support
		}
		
		return null;
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
	
	public AssetInfo(CommonPath path, Path content, long when) {
		this.path = path;
		this.when = when;
		this.fpath = content;
		
		String fname = content.getFileName().toString();
		
		this.setMimeForFile(fname);
		
		this.region = !fname.endsWith(".html") && !fname.endsWith(".js");		// TODO we need these until we support nxxUploader with translations
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
	
	public void load(WebContext ctx) {
		// if file path then we haven't processed content yet
		// note that buffer can be set once and then used as cache
		if ((this.fpath != null) && (this.buffer == null)) {
			try {
				// region does its own thing for content loading, seeabove
				if (this.region) {
					this.chunkSize = Files.size(this.fpath);
				}
				else {
					//System.out.println("expand 1: " + System.currentTimeMillis());
					
					this.buffer = ByteBufWriter.createLargeHeap();
					
					try (Stream<String> strm = Files.lines(this.fpath)) {
						strm.forEach(line -> {
							this.buffer.writeLine(ctx.expandMacros(line));
						});
					}
					
					//System.out.println("expand 2: " + System.currentTimeMillis());
					
				}
			} 
			catch (IOException x) {
				// TODO improve support
			}
		}
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
