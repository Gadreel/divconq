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

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

import divconq.hub.Hub;
import divconq.schema.DataType;
import divconq.schema.RootType;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

public class DateStruct extends ScalarStruct {
	protected LocalDate value = null;

	@Override
	public DataType getType() {
		if (this.explicitType != null)
			return super.getType();
		
		return Hub.instance.getSchema().getType("LocalDate");
	}

	public DateStruct() {		
	}

	public DateStruct(Object v) {
		this.adaptValue(v);
	}

	@Override
	public Object getGenericValue() {
		return this.value;
	}
	
	@Override
	public void adaptValue(Object v) {
		this.value = Struct.objectToDate(v);
	}

	public LocalDate getValue() {
		return this.value; 
	}
	
	public void setValue(LocalDate v) { 
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
			this.value = new LocalDate();
		
		if ("Inc".equals(op)) {
			this.value = this.value.plusDays(1);
			stack.resume();
			return;
		}
		else if ("Dec".equals(op)) {
			this.value = this.value.minusDays(1);
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
				if (code.hasAttribute("Years")) 
					this.value = this.value.plusYears((int)stack.intFromElement(code, "Years"));
				else if (code.hasAttribute("Months")) 
					this.value = this.value.plusMonths((int)stack.intFromElement(code, "Months"));
				else if (code.hasAttribute("Days")) 
					this.value = this.value.plusDays((int)stack.intFromElement(code, "Days"));
				else if (code.hasAttribute("Weeks")) 
					this.value = this.value.plusWeeks((int)stack.intFromElement(code, "Weeks"));
				else if (code.hasAttribute("Period")) {
					Period p = ISOPeriodFormat.standard().parsePeriod(stack.stringFromElement(code, "Period"));
					this.value = this.value.plus(p);
				}
			}
			catch (Exception x) {
				stack.log().error("Error doing " + op + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Subtract".equals(op)) {
			try { 
				if (code.hasAttribute("Years")) 
					this.value = this.value.minusYears((int)stack.intFromElement(code, "Years"));
				else if (code.hasAttribute("Months")) 
					this.value = this.value.minusMonths((int)stack.intFromElement(code, "Months"));
				else if (code.hasAttribute("Days")) 
					this.value = this.value.minusDays((int)stack.intFromElement(code, "Days"));
				else if (code.hasAttribute("Weeks")) 
					this.value = this.value.minusWeeks((int)stack.intFromElement(code, "Weeks"));
				else if (code.hasAttribute("Period")) {
					Period p = ISOPeriodFormat.standard().parsePeriod(stack.stringFromElement(code, "Period"));
					this.value = this.value.minus(p);
				}
			}
			catch (Exception x) {
				stack.log().error("Error doing " + op + ": " + x);
			}
			
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}

    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	DateStruct nn = (DateStruct)n;
    	nn.value = this.value;
    }
    
	@Override
	public Struct deepCopy() {
		DateStruct cp = new DateStruct();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public boolean equals(Object obj) {
		return (DateStruct.comparison(this, obj) == 0);
	}

	@Override
	public int compare(Object y) {
		return DateStruct.comparison(this, y);
	}

	@Override
	public int hashCode() {
		return (this.value == null) ? 0 : this.value.hashCode();
	}
	
	@Override
	public String toString() {
		return (this.value == null) ? "null" : this.value.toString("yyyy-MM-dd");
	}

	@Override
	public Object toInternalValue(RootType t) {
		if ((this.value != null) && (t == RootType.String))
			return this.toString();
		
		return this.value;
	}

	public static int comparison(Object x, Object y)
	{
		LocalDate xv = Struct.objectToDate(x);
		LocalDate yv = Struct.objectToDate(y);
		
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
