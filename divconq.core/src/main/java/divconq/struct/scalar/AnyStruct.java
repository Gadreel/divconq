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

public class AnyStruct extends ScalarStruct {
	protected Object value = null;

	@Override
	public DataType getType() {
		if (this.explicitType != null)
			return super.getType();
		
		return OperationContext.get().getSchema().getType("Any");
	}

	public AnyStruct() {		
	}

	public AnyStruct(Object v) {
		this.adaptValue(v);
	}
	
	@Override
	public Object getGenericValue() {
		return this.value;
	}
	
	@Override
	public void adaptValue(Object v) {
		this.value = v;
	}

	public Object getValue() {
		return this.value; 
	}
	
	public void setValue(Object v) { 
		this.value = v; 
	}

    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	AnyStruct nn = (AnyStruct)n;
    	nn.value = this.value;		// TODO clone?
    }
    
	@Override
	public Struct deepCopy() {
		AnyStruct cp = new AnyStruct();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public boolean equals(Object obj) {
		return (AnyStruct.comparison(this, obj) == 0);
	}

	@Override
	public int compare(Object y) {
		return AnyStruct.comparison(this, y);
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
	public int hashCode() {
		return (this.value == null) ? 0 : this.value.hashCode();
	}
	
	@Override
	public String toString() {
		return (this.value == null) ? "null" : this.value.toString();
	}

	@Override
	public Object toInternalValue(RootType t) {
		return this.value;
	}
	
	public static int comparison(Object x, Object y) {
		// TODO convert to "inner value"
		//x = Struct.objectToStruct(x);
		//y = Struct.objectToStruct(y);
		
		if ((y == null) && (x == null))
			return 0;

		if (y == null)
			return 1;

		if (x == null)
			return -1;

		return 0;   // TODO compare...
	}
	
	@Override
	public boolean checkLogic(StackEntry stack, XElement source) {
		return Struct.objectToBooleanOrFalse(this.value);
	}
}
