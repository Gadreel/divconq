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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import divconq.bus.IService;
import divconq.bus.ServiceRouter;
import divconq.io.LocalFileStore;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.schema.SchemaManager;
import divconq.service.DomainServiceAdapter;
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
	protected SchemaManager schema = null;
	protected Map<String, IService> registered = new HashMap<String, IService>();
	protected Map<String, ServiceRouter> routers = new HashMap<String, ServiceRouter>();
	
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
	
	public IService getService(String name) {
		return this.registered.get(name);
	}
	
	public ServiceRouter getServiceRouter(String name) {
		return this.routers.get(name);
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
	
	public SchemaManager getSchema() {
		if (this.schema != null)
			return this.schema;
		
		return Hub.instance.getSchema();
	}
	
	public void load(RecordStruct info) {
		this.info = info;
				
		this.obfuscator = DomainInfo.prepDomainObfuscator(
				info.getFieldAsString("ObscureClass"), 
				info.getFieldAsString("ObscureSeed"));
		
		this.reloadSettings();
		this.reloadServices();
	}

	/* TODO reload more settings too - consider:
	 * 
			./dcw/[domain alias]/config     holds web setting for domain
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
		
		Path cpath = fs.getFilePath().resolve("dcw/" + this.getAlias() + "/config");

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
		
		this.schema = null;
		
		Path shpath = cpath.resolve("schema.xml");

		if (Files.exists(shpath)) {
			this.schema = new SchemaManager();
			this.schema.setChain(Hub.instance.getSchema());
			this.schema.loadSchema(shpath);
			this.schema.compile();
		}		
	}
	
	public void reloadServices() {
		this.registered.clear();
		this.routers.clear();
		
		LocalFileStore fs = Hub.instance.getPublicFileStore();
		
		if (fs == null)
			return;
		
		Path dpath = fs.getFilePath().resolve("dcw/" + this.getAlias());
		Path cpath = dpath.resolve("services");

		if (Files.notExists(cpath))
			return;

		try (Stream<Path> str = Files.list(cpath)) {
			str.forEach(path -> {
				// only directories are services - files in dir are features
				if (!Files.isDirectory(path))
					return;
				
				String name = path.getFileName().toString();
				
				this.registered.put(name, new DomainServiceAdapter(name, path, dpath));
				
				ServiceRouter r = new ServiceRouter(name);
				r.indexLocal();
				
				this.routers.put(name, r);
			});
		} 
		catch (IOException x) {
			// TODO Auto-generated catch block
			x.printStackTrace();
		}
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
