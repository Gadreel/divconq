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

import divconq.bus.Message;
import divconq.filestore.CommonPath;
import divconq.filestore.local.FileSystemDriver;
import divconq.work.TaskRun;

public class Blogs {
	/******************************************************************
	 * cms Blog
	 ******************************************************************/
	
	static public void handleBlog(TaskRun request, FileSystemDriver fs, String op, Message msg, CommonPath sectionpath) {
		/*
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if ("Load".equals(op)) {
			LoadRecordRequest req = new LoadRecordRequest()
				.withTable("dcmBlog")
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
						.withField("dcmPublished", "Published")
						.withField("dcmDescription", "Description")
						.withField("dcmKeywords", "Keywords")
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
						
					/* TODO
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
					
					Blogs.loadPartDefinitions(fs, sectionpath, skelpath, new FuncCallback<ListStruct>() {						
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
					* /
					
				}
			});
					
			return;
		}
		
		// for all blog settings
		if ("LoadSettings".equals(op)) {
			XElement settings = OperationContext.get().getDomain().getSettings();
			
			if (settings == null) {
				request.returnEmpty();
				return;
			}
			
			XElement blogsettings = settings.find("Blog");
			
			if (blogsettings == null) {
				request.returnEmpty();
				return;
			}
			
			RecordStruct resp = new RecordStruct();
			
			ListStruct chans = new ListStruct();
			resp.withField("Channels", chans);
			
			for (XElement chan : blogsettings.selectAll("Channel")) {
				if (!chan.hasAttribute("Name"))
					continue;
				
				RecordStruct chanrec = new RecordStruct();

				chanrec.withField("Name", chan.getAttribute("Name"));
				
				if (chan.hasAttribute("Path"))
					chanrec.withField("Path", chan.getAttribute("Path"));
				
				// TODO support parts and custom fields
				/*
						<Field Name="Parts">
							<List Type="dcmPartDefinition" />
						</Field>
				 * /
				
				for (XElement tag : blogsettings.selectAll("Tag")) {
					if (!tag.hasAttribute("Name"))
						continue;
					
					RecordStruct tagrec = new RecordStruct();

					tagrec.withField("Name", tag.getAttribute("Name"));
					
					if (tag.hasAttribute("Alias"))
						tagrec.withField("Alias", tag.getAttribute("Alias"));
					
					// TODO support parts and custom fields
					/*
							<Field Name="Parts">
								<List Type="dcmPartDefinition" />
							</Field>
					 * /
				}
			}
			
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
						
					/* TODO
					RecordStruct page = (RecordStruct) result;
					
					String skelpath = page.getFieldAsString("SkeletonPath"); 
					
					Blogs.loadPartDefinitions(fs, sectionpath, skelpath, new FuncCallback<ListStruct>() {						
						@Override
						public void callback() {
							if (this.hasErrors()) {
								request.complete();
								return;
							}
								
							request.returnValue(this.getResult());
						}
					});
					* /
				}
			});
					
			return;
		}
		
		if ("Update".equals(op)) {
			boolean addOp = !rec.hasField("Id");
			
			// TODO support Tags too, indexing dcmBlog as needed
			
			DbRecordRequest req = addOp ? new InsertRecordRequest() : new UpdateRecordRequest();
			
			req
				.withTable("dcmBlog")		
				.withConditionallySetFields(rec, "Title", "dcmTitle", "Alias", "dcmAlias", "Keywords", "dcmKeywords", "Description", "dcmDescription")
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
			
			db.submit(req, new ObjectFinalResult(request));		
			
			return ;
		}
		
		if ("Publish".equals(op)) {
			String pid = rec.getFieldAsString("Id");
			
			DbRecordRequest req = new UpdateRecordRequest()
				.withTable("dcmBlog")		
				.withId(pid)
				.withSetField("dcmPublished", new DateTime());
		
			db.submit(req, new ObjectFinalResult(request));		
			
			// TODO set ^dcmBlog, kill old if any
			
			return ;
		}
		
		if ("Retire".equals(op)) {
			// TODO remove from ^dcmBlog
			db.submit(new RetireRecordRequest("dcmBlog", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		if ("Revive".equals(op)) {
			// TODO return to ^dcmBlog 
			db.submit(new ReviveRecordRequest("dcmBlog", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
			return ;
		}
		
		// TODO site editors load based off dcmModified index, where as site visitors use dcmBlog

		// TODO better version for real blog listing service, this is just a "sampler" feature during testing
		if ("List".equals(op)) {
			db.submit(
				new SelectDirectRequest()
					.withTable("dcmBlog")
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcmTitle", "Title")
						.withField("dcmAlias", "Alias")
						.withField("dcmDescription", "Description")
						.withSubquery("dcmAuthor", "Author", new SelectFields()
							.withField("Id")
							.withField("dcFirstName", "FirstName") 
							.withField("dcLastName", "LastName") 
						)
						.withField("dcmPublished", "Published")
					), 
				new ObjectFinalResult(request));
			
			return ;
		}		
		*/
	}
		
}
