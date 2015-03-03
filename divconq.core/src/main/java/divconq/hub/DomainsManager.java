package divconq.hub;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import divconq.bus.Message;
import divconq.db.DataRequest;
import divconq.db.ObjectResult;
import divconq.db.rocks.DatabaseManager;
import divconq.filestore.CommonPath;
import divconq.io.FileStoreEvent;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationCallback;
import divconq.log.Logger;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;

public class DomainsManager {
	// domain tracking
	protected DomainNameMapping<DomainInfo> dnamemap = new DomainNameMapping<>();
	protected ConcurrentHashMap<String, DomainInfo> dsitemap = new ConcurrentHashMap<>();
	
	public Collection<DomainInfo> getDomains() {
		return this.dsitemap.values();
	}

	public void dumpDomainNames() {
		// TODO this.dnamemap.dumpDomainNames();
	}
	
	public String resolveDomainId(String domain) {
		if (StringUtil.isEmpty(domain)) 
			return null;
		
		// if this is a domain id then return it
		if (this.dsitemap.containsKey(domain))
			return domain;

		// if not an id then try lookup of domain name
		DomainInfo di = this.dnamemap.get(domain);
		
		if (di != null)
			return di.getId();
		
		return null;
	}
	
	public DomainInfo resolveDomainInfo(String domain) {
		if (StringUtil.isEmpty(domain)) 
			return null;
		
		// if this is a domain id then return it
		DomainInfo di = this.dsitemap.get(domain);
		
		if (di != null)
			return di;

		// if not an id then try lookup of domain name
		di = this.dnamemap.get(domain);
		
		if (di != null)
			return di;
		
		return null;
	}
		
	public DomainInfo getDomainInfo(String id) {
		if (StringUtil.isEmpty(id))
			return null;
		
		return this.dsitemap.get(id);
	}
	
	public void updateDomainRecord(String did, RecordStruct drec) {
		DomainInfo di = DomainsManager.this.dsitemap.get(did);
		
		// update old
		if (di != null) {
			ListStruct names = di.getNames();

			if (names != null)
				for (Struct dn : names.getItems()) {
					String n = Struct.objectToCharsStrict(dn).toString();
					DomainsManager.this.dnamemap.remove(n);
				}
			
			di.load(drec);
		}
		// insert new
		else {
			di = new DomainInfo();
			di.load(drec);
			DomainsManager.this.dsitemap.put(did, di);
		}
		
		ListStruct names = di.getNames();

		if (names != null)
			for (Struct dn : names.getItems()) {
				String n = Struct.objectToCharsStrict(dn).toString();
				DomainsManager.this.dnamemap.add(n, di);
			}
	}
	
	public void init() {
		HubDependency domdep = new HubDependency("Domains");
		domdep.setPassRun(false);
		Hub.instance.addDependency(domdep);
		
		Hub.instance.subscribeToEvent(HubEvents.DomainAdded, new IEventSubscriber() {			
			@Override
			public void eventFired(Object e) {
				String did = (String) e;
				
				Hub.instance.getBus().sendMessage(
						(Message) new Message("dcDomains", "Manager", "Load")
							.withField("Body", new RecordStruct().withField("Id", did)), 
						result -> {
							// if this fails the hub cannot start
							if (result.hasErrors()) {
								Logger.error("Unable to load new domain into hub");
								return;
							}
							
							DomainsManager.this.updateDomainRecord(did, result.getBodyAsRec());
						}
					);
			}
		});
		
		Hub.instance.subscribeToEvent(HubEvents.DomainUpdated, new IEventSubscriber() {			
			@Override
			public void eventFired(Object e) {
				String did = (String) e;
				
				Hub.instance.getBus().sendMessage(
						(Message) new Message("dcDomains", "Manager", "Load")
							.withField("Body", new RecordStruct().withField("Id", did)), 
						result -> {
							// if this fails the hub cannot start
							if (result.hasErrors()) {
								Logger.error("Unable to update domain in hub");
								return;
							}
							
							DomainsManager.this.updateDomainRecord(did, result.getBodyAsRec());
						}
					);
			}
		});
		
		// register for file store events before we start any services that might listen to these events
		// we need to catch domain config change events 
		if (Hub.instance.getPublicFileStore() != null) { 
			/*	Examples:
				./dcw/[domain alias]/config     holds web setting for domain
					- settings.xml are the general settings (dcmHomePage - dcmDefaultTemplate[path]) - editable in CMS only
					- dictionary.xml is the domain level dictionary - direct edit by web dev
					- vars.json is the domain level variable store - direct edit by web dev
			*/
			
			FuncCallback<FileStoreEvent> localfilestorecallback = new FuncCallback<FileStoreEvent>() {
				@Override
				public void callback() {
					this.resetCalledFlag();
					
					CommonPath p = this.getResult().getPath();
					
					//System.out.println(p);
					
					// only notify on config updates
					if (p.getNameCount() < 4) 
						return;
					
					// must be inside a domain or we don't care
					String mod = p.getName(0);
					String domain = p.getName(1);
					String section = p.getName(2);
					
					if ("dcw".equals(mod) && "config".equals(section)) {
						for (DomainInfo wdomain : DomainsManager.this.dsitemap.values()) {
							if (domain.equals(wdomain.getAlias())) {
								wdomain.reloadSettings();
								Hub.instance.fireEvent(HubEvents.DomainConfigChanged, wdomain);
								break;
							}
						}
					}
					
					if ("dcw".equals(mod) && ("services".equals(section) || "glib".equals(section))) {
						for (DomainInfo wdomain : DomainsManager.this.dsitemap.values()) {
							if (domain.equals(wdomain.getAlias())) {
								wdomain.reloadServices();
								Hub.instance.fireEvent(HubEvents.DomainConfigChanged, wdomain);
								break;
							}
						}
					}
				}
			};
			
			Hub.instance.getPublicFileStore().register(localfilestorecallback);
		}		
		
		Hub.instance.getBus().sendMessage(
			new Message("dcDomains", "Manager", "LoadAll"), 
			result -> {
				// if this fails the hub cannot start
				if (result.hasErrors()) {
					// stop if we think we are connected, but if not then wait maybe we'll connect again and trigger this load again
					if (Hub.instance.state == HubState.Connected)
						Hub.instance.stop();
					
					return;
				}
				
				ListStruct domains = result.getBodyAsList();
				
				for (Struct d : domains.getItems()) {
					RecordStruct drec = (RecordStruct) d;
					
					String did = drec.getFieldAsString("Id");
					
					DomainsManager.this.updateDomainRecord(did, drec);
				}
				
				Hub.instance.removeDependency(domdep.source);
			}
		);
	}
	
	public void initFromDB(DatabaseManager db, OperationCallback callback) {
		DataRequest req = new DataRequest("dcLoadDomains").withRootDomain();	// use root for this request
	
		db.submit(req, new ObjectResult() {
			@Override
			public void process(CompositeStruct result) {
				// if this fails the hub cannot start
				if (this.hasErrors()) {
					callback.complete();
					return;
				}
				
				ListStruct domains = (ListStruct) result;
				
				for (Struct d : domains.getItems()) {
					RecordStruct drec = (RecordStruct) d;
					
					String did = drec.getFieldAsString("Id");
					
					DomainsManager.this.updateDomainRecord(did, drec);
				}
				
				callback.complete();
			}
		});
	}
}
