package divconq.db.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import divconq.db.DataRequest;
import divconq.lang.stem.IndexInfo;
import divconq.lang.stem.IndexInfo.StemEntry;
import divconq.lang.stem.IndexUtility;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;

public class FullTextSearchRequest extends DataRequest {
	// table,field
	protected Map<String,SourceInfo> sources = new HashMap<String, SourceInfo>();
	
	protected List<PhraseInfo> required = new ArrayList<PhraseInfo>(); 
	protected List<PhraseInfo> allowed = new ArrayList<PhraseInfo>(); 
	protected List<PhraseInfo> prohibited = new ArrayList<PhraseInfo>(); 
	
	/**
	 */
	public FullTextSearchRequest() {
		super("dcSearchText");
		
		RecordStruct params = new RecordStruct();
		
		this.parameters = params;
	}

	public void addSource(String table, String title, String body, String... extras) {
		SourceInfo si = new SourceInfo(title, body);
		
		for (String field : extras)
			si.addExtra(field);
		
		this.sources.put(table, si);
	}

	public void addRequired(String phrase) {
		this.required.add(new PhraseInfo(phrase, false));
	}

	public void addRequired(String phrase, boolean exact) {
		this.required.add(new PhraseInfo(phrase, exact));
	}

	public void addAllowed(String phrase) {
		this.allowed.add(new PhraseInfo(phrase, false));
	}

	public void addAllowed(String phrase, boolean exact) {
		this.allowed.add(new PhraseInfo(phrase, exact));
	}

	public void addProhibited(String phrase) {
		this.prohibited.add(new PhraseInfo(phrase, false));
	}

	public void addProhibited(String phrase, boolean exact) {
		this.prohibited.add(new PhraseInfo(phrase, exact));
	}

	public void filterField(String table, String field, String sid) {
		SourceInfo si = this.sources.get(table);
		
		if (si == null)
			return;
		
		si.addFilter(field, sid);
	}
	
	@Override
	public RecordStruct buildParams() {
		if (this.sources.size() > 0) {
			RecordStruct stables = new RecordStruct();
			RecordStruct ftables = new RecordStruct();
			
			for (String table : this.sources.keySet()) {
				SourceInfo sinfo = this.sources.get(table);
				
				RecordStruct sects = new RecordStruct();
				stables.setField(table, sects);

				if (StringUtil.isNotEmpty(sinfo.title))
					sects.setField("Title", sinfo.title);
				
				if (StringUtil.isNotEmpty(sinfo.body))
					sects.setField("Body", sinfo.body);

				if (sinfo.extras.size() > 0) {
					RecordStruct extras = new RecordStruct();
				
					sects.setField("Extras", extras);
					
					for (String extra : sinfo.extras)
						extras.setField(extra, 1);
				}
				
				RecordStruct filters = new RecordStruct();
				ftables.setField(table, filters);
				
				for (String fld : sinfo.filter.keySet()) {					
					RecordStruct sids = new RecordStruct();
					filters.setField(fld, sids);
					
					Set<String> slist = sinfo.filter.get(fld);
					
					for (String sid : slist)
						sids.setField(sid, 1);					
				}
			}
			
			((RecordStruct) this.parameters).setField("Sources", stables);
			((RecordStruct) this.parameters).setField("AllowedSids", ftables);
		}
		
		if (this.required.size() > 0) {
			RecordStruct words = new RecordStruct();
			
			for (PhraseInfo phrase : this.required) {
				boolean eonce = true;
				
				for (Entry<String, StemEntry> stem : phrase.info.entries.entrySet()) {
					RecordStruct sects = new RecordStruct();
					sects.setField("Term", 1);
					
					if (eonce && StringUtil.isNotEmpty(phrase.exact)) {
						sects.setField("Exact", phrase.exact);
						eonce = false;
					}

					String term = stem.getKey();
					
					words.setField(term, sects);
				}
			}
			
			((RecordStruct) this.parameters).setField("RequiredWords", words);
		}
		
		if (this.allowed.size() > 0) {
			RecordStruct words = new RecordStruct();
			
			for (PhraseInfo phrase : this.allowed) {
				boolean eonce = true;
				
				for (Entry<String, StemEntry> stem : phrase.info.entries.entrySet()) {
					RecordStruct sects = new RecordStruct();
					sects.setField("Term", 1);
					
					if (eonce && StringUtil.isNotEmpty(phrase.exact)) {
						sects.setField("Exact", phrase.exact);
						eonce = false;
					}

					String term = stem.getKey();
					
					words.setField(term, sects);
				}
			}
			
			((RecordStruct) this.parameters).setField("AllowedWords", words);
		}
		
		if (this.prohibited.size() > 0) {
			RecordStruct words = new RecordStruct();
			
			for (PhraseInfo phrase : this.prohibited) {
				boolean eonce = true;
				
				for (Entry<String, StemEntry> stem : phrase.info.entries.entrySet()) {
					RecordStruct sects = new RecordStruct();
					sects.setField("Term", 1);
					
					if (eonce && StringUtil.isNotEmpty(phrase.exact)) {
						sects.setField("Exact", phrase.exact);
						eonce = false;
					}

					String term = stem.getKey();
					
					words.setField(term, sects);
				}
			}
			
			((RecordStruct) this.parameters).setField("ProhibitedWords", words);
		}
		
		return (RecordStruct)this.parameters;
	}
	
	public class PhraseInfo {
		public String exact = null;
		public IndexInfo info = null;
		
		public PhraseInfo(String phrase, boolean exact) {
			if (exact)
				this.exact = phrase;
			
			this.info = IndexUtility.stemEnglishPhrase(phrase, 0);
		}
	}
	
	public class SourceInfo {
		protected String title = null;
		protected String body = null;
		protected Set<String> extras = new HashSet<String>();
		protected Map<String, Set<String>> filter = new HashMap<String, Set<String>>();

		public SourceInfo(String title, String body) {
			this.title = title;
			this.body = body;
		}
		
		public void addFilter(String field, String sid) {
			Set<String> f = this.filter.get(field);
			
			if (f == null) {
				f = new HashSet<String>();
				this.filter.put(field, f);
			}
			
			f.add(sid);
		}

		public void addExtra(String field) {
			this.extras.add(field);
		}
	}
}
