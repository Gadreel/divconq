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

		/*
			switch (operation)
			{
				case "Copy":
					Value = iv.Value;
					break;
				default:
					throw new ArgumentException();
			}
		*/

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
		boolean isok = true;
		boolean condFound = false;
		
		if (isok && source.hasAttribute("IsNull")) {
			isok = stack.boolFromElement(source, "IsNull") ? (this.value == null) : (this.value != null);
            condFound = true;
        }
		
		if (isok && source.hasAttribute("IsEmpty")) {
			isok = stack.boolFromElement(source, "IsEmpty") ? this.isEmpty() : !this.isEmpty();
            condFound = true;
        }
		
		if (!condFound) 
			isok = false;			
		
		return isok;
	}
}
