package divconq.cms.proc;

import org.joda.time.DateTime;

import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.update.DbRecordRequest;
import divconq.db.update.InsertRecordRequest;
import divconq.lang.op.OperationResult;
import divconq.session.Session;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.HashUtil;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;

public class NewThread implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		RecordStruct params = task.getParamsAsRecord();
		
		//System.out.println("NT 1 Got: " + params.toPrettyString());
		
		// TODO replicating
		// if (task.isReplicating()) 
		
		String title = params.getFieldAsString("Title");
		
		if (StringUtil.isNotEmpty(title)) 
			title = title.trim();
		
		/* this is a fine concept, but maybe not the right place for it - TODO
		String hash = null;
		
		if (StringUtil.isNotEmpty(title)) {
			title = title.trim();
			
			hash = HashUtil.getSha256(title);
			
			Object tid = db.firstInIndex("dcmThread", "dcmHash", hash, when, false);
			
			if (tid == null) {
				String titleb = title.toLowerCase();
				
				if (titleb.startsWith("re:")) {
					titleb = titleb.substring(3).trim();
					hash = HashUtil.getSha256(title);
				
					tid = db.firstInIndex("dcmThread", "dcmHash", hash, when, false);
				}
			}
			
			String threadid = tid.toString();
		}
		*/
		
		boolean trackTitle = params.getFieldAsBooleanOrFalse("TrackTitle");

		// TODO figure out how to send to future date (target date vs modified)
		
		String uuid = Session.nextUUId();
		String hash = HashUtil.getSha256((trackTitle && StringUtil.isNotEmpty(title)) ? title : uuid);
		DateTime now = new DateTime();
		DateTime target = params.getFieldAsDateTime("TargetDate");
		
		if (target == null)
			target = now;
		
		String originator = !params.isFieldEmpty("Originator") 
				? params.getFieldAsString("Originator") 
				: log.getContext().getUserContext().getUserId();
		
		DbRecordRequest req = new InsertRecordRequest()
			.withTable("dcmThread")
			.withUpdateField("dcmUuid", uuid)
			.withUpdateField("dcmHash", hash)
			.withUpdateField("dcmCreated", now)
			.withUpdateField("dcmModified", now)			
			.withUpdateField("dcmOriginator", originator)
			.withConditionallyUpdateFields(params, "Title", "dcmTitle", "EndDate", "dcmEndDate");

		if (target != null)
			req.withUpdateField("dcmTargetDate", target);
			
		ListStruct lbs = params.getFieldAsList("Labels");
		
		if ((lbs != null) && !lbs.isEmpty())
			req.withUpdateField("dcmLabels", "|" + StringUtil.join(lbs.toStringList(), "|") + "|");
			
		ListStruct parties = params.getFieldAsList("Parties");
		
		if (!parties.isEmpty()) {
			for (int i = 0; i < parties.getSize(); i++) {
				RecordStruct party = parties.getItemAsRecord(i);
				
				String pident = party.getFieldAsString("Party");
				
				req
					.withUpdateField("dcmParty", pident, pident)
					.withUpdateField("dcmFolder", pident, party.getFieldAsString("Folder"));
				
				lbs = party.getFieldAsList("PartyLabels");
				
				if ((lbs != null) && !lbs.isEmpty())
					req.withUpdateField("dcmPartyLabels", pident, "|" + StringUtil.join(lbs.toStringList(), "|") + "|");
			}
		}
		
		RecordStruct cnt = params.getFieldAsRecord("Content");
		String content = cnt.getFieldAsString("Content");
		String stamp = TimeUtil.stampFmt.print(new DateTime());
		
		req
			.withUpdateField("dcmContent", stamp, content)
			.withUpdateField("dcmContentHash", stamp, HashUtil.getSha256(content))
			.withUpdateField("dcmContentType", stamp, cnt.getFieldAsString("ContentType"));
	
		if (!cnt.isFieldEmpty("Source"))
			req.withUpdateField("dcmSource", stamp, cnt.getFieldAsString("Source"));
		
		if (!cnt.isFieldEmpty("Attributes"))
			req.withUpdateField("dcmAttributes", stamp, cnt.getFieldAsRecord("Attributes"));

		task.getDbm().submit(req, task.getResult());
	}
}
