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
package w3.html;


import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.ICodeTag;
import divconq.view.LiteralText;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.view.html.UnescapedText;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

public class Title extends Element implements ICodeTag {
    protected Node title = null;

    public Title() {
    	super();    	
    }
    
    public Title(String title) {
    	super();
        this.title = new LiteralText(title);
    }

    public Title(UnescapedText title) {
    	super();
        this.title = title;
    }

    public Title(LiteralText title) {
    	super();
        this.title = title;
    }

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	Title nn = (Title)n;
    	nn.title = this.title.deepCopy(null);  // no parent yet
    }
    
	@Override
	public Node deepCopy(Element parent) {
		Title cp = new Title();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}

    @Override
    public void build(Object... args) {
        super.build("title", this.title);
    }
}
