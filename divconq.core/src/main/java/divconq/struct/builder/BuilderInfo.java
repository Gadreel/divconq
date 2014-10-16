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
package divconq.struct.builder;

import divconq.struct.CompositeStruct;

public class BuilderInfo {
	public BuilderState State = null;
	
	// does the current field have a name yet
	public boolean IsNamed = false;
	
	// has there been more than one field or item in the container?
	// used at record or list level
	public boolean CommaNeeded = false;
	
	// used with field or scalar in list to indicate when a value is missing 
	public boolean ValueComplete = false;

	// placeholder when building Elastic tree
	public CompositeStruct CurrentE = null;
	
	// placeholders to use when building fields
	public String CurrentField = null;
	public Object CurrentValue = null;
	
	public int indent = 0;
	
	public BuilderInfo(BuilderState state) {
		this.State = state;
	}
	
	public BuilderInfo(BuilderState state, int indent) {
		this.State = state;
		this.indent = indent;
	}
}
