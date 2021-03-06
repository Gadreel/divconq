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

import groovy.lang.GroovyObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.joda.time.DateTime;

import divconq.bus.IService;
import divconq.bus.ServiceRouter;
import divconq.filestore.bucket.Bucket;
import divconq.filestore.bucket.BucketUtil;
import divconq.io.CacheFile;
import divconq.io.FileStoreEvent;
import divconq.io.LocalFileStore;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationContextBuilder;
import divconq.locale.ILocaleResource;
import divconq.locale.LocaleDefinition;
import divconq.locale.Dictionary;
import divconq.scheduler.ISchedule;
import divconq.scheduler.SimpleSchedule;
import divconq.scheduler.common.CommonSchedule;
import divconq.schema.SchemaManager;
import divconq.service.DomainServiceAdapter;
import divconq.service.DomainWatcherAdapter;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.IOUtil;
import divconq.util.ISettingsObfuscator;
import divconq.util.StringUtil;
import divconq.work.Task;
import divconq.xml.XAttribute;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class DomainInfo implements ILocaleResource {
	protected RecordStruct info = null;
	protected ISettingsObfuscator obfuscator = null;
	protected XElement overrideSettings = null;
	protected SchemaManager schema = null;
	protected Dictionary dictionary = null;
	protected Map<String, IService> registered = new HashMap<String, IService>();
	protected Map<String, ServiceRouter> routers = new HashMap<String, ServiceRouter>();
	protected Map<String, Bucket> buckets = new HashMap<String, Bucket>();
	protected DomainWatcherAdapter watcher = null;
	protected List<ISchedule> schedulenodes = new ArrayList<>();
	protected String locale = null;
	protected LocaleDefinition localedef = null;
	
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
	
	public Bucket getBucket(String name) {
		Bucket b = this.buckets.get(name);
		
		if (b == null) {
			b = BucketUtil.buildBucket(name, this);
			
			if (b != null)
				this.buckets.put(name, b);
		}
		
		return b;
	}
	
	public GroovyObject getScript(String service, String feature) {
		IService s = this.registered.get(service);
		
		if (s instanceof DomainServiceAdapter) 
			return ((DomainServiceAdapter)s).getScript(feature);
		
		return null;
	}
	
	public GroovyObject getWatcherScript() {
		if (this.watcher != null) 
			return this.watcher.getScript();
		
		return null;
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
	
	@Override
	public String getDefaultLocale() {
		if (this.locale != null)
			return this.locale;
		
		return this.getParentLocaleResource().getDefaultLocale();
	}

	@Override
	public LocaleDefinition getDefaultLocaleDefinition() {
		return this.getLocaleDefinition(this.getDefaultLocale());
	}
	
	@Override
	public Dictionary getDictionary() {
		if (this.dictionary != null)
			return this.dictionary;
		
		return this.getParentLocaleResource().getDictionary();
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
		
		int r = this.getParentLocaleResource().rateLocale(locale);
		
		if (r < 0)
			return -1;
		
		return r + 1;
	}
	
	@Override
	public ILocaleResource getParentLocaleResource() {
		return Hub.instance.getResources();
	}
	
	public void load(RecordStruct info) {
		this.info = info;
				
		this.obfuscator = DomainInfo.prepDomainObfuscator(
				info.getFieldAsString("ObscureClass"), 
				info.getFieldAsString("ObscureSeed"));
		
		this.reloadSettings();
	}

	public Path resolvePath(String path) {
		LocalFileStore fs = Hub.instance.getPublicFileStore();
		
		if (fs == null)
			return null;
		
		if (StringUtil.isEmpty(path))
			return fs.getFilePath().resolve("dcw/" + this.getAlias());
		
		if (path.charAt(0) == '/')
			return fs.getFilePath().resolve("dcw/" + this.getAlias() + path);
		
		return fs.getFilePath().resolve("dcw/" + this.getAlias() + "/" + path);
	}

	public Path getPath() {
		LocalFileStore fs = Hub.instance.getPublicFileStore();
		
		if (fs == null)
			return null;
		
		return fs.getFilePath().resolve("dcw/" + this.getAlias());
	}
	
	public String relativize(Path path) {
		path = path.normalize().toAbsolutePath();
		
		if (path == null)
			return null;
		
		String rpath = path.toString().replace('\\', '/');
		
		String dpath = this.getPath().toString().replace('\\', '/');
		
		if (!rpath.startsWith(dpath))
			return null;
		
		return rpath.substring(dpath.length());
	}

	public CacheFile resolveCachePath(String path) {
		LocalFileStore fs = Hub.instance.getPublicFileStore();
		
		if (fs == null)
			return null;
		
		if (StringUtil.isEmpty(path))
			return fs.cacheResolvePath("dcw/" + this.getAlias());
		
		if (path.charAt(0) == '/')
			return fs.cacheResolvePath("dcw/" + this.getAlias() + path);
		
		return fs.cacheResolvePath("dcw/" + this.getAlias() + "/" + path);
	}

	/* TODO reload more settings too - consider:
	 * 
			./dcw/[domain alias]/config     holds web setting for domain
				- settings.xml are the general settings (dcmHomePage - dcmDefaultTemplate[path]) - direct edit by web dev
				  (includes code tags that map to classes instead of to groovy)
				- dictionary.xml is the domain level dictionary - direct edit by web dev
				- vars.json is the domain level variable store - direct edit by web dev
				
				- cms-settings.xml is extra settings - editable in CMS
				- cms-dictionary.xml is the domain level dictionary - editable in CMS
				- cms-vars.json is the domain level variable store - editable in CMS
	 * 
	 */
	
	public void reloadSettings() {
		this.overrideSettings = null;
		
		Path cpath = this.resolvePath("/config");

		if ((cpath == null) || Files.notExists(cpath))
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
			
			if (this.overrideSettings.hasAttribute("Locale")) 
				this.locale = this.overrideSettings.getAttribute("Locale");
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
		
		// dictionary
		
		this.dictionary = null;
		
		Path dicpath = cpath.resolve("dictionary.xml");

		if (Files.exists(dicpath)) {
			this.dictionary = new Dictionary();
			this.dictionary.setParent(Hub.instance.getResources().getDictionary());
			this.dictionary.load(dicpath);
			
			this.localedef = this.getLocaleDefinition(this.getDefaultLocale());
		}		

		this.registered.clear();
		this.routers.clear();
		
		Path dpath = this.resolvePath("");
		Path spath = dpath.resolve("services");

		if (Files.exists(spath)) {
			try (Stream<Path> str = Files.list(spath)) {
				str.forEach(path -> {
					// only directories are services - files in dir are features
					if (!Files.isDirectory(path))
						return;
					
					String name = path.getFileName().toString();
					
					this.registerService(new DomainServiceAdapter(name, path, dpath));
				});
			} 
			catch (IOException x) {
				// TODO Auto-generated catch block
				x.printStackTrace();
			}
		}
		
		// watcher comes after services so it can register a service if it likes... if this came before it would be cleared from the registered list
		if (this.watcher == null)
			this.watcher = new DomainWatcherAdapter(dpath);
		
		this.watcher.init(this);
		
		for (Bucket b : this.buckets.values())
			b.tryExecuteMethod("Kill", this);
		
		this.buckets.clear();
		
		this.prepDomainSchedule();
	}
	
	public void registerService(IService service) {
		this.registered.put(service.serviceName(), service);
		
		ServiceRouter r = new ServiceRouter(service.serviceName());
		r.indexLocal();
		
		this.routers.put(service.serviceName(), r);
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
	
	public void prepDomainSchedule() {
		// cancel and remove any previous schedules 
		if (this.schedulenodes.size() > 0) {
			OperationContext.get().info("Cancelling schedules for " + this.getAlias());
			
			for (ISchedule sch : this.schedulenodes) {
				OperationContext.get().info("- schedule: " + sch.task().getTitle());
				sch.cancel();
			}
		}
		
		this.schedulenodes.clear();
		
		if (this.overrideSettings == null)
			return;
		
		// now load new schedules
		OperationContext.get().info("Prepping schedules for " + this.getAlias());
		
		for (XElement schedule : this.overrideSettings.selectAll("Schedules/*")) {
			OperationContext.get().info("- find schedule: " + schedule.getAttribute("Title"));
			
			ISchedule sched = "CommonSchedule".equals(schedule.getName()) ? new CommonSchedule() : new SimpleSchedule();
			
			sched.init(schedule);
			
			sched.setTask(new Task()
				.withId(Task.nextTaskId("DomainSchedule"))
				.withTitle("Domain Scheduled Task: " + schedule.getAttribute("Title"))
				.withContext(new OperationContextBuilder().withRootTaskTemplate().withDomainId(this.getId()).toOperationContext())
				.withWork(trun -> {
					OperationContext.get().info("Executing schedule: " + trun.getTask().getTitle() + " for domain " + trun.getTask().getContext().getDomain().getAlias());
					
					if (schedule.hasAttribute("MethodName") && (this.watcher != null))
						this.watcher.tryExecuteMethod(schedule.getAttribute("MethodName"), new Object[] { trun });
				})
			);
		
			OperationContext.get().info("- prepped schedule: " + schedule.getAttribute("Title") + " next run " + new DateTime(sched.when()));
			
			this.schedulenodes.add(sched);
			
			Hub.instance.getScheduler().addNode(sched);
		}
	}

	public void fileChanged(FileStoreEvent result) {
		this.watcher.tryExecuteMethod("FileChanged", new Object[] { result });
	}

	public void fireAfterReindex() {
		this.watcher.tryExecuteMethod("AfterReindex", new Object[] { });
	}
}
