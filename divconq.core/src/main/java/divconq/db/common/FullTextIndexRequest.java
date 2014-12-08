package divconq.db.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import divconq.db.ReplicatedDataRequest;
import divconq.lang.stem.IndexInfo;
import divconq.lang.stem.IndexInfo.StemEntry;
import divconq.lang.stem.IndexUtility;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.scalar.StringStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XNode;
import divconq.xml.XText;

public class FullTextIndexRequest extends ReplicatedDataRequest {
	// field,sid,copy
	protected Map<String,Map<String,FieldIndexInfo>> fields = new HashMap<String, Map<String,FieldIndexInfo>>();
			
	/**
	 * @param table name 
	 * @param id of record
	 */
	public FullTextIndexRequest(String table, String id) {
		super("dcUpdateText");
		
		RecordStruct params = new RecordStruct();
		
		this.parameters = params;
		
		params.setField("Table", table);	
		params.setField("Id", id);
	}

	public FieldIndexInfo index(String field) {
		return this.index(field, "1");
	}

	public FieldIndexInfo index(String field, String sid) {
		Map<String, FieldIndexInfo> sids = this.fields.get(field);
		
		if (sids == null) {
			sids = new HashMap<String, FieldIndexInfo>();
			this.fields.put(field, sids);
		}
		
		FieldIndexInfo info = sids.get(sid);
		
		if (info == null) {
			info = new FieldIndexInfo();
			sids.put(sid, info);
		}
		
		return info;
	}
	
	public void quickIndex(String field, int bonus, String content) {
		this.quickIndex(field, bonus, "1", content);
	}
	
	public void quickIndex(String field, int bonus, String sid, String content) {
		FieldIndexInfo f = this.index(field, sid);
		
		f.add(bonus, content);
	}
	
	@Override
	public CompositeStruct buildParams() {
		if (this.fields.size() > 0) {
			RecordStruct pfields = new RecordStruct();
			
			for (String field : this.fields.keySet()) {
				Map<String, FieldIndexInfo> sids = this.fields.get(field);
				
				RecordStruct psids = new RecordStruct();
				pfields.setField(field, psids);
				
				for (String sid : sids.keySet()) {
					RecordStruct sects = new RecordStruct();
					psids.setField(sid, sects);
					
					StringStruct org = new StringStruct();
					ListStruct anal = new ListStruct();
					
					sects.setField("Original", org);
					sects.setField("Analyzed", anal);		// TODO this has changed from M, review

					FieldIndexInfo info = sids.get(sid);
					
					for (Entry<String, StemEntry> stem : info.info.entries.entrySet()) {
						StemEntry e = stem.getValue();

						int score = e.computeScore(); 
						String poslist = StringUtil.join(e.positions.toArray(new String[0]), ",");
						
						String c = "|" + stem.getKey() + ":" + score + ":" + poslist;
						
						anal.addItem(c);
					}
					
					// this assumes that content has large "words" stripped, see Index Utility
					String otext = info.info.content.toString();
					
					org.setValue(otext);
				}
			}
			
			((RecordStruct)this.parameters).setField("Fields", pfields);
		}
		
		return this.parameters;
	}
	
	public class FieldIndexInfo {
		protected IndexInfo info = new IndexInfo();

		public void add(int score, String content) {
			IndexUtility.stemEnglishPhraseAppend(content, score, this.info);
		}

		public void add(Map<String, Integer> bonuses, XElement html) {
			if (html != null)
				this.addHtml(bonuses, html, 1);
		}
		
		protected void addHtml(Map<String, Integer> bonuses, XElement html, int scorecontext) {
			String tag = html.getName();
			
			if ((bonuses != null) && bonuses.containsKey(tag))
				scorecontext = bonuses.get(tag);
			
			for (XNode child : html.getChildren()) {
				if (child instanceof XElement)
					this.addHtml(bonuses, (XElement)child, scorecontext);
				else if (child instanceof XText)
					IndexUtility.stemEnglishPhraseAppend(StringUtil.stripWhitespace(((XText)child).getValue()), scorecontext, this.info);
			}
		}
	}
}
