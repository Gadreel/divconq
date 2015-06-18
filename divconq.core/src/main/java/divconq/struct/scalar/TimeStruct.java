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

import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

import divconq.lang.op.OperationContext;
import divconq.schema.DataType;
import divconq.schema.RootType;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

public class TimeStruct extends ScalarStruct {
	protected LocalTime value = null;

	@Override
	public DataType getType() {
		if (this.explicitType != null)
			return super.getType();
		
		return OperationContext.get().getSchema().getType("LocalTime");
	}

	public TimeStruct() {		
	}

	public TimeStruct(Object v) {
		this.adaptValue(v);
	}

	@Override
	public Object getGenericValue() {
		return this.value;
	}
	
	@Override
	public void adaptValue(Object v) {
		this.value = Struct.objectToTime(v);
	}

	public LocalTime getValue() {
		return this.value; 
	}
	
	public void setValue(LocalTime v) { 
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
		
		// we are loose on the idea of null/zero.  operations always perform on now, except Validate
		if ((this.value == null) && !"Validate".equals(op))
			this.value = new LocalTime();
		
		if ("Inc".equals(op)) {
			this.value = this.value.plusSeconds(1);
			stack.resume();
			return;
		}
		else if ("Dec".equals(op)) {
			this.value = this.value.minusSeconds(1);
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
		else if ("Add".equals(op)) {
			try { 
				if (code.hasAttribute("Seconds")) 
					this.value = this.value.plusSeconds((int)stack.intFromElement(code, "Seconds"));
				else if (code.hasAttribute("Minutes")) 
					this.value = this.value.plusMinutes((int)stack.intFromElement(code, "Minutes"));
				else if (code.hasAttribute("Hours")) 
					this.value = this.value.plusHours((int)stack.intFromElement(code, "Hours"));
				else if (code.hasAttribute("Millis")) 
					this.value = this.value.plusMillis((int)stack.intFromElement(code, "Millis"));
				else if (code.hasAttribute("Period")) {
					Period p = ISOPeriodFormat.standard().parsePeriod(stack.stringFromElement(code, "Period"));
					this.value = this.value.plus(p);
				}
			}
			catch (Exception x) {
				OperationContext.get().error("Error doing " + op + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Subtract".equals(op)) {
			try { 
				if (code.hasAttribute("Seconds")) 
					this.value = this.value.minusSeconds((int)stack.intFromElement(code, "Seconds"));
				else if (code.hasAttribute("Minutes")) 
					this.value = this.value.minusMinutes((int)stack.intFromElement(code, "Minutes"));
				else if (code.hasAttribute("Hours")) 
					this.value = this.value.minusHours((int)stack.intFromElement(code, "Hours"));
				else if (code.hasAttribute("Millis")) 
					this.value = this.value.minusMillis((int)stack.intFromElement(code, "Millis"));
				else if (code.hasAttribute("Period")) {
					Period p = ISOPeriodFormat.standard().parsePeriod(stack.stringFromElement(code, "Period"));
					this.value = this.value.minus(p);
				}
			}
			catch (Exception x) {
				OperationContext.get().error("Error doing " + op + ": " + x);
			}
			
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}

    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	TimeStruct nn = (TimeStruct)n;
    	nn.value = this.value;
    }
    
	@Override
	public Struct deepCopy() {
		TimeStruct cp = new TimeStruct();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public boolean equals(Object obj) {
		return (TimeStruct.comparison(this, obj) == 0);
	}

	@Override
	public int compare(Object y) {
		return TimeStruct.comparison(this, y);
	}

	@Override
	public int hashCode() {
		return (this.value == null) ? 0 : this.value.hashCode();
	}
	
	@Override
	public String toString() {
		//return (this.value == null) ? "null" : this.value.toString("HH:mm:ss.sss");  -- because LocalTime.parse is messing up millsec values
		return (this.value == null) ? "null" : this.value.toString("HH:mm");
	}

	@Override
	public Object toInternalValue(RootType t) {
		if ((this.value != null) && (t == RootType.String))
			return this.toString();
		
		return this.value;
	}

	public static int comparison(Object x, Object y)
	{
		LocalTime xv = Struct.objectToTime(x);
		LocalTime yv = Struct.objectToTime(y);
		
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
