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

import divconq.lang.FuncResult;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.scalar.IntegerStruct;
import divconq.util.StringUtil;
import divconq.work.TaskRun;

public class StackFunctionEntry extends StackBlockEntry {
	// result/state about the last command executed
    protected Struct lastResult = null;		
    protected IntegerStruct lastCode = null;		
    
    // parameter into function, make sure this is not disposed directly as the caller
    // does not want the param disposed
    protected Struct param = null;
    protected String pname = null;

    public StackFunctionEntry(Activity act, StackEntry parent, Instruction inst) {
		super(act, parent, inst);
		
        this.lastCode = new IntegerStruct();
        this.lastCode.setValue((long) 0);
    }

    @Override
    public Struct getLastResult() {
        return this.lastResult; 
    }
    
    @Override
    public void setLastResult(Struct v) {
		this.lastResult = v;

		// if this is the Main function then the last result is also the task result
        if ((this.parent == null) && (this.activity != null)) {
        	TaskRun run = this.activity.getTaskRun();
        	
        	if (run != null)
        		run.setResult(v);
        }
    }
    
    @Override
    public Long getLastCode() {
        return this.lastCode.getValue(); 
    }
    
    @Override
    public void setLastCode(Long v) {
    	// won't overwrite the existing code with 1
    	if ((v != null) && (v == 1) && (this.lastCode.getValue() > 1))
    		return;
    	
        this.lastCode.setValue(v); 
    }
    
    public Struct getParameter() {
        return this.param; 
    }
    
    public void setParameter(Struct v) {
        this.param = v; 
    }
    
    public String getParameterName() {
        return this.pname; 
    }
    
    public void setParameterName(String v) {
        this.pname = v; 
    }

	@Override
    public void collectDebugRecord(RecordStruct rec) {
		super.collectDebugRecord(rec);
		
    	RecordStruct dumpVariables = rec.getFieldAsRecord("Variables");
		
        if (StringUtil.isNotEmpty(this.pname)) 
        	dumpVariables.setField(this.pname, (this.param != null) ? this.param : null);
        else
        	dumpVariables.setField("_Param", (this.param != null) ? this.param : null);
        
        dumpVariables.setField("_LastResult", this.lastResult);        
        dumpVariables.setField("_LastCode", this.lastCode);        
    }	

	@Override
    public Struct queryVariable(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		//if (name.equals(this.pname) || "_Param".equals(this.pname))
		//	return this.param;
		
        if ("_LastResult".equals(name) || "_".equals(name)) 
        	return this.lastResult;
        
        if ("_LastCode".equals(name) || "__".equals(name)) 
        	return this.lastCode;
        
        // needed to copy all of StackBlock here, except remove the query for parent vars - replace with check for global vars

        // do not call super - that would expose vars outside of the function
        int dotpos = name.indexOf(".");

        if (dotpos > -1) {
            String oname = name.substring(0, dotpos);

            Struct ov = null;
            
    		if (oname.equals(this.pname) || oname.equals("_Param"))
    			ov = this.param;
    		
    		if (ov == null)
    			ov = this.variables.containsKey(oname) ? this.variables.get(oname) : null;

            // support global variables
            if (ov == null) 
            	ov = this.activity.queryVariable(oname);

            if (ov == null) {
            	this.log().errorTr(515, oname);
            	return null;
            }
            
            if (!(ov instanceof CompositeStruct)){
            	this.log().errorTr(516, oname);
            	return null;
            }
            
            FuncResult<Struct> sres = ((CompositeStruct)ov).select(name.substring(dotpos + 1)); 

            this.log().copyMessages(sres);
            
            return sres.getResult();
        }
        else if (this.variables.containsKey(name)) {
            return this.variables.get(name);
        }
        
        // if nothing else, try globals
        return this.activity.queryVariable(name);
    }
	
	@Override
	public StackFunctionEntry queryFunctionStack() {
		return this;
	}
}
