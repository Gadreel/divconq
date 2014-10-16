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
import divconq.lang.BigDateTime;
import divconq.schema.DataType;
import divconq.schema.RootType;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

public class BigDateTimeStruct extends ScalarStruct {
	protected BigDateTime value = null;

	@Override
	public DataType getType() {
		if (this.explicitType != null)
			return super.getType();
		
		return Hub.instance.getSchema().getType("BigDateTime");
	}

	public BigDateTimeStruct() {		
	}

	public BigDateTimeStruct(Object v) {
		this.adaptValue(v);
	}

	@Override
	public Object getGenericValue() {
		return this.value;
	}
	
	@Override
	public void adaptValue(Object v) {
		this.value = Struct.objectToBigDateTime(v);
	}

	public BigDateTime getValue() {
		return this.value; 
	}
	
	public void setValue(BigDateTime v) { 
		this.value = v; 
	}
	
	@Override
	public boolean isEmpty() {
		return ((this.value == null) || (this.value.getYear() == null));
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
    	
    	BigDateTimeStruct nn = (BigDateTimeStruct)n;
    	nn.value = this.value;		
    }
    
	@Override
	public Struct deepCopy() {
		BigDateTimeStruct cp = new BigDateTimeStruct();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public boolean equals(Object obj) {
		return (BigDateTimeStruct.comparison(this, obj) == 0);
	}

	@Override
	public Object toInternalValue(RootType t) {
		if ((this.value != null) && (t == RootType.String))
			return this.value.toString();
		
		return this.value;
	}

	@Override
	public int compare(Object y) {
		return BigDateTimeStruct.comparison(this, y);
	}

	@Override
	public int hashCode() {
		return (this.value == null) ? 0 : this.value.hashCode();
	}
	
	@Override
	public String toString() {
		return (this.value == null) ? "null" : this.value.toString();
	}

	public static int comparison(Object x, Object y)
	{
		BigDateTime xv = Struct.objectToBigDateTime(x);
		BigDateTime yv = Struct.objectToBigDateTime(y);

		if ((yv == null) && (xv == null))
			return 0;

		if (yv == null)
			return 1;

		if (xv == null)
			return -1;

		return 0;  // TODO xv.compareTo(yv);
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
