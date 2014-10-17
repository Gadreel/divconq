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
package divconq.struct;

import divconq.struct.builder.BuilderStateException;
import divconq.struct.builder.ICompositeBuilder;
import divconq.struct.builder.ICompositeOutput;

public class FieldStruct implements ICompositeOutput {
	protected String name = null;
	protected Object orgvalue = null;
	protected boolean prepped = false;
	protected Struct value = null;
	
	public String getName() {
		return this.name;
	}
	
	protected void setName(String v) {
		this.name = v;
	}
	
	public Struct getValue() {
		return this.value;
	}
	
	public Struct sliceValue() {
		Struct s = this.value;
		this.value = null;
		return s;
	}

	public void setValue(Struct v) {
		this.value = v;
		this.orgvalue = null;	// replaced with real value
	}
	
	public FieldStruct(String name) {
		this.name = name;
	}
	
	public FieldStruct(String name, Object value) {
		this.name = name;
		this.orgvalue = value;
	}
	
	@Override
	public void toBuilder(ICompositeBuilder builder) throws BuilderStateException {
		builder.field(this.name, this.value);
	}

	public boolean isEmpty() {
		if (this.value == null)
			return true;
		
		return this.value.isEmpty();
	}
    
	public FieldStruct deepCopy() {
		FieldStruct cp = new FieldStruct(this.name);
		cp.orgvalue = this.orgvalue;
		cp.prepped = this.prepped;
		
		if (this.value != null)
			cp.value = this.value.deepCopy();
		
		return cp;
	}
}
