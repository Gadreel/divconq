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

import divconq.struct.RecordStruct;
import divconq.util.BasicSettingsObfuscator;
import divconq.util.ISettingsObfuscator;
import divconq.util.StringUtil;
import divconq.xml.XAttribute;
import divconq.xml.XElement;

public class DomainInfo {
	protected RecordStruct info = null;
	protected ISettingsObfuscator obfuscator = null;
	
	public String getId() {
		return this.info.getFieldAsString("Id");
	}
	
	public String getAlias() {
		return this.info.getFieldAsString("Alias");
	}
	
	public String getTitle() {
		return this.info.getFieldAsString("Title");
	}
	
	public RecordStruct getInfo() {
		return this.info;
	}
	
	public ISettingsObfuscator getObfuscator() {
		return this.obfuscator;
	}
	
	public XElement getSettings() {
		return this.info.getFieldAsXml("Settings");
	}
	
	public DomainInfo(RecordStruct info) {
		this.info = info;
				
		String obclass = info.getFieldAsString("ObscureClass");
		String obid = null;
		
		if (StringUtil.isNotEmpty(obclass)) {
			try {
				this.obfuscator = (ISettingsObfuscator) Hub.instance.getInstance(obclass);  
			}
			catch (Exception x) {
				// TODO or.error(208, obclass);
			}
		}
		
		XElement clock1 = Hub.instance.getConfig().find("Clock");
		
		if (clock1 != null) {
			// do not use configured class here - we need domains to be portable so stick only to the db settings or std
			//obclass = clock1.getAttribute("FilterClass");
			obid = clock1.getAttribute("Id");
		}
		
		if (StringUtil.isNotEmpty(obclass)) {
			try {
				this.obfuscator = (ISettingsObfuscator) Hub.instance.getInstance(obclass);  
			}
			catch (Exception x) {
				// TODO or.error(208, obclass);
			}
		}

		if (this.obfuscator == null)
			this.obfuscator = new BasicSettingsObfuscator();
		
		// in hex
		String seed = info.getFieldAsString("ObscureSeed");
		
		this.obfuscator.init(new XElement("Clock",
				new XAttribute("Id", obid),
				new XAttribute("Feed", seed)
		));		
	}
	
	@Override
	public String toString() {
		return this.getTitle();
	}
}
