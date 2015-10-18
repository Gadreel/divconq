package divconq.cms.importer;

import java.nio.file.Path;

import divconq.lang.CountDownCallback;

abstract public class FileImporter {
	protected String area = null;
	protected String alias = null;
	protected String key = null;
	protected Path pubfile = null;
	protected Path prefile = null;
	
	public void setKey(String v){
		this.key = v;
	}
	
	public void setArea(String v) {
		this.area = v;
	}
	
	public void setAlias(String v) {
		this.alias = v;
	}
	
	public void setFile(Path v) {
		this.pubfile = v;
	}
	
	public void setPreviewFile(Path v) {
		this.prefile =v;
	}
	
	abstract public void preCheck(ImportWebsiteTool util);
	abstract public void doImport(ImportWebsiteTool util, CountDownCallback cb);
}
