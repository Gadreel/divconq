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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import w3.html.Button;
import w3.html.Div;
import w3.html.FieldSet;
import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

public class FormButtons extends Div {
	protected String saveid = null;
	protected Map<String, String> funcs = new HashMap<String, String>();
	
    public FormButtons() {
    	super();
	}
    
    public FormButtons(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		FormButtons cp = new FormButtons();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	FormButtons nn = (FormButtons)n;
    	nn.saveid = this.saveid;
    	nn.funcs = this.funcs;
    }

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));
		
		Nodes bnodes = new Nodes();
		List<XElement> btns = xel.selectAll("jqmFormButton");
		
		int bcnt = btns.size();
		
		if ("True".equals(xel.getAttribute("Save"))) 
			bcnt++;
		
		String gclass = "ui-grid-a";
		
		if (bcnt == 3)
			gclass = "ui-grid-b";
		else if (bcnt == 3)
			gclass = "ui-grid-c";

		attrs.add("class", "ui-body ui-body-c");
		
		String bclass =  "ui-block-a";
		
		for (int i = 0; i < btns.size(); i++) {
			XElement btn = btns.get(i);
			String bid = "btn" + UUID.randomUUID().toString();
			String func = btn.getAttribute("OnClick");
			
			this.funcs.put(bid, func);
			
			bnodes.add(new Div(new Attributes("Class", bclass), new Button(new Attributes("id", bid, "type", "button", "data-theme", btn.getAttribute("Theme", "d"), "data-mini","true"), btn.getAttribute("Title"))));			
			
			if (i == 0)
				bclass =  "ui-block-b";
			else if (i == 1)
				bclass =  "ui-block-c";
			else if (i == 2)
				bclass =  "ui-block-d";
		}
		
		if ("True".equals(xel.getAttribute("Save"))) {
			this.saveid = "btn" + UUID.randomUUID().toString();
			bnodes.add(new Div(new Attributes("Class", bclass), new Button(new Attributes("id", this.saveid, "type", "submit", "data-theme","b", "data-mini","true"), "Save"))); 
		}
		
		Nodes children = new Nodes(new FieldSet(new Attributes("class", gclass), bnodes));
		
        this.myArguments = new Object[] { attrs, children };
		
		nodes.add(this);
	}	
	
	@Override
	public void build(Object... args) {
		Form frm = Form.findForm(this);
		
		// TODO enhance for more buttons and proper grid placement blocks
		if (frm != null) {
			frm.addSaveButton(this.saveid);
			frm.setFormButtons(this.funcs);
		}
		
		super.build(args);
	}
}
