package divconq.web.asset;

import java.lang.reflect.Method;
import java.nio.file.Path;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import io.netty.handler.codec.http.HttpResponseStatus;
import divconq.filestore.CommonPath;
import divconq.io.ByteBufWriter;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.net.NetUtil;
import divconq.util.StringUtil;
import divconq.web.IOutputAdapter;
import divconq.web.Response;
import divconq.web.WebContext;

public class AssetOutputAdapter implements IOutputAdapter {
	public CommonPath webpath = null;
	public Path filepath = null;
	
	public AssetOutputAdapter(CommonPath web, Path file) {
		this.webpath = web;
		this.filepath = file;
	}

	// TODO a lot of improvements to be made here - caching small static files (<16kb)
	// better streaming for file output rather than build big memory blob and return
	
	@Override
	public OperationResult execute(WebContext ctx) throws Exception {
		OperationResult res = new OperationResult();
		String fpstr = this.filepath.toString();
		
		AssetInfo asset = null;

		if (fpstr.endsWith(".pui.xml")) {
			asset = PuiAssetInfo.build(ctx, this.webpath, this.filepath);	
		}
		else if (fpstr.endsWith(".gas")) {
			@SuppressWarnings("resource")
			GroovyClassLoader loader = new GroovyClassLoader();
			Class<?> groovyClass = loader.parseClass(this.filepath.toFile());
			Method runmeth = null;
			
			for (Method m : groovyClass.getMethods()) {
				if (!m.getName().startsWith("run"))
					continue;
				
				runmeth = m;
				break;
			}
			
			if (runmeth != null) {
				ByteBufWriter mem = ByteBufWriter.createLargeHeap();
				asset = AssetInfo.build(this.webpath, mem);
				asset.setMime("text/html");
				
				GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
				Object[] args2 = { ctx, mem, asset };
				
				groovyObject.invokeMethod("run", args2);
			}
			
			OperationContext.get().error("Unable to execute script!");
		}
		else {
			asset = AssetInfo.build(this.webpath, this.filepath);
		}

		if (asset == null) {
			res.errorTr(150001);
			return res;
		}
		
		String fpath = asset.getPath().toString();
		
		//if ((fpath == null) || (asset.getSize() == -1)) {
		if (fpath == null) {
			res.errorTr(150001);
			return res;
		}

		// certain resource types cannot be delivered
		if (fpath.endsWith(".class") || fpath.endsWith(".dcui.xml")) {
			res.errorTr(150001);
			return res;
		}

		Response resp = ctx.getResponse(); 
		
		resp.setHeader("Content-Type", asset.getMime());
		resp.setDateHeader("Date", System.currentTimeMillis());
		resp.setDateHeader("Last-Modified", asset.getWhen());
		resp.setHeader("X-UA-Compatible", "IE=Edge,chrome=1");
		
		if (ctx.getRequest().hasHeader("If-Modified-Since")) {
			long dd = asset.getWhen() - ctx.getRequest().getDateHeader("If-Modified-Since");  

			// getDate does not return consistent results because milliseconds
			// are not cleared correctly see:
			// https://sourceforge.net/tracker/index.php?func=detail&aid=3162870&group_id=62369&atid=500353
			// so ignore differences of less than 1000, they are false positives
			if (dd < 1000) {
				resp.setStatus(HttpResponseStatus.NOT_MODIFIED);
				ctx.send();
				return res;
			}
		}
		
		asset.load(ctx);
		
		if (asset.getCompressed())
			resp.setHeader("Content-Encoding", "gzip");
		
		String attach = asset.getAttachmentName();
		
		if (StringUtil.isNotEmpty(attach))
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + NetUtil.urlEncodeUTF8(attach) + "\"");
		
		// TODO send HttpResponse with content length...then push the file directly from here instead of setting body first and using memory 
		//ctx.getResponse().setBody(content);
		
		//System.out.println("Sending: " + fpath + " as: " + asset.getSize());
		ctx.sendStart(asset.getSize());
		
		// TODO
		if (asset.isRegion()) {
			//System.out.println("Sending: " + fpath + " as chunks ");
			ctx.send(asset.getChunks());
		}
		else {
			//System.out.println("Sending: " + fpath + " as buffer ");
			ctx.send(asset.getBuffer().getByteBuf());
		}
		
		//System.out.println("Sending: " + fpath + " as end");
		ctx.sendEnd();
		
		return res;
		
		/*
		byte[] bis = this.extension.getBundle().findFileEntry(fpath);
		
		// TODO make sure there is some way to flush responses in Simple					
		if (bis != null) 
			this.response.getOutputStream().write(bis);
		else
			notfound = true;
			*/
	}
}
