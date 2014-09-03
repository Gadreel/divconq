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

import divconq.struct.RecordStruct;
import divconq.util.MimeUtil;

abstract public class AbstractAttachment {
	protected String name = null;
	protected String mime = null;
	
	public AbstractAttachment(String name) {
		this.name = name;
		this.mime = MimeUtil.getMimeTypeForFile(name);
	}
	
	public AbstractAttachment(String name, String mime) {
		this.name = name;
		this.mime = mime;
	}
	
	abstract public RecordStruct toParam();
}
