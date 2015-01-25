package divconq.web.dcui;

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
					
					if (web.hasAttribute("SiteTitle")) 
						attrs.add("data-dcw-SiteTitle", "@val|SiteTitle@");
				}
			}
		}		

		if (xel.hasAttribute("Skeleton")) {
			String tpath = xel.getAttribute("Skeleton");
			
			CommonPath pp = new CommonPath(tpath + ".dcuis.xml");		
			
			IOutputAdapter sf = view.getDomain().findFile(view.isPreview(), pp, null);
			
			if (sf instanceof ViewTemplateAdapter) {
				XElement layout = ((ViewTemplateAdapter)sf).getSource();				
				view.contenttemplate = view.getDomain().parseXml(view, layout.find("Skeleton"));
			}
		}
		else {
			view.contenttemplate = view.getDomain().parseXml(view, xel.find("Skeleton"));
		}
		
		if (xel.hasAttribute("Title")) 
			this.addParams("PageTitle", xel.getRawAttribute("Title"));
		
		// html
		Html5AppHead hd = new Html5AppHead(xel, domconfig);

		Body bd = new Body(view.contenttemplate);
		
        this.myArguments = new Object[] { attrs, hd, bd };
		
		nodes.add(this);
	}
    
	/*
    @Override
	public void build(Object... args) {
	    super.build(args);
		
    	ContentPlaceholder ph = this.getContext().getHolder("Scripts");

    	ph.addChildren(new Script(new LiteralText(sb.toString())));
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

