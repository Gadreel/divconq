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

import divconq.lang.OperationResult;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class ExtensionLoader extends Bundle {
	// TODO add module
	protected IExtension extension = null;
	protected String name = null;
	protected XElement config = null;		// extension tag
	protected XElement setting = null;		// extension.settings tag
	protected IModule module = null;

	public String getName() {
		return this.name;
	}
	
	public IExtension getExtension() {
		return this.extension;
	}
	
	public XElement getConfig() {
		return this.config;
	}
	
	public XElement getSettings() {
		return this.setting;
	}

	public IModule getModule() {
		return this.module;
	}

	public void setModule(IModule mod) {
		this.module = mod;
	}

	public ExtensionLoader(IModule module, ClassLoader cloader) {
		super(cloader);
		this.module = module;
	}
	
	public void init(OperationResult log, XElement config) {
		try {
			this.config = config;
			this.name = config.getAttribute("Name");
			
			if (config != null) {
				for (XElement bel : config.selectAll("Library")) 
					this.addLibrary(bel.getAttribute("Package"), bel.getAttribute("Name"), bel.getAttribute("Alias"));
				
				this.setting = config.find("Settings");

				// after all bundles are loaded, instantiate the RunClass
				String runclass = config.getAttribute("RunClass");
	
				if (StringUtil.isNotEmpty(runclass)) {
					this.extension = (IExtension) this.getInstance(runclass);
					
					// TODO if (this.extension == null) 
					
					this.extension.setLoader(this);
					this.extension.init(log, this.setting);
				}
			}
		} 
		catch (Exception x) {
			// TODO log
			System.out.println("trouble loading the extension: " + x);
		}
	}

	public void start(OperationResult log) {
		if (this.extension != null)
			this.extension.start(log);
	}
	
	public void stop(OperationResult log) {
		if (this.extension != null)
			this.extension.stop(log);
	}
}
