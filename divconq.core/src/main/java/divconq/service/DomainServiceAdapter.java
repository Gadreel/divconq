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
	protected Map<String, ServiceFeature> features = new HashMap<String, ServiceFeature>();
	
	public DomainServiceAdapter(String name, Path spath) {
		this.name = name;
		this.sourcepath = spath;
	}
	
	@Override
	public String serviceName() {
		return this.name;
	}

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		
		ServiceFeature f = this.features.get(feature);
		
		if (f == null) {
			f = new ServiceFeature(feature);
			this.features.put(feature, f);
		}
		
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
				loader.addClasspath(DomainServiceAdapter.this.sourcepath.toString());
				
				Class<?> groovyClass = loader.parseClass(spath.toFile());
				
				this.script = (GroovyObject) groovyClass.newInstance();
			}
			catch (Exception x) {
				OperationContext.get().error("Unable to prepare service script: " + spath);
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
				}		
			}
			
			request.errorTr(441, DomainServiceAdapter.this.serviceName(), feature, op);
			request.complete();
		}
	}
}
