package divconq.web.ui.adapter;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.lang.reflect.Method;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import divconq.filestore.CommonPath;
import divconq.io.ByteBufWriter;
import divconq.io.CacheFile;
import divconq.lang.op.OperationContext;
import divconq.net.NetUtil;
import divconq.util.StringUtil;
import divconq.web.IOutputAdapter;
import divconq.web.Response;
import divconq.web.WebContext;
import divconq.web.WebSite;

public class GasOutputAdapter implements IOutputAdapter {
	protected CommonPath webpath = null;
	protected CacheFile file = null;
	protected String mime = null; 
	protected String attachmentName = null; 
	protected boolean compressed = false;
	
	public void setMime(String v) {
		this.mime = v;
	}
	
	public String getMime() {
		return this.mime;
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
	
	@Override
	public void init(WebSite site, CacheFile file, CommonPath web, boolean isPreview) {
		this.webpath = web;
		this.file = file;		
		this.mime = site.getMimeType(this.file.getExt());		
	}
	
	@Override
	public void execute(WebContext ctx) throws Exception {
		Response resp = ctx.getResponse(); 
		
		resp.setHeader("Content-Type", this.mime);
		resp.setDateHeader("Date", System.currentTimeMillis());
		resp.setDateHeader("Last-Modified", this.file.getWhen());
		resp.setHeader("X-UA-Compatible", "IE=Edge,chrome=1");
		
		// because of Macro support we need to rebuild this page every time it is requested
		String content = file.asString();

		try {
			@SuppressWarnings("resource")
			GroovyClassLoader loader = new GroovyClassLoader();
			Class<?> groovyClass = loader.parseClass(content);
			Method runmeth = null;
			
			for (Method m : groovyClass.getMethods()) {
				if (!m.getName().startsWith("run"))
					continue;
				
				runmeth = m;
				break;
			}
			
			if (runmeth != null) {
				ByteBufWriter buffer = ByteBufWriter.createLargeHeap();
	
				this.mime = "text/html";
				
				GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
				Object[] args2 = { ctx, buffer, this };
				
				groovyObject.invokeMethod("run", args2);

				if (StringUtil.isNotEmpty(this.attachmentName))
					resp.setHeader("Content-Disposition", "attachment; filename=\"" + NetUtil.urlEncodeUTF8(this.attachmentName) + "\"");
				
				if (this.compressed)
					resp.setHeader("Content-Encoding", "gzip");
				
				//ByteBufWriter buffer = ByteBufWriter.createLargeHeap();
				//buffer.write(content);
				
				ctx.sendStart(buffer.readableBytes());

				ctx.send(buffer.getByteBuf());

				ctx.sendEnd();			
			}
		}
		catch (Exception x) {
			OperationContext.get().error("Unable to execute script! Error: " + x);
			ctx.getResponse().setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			ctx.getResponse().setKeepAlive(false);
			ctx.send();			
		}
	}
}
