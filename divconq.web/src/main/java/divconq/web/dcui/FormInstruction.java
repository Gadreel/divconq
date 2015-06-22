package divconq.web.dcui;

import divconq.web.WebContext;
import divconq.xml.XElement;

public class FormInstruction extends MixedElement implements ICodeTag {
	public FormInstruction() {
		super();
	}
	
	public FormInstruction(Object... args) {
		super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));
		
        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(WebContext ctx, Object... args) {
	    super.build(ctx, "FormInstruction", true, args);
	}
}
