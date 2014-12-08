package divconq.db.common;

// TODO marked for deletion
public class DbUtility {
	/*
	
	static public void makePairs(String table, String field, ListStruct recs, ListStruct tags, String keyfield, String valuefield, OperationCallback rmsgs) {
				String rid = recs.getItemAsString(p);
				
				LoadRecordRequest req = new LoadRecordRequest()
					.withTable(table)
					.withId(rid)
					.withSelect(new SelectFields(new SelectField().withField(field)))
					.withCompact(false);

				// call load record
				db.submit(req, new ObjectResult() {				
					@Override
					public void process(CompositeStruct result) {
						// if load submitted but incurred errors
						if (this.hasErrors()) {
							cb.get().complete();		// trigger next round, no valid results
							return;
						}
						
						ListStruct utags = ((RecordStruct) result).getFieldAsList(field);
						
						DbRecordRequest ureq = new UpdateRecordRequest().withTable(table).withId(rid);
						
						// find which tags we should add to user
						
						if (tags != null) {
							for (int i = 0; i < tags.getSize(); i++) {
								boolean fnd = false;
								RecordStruct tag = tags.getItemAsRecord(i);
								String key = tag.getFieldAsString(keyfield);
								Struct value = tag.getField(valuefield);
								
								if (utags != null) {
									for (int ui = 0; ui < utags.getSize(); ui++) {
										RecordStruct utag = utags.getItemAsRecord(ui);
										String ukey = utag.getFieldAsString("Sid");
										Struct uvalue = utag.getField("Data");
										
										if (key.equals(ukey) && value.equals(uvalue)) {
											fnd = true;
											break;
										}
									}
									
									// add a missing tag in record that was in input list
									if (!fnd)
										ureq.withSetField(field, key, value);
								}
							}
						}
						
						// find which tags we should remove from user
						
						if (utags != null) {
							for (int ui = 0; ui < utags.getSize(); ui++) {
								boolean fnd = false;
								RecordStruct utag = utags.getItemAsRecord(ui);
								String ukey = utag.getFieldAsString("Sid");
								
								if (tags != null) {
									for (int i = 0; i < tags.getSize(); i++) {
										RecordStruct tag = tags.getItemAsRecord(i);
										String key = tag.getFieldAsString(keyfield);
										
										if (ukey.equals(key)) {
											fnd = true;
											break;
										}
									}
								}
								
								// remove if not in input list
								if (!fnd)
									ureq.withRetireField(field, ukey);
							}
						}
						
						// call update record with new tags
						db.submit(ureq, new ObjectResult() {				
							@Override
							public void process(CompositeStruct result) {
								cb.get().complete();		// trigger next round, doesn't matter if it worked or not
							}
						});
					}
				});
	}
		*/
}
