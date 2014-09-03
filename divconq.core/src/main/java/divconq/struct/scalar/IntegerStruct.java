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

import divconq.hub.Hub;
import divconq.schema.DataType;
import divconq.schema.RootType;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

public class IntegerStruct extends ScalarStruct {
	protected Long value = null;

	@Override
	public DataType getType() {
		if (this.explicitType != null)
			return super.getType();
		
		return Hub.instance.getSchema().getType("Integer");
	}
	
	public IntegerStruct() {		
	}

	public IntegerStruct(Object v) {
		this.adaptValue(v);
	}

	@Override
	public Object getGenericValue() {
		return this.value;
	}
	
	@Override
	public void adaptValue(Object v) {
		this.value = Struct.objectToInteger(v);
	}

	public Long getValue() {
		return this.value; 
	}
	
	public void setValue(Long v) { 
		this.value = v; 
	}
	
	@Override
	public boolean isEmpty() {
		return (this.value == null);
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		if ("Inc".equals(code.getName())) {
			this.value = (this.value == null) ? 1 : this.value + 1;
			stack.resume();
			return;
		}
		else if ("Dec".equals(code.getName())) {
			this.value = (this.value == null) ? -1 : this.value - 1;
			stack.resume();
			return;
		}
		else if ("Set".equals(code.getName())) {
			Struct sref = stack.refFromElement(code, "Value"); 
			this.adaptValue(sref);
			stack.resume();
			return;
		}
		else if ("Add".equals(code.getName())) {
			long me = (this.value == null) ? 0 : this.value;
			
			Struct sref = stack.refFromElement(code, "Value");
			
			Long it = Struct.objectToInteger(sref);
			
			if (it != null) 
				this.value = me + it;
			
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}

		/*
		 * TODO add more
		 * 
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
			case "Random":
				int from = 1;
				int to = 100;

				if (source.HasA("From")) from = Convert.ToInt32(proc.ResolveValueToString(inst, source.A("From")));
				if (source.HasA("To")) to = Convert.ToInt32(proc.ResolveValueToString(inst, source.A("To")));

				Value = new Random().Next(to - from + 1) + from;
				break;
			default:
				throw new IllegalArgumentException("Unknown operation: " + operation);
		}
		*/

    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	IntegerStruct nn = (IntegerStruct)n;
    	nn.value = this.value;
    }
    
	@Override
	public Struct deepCopy() {
		IntegerStruct cp = new IntegerStruct();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public boolean equals(Object obj) {
		return (IntegerStruct.comparison(this, obj) == 0);
	}

	@Override
	public int compare(Object y) {
		return IntegerStruct.comparison(this, y);
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
		Long xv = Struct.objectToInteger(x);
		Long yv = Struct.objectToInteger(y);

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
