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

import w3.html.A;
import w3.html.Div;
import w3.html.H1;
import w3.html.P;
import divconq.lang.StringBuilder32;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.ICodeTag;
import divconq.view.MixedElement;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

/**
 * A single page in a jqm document
 * 
 * @author andy
 *
 */
public class Page extends MixedElement implements ICodeTag {
	protected XElement xel = null;
	protected String name = null;
	//protected String script = null;

	// temp only
	protected ListStruct forms = new ListStruct();
	
	public String getPageName() {
		return this.name;
	}
	
	public String getPageId() {
		return "pg" + this.name;
	}
	
    public Page() {
    	super();
	}
    
    public Page(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Page cp = new Page();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	Page nn = (Page)n;
    	nn.name = this.name;
    	nn.xel = this.xel;
    	//nn.script = this.script;
    }

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		this.xel = xel;
		
		nodes.add(this);

		/*
		// page scripts
    	StringBuilder32 sb = new StringBuilder32();
    	
    	sb.appendLine();
    	sb.appendLine("$(document).bind('mobileinit', function(){");
    	
    	String opts = "PageName: '" + this.name + "'";

    	if (xel.hasAttribute("OnInit"))
        	opts += ", OnInit: " + xel.getAttribute("OnInit");				
    	
    	sb.appendLine("\t var cntdwn = new dc.lang.CountDownCallback(2, function() { $('#pg" + this.name + "').dcPage({" + opts + "}); });");

    	sb.appendLine("\t $('#pg" + this.name + "').live('pageinit', function() { cntdwn.dec(); });");
		sb.appendLine("\t $(document).bind('init.dcDocument', function() { cntdwn.dec(); });");
    	
    	sb.appendLine("\t $('#pg" + this.name + "').live('pagebeforeshow', function() {");

    	if (xel.hasAttribute("OnBeforeShow"))
        	sb.appendLine("\t\t " + xel.getAttribute("OnBeforeShow") + "();");				
    	
    	sb.appendLine("\t });");
    	
    	
    	sb.appendLine("\t $('#pg" + this.name + "').live('pageshow', function() {");

    	if (xel.hasAttribute("OnShow"))
        	sb.appendLine("\t\t " + xel.getAttribute("OnShow") + "();");				
    	
    	sb.appendLine("\t });");    	
    	
    	//sb.appendLine("\t $.mobile.ajaxEnabled = false;");
    	
    	sb.appendLine("});");
    	
    	// messages
		
    	this.script = sb.toString();    	
    	*/
	}
	
    @Override
	public void build(Object... args) {
	    Document doc = Document.findDocument(this);
    	
    	if (doc == null)
    		return;
    	
    	ViewInfo view = this.getPartRoot().getView();
    	
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		this.name = xel.getAttribute("Name");

		attrs.add("id", "pg" + this.name);
		attrs.add("data-role", "page");
		attrs.add("class", "type-interior dcmPage");

		boolean sparse = "True".equals(xel.getAttribute("Sparse"));
		
		Nodes pgnodes = new Nodes();
		
		boolean isCms = doc.isCms();
		
		//if (this.parent instanceof Document)
		//	isCms = ((Document)this.parent).isCms();
		
		if (!sparse) {
			A link1 = null;
			A link2 = null;
			
			if (isCms) {
				link1 = new A(new Attributes("href", "/dcm/View/Cms/Main", "data-icon", "home", "data-iconpos", "notext"), "Main");
				//new A(new Attributes("href", "#menu-panel", "data-icon", "reorder", "data-iconpos", "notext"), "Main"),
				link2 = new A(new Attributes("href", "javascript:dc.cms.signout();", "data-icon", "signout", "data-iconpos", "notext"), "Sign Out");				
			}
			
			// TODO dcm hard coded links - fix
			Div hdr = new Div(
					new Attributes("id", "pg" + this.name + "Header", "data-role", "header", "data-theme", "f"),
					new H1(xel.getAttribute("Title")),
					link1,
					link2
			);
			
			if ("True".equals(xel.getAttribute("FixedHeader")))
				hdr.addAttribute("data-position", "fixed");
			
			pgnodes.add(hdr);
		}
		
		XElement navbarel = xel.find("jqmNavbar"); 
		
		if (navbarel != null)
			pgnodes.add(view.getDomain().parseXml("HtmlOutput", view, navbarel).getFirst());

		// we'll want a popup either way - content or split
		Div infopopup = new Div(
				new Attributes("id", "pu" + this.name + "Info", "data-role", "popup", "data-overlay-theme", "a",  "data-theme", "b", "class", "ui-content"),
				new A(new Attributes("href", "#", "data-rel", "back", "data-role", "button", "data-theme", "a", "data-icon", "delete", "data-iconpos", "notext", "class", "ui-btn-right"), "Close"),
				new Div(new Attributes("id", "pu" + this.name + "InfoHtml"), "Missing or invalid inputs, please correct.")
		);

		Div warnpopup = new Div(
				new Attributes("id", "pu" + this.name + "Warn", "data-role", "popup", "data-overlay-theme", "a",  "data-theme", "e", "class", "ui-content"),
				new A(new Attributes("href", "#", "data-rel", "back", "data-role", "button", "data-theme", "a", "data-icon", "delete", "data-iconpos", "notext", "class", "ui-btn-right"), "Close"),
				new Div(new Attributes("id", "pu" + this.name + "WarnHtml"), "Missing or invalid inputs, please correct.")
		);
		
		Div warnmsg = new Div(new Attributes("id", "msg" + this.name + "Warn", "style", "overflow: auto; padding:10px 15px; display: none;", "class", "ui-footer ui-bar-e"));
		Div infomsg = new Div(new Attributes("id", "msg" + this.name + "Info", "style", "overflow: auto; padding:10px 15px; display: none;", "class", "ui-footer ui-bar-b"));
		
		Nodes contentnodes = new Nodes();
		XElement conxel = xel.find("jqmContent");
		
		if (conxel != null) {
			Div full = new Div(new Attributes("id", "contentFull" + this.name, "class", "content-full"), warnmsg, infomsg, infopopup, warnpopup, view.getDomain().parseXml("HtmlOutput", view, conxel));
			contentnodes.add(full);
		}
		else {
			XElement primexel = xel.find("jqmPrimary");
			
			if (primexel != null) {
				Div full = new Div(new Attributes("id", "contentPrime" + this.name, "class", "content-primary"), warnmsg, infomsg, infopopup, warnpopup, view.getDomain().parseXml("HtmlOutput", view, primexel));
				contentnodes.add(full);
			}
			
			XElement scexel = xel.find("jqmSecondary");
			
			if (scexel != null) {
				Div full = new Div(new Attributes("id", "contentSecond" + this.name, "class", "content-secondary"), view.getDomain().parseXml("HtmlOutput", view, scexel));
				contentnodes.add(full);
			}
		}
		
		/*
		for (XNode extra : xel.getChildren()) {
			if (extra instanceof XElement) {
				XElement xextra = (XElement)extra;
				
				if (!xextra.getName().startsWith("jqm")) {				
					for (Node n : view.getDomain().parseXml("HtmlOutput", view, xextra).getList())
						contentnodes.add(n);
				}
			}
		}
		*/
		
		Div content = new Div(new Attributes("id", "content" + this.name, "data-role", "content"), contentnodes);
		
		pgnodes.add(content);
		
		// TODO dcm hard coded links - fix
		//pgnodes.add(new IncludePart("/dcm/View/Cms/MainMenuPanel"));

		if (!sparse) {
			String copy = xel.getAttribute("Copy");
			String ver = xel.getAttribute("Version");
			
			if (isCms) {
				copy = "&copy; 2013 eTimeline, LLC. All rights reserved.";
				ver = "@dcmVersion@";
			}
			
			// TODO dcm hard coded links - fix
			Div ftr = new Div(
					new Attributes("id", "pg" + this.name + "Footer", "data-role", "footer", "data-theme", "c", "class", "footer-docs"),
					new P(new Attributes("class", "dcm-version"), ver),
					new P(copy)
			);
			
			pgnodes.add(ftr);
		}
		
		args = new Object[] { attrs, pgnodes };
		
	    super.build("div", args);
    	
    	RecordStruct pg = new RecordStruct(
    			new FieldStruct("PageName", this.name),
    			new FieldStruct("Forms", this.forms),
    			new FieldStruct("OnInit", xel.getAttribute("OnInit", null)),
    			new FieldStruct("LoadEvery", "True".equals(xel.getAttribute("LoadEvery")))
    	);
    	
    	doc.addPage(pg);

    	// scripts
    	StringBuilder32 sb = new StringBuilder32();
    	
    	sb.appendLine();
    	
    	sb.appendLine("\t $(document).on('pagebeforeshow', '#pg" + this.name + "', function() {");

    	if (xel.hasAttribute("OnBeforeShow"))
        	sb.appendLine("\t\t " + xel.getAttribute("OnBeforeShow") + "();");				
    	
    	sb.appendLine("\t });");
    	
    	
    	sb.appendLine("\t $(document).on('pageshow', '#pg" + this.name + "', function() {");

    	if (xel.hasAttribute("OnShow"))
        	sb.appendLine("\t\t " + xel.getAttribute("OnShow") + "();");				
    	
    	sb.appendLine("\t });");    	
    	
    	doc.addBindingScript(sb.toString());
    	
    	//ContentPlaceholder ph = this.getContext().getHolder("Scripts");

    	//if (this.script != null)
    	//	ph.addChildren(new Script(new LiteralText(this.script)));
	}
    
    public void addForm(RecordStruct v) {
    	this.forms.addItem(v);
    }
    
    static public Page findPage(Element el) {
    	while (el != null) {
    		if (el instanceof Page)
    			return (Page)el;
    		
    		el = el.getParent();
    	}
    	
    	return null;
    }
}
