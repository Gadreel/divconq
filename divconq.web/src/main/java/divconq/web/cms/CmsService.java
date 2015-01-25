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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.joda.time.DateTime;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.db.IDatabaseManager;
import divconq.db.ObjectFinalResult;
import divconq.db.ObjectResult;
import divconq.db.query.LoadRecordRequest;
import divconq.db.query.SelectDirectRequest;
import divconq.db.query.SelectFields;
import divconq.db.query.WhereEqual;
import divconq.db.query.WhereField;
import divconq.db.update.InsertRecordRequest;
import divconq.db.update.RetireRecordRequest;
import divconq.db.update.ReviveRecordRequest;
import divconq.db.update.UpdateRecordRequest;
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
import divconq.mod.ExtensionBase;
import divconq.session.Session;
import divconq.session.DataStreamChannel;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
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
			// TODO interleave phantom - domain's phantom file system
			// Path wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/phantom/www", path);
			
			// look in the domain's static file system
			//Path wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/static/www", path);
			
			//if ("galleries".equals(path.getName(0)) || "files".equals(path.getName(0)))
			//		wpath = this.getWebFile(pubfs, "/dcw/" + this.alias + "/static/", path);
					
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
					? new CommonPath("/dcw/" + domain.getAlias() + "/static")
					: new CommonPath("/dcw/" + domain.getAlias() + "/static/files");
			
			if ("FileDetail".equals(op)) {
				this.handleFileDetail(request, this.fsd, sectionpath);
				return;
			}
			
			if ("DomainFileStore".equals(feature) && "ImportUIFile".equals(op)) {
				this.handleImportUIFile(request, this.fsd, sectionpath);
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
			
			CommonPath sectionpath = new CommonPath("/dcw/" + domain.getAlias() + "/static/galleries");
			
			if ("ListFiles".equals(op)) {
				this.handleListGallery(request, this.fsd, sectionpath);
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
				this.handleAddFolder(request, this.fsd, sectionpath);
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
		
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();
		CommonPath sectionpath = new CommonPath("/dcw/" + domain.getAlias() + "/static");
		
		// =========================================================
		//  store categories
		// =========================================================
		
		if ("Category".equals(feature)) {
			this.handleCategories(request, this.fsd, op, msg);
			return;

		}
		
		// =========================================================
		//  store products
		// =========================================================
		
		if ("Product".equals(feature)) {
			this.handleProducts(request, this.fsd, op, msg);
			return;

		}
		
		// =========================================================
		//  cms skeletons
		// =========================================================
		
		if ("Skeleton".equals(feature)) {
			this.handleSkeletons(request, this.fsd, op, msg, sectionpath);
			return;

		}
		
		// =========================================================
		//  cms Pages
		// =========================================================
		
		if ("Page".equals(feature)) {
			this.handlePages(request, this.fsd, op, msg, sectionpath);
			return;

		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	/******************************************************************
	 * Products
	 ******************************************************************/
	
	public void handleProducts(TaskRun request, FileSystemDriver fs, String op, Message msg) {
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if ("Load".equals(op)) {
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmProduct")
				.withId(rec.getFieldAsString("Id"))
				.withNow()
				.withSelect(new SelectFields()
					.withField("Id")
					.withField("dcmTitle", "Title")
					.withField("dcmAlias", "Alias")
					.withField("dcmSku", "Sku")
					.withField("dcmDescription", "Description")
					.withField("dcmCustomDisplayField", "CustomDisplayField", null, true)
					.withField("dcmCategory", "Category")
					.withField("dcmCategoryPosition", "CategoryPosition")
					.withField("dcmPrice", "Price")
					.withField("dcmShipAmount", "ShipAmount")
					.withField("dcmShipWeight", "ShipWeight")
					.withField("dcmShipFree", "ShipFree")
					.withField("dcmTaxFree", "TaxFree")
					.withField("dcmShowInStore", "ShowInStore")
					.withField("dcmTag", "Tags")
				);  
			
			db.submit(req, new ObjectFinalResult(request));
			
			return;
		}
		
		if ("Update".equals(op)) {
			UpdateRecordRequest req = new UpdateRecordRequest();
			
			req
				.withTable("dcmProduct")		
				.withId(rec.getFieldAsString("Id"))
				.withConditionallySetField(rec, "Title", "dcmTitle")
				.withConditionallySetField(rec, "Alias", "dcmAlias")
				.withConditionallySetField(rec, "Sku", "dcmSku")
				.withConditionallySetField(rec, "Description", "dcmDescription")
				.withConditionallySetField(rec, "Category", "dcmCategory")
				.withConditionallySetField(rec, "CategoryPosition", "dcmCategoryPosition")
				.withConditionallySetField(rec, "Price", "dcmPrice")
				.withConditionallySetField(rec, "ShipAmount", "dcmShipAmount")
				.withConditionallySetField(rec, "ShipWeight", "dcmShipWeight")
				.withConditionallySetField(rec, "ShipFree", "dcmShipFree")
				.withConditionallySetField(rec, "TaxFree", "dcmTaxFree")
				.withConditionallySetField(rec, "ShowInStore", "dcmShowInStore")
				.withConditionallyReplaceList(rec, "Tags", "dcmTag");
			
			db.submit(req, new ObjectFinalResult(request));
			
			return ;
		}
		
		if ("Add".equals(op)) {
			InsertRecordRequest req = new InsertRecordRequest();
			
			req
				.withTable("dcmProduct")		
				.withConditionallySetField(rec, "Title", "dcmTitle")
				.withConditionallySetField(rec, "Alias", "dcmAlias")
				.withConditionallySetField(rec, "Sku", "dcmSku")
				.withConditionallySetField(rec, "Description", "dcmDescription")
				.withConditionallySetField(rec, "Category", "dcmCategory")
				.withConditionallySetField(rec, "CategoryPosition", "dcmCategoryPosition")
				.withConditionallySetField(rec, "Price", "dcmPrice")
				.withConditionallySetField(rec, "ShipAmount", "dcmShipAmount")
				.withConditionallySetField(rec, "ShipWeight", "dcmShipWeight")
				.withConditionallySetField(rec, "ShipFree", "dcmShipFree")
				.withConditionallySetField(rec, "TaxFree", "dcmTaxFree")
				.withConditionallySetField(rec, "ShowInStore", "dcmShowInStore")
				.withConditionallyReplaceList(rec, "Tags", "dcmTag");
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (!this.hasErrors()) {
						DomainInfo domain = OperationContext.get().getUserContext().getDomain();
						
						String path = "/dcw/" + domain.getAlias() + "/static/galleries/store/product/" + rec.getFieldAsString("Alias");
						
						fs.addFolder(new CommonPath(path), new FuncCallback<IFileStoreFile>() {							
							@Override
							public void callback() {
								request.returnValue(result);
							}
						});
					}
					else {
						request.returnValue(result);
					}
				}
			});
			
			return;
		}
		
		if ("Retire".equals(op)) {
			db.submit(new RetireRecordRequest("dcmProduct", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Revive".equals(op)) {
			db.submit(new ReviveRecordRequest("dcmProduct", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Lookup".equals(op)) {
			db.submit(
					new SelectDirectRequest("dcmProduct", 
							new SelectFields()
								.withField("Id")
								.withField("dcmTitle", "Title")
								.withField("dcmAlias", "Alias")
								.withField("dcmSku", "Sku")
								.withField("dcmShowInStore", "ShowInStore")
								.withField("dcmPrice", "Price")
								.withField("dcmDescription", "Description"),
							new WhereEqual(new WhereField("dcmAlias"), rec.getFieldAsString("Alias"))
					), 
					new ObjectFinalResult(request));
			
			return ;
		}
		
		if ("CatList".equals(op)) {
			// TODO support category alias lookup too
			
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmCategory")
				.withId(rec.getFieldAsString("Id"))
				.withNow()
				.withSelect(new SelectFields()
					.withField("Id", "CategoryId")
					.withField("dcmTitle", "Category")
					.withField("dcmAlias", "CategoryAlias")
				);  
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (result == null) {
						request.error("Unable to load category record");
						request.complete();
						return;
					}
					
					db.submit(
							new SelectDirectRequest("dcmProduct", 
									new SelectFields()
										.withField("Id")
										.withField("dcmTitle", "Title")
										.withField("dcmAlias", "Alias")
										.withField("dcmSku", "Sku")
										.withField("dcmShowInStore", "ShowInStore")
										.withField("dcmPrice", "Price")
										.withField("dcmDescription", "Description"),
									new WhereEqual(new WhereField("dcmCategory"), rec.getFieldAsString("Id"))
							), 
							new ObjectResult() {
								@Override
								public void process(CompositeStruct result2) {
									if (result2 == null) {
										request.error("Unable to load product list");
										request.complete();
										return;
									}
									((RecordStruct)result).setField("Products", result2);
									
									request.returnValue(result);
								}
							});
				}
			});

			return ;
		}

		if ("List".equals(op)) {
			db.submit(
				new SelectDirectRequest("dcmProduct", new SelectFields()
					.withField("Id")
					.withField("dcmTitle", "Title")
					.withField("dcmAlias", "Alias")
					.withField("dcmSku", "Sku")
					.withField("dcmShowInStore", "ShowInStore")
					.withField("dcmPrice", "Price")
					.withField("dcmDescription", "Description")
				), 
				new ObjectFinalResult(request));
			
			return ;
		}		
	}
	
	/******************************************************************
	 * Categories
	 ******************************************************************/
	
	public void handleCategories(TaskRun request, FileSystemDriver fs, String op, Message msg) {
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if ("Load".equals(op)) {
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmCategory")
				.withId(rec.getFieldAsString("Id"))
				.withNow()
				.withSelect(new SelectFields()
					.withField("Id")
					.withField("dcmTitle", "Title")
					.withField("dcmAlias", "Alias")
					.withField("dcmMode", "Mode")
					.withField("dcmParent", "Parent")
					.withField("dcmDescription", "Description")
					.withField("dcmCustomDisplayField", "CustomDisplayField", null, true)
					.withField("dcmShipAmount", "ShipAmount")
				);  
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (result == null) {
						request.error("Unable to load category record");
						request.complete();
						return;
					}
					
					db.submit(
							new SelectDirectRequest("dcmCategory", 
								new SelectFields()
									.withField("Id")
									.withField("dcmTitle", "Title")
									.withField("dcmAlias", "Alias")
									.withField("dcmMode", "Mode")
									.withField("dcmParent", "Parent")
									.withField("dcmDescription", "Description"),
								new WhereEqual(new WhereField("dcmParent"), rec.getFieldAsString("Id"))
							), 
							new ObjectResult() {
								@Override
								public void process(CompositeStruct result2) {
									if (result2 == null) {
										request.error("Unable to load category record");
										request.complete();
										return;
									}
									
									((RecordStruct)result).setField("Children", result2);
									
									request.returnValue(result);
								}
							});					
				}
			});
			
			return;
		}
		
		if ("Update".equals(op)) {
			UpdateRecordRequest req = new UpdateRecordRequest();
			
			req
				.withTable("dcmCategory")		
				.withId(rec.getFieldAsString("Id"))
				.withConditionallySetField(rec, "Title", "dcmTitle")
				.withConditionallySetField(rec, "Alias", "dcmAlias")
				.withConditionallySetField(rec, "Mode", "dcmMode")
				.withConditionallySetField(rec, "Parent", "dcmParent")
				.withConditionallySetField(rec, "Description", "dcmDescription")
				.withConditionallySetField(rec, "ShipAmount", "dcmShipAmount");
			
			db.submit(req, new ObjectFinalResult(request));
			
			return ;
		}
		
		if ("Add".equals(op)) {
			InsertRecordRequest req = new InsertRecordRequest();
			
			req
				.withTable("dcmCategory")		
				.withConditionallySetField(rec, "Title", "dcmTitle")
				.withConditionallySetField(rec, "Alias", "dcmAlias")
				.withConditionallySetField(rec, "Mode", "dcmMode")
				.withConditionallySetField(rec, "Parent", "dcmParent")
				.withConditionallySetField(rec, "Description", "dcmDescription")
				.withConditionallySetField(rec, "ShipAmount", "dcmShipAmount");
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (!this.hasErrors()) {
						DomainInfo domain = OperationContext.get().getUserContext().getDomain();
						
						String path = "/dcw/" + domain.getAlias() + "/static/galleries/store/category/" + rec.getFieldAsString("Alias");
						
						fs.addFolder(new CommonPath(path), new FuncCallback<IFileStoreFile>() {							
							@Override
							public void callback() {
								request.returnValue(result);
								
								// TODO remove this - it is specific to a website - make general purpose setting instead
								/*
								if (!this.hasErrors()) {
									CommonPath metapath = this.getResult().resolvePath(new CommonPath("/.meta.json"));
									
									fs.getFileDetail(metapath, new FuncCallback<IFileStoreFile>() {
										@Override
										public void callback() {
											if (!this.hasErrors()) {
												RecordStruct meta = new RecordStruct(
														new FieldStruct("Variations", new ListStruct(
																new RecordStruct(
																		new FieldStruct("ExactWidth", 175),
																		new FieldStruct("ExactHeight", 150),
																		new FieldStruct("Alias", "full"),
																		new FieldStruct("Name", "Full Size")
																)
														))
												);
												
												this.getResult().writeAllText(meta.toPrettyString(), new OperationCallback() {										
													@Override
													public void callback() {
														request.returnValue(result);
													}
												});
											}
											else {
												request.returnValue(result);
											}
										}
									});
								}
								else {
									request.returnValue(result);
								}
								*/
							}
						});
					}
					else {
						request.returnValue(result);
					}
				}
			});
			
			return;
		}
		
		if ("Retire".equals(op)) {
			db.submit(new RetireRecordRequest("dcmCategory", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Revive".equals(op)) {
			db.submit(new ReviveRecordRequest("dcmCategory", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Lookup".equals(op)) {
			db.submit(
					new SelectDirectRequest("dcmCategory", 
							new SelectFields()
								.withField("Id")
								.withField("dcmTitle", "Title")
								.withField("dcmAlias", "Alias")
								.withField("dcmMode", "Mode")
								.withField("dcmParent", "Parent")
								.withField("dcmDescription", "Description"),
							new WhereEqual(new WhereField("dcmAlias"), rec.getFieldAsString("Alias"))
					), 
					new ObjectFinalResult(request));
			return ;
		}

		if ("List".equals(op)) {
			db.submit(
				new SelectDirectRequest("dcmCategory", new SelectFields()
					.withField("Id")
					.withField("dcmTitle", "Title")
					.withField("dcmAlias", "Alias")
					.withField("dcmMode", "Mode")
					.withField("dcmParent", "Parent")
					.withField("dcmDescription", "Description")
				), 
				new ObjectFinalResult(request));
			
			return ;
		}		
	}
	
	/******************************************************************
	 * cms Skeletons
	 ******************************************************************/
	
	public void handleSkeletons(TaskRun request, FileSystemDriver fs, String op, Message msg, CommonPath sectionpath) {
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if ("Load".equals(op)) {
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmSkeleton")
				.withId(rec.getFieldAsString("Id"))
				.withNow()
				.withSelect(new SelectFields()
					.withField("dcmTitle", "Title")
					.withField("dcmPath", "Path")
				);  
			
			db.submit(req, new ObjectFinalResult(request));
			
			return;
		}
		
		if ("Update".equals(op)) {
			UpdateRecordRequest req = new UpdateRecordRequest();
			
			req
				.withTable("dcmSkeleton")		
				.withId(rec.getFieldAsString("Id"))
				.withConditionallySetField(rec, "Title", "dcmTitle")
				.withConditionallySetField(rec, "Path", "dcmPath");
			
			db.submit(req, new ObjectFinalResult(request));
			
			return ;
		}
		
		if ("Retire".equals(op)) {
			db.submit(new RetireRecordRequest("dcmSkeleton", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Revive".equals(op)) {
			db.submit(new ReviveRecordRequest("dcmSkeleton", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}

		if ("List".equals(op)) {
			db.submit(
				new SelectDirectRequest("dcmSkeleton", new SelectFields()
					.withField("Id")
					.withField("dcmTitle", "Title")
					.withField("dcmPath", "Path")
				), 
				new ObjectFinalResult(request));
			
			return ;
		}		
	}
	
	/******************************************************************
	 * cms Pages
	 ******************************************************************/
	
	public void handlePages(TaskRun request, FileSystemDriver fs, String op, Message msg, CommonPath sectionpath) {
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if ("Load".equals(op)) {
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmPage")
				.withId(rec.getFieldAsString("Id"))
				.withNow()
				.withSelect(
					new SelectFields()
						.withField("dcmTitle", "Title")
						.withField("dcmPath", "Path")
						.withField("dcmSkeleton", "Skeleton")
						.withForeignField("dcmSkeleton", "SkeletonPath", "dcmPath")
						.withField("dcmAuthor", "Author")
						.withForeignField("dcmAuthor", "AuthorName", "dcUsername")
						.withField("dcmCreated", "Created")
						.withField("dcmModified", "Modified")
						.withField("dcmKeywords", "Keywords")
						.withField("dcmDescription", "Description")
						.withField("dcmPartContent", "PartContent", null, true)
						.withField("dcmPartAttributes", "PartAttributes", null, true)
				);  
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (this.hasErrors()) {
						request.complete();
						return;
					}
						
					RecordStruct page = (RecordStruct) result;
					
					String skelpath = page.getFieldAsString("SkeletonPath"); 
					page.removeField("SkeletonPath");
					
					ListStruct contents = page.getFieldAsList("PartContent");
					ListStruct attribs = page.getFieldAsList("PartAttributes");
					
					page.removeField("PartContent");
					page.removeField("PartAttributes");
					
					Map<String, RecordStruct> parts = new HashMap<>();
					
					for (Struct cs : contents.getItems()) {
						RecordStruct content = (RecordStruct) cs;
						
						String sid = content.getFieldAsString("SubId");
						String data = content.getFieldAsString("Data");
						
						RecordStruct part = new RecordStruct(
								//new FieldStruct("SubId", sid),
								new FieldStruct("Content", data)
						);
						
						parts.put(sid, part);
					}
					
					for (Struct cs : attribs.getItems()) {
						RecordStruct attrib = (RecordStruct) cs;
						
						String sid = attrib.getFieldAsString("SubId");
						RecordStruct data = attrib.getFieldAsRecord("Data");
						
						RecordStruct part = parts.get(sid);
						
						if (part == null) {
							part = new RecordStruct(
								//new FieldStruct("SubId", sid),
								new FieldStruct("Content", null)
							);
						
							parts.put(sid, part);
						}
						
						if (!data.isFieldEmpty("For")) { 
							part.setField("For", data.getField("For"));
							data.removeField("For");
						}
						
						if (!data.isFieldEmpty("Format")) { 
							part.setField("Format", data.getField("Format"));
							data.removeField("Format");
						}
						
						if (!data.isFieldEmpty("Locale")) { 
							part.setField("Locale", data.getField("Locale"));
							data.removeField("Locale");
						}
						else {
							part.setField("Locale", "default");
						}
						
						part.setField("OtherAttributes", data);
					}
					
					page.setField("Parts", new ListStruct(parts.values()));

					if (!rec.getFieldAsBooleanOrFalse("ScanDefinitions")) {
						request.returnValue(result);
						return;
					}
					
					CmsService.this.loadPartDefinitions(fs, sectionpath, skelpath, new FuncCallback<ListStruct>() {						
						@Override
						public void callback() {
							if (this.hasErrors()) {
								request.complete();
								return;
							}
								
							page.setField("PartDefinitions", this.getResult());
							request.returnValue(result);
						}
					});
					
				}
			});
					
			return;
		}
		
		if ("LoadPartDefinitions".equals(op)) {
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmSkeleton")
				.withId(rec.getFieldAsString("Id"))
				.withNow()
				.withSelect(
					new SelectFields()
						.withField("dcmTitle", "Title")
						.withField("dcmPath", "SkeletonPath")
				);  
			
			db.submit(req, new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (this.hasErrors()) {
						request.complete();
						return;
					}
						
					RecordStruct page = (RecordStruct) result;
					
					String skelpath = page.getFieldAsString("SkeletonPath"); 
					
					CmsService.this.loadPartDefinitions(fs, sectionpath, skelpath, new FuncCallback<ListStruct>() {						
						@Override
						public void callback() {
							if (this.hasErrors()) {
								request.complete();
								return;
							}
								
							request.returnValue(this.getResult());
						}
					});
					
				}
			});
					
			return;
		}
		
		if ("Update".equals(op)) {
			UpdateRecordRequest req = new UpdateRecordRequest();
			
			req
				.withTable("dcmPage")		
				.withId(rec.getFieldAsString("Id"))
				.withConditionallySetField(rec, "Title", "dcmTitle")
				.withConditionallySetField(rec, "Path", "dcmPath")
				.withConditionallySetField(rec, "Skeleton", "dcmSkeleton")
				.withConditionallySetField(rec, "Keywords", "dcmKeywords")
				.withConditionallySetField(rec, "Description", "dcmDescription")
				.withSetField("dcmModified", new DateTime());

			ListStruct upparts = rec.getFieldAsList("UpdateParts");
			
			if (upparts != null) {
				for (Struct ups : upparts.getItems()) {
					RecordStruct uppart = (RecordStruct) ups;
					
					String subid = uppart.getFieldAsString("For") + "." + uppart.getFieldAsString("Locale");  
					
					req.withSetField("dcmPartContent", subid, uppart.getFieldAsString("Content"));
					
					RecordStruct other = uppart.getFieldAsRecord("OtherAttributes");
					
					if (other == null)
						other = new RecordStruct();
					
					other.setField("Format", uppart.getFieldAsString("Format"));
					other.setField("For", uppart.getFieldAsString("For"));
					other.setField("Locale", uppart.getFieldAsString("Locale"));
					
					req.withSetField("dcmPartAttributes", subid, other);
				}
			}			

			ListStruct retparts = rec.getFieldAsList("RetireParts");
			
			if (retparts != null) {
				for (Struct ups : retparts.getItems()) {
					RecordStruct uppart = (RecordStruct) ups;
					
					String subid = uppart.getFieldAsString("For") + "." + uppart.getFieldAsString("Locale");  
					
					req.withRetireField("dcmPartContent", subid);
					req.withRetireField("dcmPartAttributes", subid);
				}
			}			
			
			db.submit(req, new ObjectFinalResult(request));		// TODO recompile after update
			
			return ;
		}
		
		if ("Retire".equals(op)) {
			db.submit(new RetireRecordRequest("dcmPage", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Revive".equals(op)) {
			db.submit(new ReviveRecordRequest("dcmPage", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}

		if ("List".equals(op)) {
			db.submit(
				new SelectDirectRequest("dcmPage", new SelectFields()
					.withField("Id")
					.withField("dcmTitle", "Title")
					.withField("dcmPath", "Path")
				), 
				new ObjectFinalResult(request));
			
			return ;
		}		
	}
	
	public void loadPartDefinitions(FileSystemDriver fs, CommonPath sectionpath, String skelpath, FuncCallback<ListStruct> callback) {
		CommonPath path = sectionpath.resolve("/www" + skelpath + ".dcuis.xml");
		
		fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
			@Override
			public void callback() {
				if (callback.hasErrors()) {
					callback.complete();
					return;
				}
				
				IFileStoreFile fi = this.getResult();
				
				if (!fi.exists()) {
					callback.error("Skeleton file does not exist");
					callback.complete();
					return;
				}
				
				fi.readAllText(new FuncCallback<String>() {					
					@Override
					public void callback() {
						if (this.hasErrors()) {
							callback.error("Unable to read file skeleton file");
							callback.complete();
							return;
						}
						
						String text = this.getResult();

						FuncResult<XElement> xres = XmlReader.parse(text, true);
						
						if (xres.hasErrors()) {
							System.out.println("Error parsing skeleton file: " + xres.getMessages());
							callback.complete();
							return;
						}
						
						XElement root = xres.getResult();
						ListStruct pdefs = new ListStruct();
						
						for (XElement pel : root.selectAll("PagePartDef")) {
							RecordStruct pdef = new RecordStruct();
							RecordStruct data = new RecordStruct();
							
							for (Entry<String, String> attr : pel.getAttributes().entrySet()) {
								if (StringUtil.isEmpty(attr.getValue()))
									continue;
										
								if ("For".equals(attr.getKey())) 
									pdef.setField("For", attr.getValue());
								else if ("Title".equals(attr.getKey())) 
									pdef.setField("Title", attr.getValue());
								else if ("Editor".equals(attr.getKey())) 
									pdef.setField("Editor", attr.getValue());
								else if ("AuthTags".equals(attr.getKey())) 
									pdef.setField("AuthTags", new ListStruct((Object[])attr.getValue().split(",")));
								else
									data.setField(attr.getKey(), attr.getValue());
							}
							
							if (pel.hasText())
								pdef.setField("Content", pel.getText());
							
							pdef.setField("OtherAttributes", data);
							
							pdefs.addItem(pdef);
						}
						
						callback.setResult(pdefs);
						callback.complete();
					}
				});
			}
		});
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
					if (".meta.json".equals(file.getName())) {
						cdcb.increment();
						
						file.readAllText(new FuncCallback<String>() {							
							@Override
							public void callback() {
								if (this.isNotEmptyResult())
									info.setField("Settings", this.getResult());
								
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
		
		fs.getFileDetail(path.resolvePeer(".meta.json"), new FuncCallback<IFileStoreFile>() {			
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
						if (this.isNotEmptyResult())
							info.setField("GallerySettings", Struct.objectToComposite(this.getResult()));
						
						cdcb.countDown();
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
		
		fs.getFileDetail(path.resolve(".meta.json"), new FuncCallback<IFileStoreFile>() {			
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
						
						String spath = path.subpath(4).toString();						
						String fspath = spath.substring(0, spath.length() - 10);
						
						if (fpath.endsWith(".dcuis.xml")) {
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
							
							System.out.println("Importing page: " + root.getAttribute("Title") + " " + root.getAttribute("Skeleton"));

							// only support external skeletons
							if (!root.hasAttribute("Skeleton")) {
								request.error("Missing skeleton path, skeleton must be external");
								request.complete();
								return;
							}

							String tpath = root.getAttribute("Skeleton");
							
							Hub.instance.getDatabase().submit(
								new SelectDirectRequest("dcmSkeleton", 
									new SelectFields().withField("Id"),
									new WhereEqual(new WhereField("dcmPath"), tpath)
								), 
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
												.withSetField("dcmPath", fspath);
											
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
