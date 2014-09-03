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

import java.math.BigInteger;

import divconq.schema.RootType;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

public class BigIntegerStruct extends ScalarStruct {
	protected BigInteger value = null;

	public BigIntegerStruct() {		
	}

	public BigIntegerStruct(Object v) {
		this.adaptValue(v);
	}

	@Override
	public Object getGenericValue() {
		return this.value;
	}
	
	@Override
	public void adaptValue(Object v) {
		this.value = Struct.objectToBigInteger(v);
	}

	public BigInteger getValue() {
		return this.value; 
	}
	
	public void setValue(BigInteger v) { 
		this.value = v; 
	}
	
	@Override
	public boolean isEmpty() {
		return (this.value == null);
	}


		/*
		switch (operation)
		{
			case "Inc":
				Value++;
				break;
			case "Dec":
				Value--;
				break;
			case "Copy":
				Value = iv.Value;
				break;
			case "Add":
				Value += iv.Value;
				break;
			case "Subtract":
				Value -= iv.Value;
				break;
			case "Multiply":
				Value *= iv.Value;
				break;
			case "Divide":
				Value /= iv.Value;
				break;
			case "Min":
				Value = (iv.Value < Value) ? iv.Value : Value;
				break;
			case "Max":
				Value = (iv.Value > Value) ? iv.Value : Value;
				break;
			case "Abs":
				Value = Math.Abs(iv.Value);
				break;
			default:
				throw new IllegalArgumentException("Unknown operation: " + operation);
		}
		*/

    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	BigIntegerStruct nn = (BigIntegerStruct)n;
    	nn.value = this.value;		
    }
    
	@Override
	public Struct deepCopy() {
		BigIntegerStruct cp = new BigIntegerStruct();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public boolean equals(Object obj) {
		return (BigIntegerStruct.comparison(this, obj) == 0);
	}

	@Override
	public int compare(Object y) {
		return BigIntegerStruct.comparison(this, y);
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

	public static int comparison(Object x, Object y)
	{
		BigInteger xv = Struct.objectToBigInteger(x);
		BigInteger yv = Struct.objectToBigInteger(y);

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
