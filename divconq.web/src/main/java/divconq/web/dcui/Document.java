package divconq.web.dcui;

import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.lang.StringBuilder32;
import divconq.lang.op.OperationContext;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import w3.html.A;
import w3.html.Body;
import w3.html.Div;
import w3.html.Form;
import w3.html.H1;
import w3.html.Html;
import w3.html.Img;
import w3.html.LtrDiv;
import w3.html.P;

public class Document extends Html {
	protected XElement xel = null;
	protected boolean isCms = false;
	
	// temp only
	//StringBuilder32 initScript = null;
	protected ListStruct pages = new ListStruct();
	protected StringBuilder32 bindingscripts = null;
	
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
    	nn.isCms = this.isCms;
    }

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		this.xel = xel;
		
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		String did = OperationContext.get().getUserContext().getDomainId();
		
		if (StringUtil.isNotEmpty(did)) {
			DomainInfo domain = Hub.instance.getDomainInfo(did);
			
			XElement config = domain.getSettings();
			
			if (config != null) {
				XElement web = config.selectFirst("Web");
				
				if (web != null) {
					if (web.hasAttribute("MainPath")) {
						attrs.add("data-dcw-Main", web.getAttribute("MainPath"));
						this.addParams("MainPath", web.getAttribute("MainPath"));
					}
					
					if (web.hasAttribute("SignInPath")) {
						attrs.add("data-dcw-SignIn", web.getAttribute("SignInPath"));
						this.addParams("SignIn", web.getAttribute("SignIn"));
					}
					
					if (web.hasAttribute("HomePath")) {
						attrs.add("data-dcw-Home", web.getAttribute("HomePath"));
						this.addParams("HomePath", web.getAttribute("HomePath"));
					}
					
					if (web.hasAttribute("SiteTitle")) {
						attrs.add("data-dcw-SiteTitle", web.getAttribute("SiteTitle"));
						this.addParams("SiteTitle", web.getRawAttribute("SiteTitle"));
					}
					
					if (web.hasAttribute("SiteAuthor")) 
						this.addParams("SiteAuthor", web.getRawAttribute("SiteAuthor"));
					
					if (web.hasAttribute("SiteCopyright")) 
						this.addParams("SiteCopyright", web.getRawAttribute("SiteCopyright"));
				}
			}
		}
		

		view.contenttemplate = view.getDomain().parseXml(view, xel.find("Layout"));		
		
		if (xel.hasAttribute("Title")) 
			this.addParams("PageTitle", xel.getRawAttribute("Title"));
		
		// html
		Html5Head hd = new Html5Head(xel);

		Body bd = new Body(
				new Div("pageMain", null, 
						new Attributes("data-role", "page"),
						new LtrDiv("pageHeader", null,
								new Attributes("data-role", "header"),
								new Div("pageHeaderLogo", "header-logo",
										new A(
												new Attributes("id", "pageBtnHome", "href", "#", "data-role", "none"),
												new Img(
														new Attributes("src", "/dcw/img/logo48.png", "width", "48", "height", "48")
												)												
										)
								),
								new Div("pageLblTitle", "page-name",
										new H1("@val|PageTitle@")
								),
								/*
					            <div class="btn-notify">
									<a href="#notifyPanel" class="ui-btn ui-icon-carat-l ui-btn-icon-notext ui-corner-all">Notifications</a>
								</div>
								*/
								new Div(null, "sign-out",
										new A(
												new Attributes("id", "pageBtnSignOut", "href", "#", "data-role", "button",
														"data-theme", "a", "data-icon", "sign-out", "data-mini", "true", "data-inline", "true"),
												"Sign Out"
										)
								)
						),
						new LtrDiv("pageMain", null,
								new Attributes("data-role", "main", "class", "ui-content"),
								new LtrDiv("pageContent", null,
										view.contenttemplate
								)
						),
						new LtrDiv("pageFooter", null,
								new Attributes("data-role", "footer"),
								new P(
										new Attributes("class", "ui-title"),
										"@val|SiteTitle@ - @val|SiteCopyright@ @val|SiteAuthor@"
								)
						)
				),
				new Div("puInfo", "ui-content",
						new Attributes("data-role", "popup", "data-theme", "a", "data-overlay-theme", "b"),
						new A(
								new Attributes("class", "ui-btn-right", "href", "#", "data-role", "button", 
										"data-theme", "a", "data-rel", "back", "data-icon", "delete", "data-iconpos", "notext"),
								"Close"
						),
						new Div("puInfoHtml", null)
				),
				new Div("puConfirm", "ui-content ui-corner-all",
						new Attributes("data-role", "popup", "data-theme", "a", "data-overlay-theme", "b"),
						// <a href="#" data-rel="back" class="ui-btn ui-corner-all ui-shadow ui-btn-a ui-icon-delete ui-btn-icon-notext ui-btn-right">Close</a>
						new A(
								new Attributes("class", "ui-btn-right", "href", "#", "data-role", "button", 
										"data-theme", "a", "data-rel", "back", "data-icon", "delete", "data-iconpos", "notext"),
								"Close"
						),
						new Form(
								new Div(
										new Div("puConfirmHtml", null),
										new ButtonLink().withId("btnConfirmPopup").withLabel("Yes").withIcon("check")
								)
						)
				),
				new Div("leftPagePanel", null,
						new Attributes("data-role", "panel", "data-position", "left", "data-display", "reveal", "data-theme", "a")
				),
				new Div("rightPagePanel", null,
						new Attributes("data-role", "panel", "data-position", "right", "data-display", "reveal", "data-theme", "a")
				)
		);
		
        this.myArguments = new Object[] { attrs, hd, bd };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {		
    	// clear temp
    	this.pages.clear();
    	this.bindingscripts = new StringBuilder32();
    	
	    super.build(args);
	    
	    /*
    	ContentPlaceholder ph = this.getContext().getHolder("Scripts");

    	StringBuilder32 sb = new StringBuilder32();
    	
    	sb.appendLine();
    	sb.appendLine("$(document).bind('mobileinit', function() {");
    	sb.appendLine("\t $.mobile.ajaxEnabled = false;");

    	sb.appendLine();
    	
    	sb.append("\t $(document).dcDocument({ ");
    	
    	boolean firstprop = false;
    	
    	if (xel.hasAttribute("Auth")) {
    		sb.append(" Auth: " + new ListStruct((Object[]) xel.getAttribute("Auth").split(",")));
    		firstprop = true;
    	}
    	
    	if ("True".equals(xel.getAttribute("Cms"))) {
    		if (firstprop)
    			sb.append(',');
    		
    		sb.append(" Cms: true");
    		firstprop = true;
    	}
    	
    	if (xel.hasAttribute("OnInit")) {
    		if (firstprop)
    			sb.append(',');
    		
    		sb.append(" OnInit: '" + xel.getAttribute("OnInit") + "'");
    		firstprop = true;
    	}
    	
		if (firstprop)
			sb.append(',');
    	
		// list of page settings - { PageName: NNN, OnInit: NNN, etc } 
		sb.append(" Pages: " + this.pages);
    	
   		sb.appendLine(" });");
   		
   		// add binding scripts
   		sb.append(this.bindingscripts.toString());
    	
    	sb.appendLine("});");

    	sb.appendLine();
    	sb.appendLine("$(document).bind('pageshow', function() {");
    	sb.appendLine("\t $(document).dcDocument('show');");    	
    	sb.appendLine("});");

    	ph.addChildren(new Script(new LiteralText(sb.toString())));
    	*/
	}
    
    public void addPage(RecordStruct v) {
    	this.pages.addItem(v);
    }
    
    public void addBindingScript(String v) {
    	this.bindingscripts.append(v);
    }
    
	/*
    @Override
	public void build(Object... args) {
    	// clear temps
    	this.initScript = new StringBuilder32();
    	
	    super.build(args);
		
    	ContentPlaceholder ph = this.getContext().getHolder("Scripts");

    	// TODO bind to pageinit of the page, not of the document - need to get page id
    	StringBuilder32 sb = new StringBuilder32();
    	
    	sb.appendLine();
    	sb.appendLine("$(document).bind('mobileinit', function(){");

    	sb.appendLine("\t var cntdwn = new dc.lang.CountDownCallback(2, function() { $('#frm" + this.name + "').dcForm('load'); });");

    	sb.appendLine("\t $('#" + pg.getPageId() + "').live('pageinit', function() {\n\t\t cntdwn.dec();\n\t });");
		sb.appendLine("\t $(document).bind('init.dcDocument', function() {\n\t\t cntdwn.dec();\n\t });");
    	sb.appendLine("\t $.mobile.ajaxEnabled = false;");
    	
    	sb.appendLine("});");

    	ph.addChildren(new Script(new LiteralText(sb.toString())));
	}

	public void addToInitScript(String src) {
		this.initScript.append(src);
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

	public boolean isCms() {
		return this.isCms;
	}
}

