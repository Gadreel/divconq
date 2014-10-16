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
package divconq.script;

import java.util.HashMap;
import java.util.Map;

import divconq.lang.OperationResult;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class Script {
	protected XElement xml = null;
    protected ActivityManager manager = null;
	protected Map<String,Instruction> functions = new HashMap<String,Instruction>();
	protected Instruction main = null;

    public Script(ActivityManager man) {
    	this.manager = man;
    }

    public XElement getXml() {
        return this.xml; 
    }

    public Instruction getMain() {
    	return this.main;
    }

    public Instruction getFunction(String name) {
    	return this.functions.get(name);
    }
    
	public String getTitle() {
		if (this.xml == null)
			return null;
		
		XElement sc = this.xml.find("Script");
		
		return (sc != null) ? sc.getAttribute("Title") : "[Untitled]"; 
	}

    public OperationResult compile(XElement doc) {
        this.xml = doc;
        this.main = null;
        this.functions.clear();
        
        OperationResult log = new OperationResult();
        
        if (doc == null) {
        	log.error(1, "No script document provided, cannot compile.");		// TODO codes
        	return log;
        }
        
        for (XElement func : doc.selectAll("Function")) {
        	String fname = func.getAttribute("Name");
        	
        	if (StringUtil.isEmpty(fname))
        		continue;
        	
	        Instruction ni = this.manager.createInstruction(func);
	        ni.setXml(func);
	        ni.compile(this.manager, log);
	        
	        this.functions.put(fname, ni);
        }
        
        XElement node = doc.find("Main");

        if (node == null) {
        	log.errorTr(506);
        }
        else {
	        Instruction ni = this.manager.createInstruction(node);
	        ni.setXml(node);
	        ni.compile(this.manager, log);
	
	        this.main = ni; 
        }
        
        return log;
    }
}
