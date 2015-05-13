package divconq.web.dcui;

import w3.html.Div;
import w3.html.H4;
import divconq.xml.XElement;

public class TitledSection extends MixedElement implements ICodeTag {
	public TitledSection() {
		super();
	}
	
	public TitledSection(Object... args) {
		super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		TitledSection cp = new TitledSection();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		if (xel.hasAttribute("class"))
			xel.setAttribute("class", xel.getAttribute("class") + " ui-corner-all custom-corners");
		else
			xel.setAttribute("class", "ui-corner-all custom-corners");
		
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));

		Div title = new Div(new Attributes("class", "ui-bar ui-bar-a"), new H4(xel.getAttribute("Title")));
		
		Div body = (xel.hasAttribute("id"))
				? new Div(new Attributes("id", xel.getAttribute("id") + "Body", "class", "ui-body ui-body-a"), view.getDomain().parseXml(view, xel))
				: new Div(new Attributes("class", "ui-body ui-body-a"), view.getDomain().parseXml(view, xel));
		
        this.myArguments = new Object[] { attrs, title, body };
		
		nodes.add(this);
	}
	
    @Override
	public void build(Object... args) {
	    super.build("div", true, args);
	}
}
