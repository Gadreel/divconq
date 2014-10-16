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

import divconq.lang.OperationResult;
import divconq.util.StringUtil;

public class PathPart {
	public static PathPart[] parse(OperationResult log, String path) {
		if (StringUtil.isEmpty(path))
			return null;
		
		String[] sparts = path.split(path.contains("/") ? "\\/" :  "\\.");
		PathPart[] parts = new PathPart[sparts.length];
		
		for (int i = 0; i < sparts.length; i++) {
			parts[i] = new PathPart(log, sparts[i]);
		}
		
		return parts;
	}
	
	protected String field = null;
	protected int index = 0;
	protected OperationResult log = null;		// not required, do not expect it to be non null
	
	public PathPart(OperationResult log, String field) {
		this.log = log;
		
		if (StringUtil.isDataInteger(field))
			this.index = (int)StringUtil.parseInt(field, 0);
		else
			this.field = field;
	}
	
	public PathPart(int index) {
		this.index = index;
	}
	
	public String getField() {
		return this.field;
	}
	
	public int getIndex() {
		return this.index;
	}
	
	public boolean isField() {
		return (this.field != null);
	}
	
	public OperationResult getLog() {
		return this.log;
	}
}
