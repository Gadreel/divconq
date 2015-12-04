package divconq.web.md.plugin;

import java.util.List;
import java.util.Map;

import divconq.web.md.Plugin;
import divconq.web.md.ProcessContext;
import divconq.xml.XElement;

public class TablePlugin extends Plugin {

	public TablePlugin() {
		super("table");
	}

	/*
	private int findSeparatorLine(int beginIndex, List<String> lines) {
		for(int i = beginIndex;i<lines.size();i++) {
			String line = lines.get(i);
			if(Pattern.matches("- ", line)) {
				return i;
			}
		}
		return -1;
	}
	*/
	
	@Override
	public void emit(ProcessContext ctx, XElement parent, List<String> lines,
			Map<String, String> params) {
		// TODO Auto-generated method stub
		
	}

	/* TODO
	@Override
	public void emit(StringBuilder out, List<String> lines, Map<String, String> params) {
		StringBuffer sb2 = new StringBuffer();
		String lparams = null;
		int ioh = findSeparatorLine(0, lines);
		String headerLine = null;
		String footerLine = null;
		if(ioh != -1) {
			headerLine = lines.get(ioh);
			int iof = findSeparatorLine(ioh, lines);
			if(iof != -1) {
				footerLine = lines.get(iof);				
			}
		}
	}
		*/

}
