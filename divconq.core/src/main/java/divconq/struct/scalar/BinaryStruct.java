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

import divconq.lang.Memory;
import divconq.schema.RootType;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

public class BinaryStruct extends ScalarStruct {
	protected Memory value = null;

	public BinaryStruct() {		
	}

	public BinaryStruct(Object v) {
		this.adaptValue(v);
	}

	@Override
	public Object getGenericValue() {
		return this.value;
	}
	
	@Override
	public void adaptValue(Object v) {
		this.value = Struct.objectToBinary(v);
	}

	public Memory getValue() {
		return this.value; 
	}
	
	public void setValue(Memory v) { 
		this.value = v; 
	}
	
	@Override
	public boolean isEmpty() {
		return (this.value == null) || (this.value.getLength() == 0);
	}
	
	@Override
	public boolean isNull() {
		return (this.value == null);
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		String op = code.getName();
		
		// we are loose on the idea of null/zero.  operations always perform on now, except Validate
		if ((this.value == null) && !"Validate".equals(op))
			this.value = new Memory();
		
		if ("Position".equals(op)) {
			this.value.setPosition((int) stack.intFromElement(code, "At", 0));
			stack.resume();
			return;
		}
		else if ("Capacity".equals(op)) {
			this.value.setCapacity((int) stack.intFromElement(code, "At", 0));
			stack.resume();
			return;
		}
		else if ("Length".equals(op)) {
			this.value.setLength((int) stack.intFromElement(code, "At", 0));
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
		
		// TODO support more
		
		super.operation(stack, code);
	}

    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	BinaryStruct nn = (BinaryStruct)n;
    	nn.value = this.value;		// TODO copy
    }
    
	@Override
	public Struct deepCopy() {
		BinaryStruct cp = new BinaryStruct();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public boolean equals(Object obj) {
		return (BinaryStruct.comparison(this, obj) == 0);
	}

	@Override
	public int compare(Object y) {
		return BinaryStruct.comparison(this, y);
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
		Memory xv = Struct.objectToBinary(x);
		Memory yv = Struct.objectToBinary(y);
		
		if ((yv == null) && (xv == null))
			return 0;

		if (yv == null)
			return 1;

		if (xv == null)
			return -1;

		// TODO return xv.compareTo(yv);

		return 0;
	}
	
	@Override
	public boolean checkLogic(StackEntry stack, XElement source) {
		return Struct.objectToBooleanOrFalse(this.value);
	}
}
