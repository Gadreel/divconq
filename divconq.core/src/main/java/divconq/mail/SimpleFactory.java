package divconq.mail;

import divconq.bus.Message;
import divconq.db.Constants;
import divconq.db.DataRequest;
import divconq.db.ObjectResult;
import divconq.db.ReplicatedDataRequest;
import divconq.db.query.CollectorField;
import divconq.db.query.SelectDirectRequest;
import divconq.db.query.SelectFields;
import divconq.hub.Hub;
import divconq.lang.op.OperationCallback;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.work.Task;

public class SimpleFactory {

	// from = uid or null for root uid
	// to = array of uid - also direct party if starts with / so can include pool
	// msgtype = DGAType such as HiredApprentice
	// attrs = attributes for storing in content but also for params to message builder
	// smtpnow = t/f to send the email to the recipients now - only sends to recipients
	//	           in the "to" list that do not start with / (only direct uids)
	// cb = optional callback after message has been submitted to email sending service
	//	      or is done building if not sendnow

	static public void simpleBuildThread(String from, ListStruct to, String msgtype, RecordStruct attrs, boolean smtpnow, String namefield, OperationCallback cb) {
		String ffrom = StringUtil.isEmpty(from) ? Constants.DB_GLOBAL_ROOT_USER : from;
		
		RecordStruct fattrs = (attrs == null) ? new RecordStruct() : attrs;
		
		fattrs
			.withField("From", ffrom)
			.withField("To", to)
			.withField("MessageType", msgtype);
		
		RecordStruct bparams = new RecordStruct()
			.withField("Path", "/" + msgtype + ".dcm.xml")
			.withField("Params", fattrs);
		
		Message msg = new Message("dcmEmailBuilder", "Message", "Build", bparams);
		
		Hub.instance.getBus().sendMessage(msg, new divconq.bus.ServiceResult() {
			@Override
			public void callback() {
				if (!this.hasErrors()) {
					RecordStruct bresp = this.getBodyAsRec();
					
					fattrs.withField("TextBody", bresp.getFieldAsString("TextBody"));
					
					RecordStruct content = new RecordStruct();

					content.withField("Content", bresp.getFieldAsString("Body"));
					content.withField("ContentType", "HTML"); 
					content.withField("Attributes", fattrs); 

					ListStruct parties = new ListStruct();
					
					for (int i = 0; i < to.getSize(); i++) {
						String titem = to.getItemAsString(i);
					
						if (StringUtil.isEmpty(titem))
							continue;
						
						RecordStruct party = new RecordStruct();
	
						if (titem.startsWith("/"))
							party.withField("Party", titem);
						else
							party.withField("Party", "/Usr/" + titem);
						
						party.withField("Folder", "/InBox");
						
						parties.addItem(party);
					}
					
					RecordStruct thread = new RecordStruct()
						.withField("Title", bresp.getFieldAsString("Subject"))
						.withField("Originator", from)
						.withField("Parties", parties)
						.withField("Content", content);
						
					if (!smtpnow)
						thread.withField("Labels", new ListStruct("BatchNotice"));

					// don't bother checking if it worked in our response to service
					DataRequest req3b = new ReplicatedDataRequest("dcmThreadNewThread")
						.withParams(thread);

					Hub.instance.getDatabase().submit(req3b, new ObjectResult() {
						@Override
						public void process(CompositeStruct result3b) {
							if (!this.hasErrors() && smtpnow) {
								SimpleFactory.simpleQueueMessage(ffrom, to, namefield, bresp, cb);
							}
							else if (cb != null)
								cb.complete();
						}
					});
				}
				else if (cb != null)
					cb.complete();
			}
		});	
	}
	
	static public void simpleQueueMessage(String from, ListStruct to, String namefield, RecordStruct content, OperationCallback cb) {
		String ffrom = StringUtil.isEmpty(from) ? Constants.DB_GLOBAL_ROOT_USER : from;
		
		ListStruct people = new ListStruct(ffrom);
		
		for (int i = 0; i < to.getSize(); i++) {
			String titem = to.getItemAsString(i);

			// don't notify pools, just people
			if (StringUtil.isEmpty(titem) || titem.startsWith("/"))
				continue;
			
			people.addItem(titem);
		}
		
		// there is no one to send to (only a from)
		if (people.getSize() == 1) {
			if (cb != null)
				cb.complete();
			
			return;
		}
		
		SelectFields sflds = new SelectFields()
			.withField("Id")
			.withField("dcFirstName", "FirstName")
			.withField("dcLastName", "LastName")
			.withField("dcEmail", "Email");
		
		if (StringUtil.isNotEmpty(namefield))
			sflds.withField(namefield);
		
		SelectDirectRequest req = new SelectDirectRequest() 
			.withTable("dcUser")
			.withSelect(sflds)
			.withCollector(new CollectorField("Id").withValues(people));
	
		Hub.instance.getDatabase().submit(
			req,
			new ObjectResult() {
				@Override
				public void process(CompositeStruct result) {
					if (!this.hasErrors()) {
						String fromfmt = ""; 
						String tofmt = ""; 
						
						ListStruct peeps = (ListStruct) result;
						
						for (int i2 = 0; i2 < peeps.getSize(); i2++) {
							RecordStruct pitem = peeps.getItemAsRecord(i2);
							
							if (pitem.isFieldEmpty("Email"))
								continue;
							
							String pid = pitem.getFieldAsString("Id");

							String pfmt = StringUtil.isNotEmpty(namefield) 
									? pitem.getFieldAsString(namefield) 
									: pitem.getFieldAsString("FirstName") + " " + pitem.getFieldAsString("LastName");
							
							pfmt += " <" + pitem.getFieldAsString("Email") + ">; ";
							
							if (from.equals(pid)) 
								fromfmt = pfmt;
							
							// pid can be in To and From, check both
							for (int i = 0; i < to.getSize(); i++) {
								String titem = to.getItemAsString(i);

								// don't notify pools, just people
								if (StringUtil.isEmpty(titem) || titem.startsWith("/"))
									continue;
								
								if (titem.equals(pid)) 
									tofmt += pfmt;
							}							
						}
						
						content
							.withField("From", fromfmt)
							.withField("To", tofmt);
						
						Task stask = MailTaskFactory.createSendEmailTask(content);
						
						MailTaskFactory.sendEmail(stask);
					}
					
					if (cb != null)
						cb.complete();
				}								
			});
	}
	
}
