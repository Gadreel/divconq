package divconq.service;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.lang.op.OperationContext;
import divconq.work.TaskRun;

public class DomainServiceAdapter implements IService {
	protected String name = null;
	protected Path sourcepath = null;
	protected Path domainpath = null;
	protected Map<String, ServiceFeature> features = new HashMap<String, ServiceFeature>();
	
	public DomainServiceAdapter(String name, Path spath, Path dpath) {
		this.name = name;
		this.sourcepath = spath;
		this.domainpath = dpath;
	}
	
	public GroovyObject getScript(String name) {
		ServiceFeature f = this.getFeature(name);
		
		if (f != null) 
			return f.script;
		
		return null;
	}
	
	public ServiceFeature getFeature(String name) {
		ServiceFeature f = this.features.get(name);
		
		if (f == null) {
			f = new ServiceFeature(name);
			this.features.put(name, f);
		}
		
		return f;
	}
	
	@Override
	public String serviceName() {
		return this.name;
	}

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		
		ServiceFeature f = this.getFeature(feature);
		
		if (f != null)
			f.handle(request);
	}
	
	public class ServiceFeature {
		protected GroovyObject script = null;
		
		public ServiceFeature(String feature) {
			Path spath = DomainServiceAdapter.this.sourcepath.resolve(feature + ".groovy");
			
			if (Files.notExists(spath))
				return;
			
			// TODO Auto-generated method stub
			try (GroovyClassLoader loader = new GroovyClassLoader()) {
				Path dpath = DomainServiceAdapter.this.domainpath.resolve("glib");
				
				//System.out.println("dpath: " + dpath);
				
				if (Files.exists(dpath))
					loader.addClasspath(dpath.toString());
				
				Class<?> groovyClass = loader.parseClass(spath.toFile());
				
				this.script = (GroovyObject) groovyClass.newInstance();
			}
			catch (Exception x) {
				OperationContext.get().error("Unable to prepare service script: " + spath);
				OperationContext.get().error("Error: " + x);
			}		
		}
		
		public void handle(TaskRun request) {
			Message msg = (Message) request.getTask().getParams();
			
			String feature = msg.getFieldAsString("Feature");
			String op = msg.getFieldAsString("Op");
			
			if (this.script != null) {
				try {
					Object[] args2 = { request, msg.getProperty("Body") };					
					this.script.invokeMethod(op, args2);					
					return;
				}
				catch (Exception x) {
					OperationContext.get().error("Unable to execute script!");
					OperationContext.get().error("Error: " + x);
				}		
			}
			
			request.errorTr(441, DomainServiceAdapter.this.serviceName(), feature, op);
			request.complete();
		}
	}
}
