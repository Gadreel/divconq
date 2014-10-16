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
package divconq.session;

import divconq.work.TaskRun;

// be careful with this, don't confuse GC by having Task or Session hold on to this
public class SessionTaskInfo {
	protected Session session = null;
	protected TaskRun task = null;
	
	public Session getSession() {
		return session;
	}
	
	public TaskRun getTask() {
		return task;
	}
	
	public SessionTaskInfo(Session session, TaskRun task) {
		this.session = session;
		this.task = task;
	}
}
