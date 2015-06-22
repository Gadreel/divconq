package divconq.web.dcui;

import java.io.PrintStream;
import java.util.Map.Entry;

import divconq.web.WebContext;
import divconq.xml.XElement;

public class AdvElement extends Element implements ICodeTag {
    protected XElement src = null;
	
	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		this.src = xel;

        this.myArguments = new Object[] { ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}
    
    @Override
    public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
    	this.name = this.src.getName();
    	
    	for (Entry<String, String> attr : this.src.getAttributes().entrySet())
    		this.attributes.put(attr.getKey(), attr.getValue());
    	
    	return super.writeDynamic(buffer, tabs, first);
    }
}
