package divconq.cms.feed;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import divconq.db.DataRequest;
import divconq.db.ObjectResult;
import divconq.db.ReplicatedDataRequest;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.IOUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class FeedInfo {
	public static FeedInfo recordToInfo(RecordStruct rec) {
		String channel = rec.getFieldAsString("Channel");
		String path = rec.getFieldAsString("Path");
		String site = rec.getFieldAsString("Site");	
		
		// default to root
		if (StringUtil.isEmpty(site))
			site = "root";
		
		return FeedInfo.buildInfo(site, channel, path);
	}
	
	public static FeedInfo buildInfo(String site, String channel, String path) {
		if (StringUtil.isEmpty(channel) || StringUtil.isEmpty(path))
			return null;
		
		if (StringUtil.isEmpty(site))
			site = "root";
		
		XElement channelDef = FeedIndexer.findChannel(site, channel); 
		
		// if channelDef is null then it is not allowed for this site or does not exist
		if (channelDef == null)
			return null;
		
		FeedInfo fi = new FeedInfo();
		
		fi.site = site;
		fi.channel = channel;
		fi.outerpath = path;
		fi.channelDef = channelDef;
		
		fi.init();
		
		return fi;
	}
	
	protected String site = null;
	protected String channel = null;
	protected String outerpath = null;
	protected String innerpath = null;
	
	protected XElement channelDef = null;
	protected XElement draftDcfContent = null;
	protected XElement pubDcfContent = null;
	protected Path pubpath = null;
	protected Path prepath = null;

	// for this work correctly you need to set site, channel and path first
	public void init() {
		if (this.channelDef == null) 
			return;
		
		this.innerpath = this.channelDef.getAttribute("InnerPath", "") + this.outerpath;		// InnerPath or empty string
		
		String prepath = "root".equals(site) ? "/feed-preview" + this.innerpath + ".dcf.xml" : "/sites/" + site + "/feed-preview" + this.innerpath + ".dcf.xml";
		String ppath = "root".equals(site) ? "/feed" + this.innerpath + ".dcf.xml" : "/sites/" + site + "/feed" + this.innerpath + ".dcf.xml";
		
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();
		
		this.prepath = domain.resolvePath(prepath).toAbsolutePath().normalize();
		this.pubpath = domain.resolvePath(ppath).toAbsolutePath().normalize();
	}

	public List<String> collectExternalFileNames(boolean draft) {
		ArrayList<String> list = new ArrayList<String>();
		
		// load the feed file
		XElement dcf = draft ? this.getDraftDcfContent() : this.getPubDcfContent();
		
		if (dcf == null)
			return list;
		
		// collect all the external part names		
		BiConsumer<XElement, String> collectfunc = new BiConsumer<XElement, String>() {			
			@Override
			public void accept(XElement part, String locale) {
				// don't move if not external
				String ext = part.getAttribute("External", "false").toLowerCase();
				
				if (!"true".equals(ext))
					return;
				
				// use the override locale if present
				if (part.hasAttribute("Locale"))
					locale = part.getAttribute("Locale");	
			
				String sname = (draft ? FeedInfo.this.prepath : FeedInfo.this.pubpath).getFileName().toString();
				
				int pos = sname.indexOf('.');
				sname = sname.substring(0, pos) + "." + part.getAttribute("For") + "." + locale + "." + part.getAttribute("Format");

				list.add(sname);
			}
		}; 
		
		// TODO really this should be the Site default locale
		String deflocale = dcf.getAttribute("Locale", OperationContext.get().getDomain().getDefaultLocale());
		
		// check for external parts and move them
		for (XElement fel : dcf.selectAll("PagePart")) 
			collectfunc.accept(fel, deflocale);
		
		for (XElement afel : dcf.selectAll("Alternate")) {
			String locale = afel.getAttribute("Locale");
			
			for (XElement fel : afel.selectAll("PagePart")) 
				collectfunc.accept(fel, locale);
		}
		
		return list;
	}
	
	public String getSite() {
		return this.site;
	}
	
	public String getChannel() {
		return this.channel;
	}
	
	public String getInnerPath() {
		return this.innerpath;
	}
	
	public String getOuterPath() {
		return this.outerpath;
	}
	
	public XElement getChannelDef() {
		return this.channelDef;
	}
	
	public XElement getDraftDcfContent() {
		if (this.draftDcfContent == null) {
			if (Files.exists(this.prepath)) {
				FuncResult<XElement> res = XmlReader.loadFile(this.prepath, false);
				this.draftDcfContent = res.getResult();
			}
		}
		
		return this.draftDcfContent;
	}
	
	public XElement getPubDcfContent() {
		if (this.pubDcfContent == null) {
			if (Files.exists(this.pubpath)) {
				FuncResult<XElement> res = XmlReader.loadFile(this.pubpath, false);
				this.pubDcfContent = res.getResult();
			}
		}
		
		return this.pubDcfContent;
	}
	
	public Path getPubpath() {
		return this.pubpath;
	}
	
	public Path getPrepath() {
		return this.prepath;
	}

	public FeedAdapter getPreAdapter() {
		OperationResult op = new OperationResult();
		
		FeedAdapter adapt = new FeedAdapter();
		adapt.init(this.channel, this.prepath);		// load direct, not cache - cache may not have updated yet
		adapt.validate();
		
		// if an error occurred during the init or validate, don't use the feed
		if (op.hasErrors())
			return null;
		
		return adapt;
	}

	public FeedAdapter getPubAdapter() {
		OperationResult op = new OperationResult();
		
		FeedAdapter adapt = new FeedAdapter();
		adapt.init(this.channel, this.pubpath);		// load direct, not cache - cache may not have updated yet
		adapt.validate();
		
		// if an error occurred during the init or validate, don't use the feed
		if (op.hasErrors())
			return null;
		
		return adapt;
	}
	
	// this is not required, you may go right to saveDraftFile
	// dcui = only if a Page
	public void initDraftFile(String locale, String title, String dcui, FuncCallback<CompositeStruct> op) {
		if (StringUtil.isEmpty(locale))
			locale = OperationContext.get().getDomain().getDefaultLocale();		// TODO really want from the Site
		
		if ("Pages".equals(this.getChannel()) && StringUtil.isNotEmpty(dcui)) {
			DomainInfo domain = OperationContext.get().getDomain();
			
			// don't go to www-preview at first, www-preview would only be used by a developer showing an altered page
			// for first time save, it makes sense to have the dcui file in www
			Path uisrcpath = domain.resolvePath("/www" + this.getOuterPath() + ".dcui.xml");		// TODO per site
			
			try {
				Files.createDirectories(uisrcpath.getParent());
				IOUtil.saveEntireFile(uisrcpath, dcui);
			}
			catch (Exception x) {
				op.error("Unable to add dcui file: " + x);
				op.complete();
				return;
			}
		}

		try {
			Files.createDirectories(this.getPrepath().getParent());
		}
		catch (Exception x) {
			op.error("Unable to create draft folder: " + x);
			op.complete();
			return;
		}
		
		XElement root = new XElement("dcf")
			.withAttribute("Locale", locale)
			.with(new XElement("Field")
				.withAttribute("Value", title)
				.withAttribute("Name", "Title")
			);
		
		// TODO clear the CacheFile index for this path so that we get up to date entries when importing
		IOUtil.saveEntireFile(this.getPrepath(), root.toString(true));
		
		this.updateDb(op);
	}	
	
	// dcf = content for the dcf file
	// updates = list of records (Name, Content) to write out
	// deletes = list of filenames to remove
	public void saveFile(boolean draft, XElement dcf, ListStruct updates, ListStruct deletes, FuncCallback<CompositeStruct> op) {
		Path savepath = draft ? this.getPrepath() : this.getPubpath();
		
		try {
			Files.createDirectories(savepath.getParent());
		}
		catch (Exception x) {
			op.error("Unable to create draft folder: " + x);
			op.complete();
			return;
		}
		
		String locale = dcf.getAttribute("Locale");
		
		if (StringUtil.isEmpty(locale))
			dcf.setAttribute("Locale", OperationContext.get().getDomain().getDefaultLocale());		// TODO really want from the Site
		
		try {
			if (deletes != null)
				for (Struct df : deletes.getItems()) 
					// TODO clear the CacheFile index for this path so that we get up to date entries when importing
					Files.deleteIfExists(savepath.resolveSibling(df.toString()));
				
			if (updates != null)
				for (Struct uf : updates.getItems()) {
					RecordStruct urec = (RecordStruct) uf;
	
					// TODO clear the CacheFile index for this path so that we get up to date entries when importing
					IOUtil.saveEntireFile(savepath.resolveSibling(urec.getFieldAsString("Name")), urec.getFieldAsString("Content"));
				}
			
			if (dcf != null)
				// TODO clear the CacheFile index for this path so that we get up to date entries when importing
				IOUtil.saveEntireFile(savepath, dcf.toString(true));
			
			// cleanup any draft files, we skipped over them
			if (!draft) {
				List<String> filelist = this.collectExternalFileNames(true);
				
				// move all the external files
				for (String sname : filelist) {
					try {
						Files.deleteIfExists(this.getPrepath().resolveSibling(sname));
						// TODO clear the CacheFile index for this path so that we get up to date entries when importing
					}
					catch (Exception x) {
					}
				}
				
				// finally move the feed file itself
				try {
					Files.deleteIfExists(this.getPrepath());
					// TODO clear the CacheFile index for this path so that we get up to date entries when importing
				}
				catch (Exception x) {
					op.complete();
				}
			}
			
			this.updateDb(op);
		}
		catch (Exception x) {
			op.error("Unable to update feed: " + x);
			op.complete();
		}
	}
	
	public void publicizeFile(FuncCallback<CompositeStruct> op) {
		// if no preview available then nothing we can do here
		if (Files.notExists(this.getPrepath())) {
			op.complete();
			return;
		}

		try {
			Files.createDirectories(this.getPubpath().getParent());
		}
		catch (Exception x) {
			op.error("Unable to create publish folder: " + x);
			op.complete();
			return;
		}
		
		List<String> filelist = this.collectExternalFileNames(true);
		
		// move all the external files
		for (String sname : filelist) {
			Path ypath = this.getPrepath().resolveSibling(sname);
			
			// don't bother if there is no preview file
			if (Files.notExists(ypath))
				continue;

			try {
				Files.move(ypath, this.getPubpath().resolveSibling(sname), StandardCopyOption.REPLACE_EXISTING);
				// TODO clear the CacheFile index for this path so that we get up to date entries when importing
			}
			catch (Exception x) {
				op.error("Unable to move preview file: " + ypath +  " : " + x);
			}
		}
		
		// finally move the feed file itself
		try {
			Files.move(this.getPrepath(), this.getPubpath(), StandardCopyOption.REPLACE_EXISTING);
			// TODO clear the CacheFile index for this path so that we get up to date entries when importing
			
			this.updateDb(op);
		}
		catch (Exception x) {
			op.error("Unable to move preview file: " + this.getPrepath() +  " : " + x);
			op.complete();
		}
	}

	public void deleteFile(DeleteMode mode, OperationCallback op) {
		for (int i = 0; i < 2; i++) {
			boolean draft = (i == 0);
			
			if (draft && (mode == DeleteMode.Published))
				continue;
			
			if (!draft && (mode == DeleteMode.Draft))
				continue;
			
			Path fpath = draft ? this.getPrepath() : this.getPubpath();
			
			// if no dcf file available then nothing we can do 
			if (Files.notExists(fpath)) 
				continue;
			
			List<String> filelist = this.collectExternalFileNames(draft);
			
			// move all the external files
			for (String sname : filelist) {
				Path ypath = fpath.resolveSibling(sname);
	
				try {
					Files.deleteIfExists(ypath);
				}
				catch (Exception x) {
					op.error("Unable to delete feed external file: " + ypath +  " : " + x);
				}
			}
			
			// finally move the feed file itself
			try {
				Files.deleteIfExists(fpath);
			}
			catch (Exception x) {
				op.error("Unable to delete feed file: " + fpath +  " : " + x);
			}
			
			String channel = this.getChannel();
			String path = this.getOuterPath();
			String alias = OperationContext.get().getDomain().getAlias();
			
			// load Page definitions...
			if ("Pages".equals(channel) || "Block".equals(channel)) {
				Path srcpath = draft 
						? Hub.instance.getPublicFileStore().resolvePath("dcw/" + alias + "/www-preview/" + path + ".dcui.xml")
						: Hub.instance.getPublicFileStore().resolvePath("dcw/" + alias + "/www/" + path + ".dcui.xml");
				
				try {
					Files.deleteIfExists(srcpath);
				}
				catch  (Exception x) {
				}
			}			
		}
		
		this.deleteDb(op);
	}

	public void deleteDb(OperationCallback cb) {
		Hub.instance.getDatabase().submit(
				new ReplicatedDataRequest("dcmFeedDelete")
					.withParams(new RecordStruct()
						// TODO .withField("Site", this.site)
						.withField("Channel", this.channel)
						.withField("Path", this.innerpath)
				), 
				new ObjectResult() {
					@Override
					public void process(CompositeStruct result3b) {
						cb.complete();
					}
				});
	}

	public void updateDb(OperationCallback cb) {
		// TODO add sub site indexing, and a conversion to rebuild the indexes
		if (!this.site.equals("root")) {
			cb.complete();
			return;
		}
		
		// work through the adapters
		FeedAdapter pubfeed = this.getPubAdapter();
		FeedAdapter prefeed = this.getPreAdapter();
		
		if ((pubfeed == null) && (prefeed == null)) {
			cb.complete();
			return;
		}
		
		XElement pubxml = (pubfeed != null) ? pubfeed.getXml() : null;
		XElement prexml = (prefeed != null) ? prefeed.getXml() : null;

		// if no file is present then delete record for feed
		if ((pubxml == null) && (prexml == null)) {
			this.deleteDb(cb);
			return;
		}
		
		// if at least one xml file then update/add a record for the feed
		
		RecordStruct feed = new RecordStruct()
			// TODO .withField("Site", this.site)
			.withField("Channel", this.channel)
			.withField("Path", this.outerpath)
			.withField("Editable", true);
		
		// the "edit" authorization, not the "view" auth
		String authtags = (pubfeed != null) ? pubfeed.getAttribute("AuthTags") : prefeed.getAttribute("AuthTags");
		
		if (StringUtil.isEmpty(authtags))
			feed.withField("AuthorizationTags", new ListStruct());
		else
			feed.withField("AuthorizationTags", new ListStruct((Object[]) authtags.split(",")));

		if (pubxml != null) {
			ListStruct ctags = new ListStruct();
			
			for (XElement tag : pubxml.selectAll("Tag")) {
				String alias = tag.getAttribute("Alias");
				
				if (StringUtil.isNotEmpty(alias))
					ctags.addItem(alias);
			}
			
			feed.withField("ContentTags", ctags);
		}
		else if (prexml != null) {
			ListStruct ctags = new ListStruct();
			
			for (XElement tag : prexml.selectAll("Tag")) {
				String alias = tag.getAttribute("Alias");
				
				if (StringUtil.isNotEmpty(alias))
					ctags.addItem(alias);
			}
			
			feed.withField("ContentTags", ctags);
		}
		
		// we should always have info in the Preview fields - use the published if no draft
		if (prexml == null)
			prexml = pubxml;
		
		if (pubxml != null) {
			// public fields
			
			String primelocale = pubxml.getAttribute("Locale"); 
	
			ListStruct pubfields = new ListStruct();
			feed.withField("Fields", pubfields);
			
			for (XElement fld : pubxml.selectAll("Field")) 
				pubfields.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("Name"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for field, though it means little besides adding to search info
					.withField("Value", fld.getValue())
				);
			
			for (XElement afel : pubxml.selectAll("Alternate")) {
				String alocale = afel.getAttribute("Locale");
				
				for (XElement fld : afel.selectAll("Field")) 
					pubfields.addItem(new RecordStruct()
						.withField("Name", fld.getAttribute("Name"))
						.withField("Locale", alocale)
						.withField("Value", fld.getValue())
					);
			}
	
			ListStruct pubparts = new ListStruct();
			feed.withField("PartContent", pubparts);
			
			for (XElement fld : pubxml.selectAll("PagePart"))
				pubparts.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("For"))
					.withField("Format", fld.getAttribute("Format"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for specific part, this is not an alternate, just the default locale for that part
					.withField("Value", pubfeed.getPartValue(primelocale, fld, false))
				);
			
			for (XElement afel : pubxml.selectAll("Alternate")) {
				String alocale = afel.getAttribute("Locale");
				
				for (XElement fld : afel.selectAll("PagePart")) 
					pubparts.addItem(new RecordStruct()
						.withField("Name", fld.getAttribute("For"))
						.withField("Format", fld.getAttribute("Format"))
						.withField("Locale", alocale)
						.withField("Value", pubfeed.getPartValue(alocale, fld, false))
					);
			}
		}
		
		if (prexml != null) {
			// preview fields
			
			String primelocale = prexml.getAttribute("Locale"); 
	
			ListStruct prefields = new ListStruct();
			feed.withField("PreviewFields", prefields);
			
			for (XElement fld : prexml.selectAll("Field")) 
				prefields.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("Name"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for field, though it means little besides adding to search info
					.withField("Value", fld.getValue())
				);
			
			for (XElement afel : prexml.selectAll("Alternate")) {
				String alocale = afel.getAttribute("Locale");
				
				for (XElement fld : afel.selectAll("Field")) 
					prefields.addItem(new RecordStruct()
						.withField("Name", fld.getAttribute("Name"))
						.withField("Locale", alocale)
						.withField("Value", fld.getValue())
					);
			}	
	
			ListStruct preparts = new ListStruct();
			feed.withField("PreviewPartContent", preparts);
			
			for (XElement fld : prexml.selectAll("PagePart")) 
				preparts.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("For"))
					.withField("Format", fld.getAttribute("Format"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for specific part, this is not an alternate, just the default locale for that part
					.withField("Value", prefeed.getPartValue(primelocale, fld, true))
				);
			
			for (XElement afel : prexml.selectAll("Alternate")) {
				String alocale = afel.getAttribute("Locale");
				
				for (XElement fld : afel.selectAll("PagePart")) 
					preparts.addItem(new RecordStruct()
						.withField("Name", fld.getAttribute("For"))
						.withField("Format", fld.getAttribute("Format"))
						.withField("Locale", alocale)
						.withField("Value", prefeed.getPartValue(alocale, fld, true))
					);
			}
		}
		
		// don't bother checking if it worked in our response to service
		DataRequest req3b = new ReplicatedDataRequest("dcmFeedUpdate")
			.withParams(feed);

		Hub.instance.getDatabase().submit(req3b, new ObjectResult() {
			@Override
			public void process(CompositeStruct result3b) {
				cb.complete();
			}
		});
	}
}
