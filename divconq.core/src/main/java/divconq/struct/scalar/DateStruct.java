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
	public void operation(StackEntry stack, XElement codeEl) {
		
		if ("Add".equals(codeEl.getName())) {
			long years = stack.intFromElement(codeEl, "Years", 0);
			long months = stack.intFromElement(codeEl, "Months", 0);
			long days = stack.intFromElement(codeEl, "Days", 0);

			if (this.value != null) {
				this.value = this.value.plusYears((int) years);
				this.value = this.value.plusMonths((int) months);
				this.value = this.value.plusDays((int) days);
			}
			
			stack.resume();
			return;
		}

		super.operation(stack, codeEl);
	}

		/*
			switch (operation)
			{
				case "Set":
				case "Copy":
					Value = iv.Value;
					break;
				case "Add":
					if (source.HasA("Years"))
					{
						m_value = iv.Value.AddYears(Convert.ToInt32(proc.ResolveValueToString(inst, source.A("Years"))));
					}

					if (source.HasA("Months"))
					{
						m_value = iv.Value.AddMonths(Convert.ToInt32(proc.ResolveValueToString(inst, source.A("Months"))));
					}

					if (source.HasA("Days"))
					{
						m_value = iv.Value.AddDays(Convert.ToInt32(proc.ResolveValueToString(inst, source.A("Days"))));
					}

					if (source.HasA("Hours"))
					{
						m_value = iv.Value.AddHours(Convert.ToInt32(proc.ResolveValueToString(inst, source.A("Hours"))));
					}

					if (source.HasA("Minutes"))
					{
						m_value = iv.Value.AddMinutes(Convert.ToInt32(proc.ResolveValueToString(inst, source.A("Minutes"))));
					}

					if (source.HasA("Seconds"))
					{
						m_value = iv.Value.AddSeconds(Convert.ToInt32(proc.ResolveValueToString(inst, source.A("Seconds"))));
					}

					if (source.HasA("Weeks"))
					{
						m_value = iv.Value.AddDays(Convert.ToInt32(proc.ResolveValueToString(inst, source.A("Weeks"))) * 7);
					}
					break;
				default:
					throw new ArgumentException();
			}
		*/

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
