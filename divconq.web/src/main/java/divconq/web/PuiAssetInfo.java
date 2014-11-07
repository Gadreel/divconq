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
package divconq.web;

import java.nio.file.Path;
import java.util.Map.Entry;

import divconq.interchange.CommonPath;
import divconq.io.ByteBufWriter;
import divconq.lang.op.FuncResult;
import divconq.util.IOUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XNode;
import divconq.xml.XText;
import divconq.xml.XmlReader;

public class PuiAssetInfo extends AssetInfo {
	
	public PuiAssetInfo(WebContext ctx, CommonPath path, Path content, long when) {
		super(path, when);
		
		this.buffer = ByteBufWriter.createLargeHeap();

		this.buffer.writeLine("dc.pui.Loader.addPageDefinition('" + path + "', {");
		
		FuncResult<CharSequence> xmlres = IOUtil.readEntireFile(content);
		
		if (!xmlres.hasErrors()) {
			String xml = xmlres.getResult().toString();
			
			xml = ctx.expandMacros(xml);
			
			FuncResult<XElement> pres = XmlReader.parse(xml, true);
			
			if (pres.hasErrors())
				return;
			
			XElement root = pres.getResult();
			
			if (root.hasAttribute("Title")) {
				this.buffer.write("\tTitle: '");
				this.writeJsString(root.getAttribute("Title"));
				this.buffer.writeLine("',");
			}
			
			this.buffer.writeLine("\tLayout: [");
			
			XElement layout = root.selectFirst("Layout");
			
			this.writeLayoutChildren("\t", layout);
			
			this.buffer.writeLine();
			this.buffer.writeLine("\t],");
			
			this.buffer.writeLine("\tFunctions: {");
			
			boolean first = true;
			
			for (XElement func : root.selectAll("Function")) {
				if (!func.hasAttribute("Name"))
					continue;
				
				if (first)
					first = false;
				else
					this.buffer.writeLine(",");
				
				this.buffer.write("\t\t" + func.getAttribute("Name") + ": function(" + func.getAttribute("Params", "") + ") {");
				
				this.buffer.write(func.getText());
				
				this.buffer.write("\t\t}");
			}
			
			this.buffer.writeLine();
			
			this.buffer.writeLine("\t}");
		}
		
		this.buffer.writeLine("});");
		
		this.buffer.writeLine();
		
		this.buffer.writeLine("dc.pui.Loader.resumePageLoad();");
	}

	public void writeLayoutChildren(String tabs, XElement parent) {
		boolean first = true;
		
		for (XNode child : parent.getChildren()) {
			if (child instanceof XElement) {
				this.writeElement(tabs + "\t", first, (XElement)child);
				
				first = false;
			}
			else if (child instanceof XText) {
				String val = ((XText)child).getValue();
				
				if (StringUtil.isNotEmpty(val)) {
					if (!first)
						this.buffer.writeLine(",");
					
					this.buffer.write(tabs + "'");
					this.writeJsString(val);
					this.buffer.write("'");
					
					first = false;
				}
			}
		}
	}

	public void writeElement(String tabs, boolean first, XElement child) {
		if (!first)
			this.buffer.writeLine(",");
		
		this.buffer.writeLine(tabs + "{");
		
		this.buffer.write(tabs + "\tElement: '" + child.getName() + "'");
		
		boolean hasCoreAttrs = false;
		boolean missingFinalLine = false;
		
		for (Entry<String, String> entry : child.getAttributes().entrySet()) {
			if (Character.isLowerCase(entry.getKey().charAt(0))) {
				hasCoreAttrs = true;
				continue;
			}
			
			this.buffer.writeLine(",");
			
			this.buffer.write(tabs + "\t" + entry.getKey() + ": ");
			
			String v =  XNode.unquote(entry.getValue());
			
			if (v.startsWith("{") && v.endsWith("}"))
				this.buffer.write(v);
			else {
				this.buffer.write("'");
				this.writeJsString(v);
				this.buffer.write("'");
			}
			
			missingFinalLine = true;
		}
		
		if (hasCoreAttrs) {
			this.buffer.writeLine(",");
			
			this.buffer.writeLine(tabs + "\tAttributes: {");
			
			boolean firstattr = true;
			
			for (Entry<String, String> entry : child.getAttributes().entrySet()) {
				if (Character.isUpperCase(entry.getKey().charAt(0))) 
					continue;
				
				if (firstattr)
					firstattr = false;
				else
					this.buffer.writeLine(",");
				
				this.buffer.write(tabs + "\t\t'" + entry.getKey() + "': '");
				this.writeJsString( XNode.unquote(entry.getValue()));
				this.buffer.write("'");
			}
			
			this.buffer.writeLine();
			this.buffer.write(tabs + "\t}");
			
			missingFinalLine = true;
		}
		
		if (child.hasChildren()) {
			this.buffer.writeLine(",");
		
			this.buffer.writeLine(tabs + "\tChildren: [");
			
			this.writeLayoutChildren(tabs + "\t\t", child);
			
			this.buffer.writeLine();
			this.buffer.write(tabs + "\t]");
			
			missingFinalLine = true;
		}
		
		if (missingFinalLine)
			this.buffer.writeLine();
		
		this.buffer.write(tabs + "}");
	}
	
    public void writeJsString(String s) {
    	final int len = s.length();
    	
		for(int i = 0; i < len; i++) {
			char ch = s.charAt(i);
			
			switch(ch){
			case '\'':
				this.buffer.write("\\\'");
				break;
			case '\\':
				this.buffer.write("\\\\");
				break;
			case '\b':
				this.buffer.write("\\b");
				break;
			case '\f':
				this.buffer.write("\\f");
				break;
			case '\n':
				this.buffer.write("\\n");
				break;
			case '\r':
				this.buffer.write("\\r");
				break;
			case '\t':
				this.buffer.write("\\t");
				break;
			case '/':
				this.buffer.write("\\/");
				break;
			default:
                //Reference: http://www.unicode.org/versions/Unicode5.1.0/
				if((ch>='\u0000' && ch<='\u001F') || (ch>='\u007F' && ch<='\u009F') || (ch>='\u2000' && ch<='\u20FF')) {
					String ss=Integer.toHexString(ch);
					
					this.buffer.write("\\u");
					
					for(int k = 0; k < 4 - ss.length(); k++)
						this.buffer.writeChar('0');
					
					this.buffer.write(ss.toUpperCase());
				}
				else 
					this.buffer.writeChar(ch);
			}
		}//for
	}

	@Override
	public String getMime() {
		return "application/javascript";
	}
}
