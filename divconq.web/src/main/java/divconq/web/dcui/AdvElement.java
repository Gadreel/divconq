package divconq.web.dcui;

import java.io.PrintStream;
import java.util.Map.Entry;

import divconq.xml.XElement;

public class AdvElement extends Element implements ICodeTag {
    protected XElement src = null;
    
	@Override
	public Node deepCopy(Element parent) {
		AdvElement cp = new AdvElement();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	protected void doCopy(Node n) {
		super.doCopy(n);
		((AdvElement)n).src = this.src;
	}
	
	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		this.src = xel;

        this.myArguments = new Object[] { view.getDomain().parseXml(view, xel) };
		
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
