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
package divconq.tasks.services;

import divconq.bus.IService;
import divconq.bus.MessageUtil;
import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.mod.ExtensionBase;
import divconq.struct.RecordStruct;
import divconq.tasks.sql.Router;
import divconq.tool.Updater;
import divconq.work.TaskRun;

/*
 */

public class Main extends ExtensionBase implements IService {
	static public Main instance = null;
	
	protected IService router = null;
	
	public Main() {
		Main.instance = this;
	}

	@Override
	public void start(OperationResult log) {
		super.start(log);
		
		OperationContext.useNewRoot();
		
		FuncResult<RecordStruct> ldres = Updater.loadDeployed();
		
		if (ldres.hasErrors()) {
			System.out.println("Error reading deployed.json file: " + ldres.getMessage());
			return;
		}
		
		RecordStruct deployed = ldres.getResult();
		
		String version = deployed.getFieldAsString("Version");
		
		log.info(0, "Starting FT Tasks ver  " + version);
		
		/*
		XElement settings = this.getLoader().getSettings();

		if (settings == null) {
			log.errorTr(400484);		// TODO move to dct range
			return;
		}
		*/
		
		if (!Hub.instance.getSQLDatabase().testConnection()) {
			log.errorTr(400485);		// TODO move to dct range
			return;
		}
		
		// TODO make this a setting - load from SQL or from noSQL
		// TODO check message version and use the router for that version
		this.router = new Router();

		
		// TODO testing
		/*
		for (int i = 0; i < 10; i++) {
			GreetTask t = new GreetTask();		
			Hub.instance.getWorkPool("DemoPool").submit(t);
		}
		*/
		
		/*
		for (int i = 0; i < 10; i++) {
			Task task = new Task()
				.withTitle("Greeting Carl" + i)
				.withParams(new RecordStruct(new FieldStruct("Greet", "Carl")))
				.withWork("divconq.tasks.test.GreetTask");
	
			Hub.instance.submitToWorkQueue(task);
		}
		*/
		
		/*
		{
			Task task = new Task()
				.withTitle("Greet Test")
				.withParams(new RecordStruct(new FieldStruct("Greet", "Mike")))
				.withWork(SlowGreetWork.class);
	
			Hub.instance.submitToWorkPool(task);
		}
		*/
		
		/*
		{
			Task task = new Task()
				.withTitle("Bucket MaxSize Test 2")
				.withContext(
						OperationContext.allocateRoot()
				)
				.withWork(TestBucketMaxSize.class);
	
			Hub.instance.submitToWorkPool(task);
		}
		*/

		/*
		{
			Task task = new Task()
				.withTitle("Failed Tasks Test")
				.withRootContext()
				.withDeadline(10)		// at least 10 minutes to complete
				.withWork(TestFailedTasks.class);
	
			Hub.instance.submitToWorkPool(task);
		}
		*/
		

		/*
		{
			Task task = new Task()
				.withTitle("Bus Context Test")
				.withGuestContext()
				.withBucket("Demo")   // run as guest in the demo bucket
				.withDeadline(10)		// at least 10 minutes to complete
				.withWork(TestBusAndContext.class);
	
			Hub.instance.submitToWorkPool(task);
		}
		*/

		/*
		{
			Task task = new Task()
				.withTitle("Bus Errors Test")
				.withContext(						
						OperationContext.allocateGuest()		// run as guest 
				)
				.withBucket("Demo")			// in the demo bucket
				.withTimeout(5)			// no more than 5 minutes of inactivity
				.withDeadline(10)		// at least 10 minutes to complete, should take less than 3
				.withWork(TestBusErrors.class);
	
			Hub.instance.submitToWorkPool(task);
		}
		*/
		
		/***** GOOD TEST
		Hub.instance.getScheduler().addSystemWorker(new ISystemWork() {			
			@Override
			public void run() {
				for (int i = 0; i < 10; i++) {
					Task task = new Task()
					.withTitle("Greeting Carl " + i)
					.withParams(new RecordStruct(new FieldStruct("Greet", "Carl")))
					.withWork("divconq.tasks.test.GreetTask");
			
					Hub.instance.submitToWorkQueue(task);
				}
			}
			
			@Override
			public int period() {
				return 1;
			}
		});
		******/
		
		/*
		{
			
			// "D:/dev/divconq/hub/lib/csv4180-0.2.3.jar"
			
			Task task = new Task()
				.withTitle("Async Test Job")
				.withParams(
					new RecordStruct(
						new FieldStruct("Path", "C:/Users/andy/Downloads/qbsdk130.exe")
					)
				)
				.withTimeout(10)
				.withContext(
					OperationContext.get()
						.toBuilder()
						.withNewOpId()							// same user, logging, etc as test task, just use new op ids (it is fine to have same op id, this is just an example of how to use another op id)
						.withBucket("Demo")
						.toOperationContext()
				)
				.withDefaultLogWatcher()
				.withUsesTempFolder(true)
				.withWork(AsyncHashTask.class);
			
			Hub.instance.submitToWorkPool(task, new FuncCallback<TaskRun>() {
				@Override
				public void callback() {
					TaskRun res = this.getResult();
					
					if (res.hasErrors())
						System.out.println("Task error: " + res.getMessage());
					else {
						System.out.println("Task completed, result: " + ((RecordStruct)res.getResult()).getFieldAsString("Hash"));
						System.out.println("Task log size: " + res.getLog().length());
					}
				}
			});
		}
		*/
		
		/*
		{
			for (int i = 0; i < 4; i++)
				Hub.instance.getWorkQueue().submit(ScriptFactory.createSlowGreetTask("Dan " + i).withBucket("Greet"));
		}
		*/

		/*
		{
			Hub.instance.getWorkQueue().submit(ScriptFactory.createVerySlowTask(2));		// shouldn't work
			Hub.instance.getWorkQueue().submit(ScriptFactory.createVerySlowTask(12));		// should work
		}
		*/
		
		/*
		{
			final Task t = ScriptFactory.createSingTask("Dan");
			
			Hub.instance.getWorkQueue().submit(t);
		}
		*/
		
		/*
		{
			Task task = new Task()
				.withTitle("SubTask Greet Test")
				.withDefaultLogger()
				.withParams(new RecordStruct(new FieldStruct("Greet", "Mike")))
				.withWork(TestSubTasks.class);
	
			Hub.instance.getWorkQueue().submit(task);
		}
		*/
	}
	

	@Override
	public void handle(TaskRun request) {
		if (this.router != null) {
			this.router.handle(request);
			return;
		}
		
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		/*
		if ("Deposits".equals(feature)) {
			if ("AuthCheck".equals(op)) {
				RecordStruct rec = msg.getFieldAsRecord("Body");
				
				final String atoken = rec.getFieldAsString("AuthToken");
				
				SqlDatabase db = Hub.instance.getSQLDatabase();
				
				String sql = "SELECT params FROM uptokens WHERE Token = ? AND ExpiresOn > ?";
				
				FuncResult<ListStruct> rsres = db.executeQuery(sql, new ISqlPreparer() {			
					@Override
					public void prepare(OperationResult or, PreparedStatement stmt) throws SQLException {
						stmt.setString(1, atoken);
						stmt.setNString(2, Hub.instance.getSQLManager().getNowAsString());
					}
				});
				
				if (rsres.hasErrors()) 
					return MessageUtil.messages(rsres);
				
				ListStruct rs = rsres.getResult();

				if (!rs.isEmpty()) { 
					String params = rs.getItemAsRecord(0).getFieldAsString("params");
					
					FuncResult<CompositeStruct> jres = CompositeParser.parseJson(params);
					
					if (jres.hasErrors()) 
						return MessageUtil.messages(jres);
					
					CompositeStruct pm = jres.getResult();
					
					if (!(pm instanceof RecordStruct)) 
						return MessageUtil.error(1, "Invalid params structure");
					
					return MessageUtil.success("ForDate", ((RecordStruct) pm).getFieldAsString("Date"));
				}

				return MessageUtil.error(1, "Invalid token.");
			}
		}
		*/
		
		request.setResult(MessageUtil.errorTr(441, this.serviceName(), feature, op));
		request.complete();
	}
}
