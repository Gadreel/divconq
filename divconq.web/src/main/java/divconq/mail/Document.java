package divconq.mail;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.lang.reflect.Method;
import java.util.Map.Entry;

import divconq.filestore.CommonPath;
import divconq.lang.op.OperationContext;
import divconq.web.IOutputAdapter;
import divconq.web.dcui.Attributes;
import divconq.web.dcui.Element;
import divconq.web.dcui.HtmlUtil;
import divconq.web.dcui.Node;
import divconq.web.dcui.Nodes;
import divconq.web.dcui.ViewOutputAdapter;
import divconq.web.dcui.ViewTemplateAdapter;
import divconq.xml.XElement;
import w3.html.Div;

public class Document extends Div {
	protected XElement xel = null;
	//protected XElement sxel = null;		// for separate skeletons
	
    public Document() {
    	super();
	}
    
    public Document(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Document cp = new Document();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	Document nn = (Document)n;
    	nn.xel = this.xel;
    	//nn.sxel = this.sxel;
    }

	@Override
	public void parseElement(ViewOutputAdapter view, Nodes nodes, XElement xel) {
		this.xel = xel;
		
		Attributes attrs = HtmlUtil.initAttrs(xel);

		if (xel.hasAttribute("Skeleton")) {
			String tpath = xel.getAttribute("Skeleton");
			
			CommonPath pp = new CommonPath(tpath + ".dcuis.xml");		
			
			// TODO find file in email folders
			IOutputAdapter sf = view.getDomain().findFile(view.isPreview(), pp, null);
			
			if (sf instanceof ViewTemplateAdapter) {
				XElement layout = ((ViewTemplateAdapter)sf).getSource();			
				
				// copy all attributes over, unless they have been overridden
				for (Entry<String, String> attr : layout.getAttributes().entrySet())
					if (!xel.hasAttribute(attr.getKey()))
						xel.setAttribute(attr.getKey(), attr.getValue());
				
				// copy all child elements over
				for (XElement chel : layout.selectAll("*"))
					xel.add(chel);
			}
		}
		
		nodes.add(this);
		
		if (xel.hasAttribute("Title")) 
			view.addParams("PageTitle", xel.getRawAttribute("Title"));
		
		if (xel.hasAttribute("Id")) 
			view.addParams("PageId", xel.getRawAttribute("Id"));

		XElement skelel = xel.find("Skeleton");
		
		if (skelel != null) {
			Nodes skel = view.getDomain().parseXml(view, skelel);
			
			for (Entry<String, String> attr : skelel.getAttributes().entrySet())
				attrs.add(attr.getKey(), attr.getValue());
			
			this.myArguments = new Object[] { attrs, skel };
			
			// only set if Skeleton is present
			view.contenttemplate = nodes;
		}
		else {
			this.myArguments = new Object[] { attrs };
		}
		
		if (xel.find("TextSkeleton") != null) 
			view.textcontenttemplate = view.getDomain().parseXml(view, xel.find("TextSkeleton"));
		
		XElement vloader = xel.find("Script");
		
		if (vloader != null) {
			try (GroovyClassLoader loader = new GroovyClassLoader()) {
				Class<?> groovyClass = loader.parseClass(vloader.getText());
				
				for (Method m : groovyClass.getMethods()) {
					if (!m.getName().startsWith("run"))
						continue;
					
					view.viewloader = (GroovyObject) groovyClass.newInstance();
					break;
				}
			}
			catch (Exception x) {
				OperationContext.get().error("Unable to compile loader script!");
				OperationContext.get().error("Error: " + x);
			}
		}
	}
}

