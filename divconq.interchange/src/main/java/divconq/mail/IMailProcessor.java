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
package divconq.mail;

import divconq.lang.FuncResult;
import divconq.struct.RecordStruct;
import divconq.work.Task;

public interface IMailProcessor {
	FuncResult<RecordStruct> submit(Task mail);
	void embilishTask(Task email);
}
