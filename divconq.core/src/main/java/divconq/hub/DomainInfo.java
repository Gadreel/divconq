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

import divconq.lang.Memory;
import divconq.struct.RecordStruct;
import divconq.util.BasicSettingsObfuscator;
import divconq.util.HexUtil;
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
	
	public String getTitle() {
		return this.info.getFieldAsString("Title");
	}
	
	public RecordStruct getInfo() {
		return this.info;
	}
	
	public ISettingsObfuscator getObfuscator() {
		return this.obfuscator;
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
			obclass = clock1.getAttribute("FilterClass");
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
		
		String seed = info.getFieldAsString("ObscureSeed");
		
		Memory msd = new Memory(300);
		msd.write(seed);
		msd.setPosition(0);
		
		this.obfuscator.init(new XElement("Clock",
				new XAttribute("Id", obid),
				new XAttribute("Feed", HexUtil.bufferToHex(msd))
		));		
	}
	
	@Override
	public String toString() {
		return this.getTitle();
	}
}
