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
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;

import divconq.bus.Message;
import divconq.db.IDatabaseManager;
import divconq.db.ObjectFinalResult;
import divconq.db.ObjectResult;
import divconq.db.query.LoadRecordRequest;
import divconq.db.query.SelectDirectRequest;
import divconq.db.query.SelectFields;
import divconq.db.query.WhereEqual;
import divconq.db.query.WhereExpression;
import divconq.db.query.WhereField;
import divconq.db.update.DbRecordRequest;
import divconq.db.update.InsertRecordRequest;
import divconq.db.update.RetireRecordRequest;
import divconq.db.update.ReviveRecordRequest;
import divconq.db.update.UpdateRecordRequest;
import divconq.filestore.CommonPath;
import divconq.filestore.IFileStoreFile;
import divconq.filestore.local.FileSystemDriver;
import divconq.hub.Hub;
import divconq.io.LocalFileStore;
import divconq.lang.CountDownCallback;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.work.TaskRun;
import divconq.xml.XElement;
import divconq.xml.XText;
import divconq.xml.XmlReader;

public class Pages {
	/******************************************************************
	 * cms Skeletons
	 ******************************************************************/
	
	static public void handleSkeletons(TaskRun request, FileSystemDriver fs, String op, Message msg, CommonPath sectionpath) {
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
				.withConditionallySetFields(rec, "Title", "dcmTitle", "Path", "dcmPath");
			
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
				new SelectDirectRequest()
					.withTable("dcmSkeleton")
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcmTitle", "Title")
						.withField("dcmPath", "Path")), 
				new ObjectFinalResult(request));
			
			return ;
		}		
	}
	
	/******************************************************************
	 * cms Pages
	 ******************************************************************/
	
	static public void handlePages(TaskRun request, FileSystemDriver fs, String op, Message msg, CommonPath sectionpath) {
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
						.withField("dcmModified", "Modified")
						.withField("dcmPublished", "Published")
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
					
					Pages.loadPartDefinitions(fs, sectionpath, skelpath, new FuncCallback<ListStruct>() {						
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
					
					Pages.loadPartDefinitions(fs, sectionpath, skelpath, new FuncCallback<ListStruct>() {						
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
			boolean addOp = !rec.hasField("Id");
			
			DbRecordRequest req = addOp ? new InsertRecordRequest() : new UpdateRecordRequest();
			
			req
				.withTable("dcmPage")		
				.withConditionallySetFields(rec, "Title", "dcmTitle", "Path", "dcmPath", "Skeleton", "dcmSkeleton", "Keywords", "dcmKeywords", "Description", "dcmDescription")
				.withSetField("dcmModified", new DateTime());
			
			if (addOp) {
				req.withSetField("dcmAuthor", OperationContext.get().getUserContext().getUserId());
				req.withSetField("dcmCreated", new DateTime());
			}
			else {
				req.withId(rec.getFieldAsString("Id"));
			}
			
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
		
		if ("Publish".equals(op)) {
			String pid = rec.getFieldAsString("Id");
			
			WhereExpression wh = new WhereEqual(new WhereField("Id"), pid);
			
			Pages.compilePage(fs, sectionpath, wh, new OperationCallback() {
				@Override
				public void callback() {
					if (this.hasErrors()) {
						request.returnEmpty();
						return;
					}
					
					DbRecordRequest req = new UpdateRecordRequest()
						.withTable("dcmPage")		
						.withId(pid)
						.withSetField("dcmPublished", new DateTime());
				
					db.submit(req, new ObjectFinalResult(request));		
				}
			});
			
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
				new SelectDirectRequest()
					.withTable("dcmPage")
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcmTitle", "Title")
						.withField("dcmPath", "Path")), 
				new ObjectFinalResult(request));
			
			return ;
		}		
	}
	
	static public void compilePage(FileSystemDriver fs, CommonPath sectionpath, WhereExpression where, OperationCallback callback) {
		SelectDirectRequest req = new SelectDirectRequest()
			.withTable("dcmPage") 
			.withSelect(new SelectFields()
				.withField("Id")
				.withField("dcmTitle", "Title")
				.withField("dcmPath", "Path")
				.withField("dcmSkeleton", "Skeleton")
				.withForeignField("dcmSkeleton", "SkeletonPath", "dcmPath")
				.withField("dcmAuthor", "Author")
				.withForeignField("dcmAuthor", "AuthorName", "dcUsername")
				.withField("dcmCreated", "Created")
				.withField("dcmModified", "Modified")
				.withField("dcmModified", "Modified")
				.withField("dcmPublished", "Published")
				.withField("dcmDescription", "Description")
				.withField("dcmPartContent", "PartContent", null, true)
				.withField("dcmPartAttributes", "PartAttributes", null, true))
			.withWhere(where);
		
		Hub.instance.getDatabase().submit(req, new ObjectResult() {
			@Override
			public void process(CompositeStruct result) {
				if (this.hasErrors()) {
					callback.complete();
					return;
				}

				ListStruct pages = (ListStruct) result;
				
				CountDownCallback cdcb = new CountDownCallback(pages.getSize(), new OperationCallback() {					
					@Override
					public void callback() {
						LocalFileStore pubfs = Hub.instance.getPublicFileStore();		
						
						// TODO different for previews?
						pubfs.fireEvent("dcw/" + OperationContext.get().getDomain().getAlias() + "/www/all", false);
						
						callback.complete();
					}
				});
				
				if (cdcb.value() == 0) {
					callback.complete();
					return;
				}
				
				for (Struct ps : pages.getItems()) {
					RecordStruct page = (RecordStruct) ps;
					
					XElement compiled = new XElement("dcui");
					
					compiled.setAttribute("Title", page.getFieldAsString("Title"));				
					compiled.setAttribute("Skeleton", page.getFieldAsString("SkeletonPath")); 
					compiled.setAttribute("Id", page.getFieldAsString("Id")); 
					
					ListStruct contents = page.getFieldAsList("PartContent");
					ListStruct attribs = page.getFieldAsList("PartAttributes");
					
					Map<String, XElement> parts = new HashMap<>();
					
					for (Struct cs : contents.getItems()) {
						RecordStruct content = (RecordStruct) cs;
						
						String sid = content.getFieldAsString("SubId");
						String data = content.getFieldAsString("Data");
						
						parts.put(sid, new XElement("PagePart", new XText(true, data)));
					}
					
					for (Struct cs : attribs.getItems()) {
						RecordStruct attrib = (RecordStruct) cs;
						
						String sid = attrib.getFieldAsString("SubId");
						RecordStruct data = attrib.getFieldAsRecord("Data");
						
						XElement part = parts.get(sid);
						
						if (part == null) {
							part = new XElement("PagePart");
							parts.put(sid, part);
						}
	
						// make sure we default the locale
						// no -- part.setAttribute("Locale", "default");
						
						for (FieldStruct fname : data.getFields()) 
							part.setAttribute(fname.getName(), Struct.objectToString(fname.getValue()));
					}
		
					for (XElement p : parts.values()) 
						compiled.add(p);				
					
					/* TODO future, maybe grab def settings for annotating the page
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
					*/
					
					CommonPath path = sectionpath.resolve("/www" + page.getFieldAsString("Path") + ".dcui.xml");
					
					fs.getFileDetail(path, new FuncCallback<IFileStoreFile>() {			
						@Override
						public void callback() {
							if (callback.hasErrors()) {
								cdcb.countDown();
							}
							else {
								IFileStoreFile fi = this.getResult();
								
								fi.writeAllText(compiled.toString(true), new OperationCallback() {					
									@Override
									public void callback() {
										cdcb.countDown();
									}
								});
							}
						}
					});
				}
			}
		});
	}
	
	static public void loadPartDefinitions(FileSystemDriver fs, CommonPath sectionpath, String skelpath, FuncCallback<ListStruct> callback) {
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
							
							ListStruct temps = new ListStruct();
							
							for (XElement tel : pel.selectAll("Template")) {
								RecordStruct tdef = new RecordStruct();
								RecordStruct tdata = new RecordStruct();
								
								for (Entry<String, String> attr : tel.getAttributes().entrySet()) {
									if (StringUtil.isEmpty(attr.getValue()))
										continue;
											
									if ("For".equals(attr.getKey())) 
										tdef.setField("For", attr.getValue());
									else if ("Format".equals(attr.getKey())) 
										tdef.setField("Format", attr.getValue());
									else
										tdata.setField(attr.getKey(), attr.getValue());
								}
								
								XElement telc = tel.selectFirst("Content");
								
								if ((telc != null) && telc.hasText())
									tdef.setField("Content", telc.getText());
								
								XElement telh = tel.selectFirst("Help");
								
								if ((telh != null) && telh.hasText()) {
									tdef.setField("Help", telh.getText());
									
									/*
									try {
										String html = new Markdown4jProcessor().process(telh.getText());
										
										tdef.setField("Help", html);
									} 
									catch (IOException x) {
										System.out.println("md error: " + x);
									}
									*/
								}
								
								tdef.setField("OtherAttributes", tdata);
								
								temps.addItem(tdef);
							}
							
							pdef.setField("Templates", temps);
							pdef.setField("OtherAttributes", data);
							
							pdefs.addItem(pdef);
						}
						
						/*
							<Record>
								<Field Name="Title" Type="dcTinyString" />
								<Field Name="Format" Type="dcTinyString" />
								<Field Name="Content" Type="String" />
								<Field Name="Help" Type="String" />
								<Field Name="OtherAttributes" Type="Record" />
							</Record>
						*/
						
						
						callback.setResult(pdefs);
						callback.complete();
					}
				});
			}
		});
	}
	
}
