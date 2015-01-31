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

import java.nio.file.Files;
import java.nio.file.Path;

import divconq.io.LocalFileStore;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.IOUtil;
import divconq.util.ISettingsObfuscator;
import divconq.util.StringUtil;
import divconq.xml.XAttribute;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class DomainInfo {
	protected RecordStruct info = null;
	protected ISettingsObfuscator obfuscator = null;
	protected XElement overrideSettings = null;
	
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

	public ListStruct getNames() {
		return this.info.getFieldAsList("Names");
	}
	
	public XElement getSettings() {
		if (this.overrideSettings != null)
			return this.overrideSettings;
		
		return this.info.getFieldAsXml("Settings");
	}
	
	public void load(RecordStruct info) {
		this.info = info;
				
		this.obfuscator = DomainInfo.prepDomainObfuscator(
				info.getFieldAsString("ObscureClass"), 
				info.getFieldAsString("ObscureSeed"));
		
		this.reloadSettings();
	}

	/* TODO reload more settings too - consider:
	 * 
			./dcw/[domain alias]/static/config     holds web setting for domain
				- settings.xml are the general settings (dcmHomePage - dcmDefaultTemplate[path]) - editable in CMS only
				- extra.xml is extra settings such as routing rules - direct edit by web dev
				  (includes code tags that map to classes instead of to groovy)
				- dictionary.xml is the domain level dictionary - direct edit by web dev
				- vars.json is the domain level variable store - direct edit by web dev
				- cms-dictionary.xml is the domain level dictionary - editable in CMS
				- cms-vars.json is the domain level variable store - editable in CMS
	 * 
	 */
	
	public void reloadSettings() {
		this.overrideSettings = null;
		
		LocalFileStore fs = Hub.instance.getPublicFileStore();
		
		if (fs == null)
			return;
		
		Path cpath = fs.getFilePath().resolve("dcw/" + this.getAlias() + "/static/config");

		if (Files.notExists(cpath))
			return;
		
		Path cspath = cpath.resolve("settings.xml");

		if (Files.exists(cspath)) {
			FuncResult<CharSequence> res = IOUtil.readEntireFile(cspath);
			
			if (res.isEmptyResult())
				return;
			
			FuncResult<XElement> xres = XmlReader.parse(res.getResult(), true);
			
			if (xres.isEmptyResult())
				return;
			
			this.overrideSettings = xres.getResult();
		}
		
		// TODO check for and load dictionaries, variables, etc
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
