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
package divconq.web.dcui;

import java.nio.file.Path;

import divconq.filestore.CommonPath;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;
import divconq.util.IOUtil;
import divconq.web.IOutputAdapter;
import divconq.web.WebContext;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class ViewTemplateAdapter implements IOutputAdapter  {
	protected XElement source = null;
	protected CommonPath webpath = null;
	
	public XElement getSource() {
		return this.source;
	}
	
	public ViewTemplateAdapter(CommonPath webpath, Path filepath) {
		this.webpath = webpath;
		
		FuncResult<CharSequence> rres = IOUtil.readEntireFile(filepath);
		
		if (rres.hasErrors()) { 
			System.out.println("Error reading view: " + rres.getMessages());
			throw new IllegalArgumentException("Bad file path: cannot read");
		}
		
		String content = rres.getResult().toString();

		FuncResult<XElement> xres = XmlReader.parse(content, true);
		
		if (xres.hasErrors()) 
			System.out.println("Error parsing template: " + xres.getMessages());
		
		this.source = xres.getResult();
		
		if (this.source == null) 
			this.source = new XElement("dcuis",
					new XElement("Skeleton", 
							new XElement("h1", "Parse Error!!")
					)
			);
	}
	
	@Override
	public OperationResult execute(WebContext ctx) throws Exception {
		return new OperationResult();
	}
}
