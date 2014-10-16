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

import divconq.interchange.CommonPath;
import divconq.lang.FuncResult;
import divconq.lang.Memory;
import divconq.lang.chars.Utf8Encoder;
import divconq.util.FileUtil;
import divconq.util.IOUtil;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;

public class AssetInfo {
	//protected InputStream content = null;   TODO for efficiency someday evaluate a plan for streaming Assets - disabled for now
	protected Memory mcontent = null;
	protected byte[] bcontent = null;
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
	
	public Memory getContent() {
		if (this.mcontent != null) 
			return new Memory(this.mcontent);
		
		if (this.bcontent != null)
			return new Memory(this.bcontent);
		
		return null;  //this.content;
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
				FuncResult<CharSequence> htmlres = IOUtil.readEntireFile(content);
				
				if (htmlres.hasErrors())
					return;
				
				String html = htmlres.getResult().toString();
				
				html = ctx.expandMacros(html);
				
				this.bcontent = Utf8Encoder.encode(html);
			}
			else
				this.bcontent = Files.readAllBytes(content);
		} 
		catch (IOException x) {
			// TODO improve support
		}
	}
	
	public AssetInfo(CommonPath path, long when) {
		this.path = path;
		this.when = when;
	}
	
	public AssetInfo(CommonPath path, Memory content, long when) {
		this.path = path;
		this.mcontent = content;
		this.when = when;
	}
	
	public AssetInfo(CommonPath path, byte[] content, long when) {
		this.path = path;
		this.bcontent = content;
		this.when = when;
	}
	
	public AssetInfo(CommonPath path, byte[] content) {
		this.path = path;
		this.bcontent = content;
		this.when = System.currentTimeMillis();
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
