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

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import divconq.filestore.CommonPath;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.web.IOutputAdapter;
import divconq.web.WebContext;
import divconq.xml.XElement;

public class Fragment extends Element {
	protected AtomicInteger hasfuture = null;
	protected List<OperationCallback> callbacks = null;
	protected XElement source = null;
	protected Path path = null;
	protected CommonPath loc = null;
	protected GroovyObject script = null;
	
	public XElement getSource() {
		return this.source;
	}

    public GroovyObject getServerScript() {
		return this.script;
	}
	
	public void initializePart(WebContext ctx, IOutputAdapter adapter, OperationCallback cb, Object... args) {
		this.coreInitialize(ctx, adapter, new OperationCallback() {
			@Override
			public void callback() {
				Fragment.this.myArguments = new Object[] { args, ctx.getDomain().parseXml(ctx, Fragment.this.source.find("Skeleton")) };
				
				cb.complete();
			}
		});
	}
	
	public void initializeRoot(WebContext ctx, IOutputAdapter adapter, OperationCallback cb) {
		this.coreInitialize(ctx, adapter, new OperationCallback() {			
			@Override
			public void callback() {
				Fragment.this.myArguments = new Object[] { ctx.getDomain().parseElement(ctx, Fragment.this.source) };
				
				cb.complete();
			}
		});
	}
	
	public void coreInitialize(WebContext ctx, IOutputAdapter adapter, OperationCallback cb) {
		this.path = adapter.getFilePath();
		this.loc = adapter.getLocationPath();
		this.source = (XElement) adapter.getSource().deepCopy();
				
		XElement screl = this.source.find("ServerScript");
		
		if (screl != null) {
			try (GroovyClassLoader loader = new GroovyClassLoader()) {
				Path dpath = OperationContext.get().getDomain().resolvePath("/glib");
				
				if (Files.exists(dpath))
					loader.addClasspath(dpath.toString());
				
				Class<?> groovyClass = loader.parseClass(screl.getText());
				
				this.script = (GroovyObject) groovyClass.newInstance();
				
				this.tryExecuteMethod("run", new Object[] { ctx, this, cb });
			}
			catch (Exception x) {
				OperationContext.get().error("Unable to prepare web page server script: " + x);
			}
		}
		else {
			cb.callback();
		}
	}
	
	public void tryExecuteMethod(String name, Object... params) {
		if (this.script == null) 
			return;
		
		Method runmeth = null;
		
		for (Method m : this.script.getClass().getMethods()) {
			if (!m.getName().equals(name))
				continue;
			
			runmeth = m;
			break;
		}
		
		if (runmeth == null) 
			return;
		
		try {
			this.script.invokeMethod(name, params);
		}
		catch (Exception x) {
			OperationContext.get().error("Unable to execute watcher script!");
			OperationContext.get().error("Error: " + x);
		}		
	}

	@Override
	public void doBuild(WebContext ctx) {
		// if I am not root, copy my functions to root
		if (this.getViewRoot() != this) {
			for (XElement func : this.source.selectAll("Function")) 
				this.getViewRoot().getSource().add(func);
		}
		
        super.doBuild(ctx);
	}

	public void write(WebContext ctx) throws IOException {
		this.stream(ctx, ctx.getResponse().getPrintStream(), "", false, true);
	}

	@Override
	public void stream(WebContext ctx, PrintStream strm, String indent, boolean firstchild, boolean fromblock) {
		if (this.children.size() == 0)
			return;

		boolean fromon = fromblock;
		boolean lastblock = false;
		boolean firstch = this.getBlockIndent(); // only true once, and only if
													// bi

		for (Node node : this.children) {
			if (node.getBlockIndent() && !lastblock && !fromon)
				this.print(ctx, strm, "", true, "");

			node.stream(ctx, strm, indent, (firstch || lastblock), this.getBlockIndent());

			lastblock = node.getBlockIndent();
			firstch = false;
			fromon = false;
		}
	}
	
	@Override
	public boolean writeDynamic(PrintStream buffer, String tabs, boolean first) {
        if (this.children.size() == 0) 
        	return false;
        
		for (Node child : this.children) {
			if (child.writeDynamic(buffer, tabs, first)) 
				first = false;
		}
		
		return true;
	}

	public void incrementFuture() {
		synchronized (this) {
			if (this.hasfuture == null)
				this.hasfuture = new AtomicInteger();

			this.hasfuture.incrementAndGet();
		}
	}

	public void decrementFuture() {
		synchronized (this) {
			int cnt = (this.hasfuture != null) ? this.hasfuture.decrementAndGet() : 0;

			if (cnt == 0) {
				if (this.callbacks != null) {
					for (OperationCallback cb : this.callbacks)
						cb.complete();
				}
			}
		}
	}

	public void awaitForFutures(OperationCallback cb) {
		// at this point it is too late to register any new futures (see
		// increment above)
		// it is safe to callback immediately if there was no futures
		if (this.hasfuture == null) {
			cb.complete();
			return;
		}

		synchronized (this) {
			if (this.hasfuture.get() == 0) {
				cb.complete();
				return;
			}

			if (this.callbacks == null)
				this.callbacks = new ArrayList<OperationCallback>();

			this.callbacks.add(cb);
		}
	}
}
