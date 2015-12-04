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
    
    public String get(String name) {
    	for (int i = 0; i < this.args.length; i += 2) {
    		if (name.equals(this.args[i]) && (i + 1 < this.args.length))
    			return this.args[i + 1];
    	}
    	
    	return null;
    }
    
    public boolean update(String name, String value) {
    	for (int i = 0; i < this.args.length; i += 2) {
    		if (name.equals(this.args[i]) && (i + 1 < this.args.length)) {		// TODO add length if needed
    			this.args[i + 1] = value;
    			return true;
    		}
    	}
    	
    	return false;
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
}
