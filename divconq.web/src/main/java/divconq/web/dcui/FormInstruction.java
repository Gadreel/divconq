package divconq.web.dcui;

import divconq.xml.XElement;

public class FormInstruction extends MixedElement implements ICodeTag {
	public FormInstruction() {
		super();
	}
	
	public FormInstruction(Object... args) {
		super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		FormInstruction cp = new FormInstruction();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));
		
        this.myArguments = new Object[] { attrs, view.getDomain().parseXml(view, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {
	    super.build("FormInstruction", true, args);
	}
}
