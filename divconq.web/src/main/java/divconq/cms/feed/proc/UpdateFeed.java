package divconq.cms.feed.proc;

import java.util.concurrent.atomic.AtomicReference;

import org.joda.time.DateTime;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseResult;
import divconq.db.ObjectResult;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.TablesAdapter;
import divconq.db.query.LoadRecordRequest;
import divconq.db.query.SelectFields;
import divconq.db.update.DbRecordRequest;
import divconq.db.update.InsertRecordRequest;
import divconq.db.update.UpdateRecordRequest;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ICompositeBuilder;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;

public class UpdateFeed implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		String chann = params.getFieldAsString("Channel");
		String path = params.getFieldAsString("Path");
		Boolean edflag = params.getFieldAsBoolean("Editable");
		ListStruct atags = params.getFieldAsList("AuthorizationTags");
		ListStruct ctags = params.getFieldAsList("ContentTags");
		ListStruct fields = params.getFieldAsList("Fields");
		ListStruct prefields = params.getFieldAsList("PreviewFields");
		ListStruct parts = params.getFieldAsList("PartContent");
		ListStruct preparts = params.getFieldAsList("PreviewPartContent");
		
		// TODO replicating
		// if (task.isReplicating())
		
		// TODO support site
		
		TablesAdapter db = new TablesAdapter(conn, task); 
		
		BigDateTime when = BigDateTime.nowDateTime();
		Object oid = db.firstInIndex("dcmFeed", "dcmPath", path, when, false);
		
		AtomicReference<RecordStruct> oldvalues = new AtomicReference<>();
		AtomicReference<DateTime> updatepub = new AtomicReference<>();
		AtomicReference<DateTime> updateprepub = new AtomicReference<>();
		
		DatabaseResult fromUpdate = new ObjectResult() {
			@Override
			public void process(CompositeStruct result) {
				/*
				 * Update index
				 * 
				 * ^dcmFeedIndex(did, channel, publish datetime, id)=[content tags]
				 * 
				 * ^dcmFeedPreviewIndex(did, channel, publish datetime, id)=[content tags]
				 * 
				 */
				
				log.touch();
				
				String recid = null;
				
				if (oid != null) 
					recid = oid.toString();
				else if (result != null)
					recid = ((RecordStruct) result).getFieldAsString("Id");
				
				if (StringUtil.isEmpty(recid)) {
					log.error("Unable to update feed index - no id available");
					task.complete();
					return;
				}

				String did = task.getDomain();
				
				String ochan = null;
				DateTime opubtime = null;
				// TODO
				//DateTime oprepubtime = null;
				String otags = "|";
				
				if (oldvalues.get() != null) {
					ochan = oldvalues.get().getFieldAsString("Channel");
					opubtime = oldvalues.get().getFieldAsDateTime("Published");
					// TODO
					//oprepubtime = oldvalues.get().getFieldAsDateTime("PreviewPublished");
					
					ListStruct otlist = oldvalues.get().getFieldAsList("ContentTags");
					
					if (ctags != null) 
						otags = "|" + StringUtil.join(otlist.toStringList(), "|") + "|";
				}
				
				String nchan = chann;
				DateTime npubtime = updatepub.get();
				// TODO
				//DateTime nprepubtime = updateprepub.get();
				String ntags = "|";
				
				if (StringUtil.isEmpty(nchan))
					nchan = ochan;
				
				if (StringUtil.isEmpty(nchan)) {
					log.error("Unable to update feed index - no channel available");
					task.complete();
					return;
				}
				
				if (npubtime == null)
					npubtime = opubtime;
				
				// TODO
				//if (nprepubtime == null)
				//	nprepubtime = oprepubtime;
				
				if (ctags != null) {
					ntags = "|" + StringUtil.join(ctags.toStringList(), "|") + "|";
				}
				else {
					ntags = otags; 
				}
				
				//boolean diff1 = !ochan.equals(nchan) || !opubtime.equals(npubtime) || !oprepubtime.equals(nprepubtime);
				// TODO fix this so we update only if pubtime changes
				boolean diff1 = !nchan.equals(ochan) || ((opubtime == null) || (npubtime == null) || !opubtime.equals(npubtime));
				boolean diff2 = !ntags.equals(otags); 
				
				try {
					if (diff1 || diff2) {
						if (diff1 && StringUtil.isNotEmpty(ochan)) {
							if (opubtime != null) 
								conn.kill("dcmFeedIndex", did, ochan, conn.inverseTime(opubtime), recid);
							
							// TODO
							//if (oprepubtime != null) 
							//	conn.kill("dcmFeedPreviewIndex", did, ochan, conn.inverseTime(oprepubtime), recid);
						}
						
						if (npubtime != null) 
							conn.set("dcmFeedIndex", did, nchan, conn.inverseTime(npubtime), recid, ntags);
						
						// TODO
						//if (nprepubtime != null) 
						//	conn.set("dcmFeedPreviewIndex", did, nchan, conn.inverseTime(nprepubtime), recid, ntags);
					}
				}
				catch (Exception x) {
					log.error("Error updating feed index: " + x);
				}

				try {
					ICompositeBuilder out = task.getBuilder();
					
					out.startRecord();
					out.field("Id", recid);
					out.endRecord();
				}
				catch (Exception x) {
					log.error("Error writing record id: " + x);
				}
				
				task.complete();
			}
		};
		
		DatabaseResult fromLoad = new ObjectResult() {
			@Override
			public void process(CompositeStruct result) {
				if ((oid != null) && (result == null)) {
					log.error("Unable to update feed - id found but no record loaded");
					task.complete();
					return;
				}
				
				log.touch();
				
				oldvalues.set((RecordStruct) result);
				
				if (oid == null) {
					if (StringUtil.isEmpty(path) || StringUtil.isEmpty(chann)) {
						log.error("Unable to insert feed - missing Path or Channel");
						task.complete();
						return;
					}
				}
				
				DbRecordRequest req = (oid == null) ? new InsertRecordRequest() : new UpdateRecordRequest();
				
				req.withTable("dcmFeed");
				
				if (oid != null) {
					req.withId(oid.toString());
				}
				else {
					//req.withUpdateField("dcmUuid", uuid);
					req.withUpdateField("dcmImported", new DateTime());
				}
				
				if (chann != null)
					req.withUpdateField("dcmChannel", chann);
				
				if (path != null)
					req.withUpdateField("dcmPath", path);
				
				if (edflag != null)
					req.withUpdateField("dcmEditable", edflag);
				
				if (atags != null)
					req.withSetList("dcmAuthorizationTags", atags);
				
				if (ctags != null)
					req.withSetList("dcmContentTags", ctags);
				
				if (fields != null) {
					for (int i = 0; i < fields.getSize(); i++) {
						RecordStruct entry = fields.getItemAsRecord(i);
						String key = entry.getFieldAsString("Name") + "." + entry.getFieldAsString("Locale");
						req.withUpdateField("dcmFields", key, entry.getFieldAsString("Value"));
						
						if ("Published".equals(entry.getFieldAsString("Name"))) {
							DateTime pd = TimeUtil.parseDateTime(entry.getFieldAsString("Value")).withMillisOfSecond(0).withSecondOfMinute(0);
							updatepub.set(pd);							
							req.withUpdateField("dcmPublished", pd);
						}
						
						if ("AuthorUsername".equals(entry.getFieldAsString("Name"))) {
							Object userid = db.firstInIndex("dcUser", "dcUsername", entry.getFieldAsString("Value"), when, false);
							
							if (userid != null) {
								String uid = userid.toString();
								req.withUpdateField("dcmAuthor", uid, uid);
							}
						}
					}
				}
				
				if (prefields != null) {
					for (int i = 0; i < prefields.getSize(); i++) {
						RecordStruct entry = prefields.getItemAsRecord(i);
						String key = entry.getFieldAsString("Name") + "." + entry.getFieldAsString("Locale");
						req.withUpdateField("dcmPreviewFields", key, entry.getFieldAsString("Value"));
						
						if ("Published".equals(entry.getFieldAsString("Name"))) { 
							DateTime pd = TimeUtil.parseDateTime(entry.getFieldAsString("Value")).withMillisOfSecond(0).withSecondOfMinute(0);
							updateprepub.set(pd);							
							req.withUpdateField("dcmPreviewPublished", pd);
						}
						
						if ("AuthorUsername".equals(entry.getFieldAsString("Name"))) {
							Object userid = db.firstInIndex("dcUser", "dcUsername", entry.getFieldAsString("Value"), when, false);
							
							if (userid != null) {
								String uid = userid.toString();
								req.withUpdateField("dcmAuthor", uid, uid);
							}
						}
					}
				}
				
				if (parts != null) {
					for (int i = 0; i < parts.getSize(); i++) {
						RecordStruct entry = parts.getItemAsRecord(i);
						String key = entry.getFieldAsString("Name") + "." + entry.getFieldAsString("Locale");
						
						// TODO process different format types to their indexable state (html -> text, etc)
						// String fmt = entry.getFieldAsString("Format");
						
						req.withUpdateField("dcmPartContent", key, entry.getFieldAsString("Value"));
					}
				}
				
				if (preparts != null) {
					for (int i = 0; i < preparts.getSize(); i++) {
						RecordStruct entry = preparts.getItemAsRecord(i);
						String key = entry.getFieldAsString("Name") + "." + entry.getFieldAsString("Locale");
						
						// TODO process different format types to their indexable state (html -> text, etc)
						// String fmt = entry.getFieldAsString("Format");
						
						req.withUpdateField("dcmPreviewPartContent", key, entry.getFieldAsString("Value"));
					}
				}
				
				req.withUpdateField("dcmModified", new DateTime());
					
				task.getDbm().submit(req, fromUpdate);
			}
		};
		
		if (oid != null) {
			LoadRecordRequest lr1 = new LoadRecordRequest()
				.withTable("dcmFeed")
				.withId(oid.toString())
				.withSelect(new SelectFields()
					.withField("Id")
					.withField("dcmChannel", "Channel")
					.withField("dcmPublished", "Published")
					.withField("dcmPreviewPublished", "PreviewPublished")
					.withField("dcmContentTags", "ContentTags")
				);
			
			task.getDbm().submit(lr1, fromLoad);
		}
		else {
			fromLoad.complete();
		}
	}
}
