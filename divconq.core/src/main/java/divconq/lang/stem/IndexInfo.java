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
package divconq.lang.stem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import divconq.lang.StringBuilder32;

public class IndexInfo {
	public StringBuilder32 content = new StringBuilder32();
	public Map<String, IndexInfo.StemEntry> entries = new HashMap<String, IndexInfo.StemEntry>();
	
	public int getContentLength() {
		return this.content.length();
	}
	
	public void addContent(CharSequence content) {
		if (this.content.length() > 0)
			this.content.append(' ');
		
		this.content.append(content);		
	}
	
	public void add(String stem, int score, int pos) {
		IndexInfo.StemEntry e = this.entries.get(stem);
		
		if (e == null) {
			e = new StemEntry();
			this.entries.put(stem, e);
		}

		e.add(score, pos);
	}
	
	public class StemEntry {
		public List<Integer> positions = new ArrayList<Integer>();
		public int total = 0;
		public int max = 0;
		
		public void add(int score, int pos) {
			this.positions.add(pos);
			this.total += score;
			
			if (score > this.max)
				this.max = score;
		}
		
		public int computeScore() {
			return this.max + (this.total / this.positions.size());
		}
	}
}
