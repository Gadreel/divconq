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

import divconq.web.WebContext;
import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.ICodeTag;
import divconq.web.dcui.Nodes;
import divconq.xml.XElement;

public class Head extends Element implements ICodeTag {
    public Head() {
    	super();
	}
    
    public Head(Object... args) {
    	super(args);
	}

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);

        this.myArguments = new Object[] { attrs, ctx.getDomain().parseXml(ctx, xel) };
		
		nodes.add(this);
	}
	
    @Override
	public void build(WebContext ctx, Object... args) {
	    super.build(ctx, "head", true, args);
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
