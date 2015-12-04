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

import divconq.util.StringUtil;
import divconq.web.WebContext;

public class FormattedText extends Node {
    protected String value = "";
    protected Object[] values = null;

    public FormattedText() {
    	super();
    }
    
    public FormattedText(String value, Object... values) {
    	super();
        this.value = value;
        this.values = values;
    }

    @Override
    public void doBuild(WebContext ctx) {
       	this.value = this.expandMacro(ctx, this.value);        
        // TODO - this.value = ctx.format(this.value, this.values);
    }

    @Override
    public void stream(WebContext ctx, PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        this.print(ctx, strm, firstchild ? indent : "", false, this.value);
    }
    
    @Override
    public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
		if (StringUtil.isNotEmpty(this.value)) {
			if (!first)
				buffer.println(",");
			
			buffer.print(tabs + "'");
			Node.writeDynamicJsString(buffer, this.value);
			buffer.print("'");
			
			return true;
		}
		
		return false;
    }
    
}
