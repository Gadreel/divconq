package divconq.web.md.process;

import java.util.List;
import java.util.Map;

import divconq.web.WebContext;
import divconq.xml.XElement;

public abstract class Plugin {
	protected String idPlugin;
	
	public Plugin(String idPlugin) {
		this.idPlugin = idPlugin;
	}

	public abstract void emit(WebContext ctx, XElement parent, List<String> lines, Map<String, String> params);

	public String getIdPlugin() {
		return idPlugin;
	}
}
