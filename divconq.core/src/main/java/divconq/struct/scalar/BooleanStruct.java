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
package divconq.struct.scalar;

import divconq.lang.op.OperationContext;
import divconq.schema.DataType;
import divconq.schema.RootType;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

public class BooleanStruct extends ScalarStruct {
	protected Boolean value = null;

	@Override
	public DataType getType() {
		if (this.explicitType != null)
			return super.getType();
		
		return OperationContext.get().getSchema().getType("Boolean");
	}

	public BooleanStruct() {		
	}

	public BooleanStruct(Object v) {
		this.adaptValue(v);
	}

	@Override
	public Object getGenericValue() {
		return this.value;
	}
	
	@Override
	public void adaptValue(Object v) {
		this.value = Struct.objectToBoolean(v);
	}

	public Boolean getValue() {
		return this.value; 
	}
	
	public void setValue(Boolean v) { 
		this.value = v; 
	}
	
	@Override
	public boolean isEmpty() {
		return (this.value == null);
	}
	
	@Override
	public boolean isNull() {
		return (this.value == null);
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		String op = code.getName();
		
		// we are loose on the idea of null/zero.  operations always perform on false, except Validate
		if ((this.value == null) && !"Validate".equals(op))
			this.value = false;
		
		if ("Flip".equals(op)) {
			this.value = !this.value;
			stack.resume();
			return;
		}
		else if ("Set".equals(op)) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());
			
			this.adaptValue(sref);
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}

    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	BooleanStruct nn = (BooleanStruct)n;
    	nn.value = this.value;
    }
    
	@Override
	public Struct deepCopy() {
		BooleanStruct cp = new BooleanStruct();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public boolean equals(Object obj) {
		return (BooleanStruct.comparison(this, obj) == 0);
	}

	@Override
	public Object toInternalValue(RootType t) {
		return this.value;
	}

	@Override
	public int compare(Object y) {
		return BooleanStruct.comparison(this, y);
	}

	@Override
	public int hashCode() {
		return (this.value == null) ? 0 : this.value.hashCode();
	}
	
	@Override
	public String toString() {
		return (this.value == null) ? "null" : this.value.toString();
	}

	public static int comparison(Object x, Object y)
	{
		Boolean xv = Struct.objectToBoolean(x);
		Boolean yv = Struct.objectToBoolean(y);

		if ((yv == null) && (xv == null))
			return 0;

		if (yv == null)
			return 1;

		if (xv == null)
			return -1;

		return xv.compareTo(yv);
	}
	
	@Override
	public boolean checkLogic(StackEntry stack, XElement source) {
		return Struct.objectToBooleanOrFalse(this.value);
	}
}
