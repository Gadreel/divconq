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
package divconq.web.dcui;

import java.io.PrintStream;

import w3.html.Html;
import divconq.util.StringUtil;
import divconq.web.WebContext;

public class UnescapedText extends Node {
    protected String value = "";
    protected boolean cdata = false;

    public UnescapedText() {
    	super();
    }
    
    public UnescapedText(boolean cdata, String value) {
    	super();
    	this.cdata = cdata;
        this.value = value;
    }

    @Override
    public void doBuild(WebContext ctx) {
       	this.value = this.expandMacro(ctx, this.value);
    }

    @Override
    public void stream(WebContext ctx, PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
    	String str = this.value;
    	
    	if (this.cdata)
    		str = "<![CDATA[" + str + "]]>";
    	else
    		str = Html.escapeHtml(str);
    	
        this.print(ctx, strm, firstchild ? indent : "", false, str);        
    }
    
    @Override
    public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
		if (StringUtil.isNotEmpty(this.value)) {
			if (!first)
				buffer.println(",");
			
			buffer.print(tabs + "'");
			Node.writeDynamicJsString(buffer, Html.escapeHtml(this.value));
			buffer.print("'");
			
			return true;
		}
		
		return false;
    }
}
