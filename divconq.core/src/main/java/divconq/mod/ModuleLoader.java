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
package divconq.mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import divconq.lang.OperationResult;
import divconq.mod.Bundle;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class ModuleLoader extends Bundle {
	protected Map<String,ExtensionLoader> extensions = new HashMap<String,ExtensionLoader>();
	protected List<ExtensionLoader> orderedExts = new ArrayList<ExtensionLoader>();
	
	protected IModule module = null;
	protected String name = null;
	protected XElement config = null;
	protected XElement setting = null;

	public String getName() {
		return this.name;
	}
	
	public IModule getModule() {
		return this.module;
	}
	
	public XElement getConfig() {
		return this.config;
	}
	
	public XElement getSettings() {
		return this.setting;
	}
	
	// TODO create a method of registering new extensions
	public ExtensionLoader getExtension(String name) {
		return this.extensions.get(name);
	}

	public ModuleLoader(ClassLoader cloader) {
		super(cloader);
	}
	
	public void init(OperationResult or, XElement config) {
		try {
			this.config = config;		// TODO 
			this.name = config.getAttribute("Name");
			
			for (XElement bel : config.selectAll("Library")) 
				this.addLibrary(bel.getAttribute("Package"), bel.getAttribute("Name"), bel.getAttribute("Alias"));
			
			// after all bundles are loaded, instantiate the RunClass
			String runclass = config.getAttribute("RunClass");
			this.setting = config.find("Settings");
			
			if (StringUtil.isEmpty(runclass)) 
				runclass = "divconq.service.ServiceModule";
			
			this.module = (IModule) this.getInstance(runclass);
			this.module.setLoader(this);
			this.module.init(or, this.setting);
			
			for (XElement bel : config.selectAll("Extension")) {
				ExtensionLoader eloader = new ExtensionLoader(this.module, this);
				eloader.init(or, bel);
				this.extensions.put(eloader.getName(), eloader);
				this.orderedExts.add(eloader);
			}
		} catch (Exception x) {
			// TODO log
			System.out.println("trouble loading the module: " + x);
		}
	}

	public void start(OperationResult or) {
		for (ExtensionLoader el : this.orderedExts) 
			el.start(or);
		
		if (this.module != null)
			this.module.start(or);
	}
	
	public void stop(OperationResult or) {
		for (int i = this.orderedExts.size() - 1; i >= 0; i--) 
			this.orderedExts.get(i).stop(or);
		
		if (this.module != null)
			this.module.stop(or);
	}
}
