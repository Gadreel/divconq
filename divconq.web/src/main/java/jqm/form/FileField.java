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
package jqm.form;

import w3.html.Div;
import w3.html.Input;
import w3.html.Label;
import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.LiteralText;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

public class FileField extends Div {
    public FileField() {
    	super();
	}
    
    public FileField(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		FileField cp = new FileField();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));
		
		Nodes children = new Nodes();
		
		if (xel.hasAttribute("Name")) {
			String inputname = xel.getAttribute("Name");
			String label = xel.hasAttribute("Label") ? xel.getAttribute("Label") : inputname;
			
			String inputid = "file" + inputname;
			String lblid = "lbl" + inputname;
			
			XElement fulllbl = xel.find("Label");
			
			Nodes lblnodes = (fulllbl != null) 
				? view.getDomain().parseXml("Html", view, fulllbl)
				: new Nodes(new LiteralText(label + ":"));

			String ph = xel.getRawAttribute("Placeholder");
			String ro = "True".equals(xel.getRawAttribute("ReadOnly")) ? "true" : null;			
			String url = xel.getAttribute("Url", "/rpc/upload");		// TODO figure out default
			
			children.add(new Label(new Attributes("id", lblid, "for", inputid), lblnodes));
			children.add(new Input(new Attributes("type", "file", "id", inputid, "name", inputid, "data-mini", "true", 
					"placeholder", ph, "readonly", ro, "data-url", url, "multiple", "multiple"), attrs));
		}

        this.myArguments = new Object[] { new Attributes("data-role", "fieldcontain"), children };
		
		nodes.add(this);
	}

}
