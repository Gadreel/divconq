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

public class NullStruct extends ScalarStruct {
	static final public NullStruct instance = new NullStruct(); 

	@Override
	public DataType getType() {
		if (this.explicitType != null)
			return super.getType();

		// implied only, not explicit
		return Hub.instance.getSchema().getType("Null");
	}
	
	@Override
	public void setType(DataType v) {
		if (this == NullStruct.instance)		// do not change on global null
			return;
		
		super.setType(v);
	}
	
	@Override
	public Object getGenericValue() {
		return null;
	}
	
	@Override
	public void adaptValue(Object v) {
		// NA
	}

	public Object getValue() {
		return null; 
	}
	
	@Override
	public boolean isEmpty() {
		return true;
	}
	
	@Override
	public boolean isNull() {
		return true;
	}
    
	@Override
	public Struct deepCopy() {
		NullStruct cp = new NullStruct();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public boolean equals(Object obj) {
		return (NullStruct.comparison(this, obj) == 0);
	}

	@Override
	public int compare(Object y) {
		return NullStruct.comparison(this, y);
	}

	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public String toString() {
		return "null";
	}

	@Override
	public Object toInternalValue(RootType t) {
		return null;
	}

	public static int comparison(Object x, Object y) {
		if (x instanceof NullStruct)
			x = null;
		
		if (y instanceof NullStruct)
			y = null;
		
		if ((y == null) && (x == null))
			return 0;

		if (y == null)
			return 1;

		if (x == null)
			return -1;

		return 0;
	}
	
	@Override
	public boolean checkLogic(StackEntry stack, XElement source) {
		return false;
	}
}
