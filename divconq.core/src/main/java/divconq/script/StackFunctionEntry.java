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

import org.joda.time.DateTime;

import divconq.lang.FuncResult;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.scalar.BooleanStruct;
import divconq.struct.scalar.DateTimeStruct;
import divconq.struct.scalar.IntegerStruct;
import divconq.util.StringUtil;

public class StackFunctionEntry extends StackBlockEntry {
	// result/state about the last command executed
    protected Struct lastResult = null;		
    protected IntegerStruct lastCode = null;		
    
    // parameter into function
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
    }
    
    @Override
    public Long getLastCode() {
        return this.lastCode.getValue(); 
    }
    
    @Override
    public void setLastCode(Long v) {
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
		
        if (StringUtil.isNotEmpty(this.pname)) {
        	RecordStruct dumpVariables = rec.getFieldAsRecord("Variables");
        	dumpVariables.setField(this.pname, (this.param != null) ? this.param : null);
        }
    }	

	@Override
    public Struct queryVariable(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		if (name.equals(this.pname))
			return this.param;
		
        if ("_LastResult".equals(name) || "_".equals(name)) 
        	return this.lastResult;
        
        if ("_LastCode".equals(name) || "__".equals(name)) 
        	return this.lastCode;
        
        if ("_Errored".equals(name)) 
        	return new BooleanStruct(this.activity.getLog().hasErrors());
        
        if ("_FirstCode".equals(name)) 
        	return new IntegerStruct(this.activity.getLog().getCode());
        
        if ("_Log".equals(name)) 
        	return this.activity.getLog().getMessages();

        if ("_Now".equals(name))
        	return new DateTimeStruct(new DateTime());
        
        // needed to copy all of StackBlock here, except remove the query for parent vars - replace with check for global vars

        // do not call super - that would expose vars outside of the function
        int dotpos = name.indexOf(".");

        if (dotpos > -1) {
            String oname = name.substring(0, dotpos);

            Struct ov = null;
            
    		if (oname.equals(this.pname))
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
	
	@Override
	public void dispose() {
		super.dispose();
		
		if (this.lastResult != null) 
			this.lastResult.dispose();
		
		this.lastResult = null;
	}
}
