package divconq.web;

import divconq.filestore.CommonPath;
import divconq.io.CacheFile;

public interface IOutputAdapter {
	void init(WebSite site, CacheFile file, CommonPath loc, boolean isPreview);
	void execute(WebContext ctx) throws Exception;
}
