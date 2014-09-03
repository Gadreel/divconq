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
package divconq.interchange;

import divconq.lang.FuncCallback;
import divconq.script.StackEntry;
import divconq.struct.RecordStruct;
import divconq.xml.XElement;

public interface IFileStoreScanner {

	void scan(FuncCallback<RecordStruct> callback);
	
	// scripts
	public void operation(final StackEntry stack, XElement code);
	
}
