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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.locale.Dictionary;
import divconq.locale.ILocaleResource;
import divconq.locale.LocaleDefinition;
import divconq.log.DebugLevel;
import divconq.schema.SchemaManager;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

/**
 * Within dcFramework all features are tied together through the Hub class.  To get 
 * the hub going you need to give it access to some resources such as Schema
 * and config.  
 *  
 *  HubResources is the class that ties all the resources together and enables
 *  Hub to start.  A typical application using dcFramework will start up something
 *  like this: 
 *  
 *		HubResources resources = new HubResources("00101", true);
 *		resources.setDebugLevel(DebugLevel.Warn);
 * 		OperationResult or = resources.init();
 *			
 *		if (or.hasErrors()) {
 *			Logger.error("Unable to continue, hub resources not properly initialized");
 *			return;
 *		}
 *	
 *		Hub.instance.start(resources);
 *			
 *	[TODO add link to Quick Start ]
 *			
 *	[TODO add link to Framework Architecture ]
 *
 * 
 * TODO consider an option where all config is loaded from AWS.  Their description:
 * 
 * You can use this data to build more generic AMIs that can be modified by configuration files supplied at launch 
 * time. For example, if you run web servers for various small businesses, they can all use the same AMI and retrieve 
 * their content from the Amazon S3 bucket you specify at launch. To add a new customer at any time, simply create a 
 * bucket for the customer, add their content, and launch your AMI.
 *
 *  
 * @author Andy
 *
 */
public class HubResources implements ILocaleResource {
	static public boolean isValidHubId(String id) {
		if (StringUtil.isEmpty(id) || (id.length() != 5))
			return false;
		
		for (int i = 0; i < 5; i++) 
			if (!Character.isDigit(id.charAt(i))) 
				return false;
		
		if ("00000".equals(id))
			return false;
		
		return true;
	}	
	
	protected String deployment = "dcFileServer";
	protected String hubid = "00001";		// reserved for utilities and stand alones - 00000 reserved for system/core 
	protected String team = "one";
	protected String squad = "one";
	protected HubMode mode = HubMode.Private;		// Gateway, Public (server), Private (server or utility)
	protected boolean forTesting = false;
	protected DebugLevel startuplevel = DebugLevel.Info;
	
	// post init
	protected boolean initialized = false;
	protected boolean initsuccess = false;
	
	protected HubPackages packages = new HubPackages();
	
	protected XElement config = null;
	
	// TODO
	//protected XElement fabric = null;
	
	protected Dictionary dictionary = null;
	protected String locale = "en";
	protected LocaleDefinition localedef = null;
  	protected SchemaManager schemaman = null;
	
  	/**
  	 * HubId is a 5 digit (zero padded) number that uniquely identifies this Hub (process) in
  	 * the distributed network of Hubs (processes) in your Project (application). 
  	 * 
  	 * @return HubId
  	 */
	public String getHubId() {
		return this.hubid;
	}
	
	/**
  	 * HubId is a 5 digit (zero padded) number that uniquely identifies this Hub (process) in
  	 * the distributed network of Hubs (processes) in your Project (application). 
	 * 
	 * You should only set the HubId once per run, Hubs are not designed to change Ids mid run.
	 * 
	 * @param v HubId
	 */
	public void setHubId(String v) {
		if (!HubResources.isValidHubId(v))
			throw new IllegalArgumentException("Hub id must be 5 digits, zero padded. Id 00000 is reserved.");
		
		OperationContext.setHubId(v);
		this.hubid = v;
	}
	
	/**
	 * false: this Hub is running on a private, fire-walled, network (company LAN)
	 * true: this Hub is running on a public network such as the Internet or in a DMZ 
	 * 
	 * @return true if is public facing
	 */
	public boolean isPublicFacing() {
		return (this.mode == HubMode.Gateway) || (this.mode == HubMode.Public);
	}
	
	/**
	 * true: this Hub is not complete software, but is mostly support for network interchange 
	 * 
	 * @return true if is a gateway
	 */
	public boolean isGateway() {
		return (this.mode == HubMode.Gateway);
	}
	
	public boolean isForTesting() {
		return this.forTesting;
	}
	
	/**
	 * Squad Id is used to group a number of Hubs together.  A Squad is functionally separated from
	 * all other Squads and may operate independently (disaster recovery).  A Squad may be active 
	 * even while other Squads are, this is not a fail-over architecture but a active-active architecture.
	 * Squads may often be placed in separate data centers or spread across data centers and clouds (IaaS). 
	 * A Squad has its own copy of the Project database, the database is replicated so that each Squad 
	 * will eventually be consistent.
	 * 
	 * @return Squad Id
	 */
	public String getSquadId() {
		return this.squad;
	}
		
	/**
	 * Squad Id is used to group a number of Hubs together.  A Squad is functionally separated from
	 * all other Squads and may operate independently (disaster recovery).  A Squad may be active 
	 * even while other Squads are, this is not a fail-over architecture but a active-active architecture.
	 * Squads may often be placed in separate data centers or spread across data centers and clouds (IaaS). 
	 * A Squad has its own copy of the Project database, the database is replicated so that each Squad 
	 * will eventually be consistent.
	 * 
	 * @param v Squad Id
	 */
	public void setSquadId(String v) {
		this.squad = v;
	}
	
	public String getTeamId() {
		return this.team;
	}
	
	public void setTeamId(String v) {
		this.team = v;
	}
	
	public HubMode getMode() {
		return this.mode;
	}
	
	/**
	 * The global (default) Debug Level to use normally comes from the config file, before the config
	 * file is available Hub start-up uses the Debug Level given to this class.   
	 *   
	 * @return Debug Level used during start-up
	 */
	public DebugLevel getDebugLevel() {
		return this.startuplevel;
	}
		
	/**
	 * The global (default) Debug Level to use normally comes from the config file, before the config
	 * file is available Hub start-up uses the Debug Level given to this class.   
	 *   
	 * @param v Debug Level used during start-up
	 */
	public void setDebugLevel(DebugLevel v) {
		this.startuplevel = v;
	}
	
	/**
	 * A list of package names in the order in which the packages are loaded.  Each subsequent package overrides
	 * the previous.  So, for example, the contents of the last package overrides any similar resource in all
	 * the previous packages.
	 *    
	 * @return list of package names
	 */
	public HubPackages getPackages() {
		return this.packages;
	}
	
	/**
	 * Schema holds the custom data types used by this Project.
	 * 
	 * @return custom data type definitions
	 */
	public SchemaManager getSchema() {
		return this.schemaman;
	}
	
	/**
	 * Dictionary holds a list of Locales for which translations exists for this Project.
	 * 
	 * @return Locales for this Project
	 */
	@Override
	public Dictionary getDictionary() {
		return this.dictionary;
	}

	/**
	 * Config is an XML structure holding the master settings for this Project.  Other configuration
	 * may come from the database.
	 * 
	 * @return master Project settings
	 */
	public XElement getConfig() {
		return this.config;
	}

	/* TODO support chronology defaults
			//.withChronology("/" + DateTimeZone.getDefault().getID());		// ISOChronology w/ default zone
	 * 
	 */
	
	/* TODO
	public XElement getFabric() {
		return this.fabric;
	}
	*/
	
	public HubResources() {
	}

	/**
	 * Manage the resources for this Hub by indicating which hub this is and whether to run 
	 * in developer mode.  See class, HubId and DevMode comments for details.
	 * 
	 * @param deployment project name for the servers (one or more squads)
	 * @param squad the group of servers that forms a local operating group (one or more teams)
	 * @param team the team of servers within the squad (one or more hubs)
	 * @param hubid Hub Id
	 */
	public HubResources(String deployment, String squad, String team, String hubid) {
		if (StringUtil.isNotEmpty(deployment))
			this.deployment = deployment;
		
		if (StringUtil.isNotEmpty(squad))
			this.squad = squad;
		
		if (StringUtil.isNotEmpty(team))
			this.team = team;
		
		if (StringUtil.isNotEmpty(hubid))
			this.setHubId(hubid);
	}
	
	/**
	 * Initialize this object by loading the Schema, Dictionary, Config and Fabric.
	 * 
	 * When in Dev Mode the resources are loaded from a local copy of the Repository instead of from 
	 * the config directory.  Developers will debug applications using the local Repository copy 
	 * so that they can edit repository artifacts (schema, dictionary, resource files) in "native"
	 * repository structure.
	 *   
	 * @return messages logged while initializing this object
	 */
	public OperationResult init() {
		// do not run init twice (not thread safe, should be called by main thread only)
		if (this.initialized) {
			OperationResult or = new OperationResult();
			
			if (!this.initsuccess)
				or.error(112, "Hub resources already loaded, but contained errors");
			
			return or;
		}				
		
		this.initialized = true;
		
		// before starting we want to have a valid hub level task context
		// which requires a hub id
		OperationContext.setHubId(this.hubid);
		
		// use the startup debug level until we init Logger settings
		
		OperationResult or = new OperationResult();
		
		or.info(0, "Loading hub resources");

		or.trace(0, "Loading shared config");
		
		File fshared = new File("./config/" + this.deployment + "/_shared.xml");
		
		FuncResult<XElement> xres = XmlReader.loadFile(fshared, false); 
		
		if (xres.hasErrors()) {
			or.error(100, "Unable to load _shared.xml file, expected: " + fshared.getAbsolutePath());
			return or;
		}
		
		XElement cel = xres.getResult();
		
		this.packages.load(cel);

		or.trace(0, "Packages loaded: " + this.packages);
		
		or.trace(0, "Loading config.xml file");
		
		// find the right config file
		File f = new File("./config/" + this.deployment + "/" + this.hubid + ".xml");
		
		if (!f.exists()) 
			f = new File("./config/" + this.deployment + "/" + this.team + ".xml");
		
		if (!f.exists()) 
			f = new File("./config/" + this.deployment + "/" + this.squad + ".xml");
	
		if (!f.exists())
			f = new File("./config/" + this.deployment + "/_config.xml");
		
		if (!f.exists()) {
			or.error(101, "Unable to find config.xml file, expected: " + f.getAbsolutePath());
			return or;
		}
		
		FuncResult<XElement> xres2 = XmlReader.loadFile(f, false);
		
		if (xres2.hasErrors()) {
			or.error(102, "Unable to load config file, expected: " + f.getAbsolutePath());
			return or;
		}
		
		this.config = xres2.getResult(); 
		
		if (this.config.hasAttribute("HubId"))
			this.setHubId(this.config.getAttribute("HubId"));
		
		if (this.config.hasAttribute("Team"))
			this.setTeamId(this.config.getAttribute("Team"));
		
		if (this.config.hasAttribute("Squad"))
			this.setSquadId(this.config.getAttribute("Squad"));
		
		if (this.config.hasAttribute("Mode")) 
			this.mode = HubMode.valueOf(this.config.getAttribute("Mode"));
		
		if (this.config.hasAttribute("ForTesting")) 
			this.forTesting = Struct.objectToBooleanOrFalse(this.config.getAttribute("ForTesting"));
		
		if (this.config.hasAttribute("Locale"))
			this.locale = this.config.getAttribute("Locale", "en");
		
		or.trace(0, "Loaded config.xml file at: " + f.getAbsolutePath());
		
		or.trace(0, "Using project compiler to load schema and dictionary");
		
		ProjectCompiler comp = new ProjectCompiler();

		this.schemaman = comp.getSchema(this.packages);
		
		if (or.hasErrors()) {
			or.exit(103, "Unable to load schema file(s)");
			return or;
		}
		
		or.trace(0, "Schema loaded");
		
		this.dictionary = comp.getDictionary(or, this.packages);
		
		if (or.hasErrors()) {
			or.exit(104, "Unable to load dictionary file(s)");
			return or;
		}
		
		// ready to add definitions
		this.localedef = this.getLocaleDefinition(this.getDefaultLocale());

		or.trace(0, "Dictionary loaded");

		// TODO get fabric from ./project...
		
		// TODO load Clock Xml from http://169.254.169.254/latest/user-data
		// then over write
		//this.config.find("Clock").replace(parsed awssource);
		
		or.info(0, "Hub resources loaded");
		this.initsuccess = true;
		
		return or;
	}
	
	@Override
	public String getDefaultLocale() {
		return this.locale;
	}

	@Override
	public LocaleDefinition getDefaultLocaleDefinition() {
		return this.getLocaleDefinition(this.getDefaultLocale());
	}

	@Override
	public LocaleDefinition getLocaleDefinition(String name) {
		// TODO lookup definitions
		
		return new LocaleDefinition(name);
	}
	
	// 0 is best, higher the number the worse, -1 for not supported
	@Override
	public int rateLocale(String locale) {
		if ((this.localedef != null) && this.localedef.match(locale))
			return 0;
		
		return -1;
	}
	
	@Override
	public ILocaleResource getParentLocaleResource() {
		return null;
	}
	
	/**
	 * Scan through the local repository or config directory and reload the dictionary
	 * files.  After this any translation used will have updates from the dictionary files.
	 * This method is really only useful in development mode and is not typically called 
	 * by application code.
	 *   
	 * @return messages logged while reloading dictionary
	 */
	public OperationResult reloadDictionary() {
		OperationResult or = new OperationResult();
		
		or.trace(0, "Loading Dictionary");
		
		ProjectCompiler comp = new ProjectCompiler();
		
		this.dictionary = comp.getDictionary(or, this.packages);
		
		if (or.hasErrors()) 
			or.exit(104, "Unable to load dictionary file(s)");
		else		
			or.trace("Dictionary loaded");
		
		return or;
	}
	
	/**
	 * Get a reference to a resource file specific for this Project.
	 *  
	 * @param filename name of the file, path relative to the resources/ folder in config
	 * @return Path reference if found, if not error messages in FuncResult
	 */
	public FuncResult<Path> getProjectResource(String filename) {
		FuncResult<Path> res = new FuncResult<>(); 
		
		Path f = Paths.get("./config/" + this.deployment + "/resources/" + filename);
		
		if (Files.exists(f))  
			res.setResult(f);
		else
			res.errorTr(201, f.toString());
		
		return res;
	}
}
