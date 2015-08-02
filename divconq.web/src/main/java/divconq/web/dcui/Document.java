package divconq.web.dcui;

import divconq.web.WebContext;
import divconq.xml.XElement;
import w3.html.Body;
import w3.html.Html;

public class Document extends Html {
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
		
		Attributes attrs = HtmlUtil.initAttrs(xel);

		//Nodes contenttemplate = ctx.getDomain().parseXml(ctx, xel.find("Skeleton"));
		
		Nodes bnodes = new Nodes();
		
		if (xel.hasAttribute("Title")) 
			ctx.putInternalParam("PageTitle", xel.getRawAttribute("Title"));
		
		if (xel.hasAttribute("Id")) 
			ctx.putInternalParam("PageId", xel.getRawAttribute("Id"));
		
		// html
		Html5AppHead hd = new Html5AppHead(xel);

		//Body bd = new Body(contenttemplate);
		
		XElement skel = xel.find("Skeleton");
		
		// copy page class
		if (xel.hasAttribute("PageClass")) {
			if (skel.hasAttribute("class"))
				skel.setAttribute("class", xel.getRawAttribute("PageClass") + " " + skel.getAttribute("class"));
			else
				skel.setAttribute("class", xel.getRawAttribute("PageClass"));
		}
		
		Body bd = new Body();
		bd.parseElement(ctx, bnodes, skel);
		
        this.myArguments = new Object[] { attrs, hd, bnodes };
		
		nodes.add(this);
	}
	
    static public Document findDocument(Element el) {
    	while (el != null) {
    		if (el instanceof Document)
    			return (Document)el;
    		
    		el = el.getParent();
    	}
    	
    	return null;
    }
}

