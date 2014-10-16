/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package jqm;

import w3.html.Body;
import w3.html.Html;
import w3.html.Script;
import w3.html.Style;
import divconq.lang.StringBuilder32;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.view.Attributes;
import divconq.view.ContentPlaceholder;
import divconq.view.Element;
import divconq.view.LiteralText;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.Html5Head;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XAttribute;
import divconq.xml.XElement;

/**
 * A jqm document containing 1 or more pages
 * 
 * @author andy
 *
 */
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
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		this.xel = xel;
		
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		// html
		this.isCms = "True".equals(xel.getAttribute("Cms"));
		String css = isCms ? "/css/dcm.css" : xel.getAttribute("Css", "/css/mobile-default.css");
		String inc = "dcCore,dcManager,jQueryMobile";
		
		if (xel.hasAttribute("Extras"))
			inc += "," + xel.getAttribute("Extras");
		
		Html5Head hd = new Html5Head(
				new XElement("Html5Head",
						new XAttribute("Title", xel.getAttribute("Title") + ": @dcmSiteTitle@"),
						new XAttribute("Public", this.isCms ? "False" : xel.getAttribute("Public", "True")),
						new XAttribute("Mobile", "True"),
						new XAttribute("Icon", "True".equals(xel.getAttribute("Icon")) ? "/local/Asset/logo" : "/dcw/Asset/logo"),		// may be redundant, dcw is backup for local?
						new XAttribute("Include", inc)
				), 
				new Style(css),
				this.isCms ? new Script("/lib/user/dcCms.js") : null
		);

		Body bd = new Body(
				view.getDomain().parseXml("HtmlOutput", view, xel)
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
