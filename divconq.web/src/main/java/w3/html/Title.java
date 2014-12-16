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


import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.LiteralText;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.UnescapedText;
import divconq.web.dcui.ViewOutputAdapter;
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
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, view.getDomain().parseXml(view, xel) };
		
		nodes.add(this);
	}

    @Override
    public void build(Object... args) {
        super.build("title", this.title);
    }
}
