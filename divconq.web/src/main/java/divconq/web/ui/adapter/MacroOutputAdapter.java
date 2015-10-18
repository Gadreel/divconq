package divconq.web.ui.adapter;

import divconq.filestore.CommonPath;
import divconq.io.ByteBufWriter;
import divconq.io.CacheFile;
import divconq.web.IOutputAdapter;
import divconq.web.Response;
import divconq.web.WebContext;
import divconq.web.WebSite;

public class MacroOutputAdapter implements IOutputAdapter {
	public CommonPath webpath = null;
	public CacheFile file = null;
	protected String mime = null; 
	
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
		content = ctx.expandMacros(content);

		// TODO add compression
		//if (asset.getCompressed())
		//	resp.setHeader("Content-Encoding", "gzip");
		
		ByteBufWriter buffer = ByteBufWriter.createLargeHeap();
		buffer.write(content);
		
		ctx.sendStart(buffer.readableBytes());

		ctx.send(buffer.getByteBuf());

		ctx.sendEnd();
	}
}
