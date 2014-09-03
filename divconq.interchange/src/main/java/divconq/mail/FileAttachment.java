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

import java.io.File;
import java.nio.file.Path;

import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.util.IOUtil;

public class FileAttachment extends AbstractAttachment {
	protected Path file = null;
	
	public FileAttachment(Path file) {
		super(file.getFileName().toString());
		this.file = file;
	}
	
	public FileAttachment(String name, Path file) {
		super(name);
		this.file = file;
	}
	
	public FileAttachment(String name, File file) {
		super(name);
		this.file = file.toPath();
	}

	@Override
	public RecordStruct toParam() {
		return new RecordStruct(
				new FieldStruct("Name", this.name),
				new FieldStruct("Mime", this.mime),
				new FieldStruct("Content", IOUtil.readEntireFileToMemory(this.file))
		);
	}
}
