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
package divconq.view;

import divconq.util.ArrayUtil;

public class Attributes {
    protected String[] args = null;
    protected int pos = 0;

    public Attributes(String... args) {
        this.args = args;
    }
    
    public void add(String name, String value) {
    	if ((args == null) || (args.length == 0)) 
    		this.args = new String[] { name, value };
    	else 
    		this.args = (String[]) ArrayUtil.addAll(this.args, name, value);
    }
    
    public void addAll(String... attrs) {
    	if ((args == null) || (args.length == 0)) 
    		this.args = attrs;
    	else 
    		this.args = (String[]) ArrayUtil.addAll(this.args, attrs);
    }

    public boolean hasMore() {
    	return (this.pos < this.args.length);
    }
    
    public String pop() {
        if (this.pos >= this.args.length)
        	return null;
        
        String value = this.args[this.pos];
        
        if (value == null) 
        	value = "";  // default
        
        this.pos++;

        return value;
    }

	public Attributes deepCopy() {
		Attributes attrs = new Attributes();
   		attrs.args = this.args;    	
    	return attrs;
	}
}
