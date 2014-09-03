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
package divconq.util;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import divconq.interchange.CommonPath;
import divconq.xml.XAttribute;
import divconq.xml.XElement;

public class MimeUtil {
	static protected Map<String,MimeInfo> mimeMapping = new HashMap<String,MimeInfo>();
	static protected Map<String,MimeInfo> mimeTypeMapping = new HashMap<String,MimeInfo>();
	 
	static {
		MimeUtil.load(
				new XElement("MimeList", 
						new XElement("MimeDef", new XAttribute("Ext", "html"), new XAttribute("Type", "text/html"), new XAttribute("Compress", "True")),
						new XElement("MimeDef", new XAttribute("Ext", "xml"), new XAttribute("Type", "text/xml"), new XAttribute("Compress", "True")),
						new XElement("MimeDef", new XAttribute("Ext", "txt"), new XAttribute("Type", "text/plain"), new XAttribute("Compress", "True")),
						new XElement("MimeDef", new XAttribute("Ext", "jpg"), new XAttribute("Type", "image/jpeg")),
						new XElement("MimeDef", new XAttribute("Ext", "png"), new XAttribute("Type", "image/png")),
						new XElement("MimeDef", new XAttribute("Ext", "gif"), new XAttribute("Type", "image/gif")),
						new XElement("MimeDef", new XAttribute("Ext", "css"), new XAttribute("Type", "text/css"), new XAttribute("Compress", "True")),
						new XElement("MimeDef", new XAttribute("Ext", "json"), new XAttribute("Type", "application/json"), new XAttribute("Compress", "True")),
						new XElement("MimeDef", new XAttribute("Ext", "js"), new XAttribute("Type", "application/javascript"), new XAttribute("Compress", "True")),
						new XElement("MimeDef", new XAttribute("Ext", "yaml"), new XAttribute("Type", "text/yaml"), new XAttribute("Compress", "True")),
						new XElement("MimeDef", new XAttribute("Ext", "woff"), new XAttribute("Type", "application/x-font-woff")),
						new XElement("MimeDef", new XAttribute("Ext", "cur"), new XAttribute("Type", "image/vnd.microsoft.icon")),
						new XElement("MimeDef", new XAttribute("Ext", "ico"), new XAttribute("Type", "image/vnd.microsoft.icon"))
				)
		);
	}
	
	static public void load(XElement config) {
		if (config != null) {
			for (XElement mimeinfo : config.selectAll("MimeDef")) {
				String mtype = mimeinfo.getAttribute("Type");
				String ext = mimeinfo.getAttribute("Ext");
				
				if (!StringUtil.isEmpty(mtype) && !StringUtil.isEmpty(ext)) {
					MimeInfo info = new MimeInfo();
					info.ext = ext;
					info.type = mtype;
					info.compress = "True".equals(mimeinfo.getAttribute("Compress"));
					MimeUtil.mimeMapping.put(ext, info);
					MimeUtil.mimeTypeMapping.put(mtype, info);
				}
			}
		}
	}
	
	static public String getMimeType(String ext) {
		if (ext == null)
			return "application/octetstream";
		
		MimeInfo mt = MimeUtil.mimeMapping.get(ext.toLowerCase());		
		return (mt != null) ? mt.type : "application/octetstream";
	}
	
	static public String getMimeType(CommonPath path) {
		return MimeUtil.getMimeType(path.getFileExtension());
	}
	
	static public String getMimeType(Path path) {
		String ext = FileUtil.getFileExtension(path);
		
		return MimeUtil.getMimeType(ext);
	}
	
	static public String getMimeTypeForFile(String fname) {
		String ext = FileUtil.getFileExtension(fname);
		
		return MimeUtil.getMimeType(ext);
	}

	static public boolean getMimeCompress(String ext) {
		if (ext == null)
			return false;
		
		MimeInfo mt = MimeUtil.mimeMapping.get(ext.toLowerCase());		
		
		if (mt == null)
			mt = MimeUtil.mimeTypeMapping.get(ext);
		
		return (mt != null) ? mt.compress : false;
	}
	
	static public class MimeInfo {
		public String ext = null;
		public String type = null;
		public boolean compress = false;
	}

	public static String octetStream() {
		return "application/octetstream";
	}
}
