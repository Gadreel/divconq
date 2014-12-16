package divconq.web.dcui;

import divconq.web.WebContext;

public interface IContentBuilder {
	Nodes getContent(WebContext ctx, ViewOutputAdapter info, Element parent) throws Exception;
}
