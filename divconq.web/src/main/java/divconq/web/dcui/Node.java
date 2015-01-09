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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import divconq.util.StringUtil;
import divconq.web.WebContext;

abstract public class Node {
	// @[a-zA-Z0-9_\\-,:/]+@
	public static Pattern macropatten =  Pattern.compile("@\\S+?@", Pattern.MULTILINE);

	protected boolean blockindent = false;
    protected Fragment viewroot = null;
    protected Fragment partroot = null;
    protected Element parent = null;
	
	protected Map<String,String> valueparams = null;
	protected Map<String,Nodes> complexparams = null;

    public boolean getBlockIndent() {
        return this.blockindent; 
    }
    
    public void setBlockIndent(boolean value) {
    	this.blockindent = value; 
    }

    public Fragment getViewRoot() {
        return this.viewroot; 
    }

    public Fragment getPartRoot() {
        return (this.partroot != null) ? this.partroot : this.viewroot; 
    }

    public Element getParent() {
        return this.parent;
    }
    
    abstract public Node deepCopy(Element parent);
    
    protected void doCopy(Node n) {
    	n.blockindent = this.blockindent;
    	
    	// make copies
    	if (this.valueparams != null) {
    		n.valueparams = new HashMap<String, String>();

	    	for (String name : this.valueparams.keySet())
	    		n.valueparams.put(name, this.valueparams.get(name));
    	}
    	
    	if (this.complexparams != null) {
    		n.complexparams = new HashMap<String, Nodes>();

	    	for (String name : this.complexparams.keySet())
	    		n.complexparams.put(name, this.complexparams.get(name).deepCopy());
    	}
    }
    
    public void setParent(Element value) {
    	this.parent = value; 
    	
    	if (value != null) {
    		this.partroot = value.getPartRoot();
    		
    		// our view root is always the same as the parent's
	        this.viewroot = value.getViewRoot();
    	}
    }
    
    public void setPartRoot(Fragment part) {
    	this.partroot = part;
    }

    public void setParams(Map<String, String> params) {
    	this.valueparams = params;
    }

    public void addParams(String... params) {
    	if (this.valueparams == null)
    		this.valueparams = new HashMap<String, String>();
    	
		for (int i = 1; i < params.length; i += 2) 
			this.valueparams.put(params[i-1], params[i]);
    }
    
    public void putParam(String name, String value) {
    	if (this.valueparams == null)
    		this.valueparams = new HashMap<String, String>();
    	
    	this.valueparams.put(name, value);
    }
    
    public boolean hasParam(String name) {
    	if ((this.valueparams != null) && this.valueparams.containsKey(name))
    		return true;
    	
    	if (this.parent != null)
    		return this.parent.hasParam(name);
    	
    	return this.getContext().hasInternalParam(name);
    }
    
    public String getParam(String name) {
    	if ((this.valueparams != null) && this.valueparams.containsKey(name))
    		return this.valueparams.get(name);
    	
    	if (this.parent != null)
    		return this.parent.getParam(name);
    	
    	return this.getContext().getInternalParam(name);
    }

    public void setComplexParams(Map<String, Nodes> params) {
    	this.complexparams = params;
    }
    
    public void putParam(String name, Nodes list) {
    	if (this.complexparams == null)
    		this.complexparams = new HashMap<String, Nodes>();
    	
    	this.complexparams.put(name, list);
    }
    
    public Nodes getComplexParam(String name) {
    	if ((this.complexparams != null) && this.complexparams.containsKey(name))
    		return this.complexparams.get(name);
    	
    	if (this.parent != null)
    		return this.parent.getComplexParam(name);
    	
    	return null;
    }
    
    public WebContext getContext() {
    	if (this.viewroot != null)
    		return this.viewroot.getContext();
    	
    	return null;
    }
	  
	  public String expandMacro(String value) {
		  if (StringUtil.isEmpty(value))
			  return null;
		  
		  boolean checkmatches = true;
		  
		  while (checkmatches) {
			  checkmatches = false;
			  Matcher m = Node.macropatten.matcher(value);
			  
			  while (m.find()) {
				  String grp = m.group();
				  
				  String macro = grp.substring(1, grp.length() - 1);
				  String val = null;
				  
				  // params on this tree
				  if (macro.startsWith("val|"))
					  val = this.getParam(macro.substring(4));
				  else 
					  val = this.getContext().expandMacro(macro);
				  
				  // if any of these, then replace and check (expand) again 
				  if (val != null) {
					  value = value.replace(grp, val);
					  checkmatches = true;
				  }
			  }
		  }
		  
		  return value;
	  }

    public void stream(PrintStream strm) {
        this.stream(strm, "", false, false);
    }

    public abstract void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock);
    
    public abstract void doBuild();
    
    public void print(PrintStream strm, String indent, boolean newline, String copy, Object... args) {
        if (args.length > 0) 
            strm.print(this.getContext().format(indent + copy, args));
        else 
            strm.print(indent + copy);

        if (newline) 
        	strm.println();
	}

	public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
		return false;
    }
	
    static public void writeDynamicJsString(PrintStream buffer, String s) {
    	final int len = s.length();
    	
		for(int i = 0; i < len; i++) {
			char ch = s.charAt(i);
			
			switch(ch){
			case '\'':
				buffer.print("\\\'");
				break;
			case '\\':
				buffer.print("\\\\");
				break;
			case '\b':
				buffer.print("\\b");
				break;
			case '\f':
				buffer.print("\\f");
				break;
			case '\n':
				buffer.print("\\n");
				break;
			case '\r':
				buffer.print("\\r");
				break;
			case '\t':
				buffer.print("\\t");
				break;
			case '/':
				buffer.print("\\/");
				break;
			default:
                //Reference: http://www.unicode.org/versions/Unicode5.1.0/
				if((ch>='\u0000' && ch<='\u001F') || (ch>='\u007F' && ch<='\u009F') || (ch>='\u2000' && ch<='\u20FF')) {
					String ss = Integer.toHexString(ch);
					
					buffer.print("\\u");
					
					for(int k = 0; k < 4 - ss.length(); k++)
						buffer.print('0');
					
					buffer.print(ss.toUpperCase());
				}
				else 
					buffer.print(ch);
			}
		}//for
	}
}
