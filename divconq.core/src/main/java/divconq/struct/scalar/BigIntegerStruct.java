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
	
	@Override
	public boolean isNull() {
		return (this.value == null);
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		// we are loose on the idea of null/zero.  operations always perform on 0, except Validate
		if ((this.value == null) && !"Validate".equals(code.getName()))
			this.value = BigInteger.ZERO;
		
		if ("Inc".equals(code.getName())) {
			this.value = this.value.add(BigInteger.ONE);
			stack.resume();
			return;
		}
		else if ("Dec".equals(code.getName())) {
			this.value = this.value.subtract(BigInteger.ONE);
			stack.resume();
			return;
		}
		else if ("Set".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());
			
			this.adaptValue(sref);			
			
			stack.resume();
			return;
		}
		else if ("Add".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			try {
				this.value = this.value.add(Struct.objectToBigInteger(sref));			
			}
			catch (Exception x) {
				stack.log().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Subtract".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			try {
				this.value = this.value.subtract(Struct.objectToBigInteger(sref));			
			}
			catch (Exception x) {
				stack.log().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Multiply".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			try {
				this.value = this.value.multiply(Struct.objectToBigInteger(sref));			
			}
			catch (Exception x) {
				stack.log().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Divide".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			try {
				this.value = this.value.divide(Struct.objectToBigInteger(sref));			
			}
			catch (Exception x) {
				stack.log().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Min".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			try {
				this.value = this.value.min(Struct.objectToBigInteger(sref));			
			}
			catch (Exception x) {
				stack.log().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Max".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());
		
			try {
				this.value = this.value.max(Struct.objectToBigInteger(sref));			
			}
			catch (Exception x) {
				stack.log().error("Error doing " + code.getName() + ": " + x);
			}
				
			stack.resume();
			return;
		}
		else if ("Abs".equals(code.getName())) {
			this.value = this.value.abs();			
			
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}

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
		return Struct.objectToBooleanOrFalse(this.value);
	}
}
