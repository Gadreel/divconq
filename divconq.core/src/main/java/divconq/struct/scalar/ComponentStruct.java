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

import divconq.struct.ScalarStruct;

// remember to implement dispose in subclasses
// should maybe be a recordstruct instead?
abstract public class ComponentStruct extends ScalarStruct {
	/*
	protected IComponent value = null;

	@Override
	public Object getGenericValue() {
		return this.value;
	}
	
	@Override
	public void adaptValue(Object v) {
		// NA
		this.value = null;
	}

	public IComponent getValue() {
		return this.value; 
	}
	
	public void setValue(IComponent v) { 
		this.value = v; 
	}
	
	@Override
	public boolean isEmpty() {
		return (this.value == null);
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
		* /

    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	ComponentStruct nn = (ComponentStruct)n;
    	nn.value = this.value;
    }
    
	@Override
	public Struct deepCopy() {
		ComponentStruct cp = new ComponentStruct();
		this.doCopy(cp);
		return cp;
	}
	
	@Override
	public void dispose() {
		// TODO support this!!!
		super.dispose();
	}

	@Override
	public boolean equals(Object obj) {
		return (ComponentStruct.comparison(this, obj) == 0);
	}

	@Override
	public int compare(Object y) {
		return ComponentStruct.comparison(this, y);
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
		if ((y == null) && (x == null))
			return 0;

		if (y == null)
			return 1;

		if (x == null)
			return -1;

		/*  TODO
		if ((x instanceof ComponentStruct) && (y instanceof ComponentStruct)) {
			DateTime xv = ((ComponentStruct)x).getValue();
			DateTime yv = ((ComponentStruct)y).getValue();

			if ((yv == null) && (xv == null))
				return 0;

			if (yv == null)
				return 1;

			if (xv == null)
				return -1;

			return xv.compareTo(yv);
		}
		* /
		
		// TODO add support for other types

		return 0;
	}
	
	@Override
	public void operation(Activity activity, StackEntry stack, XElement code) {
		super.operation(activity, stack, code);
	}
	*/
}
