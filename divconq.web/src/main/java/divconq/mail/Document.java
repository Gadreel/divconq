package divconq.mail;

import java.io.PrintStream;
import java.util.Map.Entry;

import divconq.web.WebContext;
import divconq.web.dcui.Attributes;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;
import w3.html.Div;

public class Document extends Div {
	protected XElement xel = null;
	
    public Document() {
    	super();
	}
    
    public Document(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		this.xel = xel;
		
		EmailInnerContext ictx = (EmailInnerContext) ctx.getInnerContext();
		
		Attributes attrs = HtmlUtil.initAttrs(xel);

		XElement skelel = xel.find(ictx.isTextMode() ? "TextSkeleton" : "Skeleton");
		
		if (skelel != null) {
			Nodes skel = ctx.getDomain().parseXml(ctx, skelel);
			
			for (Entry<String, String> attr : skelel.getAttributes().entrySet())
				attrs.add(attr.getKey(), attr.getValue());
			
			this.myArguments = new Object[] { attrs, skel };

			// only add if we have something to contribute
			nodes.add(this);
		}
	}

	@Override
	public void stream(WebContext ctx, PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
		EmailInnerContext ictx = (EmailInnerContext) ctx.getInnerContext();
		
		if (!ictx.isTextMode()) {
			super.stream(ctx, strm, indent, firstchild, fromblock);
			return;
		}
		
		if (this.children.size() == 0)
			return;

		boolean fromon = fromblock;
		boolean lastblock = false;
		boolean firstch = this.getBlockIndent(); // only true once, and only if
													// bi

		for (Node node : this.children) {
			if (node.getBlockIndent() && !lastblock && !fromon)
				this.print(ctx, strm, "", true, "");

			node.stream(ctx, strm, indent, (firstch || lastblock), this.getBlockIndent());

			lastblock = node.getBlockIndent();
			firstch = false;
			fromon = false;
		}
	}
}

