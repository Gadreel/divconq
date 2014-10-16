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
package divconq.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import divconq.lang.Memory;
import divconq.util.StringUtil;

/**
 * Utilities to help with Xml output.
 * 
 * @author Andy
 *
 */
public class XmlWriter {
	/**
	 * Write a xml node and all children to a file
	 * 
	 * @param xml node to write
	 * @param filename name of file to create/overwrite
	 */
	static public void writeToFile(XNode xml, String filename) {
		if (StringUtil.isEmpty(filename))
			return;
		
		XmlWriter.writeToFile(xml, new File(filename));
	}
	
	/**
	 * Write a xml node and all children to a file
	 * 
	 * @param xml node to write
	 * @param dest file to create/overwrite
	 */
	static public void writeToFile(XNode xml, File dest) {
		if ((xml == null) || (dest == null))
			return;
		
		// make sure the folder is there
		File folder = dest.getParentFile();
		folder.mkdirs();
		
		// TODO use more efficient approach than copy to memory first		
		Memory content = xml.toMemory(true);
		content.setPosition(0);
		
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream(dest);
			content.copyToStream(fos);
		}
		catch (IOException x) {
			
		}
		
		try {
			if (fos != null)
				fos.close();
		}
		catch (IOException x) {
			
		}
	}
}
