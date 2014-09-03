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
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

public class Head extends Element implements ICodeTag {
    public Head() {
    	super();
	}
    
    public Head(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Head cp = new Head();
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
	public void doBuild() {
	    this.build(this.myArguments);
	    // TODO --- DocContext.getContext().addBeforeWriteListener(this.buildCSS);
	}
	
    @Override
	public void build(Object... args) {
    	//this.getContext().get
    	
	    super.build("head", true, args);
	}
	
    /* TODO
	public Style CreateStyle(LibraryStyle stl)
	{
	    string code = stl.Code;
	
	    MatchCollection mc = Regex.Matches(code, "@\\w+@", RegexOptions.Multiline);
	
	    foreach (Match m in mc)
	    {
	        string ralias = m.Value.Substring(1, m.Value.Length - 2);
	
	        ResourceItem ri = Root.Context.RADApplication.LookupThemeResource(Root.Context, ralias);
	
	        if (ri != null)
	        {
	            code = code.Replace(m.Value, ri.ExternalPath);
	        }
	        else
	        {
	            ri = Root.Context.RADApplication.LookupResource(Root.Context, ralias);
	
	            if (ri != null) code = code.Replace(m.Value, ri.ExternalPath);
	        }
	    }
	
	    return new STYLE(new FormattedText(code));
	}
	
	public void BuildCSS()
	{
	    // %%% support media type
	
	    foreach (LibraryStyle stl in Root.Context.RADApplication.LookupLayoutCSS(Root.Context))
	    {
	        base.Build(CreateSTYLE(stl));
	    }
	
	    foreach (LibraryStyle stl in Root.Context.RADApplication.LookupThemeCSS(Root.Context))
	    {
	        base.Build(CreateSTYLE(stl));
	    }
	
	    // %%% load css blocks from page context as well ???
	
	    foreach (SCRIPT script in Root.Context.PageContext.Scripts)
	    {
	        base.Build(script);
	    }
	}
	*/
}
