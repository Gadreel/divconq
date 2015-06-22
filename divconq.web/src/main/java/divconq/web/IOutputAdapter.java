package divconq.web;

import java.nio.file.Path;

import divconq.filestore.CommonPath;
import divconq.xml.XElement;

public interface IOutputAdapter {
	void init(WebDomain domain, Path file, CommonPath loc, boolean isPreview);
	Path getFilePath();
	CommonPath getLocationPath();
	XElement getSource();
	
	void execute(WebContext ctx) throws Exception;
}
