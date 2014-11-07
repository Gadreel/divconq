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

import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.Struct;
import divconq.struct.RecordStruct;
import divconq.struct.scalar.NullStruct;
import divconq.struct.scalar.StringStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class StackEntry {
	protected ExecuteState state = ExecuteState.Ready;
	protected StackEntry parent = null;
	protected Activity activity = null;
	protected Instruction inst = null;
	protected RecordStruct inststore = new RecordStruct();
	protected IInstructionCallback callback = null;
	
	public StackEntry(Activity act, StackEntry parent, Instruction inst) {
		this.activity = act;
		this.parent = parent;
		this.inst = inst;
	}
	
    public StackEntry getParent() {
    	return this.parent;
    }
    
    public void setParent(StackEntry v) {
    	this.parent = v;	
    }
    
	public RecordStruct getStore() {
		return this.inststore;
	}
	
	public Instruction getInstruction() {
		return this.inst;
	}
	
	public void setInstruction(Instruction v) {
		this.inst = v;
	}
	
    public ExecuteState getState() {
        return (this.state != null) ? this.state : ExecuteState.Done;
    }
    
    public void setState(ExecuteState v) {
    	this.state = v;
    }

	public void updateCallback(final IInstructionCallback cb) {
		final IInstructionCallback old = this.callback;
		
		//System.out.println("Updating callback on " + this.inst.source.getName() + " - thread " + Thread.currentThread().getName());
		
		this.callback = new IInstructionCallback() {			
			@Override
			public void resume() {
				cb.resume();
				old.resume();
			}
		};
	}	
    
    public void resume() {
    	if (this.callback != null) {
    		this.callback.resume();
    		return;
    	}
    	
    	if (this.parent != null) 
    		this.parent.resume();
    }

    public Struct queryVariable(String name) {
        return (this.parent != null) ? this.parent.queryVariable(name) : null;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public StackBlockEntry queryBlockStack() {
        if (this.parent == null)
            return null;

        return this.parent.queryBlockStack();
    }

    public StackFunctionEntry queryFunctionStack() {
        if (this.parent == null)
            return null;

        return this.parent.queryFunctionStack();
    }
    
    public Struct resolveValue(String val) {
    	if (val == null)
    		return NullStruct.instance;
    	
    	val = this.resolveValueToString(val);
    	
		// var flag - return the reference to the variable pointed to
    	if (val.startsWith("$")) 
    		return this.queryVariable(val.substring(1));
    	
		// literal flag - return the string as is
    	if (val.startsWith("`")) 
			return new StringStruct(val.substring(1));
    	
    	// otherwise just treat this as a string
		return new StringStruct(val);
    }

	public void operate(Struct target, XElement source) {
		Hub.instance.getActivityManager().operate(this, target, source);
	}
    
    public Struct refFromSource(String attr) {
        return this.refFromElement(this.inst.source, attr);
    }
    
    public Struct refFromElement(XElement el, String attr) {
    	if ((el == null) || StringUtil.isEmpty(attr))
    		return NullStruct.instance;
    	
        return this.resolveValue(el.getAttribute(attr));
    }
    
    public String stringFromSource(String attr) {
        return this.stringFromElement(this.inst.source, attr, null);
    }
    
    public String stringFromSource(String attr, String def) {
        return this.stringFromElement(this.inst.source, attr, def);
    }
    
    public String stringFromElement(XElement el, String attr) {
        return this.stringFromElement(el, attr, null);
    }
    
    public String stringFromElement(XElement el, String attr, String def) {
    	if ((el == null) || StringUtil.isEmpty(attr))
    		return def;
    	
        Struct dt = this.refFromElement(el, attr);
        
        if (dt == NullStruct.instance)
        	return def;
        
    	String ret = Struct.objectToString(dt);
    	
    	if (StringUtil.isNotEmpty(ret))
    		return ret;
        
        return def;
    }
    
    public long intFromSource(String attr) {
        return this.intFromElement(this.inst.source, attr, 0);
    }
    
    public long intFromSource(String attr, int def) {
        return this.intFromElement(this.inst.source, attr, def);
    }
    
    public long intFromElement(XElement el, String attr) {
        return this.intFromElement(el, attr, 0);
    }
    
    public long intFromElement(XElement el, String attr, int def) {
    	if ((el == null) || StringUtil.isEmpty(attr))
    		return def;
    	
        Struct dt = this.refFromElement(el, attr);
        
    	Long ret = Struct.objectToInteger(dt);
    	
    	if (ret != null)
    		return ret;
        
        return def;
    }
    
    public boolean boolFromSource(String attr) {
        return this.boolFromElement(this.inst.source, attr, false);
    }
    
    public boolean boolFromSource(String attr, boolean def) {
        return this.boolFromElement(this.inst.source, attr, def);
    }
    
    public boolean boolFromElement(XElement el, String attr) {
        return this.boolFromElement(el, attr, false);
    }
    
    public boolean boolFromElement(XElement el, String attr, boolean def) {
    	if ((el == null) || StringUtil.isEmpty(attr))
    		return def;
    	
        Struct dt = this.refFromElement(el, attr);
        
    	Boolean ret = Struct.objectToBoolean(dt);
    	
    	if (ret != null)
    		return ret;
        
        return def;
    }

    public String resolveValueToString(String val) {
    	if (val == null)
    		return "";
    	
        // the expansion of variables is per Attribute Value Templates in XSLT
        // http://www.w3.org/TR/xslt#attribute-value-templates

        StringBuilder sb = new StringBuilder();

        int lpos = 0;
        int bpos = val.indexOf("{$");

        while (bpos != -1) {
            int epos = val.indexOf("}", bpos);
            if (epos == -1) 
            	break;

            sb.append(val.substring(lpos, bpos));

            lpos = epos + 1;

            String varname = val.substring(bpos + 2, epos).trim();
            String[] vparts = varname.split("\\.");

            varname = "";

            for (String vpart : vparts) {
                if (varname.length() == 0) {
                    varname = vpart;
                }
                else {
                    if (vpart.startsWith("$")) {
                        Struct qvar = this.queryVariable(vpart.substring(1));

                        if (qvar != null) {
                            varname += "." + qvar.toString();
                            continue;
                        }
                    }

                    varname += "." + vpart;
                }
            }

            Struct qvar2 = this.queryVariable(varname);

            if (qvar2 != null) 
                sb.append(qvar2.toString());
            else {
            	OperationContext.get().warnTr(500, varname);
                sb.append(val.substring(bpos, epos + 1));
            }

            bpos = val.indexOf("{$", epos);
        }

        sb.append(val.substring(lpos));

        return sb.toString();
    }

    public void addVariable(String type, String name) {
    	this.addVariable(name, this.activity.createStruct(type));
    }

    public void addVariable(String name, Struct var) {
    	if (var == null) {
    		OperationContext.get().errorTr(512);
    		return;
    	}
    		
        StackBlockEntry b = this.queryBlockStack();
        
        if (b == null) 
        	OperationContext.get().errorTr(513, name);
        else
        	b.addVariable(name, var);
    }

    // Result Code and Value
    
    public void setLastCode(Long code) {
    	StackFunctionEntry func = this.queryFunctionStack();
    	
    	if (func != null) 
	        func.setLastCode(code);
    }
    
    public Long getLastCode() {
    	StackFunctionEntry func = this.queryFunctionStack();
    	
    	if (func != null) 
	        return func.getLastCode();
    	
    	return null;
    }

    public void setLastResult(Struct val) {
    	StackFunctionEntry func = this.queryFunctionStack();
    	
    	if (func != null) 
	        func.setLastResult(val);
    }

    public void setLastResult(long code, Struct val) {
    	StackFunctionEntry func = this.queryFunctionStack();
    	
    	if (func != null) {
	        func.setLastCode(code);
	        func.setLastResult(val);
    	}
    }
    
    public void setLastResult(OperationResult v) {
    	StackFunctionEntry func = this.queryFunctionStack();
    	
    	if (func != null) {
	        func.setLastCode(v.getCode());
	        func.setLastResult(v.toLogMessage());		// TODO really, a message?
    	}
    }
    
    public Struct getLastResult() {
    	StackFunctionEntry func = this.queryFunctionStack();
    	
    	if (func != null) 
	        return func.getLastResult();
    	
    	return null;
    }
    
    // Run
    
    public void run(IInstructionCallback cb) {
    	this.callback = cb;
    	this.run();
    }
    
    public void run() {
    	if (this.inst != null)
    		this.inst.run(this);
    }
    
    public void cancel() {
    	if (this.inst != null)
    		this.inst.cancel(this);
    }
    
    public void debugStack(ListStruct dumpList) {
    	RecordStruct dumpRec = new RecordStruct();
    	dumpList.addItem(dumpRec);
    	
    	this.collectDebugRecord(dumpRec);
    	RecordStruct subRec = this.inst.collectDebugRecord(this, dumpRec);

    	if (subRec != null)
    		dumpList.addItem(subRec);
    }
    
    public void collectDebugRecord(RecordStruct rec) {
    }

	public boolean codeHasAttribute(String attr) {
    	if (this.inst != null)
    		return this.inst.source.hasAttribute(attr);
    	
    	return false;
	}

	public StackEntry getExecutingStack() {
		return this;
	}
}
