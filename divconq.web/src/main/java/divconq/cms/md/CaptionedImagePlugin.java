package divconq.cms.md;

import java.util.List;
import java.util.Map;

import divconq.cms.md.process.Plugin;

public class CaptionedImagePlugin extends Plugin {

	public CaptionedImagePlugin() {
		super("CapitionedImage");
	}

	@Override
	public void emit(StringBuilder out, List<String> lines, Map<String, String> params) {
		out.append("<img ");
		
        for (String name : params.keySet()) 
            out.append(name + "=\"" + divconq.xml.XNode.quote(params.get(name)) + "\" ");
		
		out.append("/>");
	}
}
