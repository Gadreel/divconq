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
import divconq.lang.op.OperationContext;
import divconq.schema.DataType;
import divconq.schema.RootType;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.util.FileUtil;
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
	public boolean isNull() {
		return (this.value == null);
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		// we are loose on the idea of null/zero.  operations always perform on 0, except Validate
		if ((this.value == null) && !"Validate".equals(code.getName()))
			this.value = 0L;
		
		if ("Inc".equals(code.getName())) {
			this.value++;
			stack.resume();
			return;
		}
		else if ("Dec".equals(code.getName())) {
			this.value--;
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
			
			Long it = Struct.objectToInteger(sref);
			
			try { 
				this.value += it;
			}
			catch (Exception x) {
				OperationContext.get().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Subtract".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			try {
				this.value -= Struct.objectToInteger(sref);			
			}
			catch (Exception x) {
				OperationContext.get().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Multiply".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			try {
				this.value *= Struct.objectToInteger(sref);			
			}
			catch (Exception x) {
				OperationContext.get().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Divide".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			try {
				this.value /= Struct.objectToInteger(sref);			
			}
			catch (Exception x) {
				OperationContext.get().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Min".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			try {
				this.value = Math.min(this.value, Struct.objectToInteger(sref));			
			}
			catch (Exception x) {
				OperationContext.get().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Max".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			try {
				this.value = Math.max(this.value, Struct.objectToInteger(sref));			
			}
			catch (Exception x) {
				OperationContext.get().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Abs".equals(code.getName())) {
			this.value = Math.abs(this.value);			
			
			stack.resume();
			return;
		}
		else if ("Random".equals(code.getName())) {
			long from = 1;
			long to = 100;
			
			try {
				if (code.hasAttribute("From")) 
						from = Struct.objectToInteger(stack.refFromElement(code, "From"));
				
				if (code.hasAttribute("To")) 
						to = Struct.objectToInteger(stack.refFromElement(code, "To"));
				
				this.value = FileUtil.testrnd.nextInt((int) (to - from)) + from;			
			}
			catch (Exception x) {
				OperationContext.get().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}

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
		return Struct.objectToBooleanOrFalse(this.value);
	}
}
