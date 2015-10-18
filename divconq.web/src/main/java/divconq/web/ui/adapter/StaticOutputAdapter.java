package divconq.web.ui.adapter;

import java.io.IOException;

import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.stream.ChunkedNioFile;
import divconq.filestore.CommonPath;
import divconq.io.CacheFile;
import divconq.web.IOutputAdapter;
import divconq.web.Response;
import divconq.web.WebContext;
import divconq.web.WebSite;

public class StaticOutputAdapter implements IOutputAdapter {
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
		
		if (ctx.getRequest().hasHeader("If-Modified-Since")) {
			long dd = this.file.getWhen() - ctx.getRequest().getDateHeader("If-Modified-Since");  

			// getDate does not return consistent results because milliseconds
			// are not cleared correctly see:
			// https://sourceforge.net/tracker/index.php?func=detail&aid=3162870&group_id=62369&atid=500353
			// so ignore differences of less than 1000, they are false positives
			if (dd < 1000) {
				resp.setStatus(HttpResponseStatus.NOT_MODIFIED);
				ctx.send();
				return;
			}
		}

		// TODO add compression
		//if (asset.getCompressed())
		//	resp.setHeader("Content-Encoding", "gzip");
		
		ctx.sendStart(this.file.getSize());

		// TODO send from memory cache if small enough
		try {
			ctx.send(new HttpChunkedInput(new ChunkedNioFile(this.file.getFilePath().toFile())));		// TODO not ideal, cleanup so direct reference to path is not needed
		} 
		catch (IOException x) {
			// TODO improve support
		}

		ctx.sendEnd();
	}
}
