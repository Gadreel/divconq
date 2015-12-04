package divconq.web.md;

import divconq.web.WebContext;

public class ProcessContext {
	protected Configuration config = null;
	protected WebContext web = null;
	
	public Configuration getConfig() {
		return this.config;
	}
	
	public void setConfig(Configuration v) {
		this.config = v;
	}
	
	public WebContext getWeb() {
		return this.web;
	}
	
	public void setWeb(WebContext v) {
		this.web = v;
	}
	
	public ProcessContext(Configuration config, WebContext web) {
		this.config = config;
		this.web = web;
	}
}
