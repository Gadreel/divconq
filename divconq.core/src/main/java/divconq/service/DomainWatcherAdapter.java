package divconq.service;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import divconq.hub.DomainInfo;
import divconq.lang.op.OperationContext;

public class DomainWatcherAdapter {
	protected Path domainpath = null;
	protected GroovyObject script = null;
	
	public DomainWatcherAdapter(Path dpath) {
		this.domainpath = dpath;
	}
	
	public GroovyObject getScript() {
		return this.script;
	}

	public void init(DomainInfo domaininfo) {
		if (this.script != null) {
			this.tryExecuteMethod("Kill", new Object[] { });
			this.script = null;
		}
		
		Path cpath = this.domainpath.resolve("config");

		if (Files.notExists(cpath))
			return;
		
		Path spath = cpath.resolve("Watcher.groovy");
		
		if (Files.notExists(spath))
			return;
		
		// TODO Auto-generated method stub
		try (GroovyClassLoader loader = new GroovyClassLoader()) {
			Path dpath = DomainWatcherAdapter.this.domainpath.resolve("glib");
			
			//System.out.println("dpath: " + dpath);
			
			if (Files.exists(dpath))
				loader.addClasspath(dpath.toString());
			
			Class<?> groovyClass = loader.parseClass(spath.toFile());
			
			this.script = (GroovyObject) groovyClass.newInstance();
			
			this.tryExecuteMethod("Init", new Object[] { domaininfo });
		}
		catch (Exception x) {
			OperationContext.get().error("Unable to prepare domain watcher script: " + spath);
			OperationContext.get().error("Error: " + x);
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
}
