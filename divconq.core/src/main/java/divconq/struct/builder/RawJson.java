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


public class RawJson implements ICompositeOutput {
	protected Object raw = null;
	
	public RawJson(Object raw) {
		this.raw = raw;
	}
	
	@Override
	public void toBuilder(ICompositeBuilder builder) throws BuilderStateException {
		builder.rawJson(this.raw);		// TODO
	}
	
	@Override
	public String toString() {
		if (this.raw != null)
			return this.raw.toString();
		
		return "null";
	}
}
