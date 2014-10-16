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
package divconq.hub;

public class HubDependency {
	protected String source = null;
	protected boolean added = false;
	
	protected boolean passBoot = true;
	protected boolean passConnected = true;
	protected boolean passRun = true;
	
	public HubDependency(String source) {
		this.source = source;
	}
	
	public void setPassBoot(boolean v) {
		if (this.passBoot == v)
			return;
		
		this.passBoot = v;
		this.changed();
	}
	
	public void setPassConnected(boolean v) {
		if (this.passConnected == v)
			return;
		
		this.passConnected = v;
		this.changed();
	}
	
	public void setPassRun(boolean v) {
		if (this.passRun == v)
			return;
		
		this.passRun = v;
		this.changed();
	}
	
	public void changed() {
		if (this.added)
			Hub.instance.dependencyChanged();
	}
}
