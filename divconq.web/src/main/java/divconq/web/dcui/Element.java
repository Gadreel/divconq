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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import divconq.util.ArrayUtil;
import divconq.web.WebContext;
import divconq.xml.XNode;

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

	public boolean hasAttribute(String name) {
		return this.attributes.containsKey(name);
	}
	
	public void addAttribute(String name, String value) {
		this.attributes.put(name, value);
	}
    
    public void addArgs(Object... args) {
    	if ((myArguments == null) || (myArguments.length == 0)) 
    		this.myArguments = args;
    	else 
    		this.myArguments = (Object[]) ArrayUtil.addAll(this.myArguments, args);
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
    public void doBuild(WebContext ctx) {
        this.build(ctx, this.myArguments);
        this.myArguments = null;
    }

    public void build(WebContext ctx, Object... args) {
        PartCollectInfo pci = new PartCollectInfo();
        pci.args = args;
        pci.top = true;
        collectParts(ctx, pci);
    }

    // changes to this, please also update copyArgs
    private void collectParts(WebContext ctx, PartCollectInfo info) {
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
                    txt.doBuild(ctx);
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
            	
            	((FutureNodes)obj).setNotify(ctx, placeholder);
            }
            else if (obj instanceof Nodes) {
            	try {
	                for (Node nn : ((Nodes)obj).getList()) {
	                    nn.setParent(this);
	                    this.children.add(nn);
	                    nn.doBuild(ctx);
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
                collectParts(ctx, pci);
            }
            else if (obj instanceof Attributes) {
                Attributes attrs = (Attributes)obj;

                while (attrs.hasMore()) {
                    String aname = attrs.pop();
                    String avalue = this.expandMacro(ctx, attrs.pop());
                    
                    if (avalue != null)
                    	this.attributes.put(aname, avalue);
                }
            }
            else if (obj instanceof Node) {
                Node nn = (Node)obj;
                nn.setParent(this);
                this.children.add(nn);
                nn.doBuild(ctx);
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
	
	public void writeDynamicChildren(PrintStream buffer, String tabs) {
		boolean first = true;
		
		for (Node child : this.children) {
			if (child.writeDynamic(buffer, tabs + "\t", first)) 
				first = false;
		}
	}

	@Override
	public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
		if (!first)
			buffer.println(",");
		
		buffer.println(tabs + "{");
		
		buffer.print(tabs + "\tElement: '" + this.name + "'");
		
		boolean hasCoreAttrs = false;
		boolean missingFinalLine = false;
		
		for (Entry<String, String> entry : this.attributes.entrySet()) {
			if (Character.isLowerCase(entry.getKey().charAt(0))) {
				hasCoreAttrs = true;
				continue;
			}
			
			buffer.println(",");
			
			buffer.print(tabs + "\t" + entry.getKey() + ": ");
			
			String v =  XNode.unquote(entry.getValue());
			
			if (v == null) {
				buffer.print("null");
			}
			else if (v.startsWith("{") && v.endsWith("}"))
				buffer.print(v);
			else {
				buffer.print("'");
				Node.writeDynamicJsString(buffer, v);
				buffer.print("'");
			}
			
			missingFinalLine = true;
		}
		
		if (hasCoreAttrs) {
			buffer.println(",");
			
			buffer.println(tabs + "\tAttributes: {");
			
			boolean firstattr = true;
			
			for (Entry<String, String> entry : this.attributes.entrySet()) {
				if (Character.isUpperCase(entry.getKey().charAt(0))) 
					continue;
				
				if (firstattr)
					firstattr = false;
				else
					buffer.println(",");
				
				String v =  XNode.unquote(entry.getValue());
				
				if (v == null) {
					buffer.print(tabs + "\t\t'" + entry.getKey() + "': null");
				}
				else {
					buffer.print(tabs + "\t\t'" + entry.getKey() + "': '");
					Node.writeDynamicJsString(buffer, v);
					buffer.print("'");
				}
			}
			
			buffer.println();
			buffer.print(tabs + "\t}");
			
			missingFinalLine = true;
		}
		
		if (this.children.size() > 0) {
			buffer.println(",");
		
			buffer.println(tabs + "\tChildren: [");
			
			this.writeDynamicChildren(buffer, tabs + "\t\t");
			
			buffer.println();
			buffer.print(tabs + "\t]");
			
			missingFinalLine = true;
		}
		
		if (missingFinalLine)
			buffer.println();
		
		buffer.print(tabs + "}");
		
		return true;
	}
    
    @Override
    public void stream(WebContext ctx, PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
        if (this.name == null) 
        	return;

        String newindent = indent;

        if (this.getBlockIndent() || firstchild) {
            this.print(ctx, strm, indent, false, "<" + this.name);
            newindent = indent + "   ";
        }
        else {
        	this.print(ctx, strm, "", false, "<" + this.name);
        }

        for (String name : this.attributes.keySet()) {
            String ev = this.attributes.get(name);
            //this.print(strm, "", false, " " + name + "=\"" + divconq.xml.XNode.quote(ev) + "\"");
            this.print(ctx, strm, "", false, " " + name + "=\"" + ev + "\"");
        }

        if (this.children.size() > 0) {
           	this.print(ctx, strm, "", this.getBlockIndent(), ">");

            boolean fromon = fromblock;
            boolean lastblock = false;
            boolean firstch = this.getBlockIndent();   // only true once, and only if bi

            for (Node node : this.children) {
                if (node.getBlockIndent() && !lastblock && !fromon) 
                	this.print(ctx, strm, "", true, "");
                
                node.stream(ctx, strm, newindent, (firstch || lastblock), this.getBlockIndent());
                
                lastblock = node.getBlockIndent();
                firstch = false;
                fromon = false;
            }

            if (this.getBlockIndent()) {
                if (!lastblock) 
                	this.print(ctx, strm, "", true, "");
                
                this.print(ctx, strm, indent, true, "</" + this.name + "> ");
            }
            else {
            	this.print(ctx, strm, "", false, "</" + this.name + "> ");
            }
        }
        else {
            if (this.getBlockIndent()) {
            	this.print(ctx, strm, "", true, "></" + this.name + "> ");
            }
            else {
            	this.print(ctx, strm, "", false, "/> ");
            }
        }
    }
 }
