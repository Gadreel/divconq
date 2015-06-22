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
import divconq.xml.XElement;


public class LiteralText extends Node implements ICodeTag {
    protected String value = "";

    public LiteralText() {
    	super();
    }
    
    public LiteralText(String value) {
    	super();
        this.value = value;
    }

	@Override
	public void parseElement(WebContext ctx, Nodes nodes, XElement xel) {
		this.value = xel.getText();
		
		nodes.add(this);
	}
	
    @Override
    public void doBuild(WebContext ctx) {
       	this.value = this.expandMacro(ctx, this.value);        
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
