package divconq.web.dcui;

import java.util.Map.Entry;

import divconq.filestore.CommonPath;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.util.StringUtil;
import divconq.web.IOutputAdapter;
import divconq.xml.XElement;
import w3.html.Body;
import w3.html.Html;

public class Document extends Html {
	protected XElement xel = null;
	//protected XElement sxel = null;		// for separate skeletons
	
	// temp only
	
    public Document() {
    	super();
	}
    
    public Document(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Document cp = new Document();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	Document nn = (Document)n;
    	nn.xel = this.xel;
    	//nn.sxel = this.sxel;
    }

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		this.xel = xel;
		
		XElement domconfig = null;
		
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		String did = OperationContext.get().getUserContext().getDomainId();
		
		if (StringUtil.isNotEmpty(did)) {
			DomainInfo domain = Hub.instance.getDomainInfo(did);
			
			domconfig = domain.getSettings();
			
			if (domconfig!= null) {
				XElement web = domconfig.selectFirst("Web");
				
				if (web != null) {
					if (web.hasAttribute("SignInPath")) 
						attrs.add("data-dcw-SignIn", "@val|SignInPath@");
					
					if (web.hasAttribute("HomePath")) 
						attrs.add("data-dcw-Home", "@val|HomePath@");
					
					if (web.hasAttribute("PortalPath")) 
						attrs.add("data-dcw-Portal", "@val|PortalPath@");
					
					if (web.hasAttribute("SiteTitle")) 
						attrs.add("data-dcw-SiteTitle", "@val|SiteTitle@");
				}
			}
		}		
		
		// TODO how about just copying

		if (xel.hasAttribute("Skeleton")) {
			String tpath = xel.getAttribute("Skeleton");
			
			CommonPath pp = new CommonPath(tpath + ".dcuis.xml");		
			
			IOutputAdapter sf = view.getDomain().findFile(view.isPreview(), pp, null);
			
			if (sf instanceof ViewTemplateAdapter) {
				XElement layout = ((ViewTemplateAdapter)sf).getSource();			
				
				/*
				//this.sxel = layout;
		    	view.addLibs(layout.selectAll("RequireLib"));
		    	view.addStyles(layout.selectAll("RequireStyle"));
				view.addFunctions(layout.selectAll("Function"));
				
				// copy fixed page parts
				for (XElement ppel : layout.selectAll("PagePart"))
					xel.add(ppel);
				
				*/

				// copy all attributes over, unless they have been overridden
				for (Entry<String, String> attr : layout.getAttributes().entrySet())
					if (!xel.hasAttribute(attr.getKey()))
						xel.setAttribute(attr.getKey(), attr.getValue());
				
				// copy all child elements over
				for (XElement chel : layout.selectAll("*"))
					xel.add(chel);
				
				//view.contenttemplate = view.getDomain().parseXml(view, layout.find("Skeleton"));
			}
		}
		//else {
			view.contenttemplate = view.getDomain().parseXml(view, xel.find("Skeleton"));
		//}
		
		if (xel.hasAttribute("Title")) 
			view.addParams("PageTitle", xel.getRawAttribute("Title"));
		
		if (xel.hasAttribute("Id")) 
			view.addParams("PageId", xel.getRawAttribute("Id"));
		
		// html
		Html5AppHead hd = new Html5AppHead(xel, domconfig);

		Body bd = new Body(view.contenttemplate);
		
        this.myArguments = new Object[] { attrs, hd, bd };
		
		nodes.add(this);
	}
    
	/*
    @Override
	public void build(Object... args) {
	    if (this.sxel != null) {
	    	this.getContext().addLibs(this.sxel.selectAll("RequireLib"));
	    	this.getContext().addStyles(this.sxel.selectAll("RequireStyle"));
	    }
	    
	    super.build(args);	    
	}
	*/
	
    static public Document findDocument(Element el) {
    	while (el != null) {
    		if (el instanceof Document)
    			return (Document)el;
    		
    		el = el.getParent();
    	}
    	
    	return null;
    }
}

