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
package divconq.view;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class Element extends Node {
    
    class PartCollectInfo
    {
        public Object[] args = null;
        public boolean top = false;
    }

    protected String name = null;
    protected Map<String, String> attributes = new HashMap<String, String>();
    protected List<Node> children = new ArrayList<Node>();
    public Object[] myArguments = null;

    public String getName() {
        return this.name; 
    }
    
    public void setName(String value) {
        this.name = value; 
    }

    public Map<String, String> getAttributes() {
        return this.attributes; 
    }

    public Collection<Node> getChildren() {
        return this.children; 
    }

	public String getAttribute(String name) {
		return this.attributes.get(name);
	}
	
	public void addAttribute(String name, String value) {
		this.attributes.put(name, value);
	}

    public Element(Object... args) {
    	super();
        this.myArguments = args;
    	
    	/* not
    	if (args.length > 0)
    		this.build(args);
    		*/
    }

    @Override
    public void doBuild() {
        this.build(this.myArguments);
        this.myArguments = null;
    }

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	Element nn = (Element)n;
    	
    	nn.name = this.name;
    	
    	for (Node h : this.children) 
    		nn.children.add(h.deepCopy(this));
    	
    	for (String s : this.attributes.keySet()) 
    		nn.attributes.put(s, this.attributes.get(s));
    	
    	nn.myArguments = this.copyArgs(this.myArguments);
    }
    
    protected Object[] copyArgs(Object... args) {
    	Object[] results = new Object[args.length];
    	
        for (int i = 0; i < args.length; i++) {
        	Object obj = args[i];
        	
            if (obj == null) 
            	results[i] = obj;
            else if (obj instanceof CharSequence) 
            	results[i] = obj;
            else if (obj instanceof Boolean) 
            	results[i] = obj;
            else if (obj instanceof Nodes) 
            	results[i] = ((Nodes) obj).deepCopy();
            else if (obj instanceof Object[]) 
            	results[i] = this.copyArgs(obj);
            else if (obj instanceof Attributes) 
            	results[i] = ((Attributes) obj).deepCopy();
            else if (obj instanceof Node) 
            	results[i] = ((Node) obj).deepCopy(null);		// no parent yet
        }
        
        return results;
    }

    public void build(Object... args) {
        PartCollectInfo pci = new PartCollectInfo();
        pci.args = args;
        pci.top = true;
        collectParts(pci);
    }

    // changes to this, please also update copyArgs
    private void collectParts(PartCollectInfo info) {
    	if ((info == null) || (info.args == null))
    		return;
    	
        for (Object obj : info.args) {
            if (obj == null) 
            	continue;

            if (obj instanceof CharSequence) {
                if (info.top) {
                	this.name = obj.toString();
                }
                else {
                    LiteralText txt = new LiteralText(obj.toString());
                    txt.setParent(this);
                    this.children.add(txt);
                    txt.doBuild();
                }
            }
            else if (obj instanceof Boolean) {
            	this.setBlockIndent((Boolean)obj);
            }
            else if (obj instanceof FutureNodes) {
            	FuturePlaceholder placeholder = new FuturePlaceholder();
            	placeholder.setParent(this);
            	this.children.add(placeholder);
            	placeholder.incrementFuture();
            	
            	((FutureNodes)obj).setNotify(placeholder);
            }
            else if (obj instanceof Nodes) {
            	try {
	                for (Node nn : ((Nodes)obj).getList()) {
	                    nn.setParent(this);
	                    this.children.add(nn);
	                    nn.doBuild();
	                }
            	}
            	catch (Exception x) {
            		// TODO tracing - catches issues with FutureNodes
            	}
            }
            else if (obj instanceof Object[]) {
                PartCollectInfo pci = new PartCollectInfo();
                pci.args = (Object[])obj;
                pci.top = false;
                collectParts(pci);
            }
            else if (obj instanceof Attributes) {
                Attributes attrs = (Attributes)obj;

                while (attrs.hasMore()) {
                    String aname = attrs.pop();
                    String avalue = this.expandMacro(attrs.pop());
                    
                    if (avalue != null)
                    	this.attributes.put(aname, avalue);
                }
            }
            else if (obj instanceof Node) {
                Node nn = (Node)obj;
                nn.setParent(this);
                this.children.add(nn);
                nn.doBuild();
            }
        }
    }

    /*
    @Override
    public void awaitForFutures(final OperationCallback cb) {
    	super.awaitForFutures(new OperationCallback() {
			@Override
			public void callback() {
				final AtomicInteger counter = new AtomicInteger(Element.this.children.size());
				
		        for (Node node : Element.this.children) {
		        	node.awaitForFutures(new OperationCallback() {
						@Override
						public void callback() {
							int cnt = counter.decrementAndGet();
							
							if (cnt == 0)
								cb.callback();
						}
					});
		        }
			}
		});
    }
    */
    
    @Override
    public void stream(PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        if (this.name == null) 
        	return;

        String newindent = indent;

        if (this.getBlockIndent() || firstchild) {
            this.print(strm, indent, false, "<" + this.name);
            newindent = indent + "   ";
        }
        else {
        	this.print(strm, "", false, "<" + this.name);
        }

        for (String name : this.attributes.keySet()) {
            String ev = this.attributes.get(name);
            //this.print(strm, "", false, " " + name + "=\"" + divconq.xml.XNode.quote(ev) + "\"");
            this.print(strm, "", false, " " + name + "=\"" + ev + "\"");
        }

        if (this.children.size() > 0) {
           	this.print(strm, "", this.getBlockIndent(), ">");

            boolean fromon = fromblock;
            boolean lastblock = false;
            boolean firstch = this.getBlockIndent();   // only true once, and only if bi

            for (Node node : this.children) {
                if (node.getBlockIndent() && !lastblock && !fromon) 
                	this.print(strm, "", true, "");
                
                node.stream(strm, newindent, (firstch || lastblock), this.getBlockIndent());
                
                lastblock = node.getBlockIndent();
                firstch = false;
                fromon = false;
            }

            if (this.getBlockIndent()) {
                if (!lastblock) 
                	this.print(strm, "", true, "");
                
                this.print(strm, indent, true, "</" + this.name + "> ");
            }
            else {
            	this.print(strm, "", false, "</" + this.name + "> ");
            }
        }
        else {
            if (this.getBlockIndent()) {
            	this.print(strm, "", true, "></" + this.name + "> ");
            }
            else {
            	this.print(strm, "", false, "/> ");
            }
        }
    }
 }
