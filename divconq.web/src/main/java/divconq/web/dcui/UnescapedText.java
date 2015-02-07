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
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	UnescapedText nn = (UnescapedText)n;
    	nn.value = this.value;
    	nn.cdata = this.cdata;
    }
    
	@Override
	public Node deepCopy(Element parent) {
		UnescapedText cp = new UnescapedText();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

    @Override
    public void doBuild() {
       	this.value = this.expandMacro(this.value);
    }

    @Override
    public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
    	String str = this.value;
    	
    	if (this.cdata)
    		str = "<![CDATA[" + str + "]]>";
    	else
    		str = Html.escapeHtml(str);
    	
        this.print(strm, firstchild ? indent : "", false, str);        
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
