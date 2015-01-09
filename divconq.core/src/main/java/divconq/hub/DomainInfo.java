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

import divconq.lang.op.OperationContext;
import divconq.struct.RecordStruct;
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
				
		this.obfuscator = DomainInfo.prepDomainObfuscator(
				info.getFieldAsString("ObscureClass"), 
				info.getFieldAsString("ObscureSeed"));
	}
	
	@Override
	public String toString() {
		return this.getTitle();
	}
	
	static public ISettingsObfuscator prepDomainObfuscator(String obclass, String seed) {
		ISettingsObfuscator obfuscator = null;
		
		if (StringUtil.isEmpty(obclass)) 
			obclass = "divconq.util.BasicSettingsObfuscator";
			
		try {
			obfuscator = (ISettingsObfuscator) Hub.instance.getInstance(obclass);  
		}
		catch (Exception x) {
			OperationContext.get().error("Bad Settings Obfuscator");
			return null;
		}
		
		XElement clock1 = Hub.instance.getConfig().find("Clock");
		
		String obid = (clock1 != null) ? clock1.getAttribute("Id") : null;
		
		obfuscator.init(new XElement("Clock",
				new XAttribute("Id", obid),
				new XAttribute("Feed", seed)
		));
		
		return obfuscator;
	}
}
