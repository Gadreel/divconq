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
package divconq.io;

import java.nio.file.Path;
import java.nio.file.Paths;

import divconq.filestore.CommonPath;

public class FileStoreEvent {
	protected boolean delete = false;		// else it was modified
	protected String packagename = null;
	protected CommonPath path = null;		// relative to the package
	
	public boolean isDeleted() {
		return this.delete;
	}
	
	public String getPackage() {
		return this.packagename;
	}
	
	public CommonPath getPath() {
		return this.path;
	}
	
	public Path getFile() {
		return Paths.get("./files/" + this.packagename + this.path);
	}
}

