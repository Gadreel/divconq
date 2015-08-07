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
package divconq.web.cms;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.db.DataRequest;
import divconq.db.ObjectFinalResult;
import divconq.db.ObjectResult;
import divconq.db.ReplicatedDataRequest;
import divconq.db.query.CollectorField;
import divconq.db.query.SelectDirectRequest;
import divconq.db.query.SelectFields;
import divconq.db.query.WhereEqual;
import divconq.db.query.WhereField;
import divconq.db.update.InsertRecordRequest;
import divconq.filestore.CommonPath;
import divconq.filestore.IFileStoreFile;
import divconq.filestore.local.FileSystemDriver;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.io.LocalFileStore;
import divconq.lang.CountDownCallback;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationContextBuilder;
import divconq.lang.op.OperationResult;
import divconq.mod.ExtensionBase;
import divconq.session.Session;
import divconq.session.DataStreamChannel;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.IOUtil;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
import divconq.web.importer.ImportWebsiteTool;
import divconq.work.IWork;
import divconq.work.Task;
import divconq.work.TaskRun;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class CmsService extends ExtensionBase implements IService {
	protected FileSystemDriver fsd = new FileSystemDriver();
	
	protected Session channels = null;
	protected String bestEvidence = null;
	protected String minEvidence = null;
	
	@Override
	public void init(XElement config) {
		super.init(config);
		
		this.fsd.setRootFolder(".\temp");
		this.bestEvidence = "SHA256";
		this.minEvidence = "Size";
		
		LocalFileStore pubfs = Hub.instance.getPublicFileStore();
		// TODO interleave private - LocalFileStore prifs = Hub.instance.getPrivateFileStore();
		
		if (pubfs != null) {
			// TODO interleave www-preview - domain's file system
			// Path wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/www-preview", path);
			
			// look in the domain's file system
			//Path wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/www", path);
			
			//if ("galleries".equals(path.getName(0)) || "files".equals(path.getName(0)))
			//		wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/", path);
					
			this.fsd.setRootFolder(pubfs.getPath());
			
			// don't wait on this, it'll log correctly
			this.fsd.connect(null, new OperationCallback() {					
				@Override
				public void callback() {
					// NA
				}
			});
		}
		
		this.channels = Hub.instance.getSessions().createForService();
	}
	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("DomainFileStore".equals(feature) || "WebFileStore".equals(feature)) {
			DomainInfo domain = OperationContext.get().getUserContext().getDomain();
			
			LocalFileStore pubfs = Hub.instance.getPublicFileStore();
			
			if (pubfs == null) {
				request.error("Missing file store");
				request.complete();
				return;
			}
			
			CommonPath sectionpath = "DomainFileStore".equals(feature)
					? new CommonPath("/dcw/" + domain.getAlias() + "/")
					: new CommonPath("/dcw/" + domain.getAlias() + "/files");
			
			if ("FileDetail".equals(op)) {
				this.handleFileDetail(request, this.fsd, sectionpath);
				return;
			}
			
			if ("DomainFileStore".equals(feature) && "ImportUIFile".equals(op)) {
				this.handleImportUIFile(request, this.fsd, sectionpath);
				return;
			}
			
			if ("DomainFileStore".equals(feature) && "LoadFile".equals(op)) {
				this.handleLoadFile(request, this.fsd, sectionpath);
				return;
			}
			
			if ("DomainFileStore".equals(feature) && "SaveFile".equals(op)) {
				this.handleSaveFile(request, this.fsd, sectionpath);
				return;
			}
			
			if ("DeleteFile".equals(op)) {
				this.handleDeleteFile(request, this.fsd, sectionpath);
				return;
			}
			
			if ("DeleteFolder".equals(op)) {
				this.handleDeleteFolder(request, this.fsd, sectionpath);
				return;
			}
			
			if ("AddFolder".equals(op)) {
				this.handleAddFolder(request, this.fsd, sectionpath);
				return;
			}
			
			if ("ListFiles".equals(op)) {
				this.handleListFiles(request, this.fsd, sectionpath, "DomainFileStore".equals(feature));
				return;
			}
			
			if ("StartUpload".equals(op)) {
				this.handleStartUpload(request, this.fsd, sectionpath);
				return;
			}

			if ("FinishUpload".equals(op)) {
				this.handleFinishUpload(request, this.fsd, sectionpath);
				return;
			}
			
			if ("StartDownload".equals(op)) {
				this.handleStartDownload(request, this.fsd, sectionpath);
				return;
			}

			if ("FinishDownload".equals(op)) {
				this.handleFinishDownload(request, this.fsd, sectionpath);
				return;
			}
		}
		
		if ("WebGallery".equals(feature)) {
			DomainInfo domain = OperationContext.get().getUserContext().getDomain();
			
			LocalFileStore pubfs = Hub.instance.getPublicFileStore();
			
			if (pubfs == null) {
				request.error("Missing file store");
				request.complete();
				return;
			}
			
			CommonPath sectionpath = new CommonPath("/dcw/" + domain.getAlias() + "/galleries");
			
			if ("ListFiles".equals(op)) {
				this.handleListGallery(request, this.fsd, sectionpath);
				return;
			}
			
			if ("ListVariations".equals(op)) {
				this.handleListGalleryVariations(request, this.fsd, sectionpath);
				return;
			}
			
			if ("FileDetail".equals(op)) {
				this.handleFileDetail(request, this.fsd, sectionpath);
				return;
			}
			
			if ("ImageDetail".equals(op)) {
				this.handleImageDetail(request, this.fsd, sectionpath);
				return;
			}
			
			if ("LoadSlideShow".equals(op)) {
				this.handleLoadSlideShow(request, this.fsd, sectionpath);
				return;
			}
			
			if ("UpdateGallery".equals(op)) {
				this.handleUpdateGallery(request, this.fsd, sectionpath);
				return;
			}
			
			if ("DeleteFile".equals(op)) {
				this.handleDeleteFile(request, this.fsd, sectionpath);
				return;
			}
			
			if ("DeleteFolder".equals(op)) {
				this.handleDeleteFolder(request, this.fsd, sectionpath);
				return;
			}
			
			if ("AddFolder".equals(op)) {
				this.handleAddGalleryFolder(request, this.fsd, sectionpath);
				return;
			}
			
			if ("AddImage".equals(op)) {
				this.handleAddGalleryImage(request, this.fsd, sectionpath);
				return;
			}
			
			if ("StartUpload".equals(op)) {
				this.handleStartUpload(request, this.fsd, sectionpath);
				return;
			}

			if ("FinishUpload".equals(op)) {
				this.handleFinishUpload(request, this.fsd, sectionpath);
				return;
			}
			
			if ("StartDownload".equals(op)) {
				this.handleStartDownload(request, this.fsd, sectionpath);
				return;
			}

			if ("FinishDownload".equals(op)) {
				this.handleFinishDownload(request, this.fsd, sectionpath);
				return;
			}
		}		
		
		// =========================================================
		//  custom file store
		// =========================================================
		
		//if ("CustomFileStore".equals(feature)) {
		//	CustomFileStore.handle(request, op, msg);
		//	return;
		//}
		
		// =========================================================
		//  store categories
		// =========================================================
		
		if ("Category".equals(feature)) {
			Products.handleCategories(request, this.fsd, op, msg);
			return;
		}
		
		// =========================================================
		//  store products
		// =========================================================
		
		if ("Product".equals(feature)) {
			Products.handleProducts(request, this.fsd, op, msg);
			return;
		}
		
		// =========================================================
		//  cms Site
		// =========================================================
		
		if ("Site".equals(feature)) {
			if ("BuildMap".equals(op)) {
				this.handleSiteBuildMap(request);
				return;
			}
			
			if ("ImportSite".equals(op)) {
				// TODO turn this into a task in the service pool so all imports are processed separately
				ImportWebsiteTool iutil = new ImportWebsiteTool();
				
				iutil.importSite(new OperationCallback() {
					@Override
					public void callback() {
						request.complete();
					}
				});
				
				return;
			}
		}
		
		// =========================================================
		//  cms Threads
		// =========================================================
		
		if ("Threads".equals(feature)) {
			DataRequest req = null;
			
			if ("NewThread".equals(op) || "UpdateThreadCore".equals(op) || "ChangePartiesAction".equals(op) || "ChangeFolderAction".equals(op) || "AddContentAction".equals(op) || "ChangeStatusAction".equals(op) || "ChangeLabelsAction".equals(op))
				req = new ReplicatedDataRequest("dcmThread" + op)
					.withParams(msg.getFieldAsComposite("Body"));
			else if ("ThreadDetail".equals(op) || "FolderListing".equals(op) || "FolderCounting".equals(op))
				req = new DataRequest("dcmThread" + op)
					.withParams(msg.getFieldAsComposite("Body"));
			
			if (req != null) {
				Hub.instance.getDatabase().submit(req, new ObjectFinalResult(request));
				return;
			}
		}
		
		// =========================================================
		//  cms Feeds
		// =========================================================
		
		if ("Feeds".equals(feature)) {
			if ("LoadFeedsDefinition".equals(op)) {
				this.handleLoadFeedsDefinitions(request);
				return;
			}
			
			if ("LoadList".equals(op)) {
				this.handleFeedLoadList(request);
				return;
			}
			
			if ("AddFeedFiles".equals(op)) {
				this.handleAddFeedFiles(request);
				return;
			}
			
			if ("AddPageFiles".equals(op)) {
				this.handleAddPageFiles(request);
				return;
			}
			
			if ("LoadFeedFiles".equals(op)) {
				this.handleLoadFeedFiles(request);
				return;
			}
			
			if ("UpdateFeedFiles".equals(op)) {
				this.handleUpdateFeedFiles(request);
				return;
			}
			
			if ("PublishFeedFiles".equals(op)) {
				this.handlePublishFeedFiles(request);
				return;
			}
			
			if ("ImportFeedFiles".equals(op)) {
				this.handleImportFeedFiles(request);
				return;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	public void handleFeedLoadList(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String channel = rec.getFieldAsString("Channel");
		
		Hub.instance.getDatabase().submit(
				new SelectDirectRequest() 
					.withTable("dcmFeed")
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcmPath", "Path")
						.withSubField("dcmFields", "Published.en", "Published")
						.withSubField("dcmFields", "Title.en", "Title")
						.withSubField("dcmFields", "Image.en", "Image")
						.withSubField("dcmFields", "Description.en", "Description")
				)
				.withCollector(
					new CollectorField("dcmChannel")
						.withValues(channel)
				),
				new ObjectFinalResult(request));
	}
	
	public void handleAddFeedFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		
		String locale = "en";		// TODO elaborate
		String channel = rec.getFieldAsString("Channel");
		String path = rec.getFieldAsString("Path");
		
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();
		
		XElement feed = domain.getSettings().find("Feed");
		
		if (feed == null) { 
			request.error("Unable to find feed settings: " + channel);
			request.complete();
			return;
		}
		
		// there are two special channels - Pages and Blocks
		for (XElement chan : feed.selectAll("Channel")) { 
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			if (calias.equals(channel)) {
				path = chan.getAttribute("InnerPath", "") + path;		// InnerPath or empty string
				break;
			}			
		}

		Path srcpath = domain.resolvePath("/feed-preview" + path + ".dcf.xml");
		
		try {
			Files.createDirectories(srcpath.getParent());
			
			String uuid = UUID.randomUUID().toString();
			
			// put a record in database so we'll show up in the listing
			Hub.instance.getDatabase().submit(
				new ReplicatedDataRequest("dcmFeedUpdate")
					.withParams(new RecordStruct()
						.withField("Uuid", uuid)
						.withField("Channel", channel)
						.withField("Path", path)
						.withField("Editable", true)
						.withField("Fields", new ListStruct()
							.withItems(new RecordStruct()
								.withField("Name", "Title")
								.withField("Locale", locale)		
								.withField("Value", rec.getFieldAsString("Title"))
							)
						)
					), 
				new ObjectResult() {
					@Override
					public void process(CompositeStruct result3b) {
						XElement root = new XElement("dcf")
							.withAttribute("Locale", locale)
							.withAttribute("Uuid", uuid)
							.with(new XElement("Field")
								.withAttribute("Value", rec.getFieldAsString("Title"))
								.withAttribute("Name", "Title")
							);
						
						// allow overwrites - else we will orphan new files that cannot be indexed
						IOUtil.saveEntireFile(srcpath, root.toString(true));
						
						request.complete();
					}
				});
		}
		catch (Exception x) {
			request.error("Unable to add feed: " + x);
			request.complete();
		}
	}
	
	public void handleAddPageFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String channel = "Pages";
		String locale = "en";		// TODO elaborate
		String path = rec.getFieldAsString("Path");
		
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();
		
		XElement feed = domain.getSettings().find("Feed");
		
		if (feed == null) { 
			request.error("Unable to find feed settings: " + channel);
			request.complete();
			return;
		}
		
		String ipath = path;
		
		// there are two special channels - Pages and Blocks
		for (XElement chan : feed.selectAll("Channel")) { 
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			if (calias.equals(channel)) {
				ipath = chan.getAttribute("InnerPath", "") + path;		// InnerPath or empty string
				break;
			}			
		}

		Path srcpath = domain.resolvePath("/feed-preview" + ipath + ".dcf.xml");

		// TODO someday work this out so that it will go to www-preview at first
		Path uisrcpath = domain.resolvePath("/www" + path + ".dcui.xml");
		
		try {
			Files.createDirectories(srcpath.getParent());
			Files.createDirectories(uisrcpath.getParent());
			
			String uuid = UUID.randomUUID().toString();
			
			// put a record in database so we'll show up in the listing
			Hub.instance.getDatabase().submit(
				new ReplicatedDataRequest("dcmFeedUpdate")
					.withParams(new RecordStruct()
						.withField("Uuid", uuid)
						.withField("Channel", channel)
						.withField("Path", path)
						.withField("Editable", true)
						.withField("Fields", new ListStruct()
							.withItems(new RecordStruct()
								.withField("Name", "Title")
								.withField("Locale", locale)		
								.withField("Value", rec.getFieldAsString("Title"))
							)
						)
					), 
				new ObjectResult() {
					@Override
					public void process(CompositeStruct result3b) {
						IOUtil.saveEntireFile(uisrcpath, rec.getFieldAsString("UIContentXml"));
	
						XElement root = new XElement("dcf")
							.withAttribute("Locale", locale)
							.withAttribute("Uuid", uuid)
							.with(new XElement("Field")
								.withAttribute("Value", rec.getFieldAsString("Title"))
								.withAttribute("Name", "Title")
							);
						
						// allow overwrites - else we will orphan new files that cannot be indexed
						IOUtil.saveEntireFile(srcpath, root.toString(true));
						
						request.complete();
					}
				});
		}
		catch (Exception x) {
			request.error("Unable to add feed: " + x);
			request.complete();
		}
	}
	
	public void handleUpdateFeedFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String channel = rec.getFieldAsString("Channel");
		String path = rec.getFieldAsString("Path");
		
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();
		
		XElement feed = domain.getSettings().find("Feed");
		
		if (feed == null) { 
			request.error("Unable to find feed settings: " + channel);
			request.complete();
			return;
		}
		
		// there are two special channels - Pages and Blocks
		for (XElement chan : feed.selectAll("Channel")) { 
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			if (calias.equals(channel)) {
				path = chan.getAttribute("InnerPath", "") + path;		// InnerPath or empty string
				break;
			}			
		}

		//if ("Pages".equals(channel)) 
		//	path = "/Pages" + path;

		Path srcpath = domain.resolvePath("/feed-preview" + path + ".dcf.xml");
		Path psrcpath = domain.resolvePath("/feed" + path + ".dcf.xml");
		
		if (Files.notExists(srcpath) && Files.notExists(psrcpath))
			try {
				XElement root = rec.getFieldAsXml("ContentXml");
				
				String locale = root.hasAttribute("Locale") ? root.getAttribute("Locale") : "en";
				
				Files.createDirectories(srcpath.getParent());
				
				String uuid = UUID.randomUUID().toString();
				
				// put a record in database so we'll show up in the listing
				Hub.instance.getDatabase().submit(
					new ReplicatedDataRequest("dcmFeedUpdate")
						.withParams(new RecordStruct()
							.withField("Uuid", uuid)
							.withField("Channel", channel)
							.withField("Path", path)
							.withField("Editable", true)
							.withField("Fields", new ListStruct()
								.withItems(new RecordStruct()
									.withField("Name", "Title")
									.withField("Locale", locale)		
									.withField("Value", rec.getFieldAsString("Title"))
								)
							)
						), 
					new ObjectResult() {
						@Override
						public void process(CompositeStruct result3b) {
							root.withAttribute("Uuid", uuid);
							root.withAttribute("Locale", locale);
							
							if (rec.hasField("UpdateFiles"))
								for (Struct uf : rec.getFieldAsList("UpdateFiles").getItems()) {
									RecordStruct urec = (RecordStruct) uf;
					
									IOUtil.saveEntireFile(srcpath.resolveSibling(urec.getFieldAsString("Name")), urec.getFieldAsString("Content"));
								}
							
							// allow overwrites - else we will orphan new files that cannot be indexed
							IOUtil.saveEntireFile(srcpath, root.toString(true));
							
							request.complete();
						}
					});
			}
			catch (Exception x) {
				request.error("Unable to add feed: " + x);
				request.complete();
			}		
		else
			try {
				if (rec.hasField("DeleteFiles"))
					for (Struct df : rec.getFieldAsList("DeleteFiles").getItems()) {
						try {
							Files.deleteIfExists(srcpath.resolveSibling(df.toString()));
						}
						catch (Exception x) {
							System.out.println("Problem deleting: " + df);
						}
					}
					
				if (rec.hasField("UpdateFiles"))
					for (Struct uf : rec.getFieldAsList("UpdateFiles").getItems()) {
						RecordStruct urec = (RecordStruct) uf;
		
						IOUtil.saveEntireFile(srcpath.resolveSibling(urec.getFieldAsString("Name")), urec.getFieldAsString("Content"));
					}
				
				if (rec.hasField("ContentXml"))
					IOUtil.saveEntireFile(srcpath, rec.getFieldAsString("ContentXml"));
			}
			catch (Exception x) {
				request.error("Unable to update feed: " + x);
			}
		
		request.complete();
	}
	
	public void handleLoadFeedsDefinitions(TaskRun request) {
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();
		
		XElement feed = domain.getSettings().find("Feed");
		
		if (feed == null) {
			request.error("Feed definition does not exist.");
			request.complete();
			return;
		}
		
		RecordStruct resp = new RecordStruct()
			.withField("FeedsXml", feed);
		 
		request.returnValue(resp);
	}
		
	public void handleLoadFeedFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String channel = rec.getFieldAsString("Channel");
		String path = rec.getFieldAsString("Path");
		
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();
		
		XElement feed = domain.getSettings().find("Feed");
		
		if (feed == null) {
			request.error("Feed definition does not exist.");
			request.complete();
			return;
		}
		
		XElement definition = null;
		
		for (XElement chan : feed.selectAll("Channel")) {
			String alias = chan.getAttribute("Alias");
			
			if (alias == null)
				alias = chan.getAttribute("Name");
			
			if (channel.equals(alias)) {
				definition = chan;
				break;
			}
		}
		
		if (definition == null) {
			request.error("Feed channel definition (" + channel + ") does not exist.");
			request.complete();
			return;
		}
		
		// copy it because we might alter it
		definition = (XElement) definition.deepCopy();
		
		String help = null;
		
		// load Page definitions...
		if ("Pages".equals(channel) || "Block".equals(channel)) {
			Path srcpath = Hub.instance.getPublicFileStore().resolvePath("dcw/" + domain.getAlias() + "/www-preview/" + path + ".dcui.xml");
			
			if (Files.notExists(srcpath))
				srcpath = Hub.instance.getPublicFileStore().resolvePath("dcw/" + domain.getAlias() + "/www/" + path + ".dcui.xml");
			
			if (Files.notExists(srcpath)) {
				request.error("Feed page " + path + " does not exist.");
				request.complete();
				return;
			}
			
			FuncResult<XElement> res = XmlReader.loadFile(srcpath, false);
			
			if (res.hasErrors()) {
				request.error("Bad page file " + path + ".");
				request.complete();
				return;
			}

			for (XElement ppd : res.getResult().selectAll("PagePartDef"))
				definition.add(ppd);

			for (XElement ppd : res.getResult().selectAll("Help"))
				definition.add(ppd);
			
			XElement hd = res.getResult().find("Help");
			
			if (hd != null)
				help = hd.getValue();
		}
		
		path = definition.getAttribute("InnerPath", "") + path;		// InnerPath or empty string

		//if ("Pages".equals(channel)) 
		//	path = "/Pages" + path;
			
		Path previewpath = domain.resolvePath("/feed-preview" + path + ".dcf.xml");
		Path pubpath = domain.resolvePath("/feed" + path + ".dcf.xml");
		
		Path srcpath = previewpath;
		
		if (Files.notExists(srcpath))
			srcpath = pubpath;
		
		if (Files.notExists(srcpath)) {
			request.error("Feed file " + path + " does not exist.");
			request.complete();
			return;
		}
		
		FuncResult<XElement> res = XmlReader.loadFile(srcpath, false);
		
		if (res.hasErrors()) {
			request.error("Bad feed file " + path + ".");
			request.complete();
			return;
		}
		
		String fname = srcpath.getFileName().toString();
		String ffname = fname.substring(0, fname.indexOf('.'));
		
		XElement xml = res.getResult();
		
		ListStruct files = new ListStruct();
		
		RecordStruct resp = new RecordStruct()
			.withField("ChannelXml", definition)
			.withField("ContentXml", xml.toString(true))
			.withField("Files", files);
		
		BiConsumer<XElement, String> addfilefunc = new BiConsumer<XElement, String>() {			
			@Override
			public void accept(XElement part, String locale) {
				String ext = part.getAttribute("External", "false").toLowerCase();
				
				if (!"true".equals(ext))
					return;
				
				if (part.hasAttribute("Locale"))
					locale = part.getAttribute("Locale");	// use the override locale if present
			
				String sname = ffname + "." + part.getAttribute("For") + "." + locale + "." + part.getAttribute("Format");
				Path spath = previewpath.resolveSibling(sname);
				
				if (Files.notExists(spath))
					spath = pubpath.resolveSibling(sname);
				
				FuncResult<CharSequence> mres = IOUtil.readEntireFile(spath);
				
				if (mres.isNotEmptyResult()) {
					RecordStruct fentry = new RecordStruct()
						.withField("Name", sname)
						.withField("Content", mres.getResult().toString());
					
					files.addItem(fentry);
				}
			}
		}; 
		
		String deflocale = xml.getAttribute("Locale", OperationContext.get().getDomain().getLocale());
		
		for (XElement fel : xml.selectAll("PagePart")) 
			addfilefunc.accept(fel, deflocale);
		
		for (XElement afel : xml.selectAll("Alternate")) {
			String locale = afel.getAttribute("Locale");
			
			for (XElement fel : afel.selectAll("PagePart")) 
				addfilefunc.accept(fel, locale);
		}
		
		Path hpath = pubpath.resolveSibling("readme.en.md");		// TODO local aware
		
		if (Files.exists(hpath)) {
			FuncResult<CharSequence> mres = IOUtil.readEntireFile(hpath);
			
			if (mres.isNotEmptyResult()) 
				resp.withField("Help", StringUtil.isNotEmpty(help) ? help + "\n\n----\n\n" + mres.getResult() : mres.getResult());
		}
		
		request.returnValue(resp);
	}
	
	public void handlePublishFeedFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String channel = rec.getFieldAsString("Channel");
		String path = rec.getFieldAsString("Path");
		
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();
		
		XElement feed = domain.getSettings().find("Feed");
		
		if (feed == null) { 
			request.error("Unable to find feed settings: " + channel);
			request.complete();
			return;
		}
		
		// there are two special channels - Pages and Blocks
		for (XElement chan : feed.selectAll("Channel")) { 
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			if (calias.equals(channel)) {
				path = chan.getAttribute("InnerPath", "") + path;		// InnerPath or empty string
				break;
			}			
		}
		
		//if ("Pages".equals(channel)) 
		//	path = "/Pages" + path;		// TODO user InnerPath from settings - see also two places above

		Path srcpath = domain.resolvePath("/feed-preview" + path + ".dcf.xml");
		
		// if no preview available then nothing we can do here
		if (Files.notExists(srcpath)) {
			request.complete();
			return;
		}
		
		// make sure we have someplace to put the published file
		
		Path destpath = domain.resolvePath("/feed" + path + ".dcf.xml");
		
		try {
			Files.createDirectories(destpath.getParent());
		}
		catch (Exception x) {
			request.error("Unable to create publish folder: " + x);
			request.complete();
			return;
		}
		
		// load the feed file
		
		FuncResult<CharSequence> fres = IOUtil.readEntireFile(srcpath);
		
		if (fres.hasErrors()) {
			request.error("Missing feed file " + path + ".");
			request.complete();
			return;
		}
		
		FuncResult<XElement> res = XmlReader.parse(fres.getResult(), false);
		
		if (res.hasErrors()) {
			request.error("Bad feed file " + path + ".");
			request.complete();
			return;
		}
		
		XElement xml = res.getResult();
		
		// find and update all the external parts
		
		BiConsumer<XElement, String> addfilefunc = new BiConsumer<XElement, String>() {			
			@Override
			public void accept(XElement part, String locale) {
				String ext = part.getAttribute("External", "false").toLowerCase();
				
				if (!"true".equals(ext))
					return;
				
				if (part.hasAttribute("Locale"))
					locale = part.getAttribute("Locale");	// use the override locale if present
			
				String sname = srcpath.getFileName().toString();
				
				int pos = sname.indexOf('.');
				sname = sname.substring(0, pos) + "." + part.getAttribute("For") + "." + locale + "." + part.getAttribute("Format");
				
				Path ypath = srcpath.resolveSibling(sname);
				
				// don't bother if there is no preview file
				if (Files.notExists(ypath))
					return;
				
				FuncResult<CharSequence> mres = IOUtil.readEntireFile(ypath);
				
				if (!mres.hasErrors()) {
					OperationResult oor = IOUtil.saveEntireFile(destpath.resolveSibling(sname), mres.getResult().toString());
					
					if (!oor.hasErrors())
						try {
							Files.delete(ypath);
						}
						catch (Exception x) {
							System.out.println("Unable to delete preview file: " + ypath +  " : " + x);
						}
				}
			}
		}; 
		
		String deflocale = xml.getAttribute("Locale", OperationContext.get().getDomain().getLocale());
		
		for (XElement fel : xml.selectAll("PagePart")) 
			addfilefunc.accept(fel, deflocale);
		
		for (XElement afel : xml.selectAll("Alternate")) {
			String locale = afel.getAttribute("Locale");
			
			for (XElement fel : afel.selectAll("PagePart")) 
				addfilefunc.accept(fel, locale);
		}
		
		// finally save the feed file itself
		
		OperationResult oor = IOUtil.saveEntireFile(destpath, fres.getResult().toString());
		
		if (!oor.hasErrors())
			try {
				Files.delete(srcpath);
			}
			catch (Exception x) {
				System.out.println("Unable to delete preview file: " + srcpath +  " : " + x);
			}
		 
		request.complete();
	}
	
	public void handleImportFeedFiles(TaskRun request) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String path = rec.getFieldAsString("Path");
		
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();
		
		Task task = new Task()
			.withWork(new IWork() {
				@Override
				public void run(TaskRun trun) {
					ImportWebsiteTool iutil = new ImportWebsiteTool();
					
					iutil.importFeedFile(domain.resolvePath(path), new OperationCallback() {
						@Override
						public void callback() {
							trun.complete();
						}
					});
				}
			})
			.withTitle("Importing feed " + path)
			.withBucket("ServicePool")		// only one at a time
			.withContext(new OperationContextBuilder()
				.withRootTaskTemplate()
				.withDomainId(domain.getId())
				.toOperationContext()
			);
		
		Hub.instance.getWorkPool().submit(task);
		 
		request.complete();
	}
	
	public void handleSiteBuildMap(TaskRun request) {
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();

		Path webdir = Hub.instance.getPublicFileStore().resolvePath("dcw/" + domain.getAlias() + "/www");
		
		XElement dsel = domain.getSettings();
		
		if (dsel == null) {
			request.warn("Missing IndexUrl");
			request.complete();
			return;
		}
		
		XElement wsel = dsel.find("Web");
		
		if (wsel == null) {
			request.warn("Missing IndexUrl");
			request.complete();
			return;
		}
		
		String indexurl = wsel.getAttribute("IndexUrl");
		
		if (StringUtil.isEmpty(indexurl)) {
			request.warn("Missing IndexUrl");
			request.complete();
			return;
		}
		
		XElement smel = new XElement("urlset")
			.withAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
		
		DateTimeFormatter lmFmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		
        try {
        	if (Files.exists(webdir)) { 
				Files.walkFileTree(webdir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 5,
				        new SimpleFileVisitor<Path>() {
				            @Override
				            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				                throws IOException
				            {
				            	String fname = file.getFileName().toString();
				            	
				            	if (!fname.endsWith(".dcui.xml")) 
					                return FileVisitResult.CONTINUE;
				            	
				            	FuncResult<CharSequence> textres = IOUtil.readEntireFile(file);
				            	
				            	if (textres.isEmptyResult())
					                return FileVisitResult.CONTINUE;
				            	
								FuncResult<XElement> xres = XmlReader.parse(textres.getResult(), true);
								
								if (xres.hasErrors()) 
					                return FileVisitResult.CONTINUE;
								
								XElement root = xres.getResult();
								
								if (!root.getName().equals("dcui"))
					                return FileVisitResult.CONTINUE;
								
								String auth = root.getAttribute("AuthTags");
								
								if (StringUtil.isNotEmpty(auth) && !auth.contains("Guest"))
					                return FileVisitResult.CONTINUE;
								
								if ("True".equals(root.getAttribute("NoIndex")))
					                return FileVisitResult.CONTINUE;
			            		
			            		int pos = -1;
			            		
			            		for (int i = 0; i < file.getNameCount(); i++) {
			            			if ("www".equals(file.getName(i).toString())) {
			            				pos = i;
			            				continue;
			            			}
			            		}
			            		
			            		if (pos == -1)
					                return FileVisitResult.CONTINUE;
								
			            		//System.out.println("- sitemap for " + file);
			            		
			            		// TODO look for an indexing script in the page
			            		// TODO look for a gallery list in the page
		            		
			            		XElement sel = new XElement("url");
			            		
			            		String loc = file.subpath(pos + 1, file.getNameCount()).toString();
			            		
			            		sel.add(new XElement("loc", indexurl + loc.substring(0, loc.indexOf("."))));
			            		sel.add(new XElement("lastmod", lmFmt.print(Files.getLastModifiedTime(file).toMillis())));
			            		
			            		smel.add(sel);
			            		
			            		/*
								XElement keywords = root.find("Keywords");
								
								if ((keywords != null) && keywords.hasText())
									req.withSetField("dcmKeywords", keywords.getText());
								
								XElement desc = root.find("Description");
								
								if ((desc != null) && desc.hasText())
									req.withSetField("dcmDescription", desc.getText());
				            	*/
			            					            		
				                return FileVisitResult.CONTINUE;
				            }
				        });
        	}
        	
        	Path smfile = webdir.resolve("sitemap.xml");
        	
        	IOUtil.saveEntireFile2(smfile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        			+ smel.toString(true));
            
            //System.out.println("map: " + smel.toString(true));
		} 
        catch (IOException x) {
        	request.error("Error building sitemap file: " + x);
		}
        
		request.complete();
	}

	/******************************************************************
	 * Gallery Files
	 ******************************************************************/
	
	public void handleListGallery(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFolderListing(path, new FuncCallback<List<IFileStoreFile>>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;					
				}
				
				RecordStruct info = new RecordStruct();
				
				ListStruct files = new ListStruct();
				
				info.setField("Files", files);
				
				CountDownCallback cdcb = new CountDownCallback(1, new OperationCallback() {
					@Override
					public void callback() {
						request.returnValue(info);
					}
				});
				
				
				for (IFileStoreFile file : this.getResult()) {
					if ("meta.json".equals(file.getName())) {
						cdcb.increment();
						
						file.readAllText(new FuncCallback<String>() {							
							@Override
							public void callback() {
								if (this.isNotEmptyResult())
									info.setField("Settings", Struct.objectToComposite(this.getResult()));
								
								cdcb.countDown();
							}
						});
						
						continue;
					}
					
					// TODO localize
					if ("readme.en.md".equals(file.getName())) {
						cdcb.increment();
						
						file.readAllText(new FuncCallback<String>() {							
							@Override
							public void callback() {
								if (this.isNotEmptyResult())
									info.setField("Help", this.getResult());
								
								cdcb.countDown();
							}
						});
						
						continue;
					}
					
					if (file.getName().startsWith(".")) 
						continue;
					
					if (!file.isFolder())
						continue;
					
					boolean isImage = file.getName().endsWith(".v");
					
					RecordStruct fdata = new RecordStruct();
					
					fdata.setField("FileName", isImage ? file.getName().substring(0, file.getName().length() - 2) : file.getName());
					fdata.setField("IsFolder", !isImage);
					
					if (isImage) {
						// TODO set modified and size based on the `original` variation 
					}
					
					fdata.setField("LastModified", file.getModificationTime());
					fdata.setField("Size", file.getSize());
					
					files.addItem(fdata);
				}
				
				cdcb.countDown();
			}
		});
	}
		
	public void handleListGalleryVariations(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("Path");
		
		if (!fpath.endsWith(".v"))
			fpath += ".v";
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFolderListing(path, new FuncCallback<List<IFileStoreFile>>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;					
				}
				
				RecordStruct info = new RecordStruct();
				
				ListStruct files = new ListStruct();
				
				info.setField("Variants", files);
				
				CountDownCallback cdcb = new CountDownCallback(3, new OperationCallback() {
					@Override
					public void callback() {
						request.returnValue(info);
					}
				});
				
				for (IFileStoreFile file : this.getResult()) {
					if (file.getName().startsWith(".")) 
						continue;
					
					if (file.isFolder())
						continue;
					
					RecordStruct fdata = new RecordStruct();
					
					fdata.setField("Name", file.getName());
					fdata.setField("LastModified", file.getModificationTime());
					fdata.setField("Size", file.getSize());
					
					files.addItem(fdata);
				}
				
				fs.getFileDetail(path.getParent().resolve("meta.json"), new FuncCallback<IFileStoreFile>() {					
					@Override
					public void callback() {
						if (this.getResult().exists()) {
							this.getResult().readAllText(new FuncCallback<String>() {							
								@Override
								public void callback() {
									if (this.isNotEmptyResult())
										info.setField("Settings", Struct.objectToComposite(this.getResult()));
									
									cdcb.countDown();
								}
							});
						}
						else {
							cdcb.countDown();
						}
					}
				});
				
				// localize
				fs.getFileDetail(path.getParent().resolve("readme.en.md"), new FuncCallback<IFileStoreFile>() {					
					@Override
					public void callback() {
						if (this.getResult().exists()) {
							this.getResult().readAllText(new FuncCallback<String>() {							
								@Override
								public void callback() {
									if (this.isNotEmptyResult())
										info.setField("Help", this.getResult());
									
									cdcb.countDown();
								}
							});
						}
						else {
							cdcb.countDown();
						}
					}
				});
				
				cdcb.countDown();
			}
		});
	}
	
	public void handleImageDetail(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("ImagePath") + ".v";
		
		CommonPath path = sectionpath.resolve(fpath);
		
		RecordStruct info = new RecordStruct();
		
		info.setField("GalleryPath", new CommonPath("/galleries" + fpath).getParent());
		info.setField("FileName", path.getFileName());						
		
		ListStruct files = new ListStruct();
		
		info.setField("Variations", files);
		
		CountDownCallback cdcb = new CountDownCallback(2, new OperationCallback() {
			@Override
			public void callback() {
				request.returnValue(info);
			}
		});
		
		fs.getFolderListing(path, new FuncCallback<List<IFileStoreFile>>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;					
				}
				
				for (IFileStoreFile file : this.getResult()) {
					if (file.getName().startsWith(".")) 
						continue;
					
					if (!file.getName().endsWith(".jpg") && !file.getName().endsWith(".jpeg") && !file.getName().endsWith(".png")
							&& !file.getName().endsWith(".gif")) 
						continue;
					
					RecordStruct fdata = new RecordStruct();
					
					String ext = file.getExtension();
					String name = file.getName();
					
					fdata.setField("Alias", name.substring(0, name.length() - ext.length() - 1));
					fdata.setField("Extension", ext);					
					fdata.setField("LastModified", file.getModificationTime());
					fdata.setField("Size", file.getSize());
					
					files.addItem(fdata);
				}
				
				cdcb.countDown();
			}
		});
		
		fs.getFileDetail(path.resolvePeer("meta.json"), new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					//request.error("Meta file does not exist");
					// default is empty					
					info.setField("GallerySettings", new RecordStruct(new FieldStruct("Variations", new ListStruct())));
					cdcb.countDown();
					return;
				}
				
				fi.readAllText(new FuncCallback<String>() {							
					@Override
					public void callback() {
						if (this.isNotEmptyResult())
							info.setField("GallerySettings", Struct.objectToComposite(this.getResult()));
						
						cdcb.countDown();
					}
				});
			}
		});
	}
	
	public void handleLoadSlideShow(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("GalleryPath");
		String alias = rec.getFieldAsString("Alias");
		
		// TODO check that user has access to this folder
		
		fs.getFileDetail(sectionpath.resolve(fpath + "/meta.json"), new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					request.error("Meta file does not exist");
					request.complete();
					return;
				}
				
				fi.readAllText(new FuncCallback<String>() {							
					@Override
					public void callback() {
						if (this.isEmptyResult()) {
							request.returnEmpty();
							return;
						}
							
						RecordStruct meta = (RecordStruct) Struct.objectToComposite(this.getResult());
						
						if (meta.isFieldEmpty("Shows")) {
							request.error("Requested show not found");
							request.returnEmpty();
							return;
						}
						
						for (Struct ss : meta.getFieldAsList("Shows").getItems()) {
							RecordStruct show = (RecordStruct) ss;
							
							if (((alias == null) && !show.getFieldAsString("Alias").equals("default")) || !show.getFieldAsString("Alias").equals(alias)) 
								continue;
							
							RecordStruct info = new RecordStruct();
						
							String valias = show.getFieldAsString("Variation");
							
							info.setField("Title", show.getFieldAsString("Title"));
							info.setField("Variation", valias);
							info.setField("Images", show.getFieldAsList("Images"));
							info.setField("Order", show.getFieldAsString("Order"));
							
							if (!meta.isFieldEmpty("Variations")) {
								for (Struct vs : meta.getFieldAsList("Variations").getItems()) {
									RecordStruct var = (RecordStruct) vs;
									
									if (!var.getFieldAsString("Alias").equals(valias)) 
										continue;
								
									info.setField("VariationSettings", var);
								}
							}
							
							request.returnValue(info);
							return;
						}
						
						request.error("Requested show not found");
						request.returnEmpty();
					}
				});
			}
		});
	}
	
	public void handleUpdateGallery(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		CompositeStruct settings = rec.getFieldAsRecord("Settings");
		
		if (settings == null) {
			request.error("Settings invalid");
			request.returnEmpty();
		}
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFileDetail(path.resolve("meta.json"), new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				fi.writeAllText(settings.toPrettyString(), new OperationCallback() {
					@Override
					public void callback() {
						request.returnEmpty();
					}
				});
			}
		});
	}
		
	/******************************************************************
	 * Domain Files
	 ******************************************************************/
	
	public void handleFileDetail(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					request.error("File does not exist");
					request.complete();
					return;
				}
				
				final RecordStruct fdata = new RecordStruct();
				
				fdata.setField("FileName", fi.getName());
				fdata.setField("IsFolder", fi.isFolder());
				fdata.setField("LastModified", fi.getModificationTime());
				fdata.setField("Size", fi.getSize());
				
				String meth = rec.getFieldAsString("Method");
				
				if (StringUtil.isEmpty(meth) || fi.isFolder()) {
					request.setResult(fdata);
					request.complete();
					return;
				}

				fi.hash(meth, new FuncCallback<String>() {						
					@Override
					public void callback() {
						if (!request.hasErrors()) {
							fdata.setField("Hash", this.getResult());
							request.setResult(fdata);
						}
						
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleImportUIFile(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					request.error("File does not exist");
					request.complete();
					return;
				}
				
				fi.readAllText(new FuncCallback<String>() {					
					@Override
					public void callback() {
						if (this.hasErrors()) {
							request.error("Unable to read file");
							request.complete();
							return;
						}
						
						String text = this.getResult();

						FuncResult<XElement> xres = XmlReader.parse(text, true);
						
						if (xres.hasErrors()) {
							System.out.println("Error parsing file: " + xres.getMessages());
							request.complete();
							return;
						}
						
						XElement root = xres.getResult();
						
						String spath = path.subpath(3).toString();						// remove the www 
						
						if (fpath.endsWith(".dcuis.xml")) {
							String fspath = spath.substring(0, spath.length() - 10);		// remove the extension
							
							System.out.println("Importing skeleton: " + root.getAttribute("Title") + " " + fspath);
							
							InsertRecordRequest req = new InsertRecordRequest();
							
							req
								.withTable("dcmSkeleton")		
								.withSetField("dcmTitle", root.getAttribute("Title"))
								.withSetField("dcmPath", fspath);
							
							Hub.instance.getDatabase().submit(req, new ObjectFinalResult(request));
						}
						else if (root.getName().equals("dcuip")) {
							// TODO deal with block
							
							request.returnValue(5);		// TODO page id
						}
						else {
							// TODO check for ReqLib, ReqStyle, Function - these cannot be imported so return an error
							String fspath = spath.substring(0, spath.length() - 9);		// remove the extension
							
							System.out.println("Importing page: " + root.getAttribute("Title") + " " + root.getAttribute("Skeleton"));

							// only support external skeletons
							if (!root.hasAttribute("Skeleton")) {
								request.error("Missing skeleton path, skeleton must be external");
								request.complete();
								return;
							}

							String tpath = root.getAttribute("Skeleton");
							
							Hub.instance.getDatabase().submit(
								new SelectDirectRequest()
									.withTable("dcmSkeleton") 
									.withSelect(new SelectFields().withField("Id"))
									.withWhere(new WhereEqual(new WhereField("dcmPath"), tpath)), 
								new ObjectResult() {
									@Override
									public void process(CompositeStruct result) {
										if (this.hasErrors()) {
											request.complete();
											return;
										}
										
										if (result == null) {
											request.error("Search for skeleton failed");
											request.complete();
											return;
										}
										
										FuncCallback<String> processPage = new FuncCallback<String>() {
											@Override
											public void callback() {
												if (this.isEmptyResult()) {
													request.error("Skeleton id cannot be estalished");
													request.complete();
													return;
												}
												
												String sid = this.getResult();
												
												InsertRecordRequest req = new InsertRecordRequest();
												
												req
													.withTable("dcmPage")		
													.withSetField("dcmTitle", root.getAttribute("Title"))
													.withSetField("dcmPath", fspath)
													.withSetField("dcmSkeleton", sid)
													.withSetField("dcmAuthor", OperationContext.get().getUserContext().getUserId())
													.withSetField("dcmCreated", new DateTime())
													.withSetField("dcmModified", new DateTime());
												
												XElement keywords = root.find("Keywords");
												
												if ((keywords != null) && keywords.hasText())
													req.withSetField("dcmKeywords", keywords.getText());
												
												XElement desc = root.find("Description");
												
												if ((desc != null) && desc.hasText())
													req.withSetField("dcmDescription", desc.getText());
												
												for (XElement part : root.selectAll("PagePart")) {
													String content = part.getText();
													
													String locale = part.getAttribute("Locale", "default");
													String forid = part.getAttribute("For", "main-content");
													
													String subkey = forid + "." + locale;
													
													req.withSetField("dcmPartContent", subkey, content);
													
													RecordStruct pattrs = new RecordStruct();
													
													for (Entry<String, String> attr : part.getAttributes().entrySet()) 
														pattrs.setField(attr.getKey(), attr.getValue());
													
													req.withSetField("dcmPartAttributes", subkey, pattrs);
												}
												
												Hub.instance.getDatabase().submit(req, new ObjectFinalResult(request));
											}
										};
										
										ListStruct sklist = (ListStruct) result;
										
										if (sklist.getSize() ==  0) {
											InsertRecordRequest req = new InsertRecordRequest();
											
											req
												.withTable("dcmSkeleton")		
												.withSetField("dcmTitle", root.getAttribute("Title"))
												.withSetField("dcmPath", root.getAttribute("Skeleton"));
											
											Hub.instance.getDatabase().submit(req, new ObjectResult() {
												@Override
												public void process(CompositeStruct result) {
													if (this.isNotEmptyResult())
														processPage.setResult(this.getResultAsRec().getFieldAsString("Id"));
													
													processPage.complete();
												}
											});
										}
										else {
											processPage.setResult(sklist.getItemAsRecord(0).getFieldAsString("Id"));
											processPage.complete();
										}
									}
								});
						}
					}
				});
			}
		});
	}
	
	public void handleLoadFile(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					request.error("File does not exist");
					request.complete();
					return;
				}
				
				if (fi.getSize() > 16 * 1000 * 1000) {
					request.error("File too large");
					request.complete();
					return;
				}
				
				fi.readAllText(new FuncCallback<String>() {					
					@Override
					public void callback() {
						if (this.hasErrors()) {
							request.error("Unable to read file");
							request.complete();
							return;
						}
						
						String text = this.getResult();

						request.returnValue(new RecordStruct().withField("Content", text));
					}
				});
			}
		});
	}
	
	public void handleSaveFile(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				String content = rec.getFieldAsString("Content");
				IFileStoreFile fi = this.getResult();
				
				fi.writeAllText(content, new OperationCallback() {			
					@Override
					public void callback() {
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleDeleteFile(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					request.complete();
					return;
				}
				
				if (fi.isFolder()) {
					request.error("Path is folder, use DeleteFolder to remove a folder");
					request.complete();
					return;
				}

				fi.remove(new OperationCallback() {					
					@Override
					public void callback() {
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleDeleteFolder(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					request.complete();
					return;
				}
				
				if (!fi.isFolder()) {
					request.error("Path is not folder, use DeleteFile to remove a file");
					request.complete();
					return;
				}

				fi.remove(new OperationCallback() {					
					@Override
					public void callback() {
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleAddFolder(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.addFolder(path, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				request.complete();
			}
		});
	}
	
	public void handleAddGalleryFolder(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.addFolder(path, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				// TODO really this is not going to be a remote/special store - just treat it like a FS throughout
				Path rpath = OperationContext.get().getDomain().resolvePath("/galleries" + fpath);
				Path mspath = rpath.getParent().resolve("meta-sub.json");
				Path rspath = rpath.getParent().resolve("readme-sub.en.md");
				
				try {
					if (Files.exists(mspath)) {
						Files.copy(mspath, rpath.resolve("meta.json"));
						Files.copy(mspath, rpath.resolve("meta-sub.json"));
					}
				}
				catch (Exception x) {
					request.error("Error copying meta-sub.json: " + x);
				}
				
				try {
					if (Files.exists(rspath)) {
						Files.copy(rspath, rpath.resolve("readme.en.md"));	// localize
						Files.copy(rspath, rpath.resolve("readme-sub.en.md"));	// localize
					}
				}
				catch (Exception x) {
					request.error("Error copying readme-sub.en.md: " + x);
				}
				
				request.complete();
			}
		});
	}
	
	public void handleAddGalleryImage(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath1 = rec.getFieldAsString("ImagePath");
		
		if (!fpath1.endsWith(".v"))
			fpath1 += ".v";
		
		String fpath = fpath1;
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.addFolder(path, new FuncCallback<IFileStoreFile>() {
			@Override
			public void callback() {
				request.complete();
			}
		});
	}
	
	public void handleListFiles(TaskRun request, FileSystemDriver fs, CommonPath sectionpath, boolean showHidden) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FolderPath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFolderListing(path, new FuncCallback<List<IFileStoreFile>>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;					
				}
				
				ListStruct files = new ListStruct();
				
				for (IFileStoreFile file : this.getResult()) {
					if (!showHidden && file.getName().startsWith("."))		// TODO make sure only right users can update/delete hidden 
						continue;
					
					RecordStruct fdata = new RecordStruct();
					
					fdata.setField("FileName", file.getName());
					fdata.setField("IsFolder", file.isFolder());
					fdata.setField("LastModified", file.getModificationTime());
					fdata.setField("Size", file.getSize());
					
					files.addItem(fdata);
				}
				
				request.returnValue(files);
			}
		});
	}
	
	public void handleStartUpload(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				boolean forceover = rec.getFieldAsBooleanOrFalse("ForceOverwrite");
				boolean resume = !forceover && this.getResult().exists();

				// define channel binding
				RecordStruct binding = new RecordStruct(
						new FieldStruct("FilePath", path),
						new FieldStruct("FileSize", rec.getFieldAsInteger("FileSize")),
						new FieldStruct("Hub", OperationContext.get().getSessionId().substring(0, 5)),
						new FieldStruct("Session", OperationContext.get().getSessionId()),
						new FieldStruct("Channel", rec.getFieldAsString("Channel")),
						new FieldStruct("Append", resume)
				);
				
				final DataStreamChannel chan = new DataStreamChannel(CmsService.this.channels.getId(), "Uploading " + fpath, binding);
				
				if (rec.hasField("Params"))
					chan.setParams(rec.getField("Params"));
				
				// apply the channel to a write stream from selected file
				this.getResult().openWrite(chan, new FuncCallback<RecordStruct>() {					
					@Override
					public void callback() {
						if (!request.hasErrors()) {
							// add the channel only after we know it is open
							CmsService.this.channels.addChannel(chan);	
							
							RecordStruct res = this.getResult();
							
							res.setField("BestEvidence", CmsService.this.bestEvidence);		
							res.setField("MinimumEvidence", CmsService.this.minEvidence);
							
							// get the binding info to return
							request.setResult(res);
						}
						
						request.complete();
					}
				});
			}
		});
	}
	
	public void handleFinishUpload(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		if ("Faliure".equals(rec.getFieldAsString("Status"))) {
			request.warn("File upload incomplete or corrupt: " + path);
			
			if (!rec.isFieldEmpty("Note"))
				request.warn("File upload note: " + rec.getFieldAsString("Note"));
			
			request.complete();
			return;
		}
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				RecordStruct evidinfo = rec.getFieldAsRecord("Evidence");
				
				String evidenceType = null;

				// pick best evidence if available, we really don't care if higher is available
				if (!evidinfo.isFieldEmpty(CmsService.this.bestEvidence)) { 
					evidenceType = CmsService.this.bestEvidence;
				}
				// else pick the highest available evidence given
				else {
					for (FieldStruct fld : evidinfo.getFields()) 
						evidenceType = CmsService.this.maxEvidence(fld.getName(), evidenceType);
				}

				final String selEvidenceType = evidenceType;
				
				IFileStoreFile fresult =  this.getResult();
				
				final Consumer<Boolean> afterVerify = (pass) -> {
					if (pass) {
						if (CmsService.this.isSufficentEvidence(CmsService.this.bestEvidence, selEvidenceType))
							request.info("Verified best evidence for upload: " + path);
						else if (CmsService.this.isSufficentEvidence(CmsService.this.minEvidence, selEvidenceType))
							request.info("Verified minimum evidence for upload: " + path);
						else
							request.error("Verified evidence for upload, however evidence is insuffcient: " + path);
					}
					else {
						request.error("File upload incomplete or corrupt: " + path);
					}
					
					if (!request.hasErrors())
						CmsService.this.watch("Upload", fresult);
					
					if (!rec.isFieldEmpty("Note"))
						request.info("File upload note: " + rec.getFieldAsString("Note"));
					
					request.complete();
				};
				
				// TODO wait for file to flush out?
				if ("Size".equals(selEvidenceType)) {
					Long src = evidinfo.getFieldAsInteger("Size");
					long dest = fresult.getSize();
					boolean match = (src == dest);
					
					if (match)
						request.info("File sizes match");
					else
						request.error("File sizes do not match");
					
					afterVerify.accept(match);
				}
				else if (StringUtil.isNotEmpty(selEvidenceType)) {
					fresult.hash(selEvidenceType, new FuncCallback<String>() {						
						@Override
						public void callback() {
							if (request.hasErrors()) {
								afterVerify.accept(false);
							}
							else {
								String src = evidinfo.getFieldAsString(selEvidenceType);
								String dest = this.getResult();
								boolean match = (src.equals(dest));
								
								if (match)
									request.info("File hashes match (" + selEvidenceType + ")");
								else
									request.error("File hashes do not match (" + selEvidenceType + ")");
								
								afterVerify.accept(match);
							}
						}
					});
				}
				else {
					request.error("Missing any form of evidence, supply at least size");
					
					afterVerify.accept(false);
				}
			}
		});
	}
	
	public Message handleStartDownload(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}

				// define channel binding
				RecordStruct binding = new RecordStruct(
						new FieldStruct("FilePath", path),
						new FieldStruct("Offset", rec.getFieldAsInteger("Offset")),
						new FieldStruct("Hub", OperationContext.get().getSessionId().substring(0, 5)),
						new FieldStruct("Session", OperationContext.get().getSessionId()),
						new FieldStruct("Channel", rec.getFieldAsString("Channel"))
				);
				
				final DataStreamChannel chan = new DataStreamChannel(CmsService.this.channels.getId(), "Downloading " + fpath, binding);
				
				if (rec.hasField("Params"))
					chan.setParams(rec.getField("Params"));
				
				this.getResult().openRead(chan, new FuncCallback<RecordStruct>() {					
					@Override
					public void callback() {
						if (!request.hasErrors()) {
							// add the channel only after we know it is open
							CmsService.this.channels.addChannel(chan);		
							
							RecordStruct res = this.getResult();
							
							// always return path - if token was used to get file then here is the first chance to know the path/file we are collecting
							res.setField("FilePath", path);
							res.setField("Mime", MimeUtil.getMimeType(path));
							
							res.setField("BestEvidence", CmsService.this.bestEvidence);		
							res.setField("MinimumEvidence", CmsService.this.minEvidence);
							
							// get the binding info to return
							request.setResult(res);
						}
						
						request.complete();
					}
				});
			}
		});
		
		return null;
	}
	
	public void handleFinishDownload(TaskRun request, FileSystemDriver fs, CommonPath sectionpath) {
		RecordStruct rec = MessageUtil.bodyAsRecord(request);
		String fpath = rec.getFieldAsString("FilePath");
		
		CommonPath path = sectionpath.resolve(fpath);
		
		if ("Faliure".equals(rec.getFieldAsString("Status"))) {
			request.warn("File download incomplete or corrupt: " + path);
			
			if (!rec.isFieldEmpty("Note"))
				request.warn("File download note: " + rec.getFieldAsString("Note"));
			
			request.complete();
			return;
		}
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (request.hasErrors()) {
					request.complete();
					return;
				}
				
				RecordStruct evidinfo = rec.getFieldAsRecord("Evidence");
				
				String evidenceType = null;

				// pick best evidence if available, we really don't care if higher is available
				if (!evidinfo.isFieldEmpty(CmsService.this.bestEvidence)) { 
					evidenceType = CmsService.this.bestEvidence;
				}
				// else pick the highest available evidence given
				else {
					for (FieldStruct fld : evidinfo.getFields()) 
						evidenceType = CmsService.this.maxEvidence(fld.getName(), evidenceType);
				}

				final String selEvidenceType = evidenceType;
				
				final Consumer<Boolean> afterVerify = (pass) -> {
					if (pass) {
						if (CmsService.this.isSufficentEvidence(CmsService.this.bestEvidence, selEvidenceType))
							request.info("Verified best evidence for download: " + path);
						else if (CmsService.this.isSufficentEvidence(CmsService.this.minEvidence, selEvidenceType))
							request.info("Verified minimum evidence for download: " + path);
						else
							request.error("Verified evidence for download, however evidence is insuffcient: " + path);
					}
					else {
						request.error("File download incomplete or corrupt: " + path);
					}
					
					if (!rec.isFieldEmpty("Note"))
						request.info("File download note: " + rec.getFieldAsString("Note"));
					
					request.complete();
				};
				
				if ("Size".equals(selEvidenceType)) {
					Long src = evidinfo.getFieldAsInteger("Size");
					long dest = this.getResult().getSize();
					boolean match = (src == dest);
					
					if (match)
						request.info("File sizes match");
					else
						request.error("File sizes do not match");
					
					afterVerify.accept(match);
				}
				else if (StringUtil.isNotEmpty(selEvidenceType)) {
					this.getResult().hash(selEvidenceType, new FuncCallback<String>() {						
						@Override
						public void callback() {
							if (request.hasErrors()) {
								afterVerify.accept(false);
							}
							else {
								String src = evidinfo.getFieldAsString(selEvidenceType);
								String dest = this.getResult();
								boolean match = (src.equals(dest));
								
								if (match)
									request.info("File hashes match (" + selEvidenceType + ")");
								else
									request.error("File hashes do not match (" + selEvidenceType + ")");
								
								afterVerify.accept(match);
							}
						}
					});
				}
				else {
					request.error("Missing any form of evidence, supply at least size");
					
					afterVerify.accept(false);
				}
			}
		});
	}
	
	public boolean isSufficentEvidence(String lookingfor, String got) {
		if ("Size".equals(lookingfor)) 
			return ("Size".equals(got)  || "MD5".equals(got) || "SHA128".equals(got) || "SHA256".equals(got) || "SHA512".equals(got));
		
		if ("MD5".equals(lookingfor)) 
			return ("MD5".equals(got) || "SHA128".equals(got) || "SHA256".equals(got) || "SHA512".equals(got));
		
		if ("SHA128".equals(lookingfor)) 
			return ("SHA128".equals(got) || "SHA256".equals(got) || "SHA512".equals(got));
		
		if ("SHA256".equals(lookingfor)) 
			return ("SHA256".equals(got) || "SHA512".equals(got));
		
		if ("SHA512".equals(lookingfor)) 
			return ("SHA512".equals(got));
		
		return false;
	}
	
	public String maxEvidence(String lhs, String rhs) {
		if ("Size".equals(lhs) && ("MD5".equals(rhs) || "SHA128".equals(rhs) || "SHA256".equals(rhs) || "SHA512".equals(rhs)))
			return rhs;
		
		if ("MD5".equals(lhs) && ("SHA128".equals(rhs) || "SHA256".equals(rhs) || "SHA512".equals(rhs)))
			return rhs;
		
		if ("SHA128".equals(lhs) && ("SHA256".equals(rhs) || "SHA512".equals(rhs)))
			return rhs;
		
		if ("SHA256".equals(lhs) && "SHA512".equals(rhs))
			return rhs;
		
		return lhs;
	}
	
	public void watch(String op, IFileStoreFile file) {
		/* TODO review
		XElement settings = this.getLoader().getSettings();
		
		if (settings != null) {
	        for (XElement watch : settings.selectAll("Watch")) {
	        	String wpath = watch.getAttribute("FilePath");
	        	
	        	// if we are filtering on path make sure the path is a parent of the triggered path
	        	if (StringUtil.isNotEmpty(wpath)) {
	        		CommonPath wp = new CommonPath(wpath);
	        		
	        		if (!wp.isParent(file.path()))
	        			continue;
	        	}
        	
                String tasktag = op + "Task";
                
    			for (XElement task : watch.selectAll(tasktag)) {
    				String id = task.getAttribute("Id");
    				
    				if (StringUtil.isEmpty(id))
    					id = Task.nextTaskId();
    				
    				String title = task.getAttribute("Title");			        				
    				String script = task.getAttribute("Script");			        				
    				String params = task.selectFirstText("Params");
    				RecordStruct prec = null;
    				
    				if (StringUtil.isNotEmpty(params)) {
    					FuncResult<CompositeStruct> pres = CompositeParser.parseJson(params);
    					
    					if (pres.isNotEmptyResult())
    						prec = (RecordStruct) pres.getResult();
    				}
    				
    				if (prec == null) 
    					prec = new RecordStruct();
    				
			        prec.setField("File", file);
    				
    				if (script.startsWith("$"))
    					script = script.substring(1);
    				
    				Task t = new Task()
    					.withId(id)
    					.withTitle(title)
    					.withParams(prec)
    					.withRootContext();
    				
    				if (!ScriptWork.addScript(t, Paths.get(script))) {
    					Logger.error("Unable to run script for file watcher: " + watch.getAttribute("FilePath"));
    					continue;
    				}
    				
    				Hub.instance.getWorkPool().submit(t);
    			}
	        }
		}
		*/
	}
}
