package divconq.tasks.test;

import org.joda.time.DateTime;

import divconq.bus.Message;
import divconq.bus.ServiceResult;
import divconq.hub.Hub;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.work.IWork;
import divconq.work.TaskRun;

public class OperationsWork implements IWork {
	protected enum WorkState {
		OR,
		FR,
		CB,
		FCB,
		BUS,
		LOG,
		DONE
	}

	protected WorkState state = WorkState.OR;
	
	@Override
	public void run(TaskRun trun) {
		System.out.println();
		System.out.println("=====================================================");
		System.out.println();
		
		switch (this.state) {
		case OR:
			this.orTests(trun);
			break;
		case FR:
			this.frTests(trun);
			break;
		case CB:
			this.cbTests(trun);
			break;
		case FCB:
			this.fcbTests(trun);
			break;
		case BUS:
			this.busTests(trun);
			break;
		case LOG:
			this.logTests(trun);
			break;
		case DONE:
			trun.complete();;
			break;
		}
	}
	
	protected void transition(TaskRun trun, WorkState v) {
		this.state = v;
		
		// reset the error code - both for the tests and also you cannot resume an errored task
		trun.exit(0, null);
		
		trun.resume();
	}
	
	public void orTests(TaskRun trun) {
		trun.info("Before Test 1");
		
		OperationResult t1 = this.test1();
		
		trun.info("Before Test 2");
		
		OperationResult t2 = this.test2();
		
		trun.info("Before Test 3");
		
		OperationResult t3 = this.test3();
		
		trun.info("Before Test 4");

		// reset the error code
		trun.exit(0, null);
		
		OperationResult t4 = this.test4();
		
		trun.info("After all OR Tests");

		System.out.println("Log for t1:");
		OperationsWork.dumpLog(t1);

		System.out.println();
		System.out.println("Log for t2:");
		OperationsWork.dumpLog(t2);

		System.out.println();
		System.out.println("Log for t3:");
		OperationsWork.dumpLog(t3);
		
		System.out.println();
		System.out.println("Log for t4:");
		OperationsWork.dumpLog(t4);

		System.out.println();
		System.out.println("Log for total:");
		OperationsWork.dumpLog(trun);
		
		System.out.println();
		System.out.println("t2 has errors: " + t2.hasErrors());
		System.out.println("t3 has errors: " + t3.hasErrors());
		System.out.println("t4 has errors: " + t4.hasErrors());
		
		System.out.println();
		System.out.println("t2 has code 55: " + t2.hasCode(55));
		System.out.println("t3 has code 55: " + t3.hasCode(55));
		System.out.println("t4 has code 55: " + t4.hasCode(55));
		
		System.out.println();
		System.out.println("t2 has code 95: " + t2.hasCode(95));
		System.out.println("t3 has code 95: " + t3.hasCode(95));
		System.out.println("t4 has code 95: " + t4.hasCode(95));
		
		System.out.println();
		System.out.println("t2 finish code: " + t2.getCode());
		System.out.println("t3 finish code: " + t3.getCode());
		System.out.println("t4 finish code: " + t4.getCode());
		
		System.out.println();
		System.out.println("t2 # msgs: " + t2.getMessages().getSize());
		System.out.println("t3 # msgs: " + t3.getMessages().getSize());
		System.out.println("t4 # msgs: " + t4.getMessages().getSize());
		
		System.out.println();
		System.out.println("trun has errors: " + trun.hasErrors());
		System.out.println("trun has code 55: " + trun.hasCode(55));
		System.out.println("trun has code 95: " + trun.hasCode(95));
		System.out.println("trun finish code: " + trun.getCode());
		System.out.println("trun # msgs: " + trun.getMessages().getSize());
		
		System.out.println();
		System.out.println("ctx has errors: " + trun.getContext().hasErrors());
		System.out.println("ctx has code 55: " + trun.getContext().hasCode(55));
		System.out.println("ctx has code 95: " + trun.getContext().hasCode(95));
		System.out.println("ctx finish code: " + trun.getContext().getCode());
		System.out.println("ctx # msgs: " + trun.getContext().getMessages().getSize());
		
		this.transition(trun, WorkState.FR);
	}
	
	public void frTests(TaskRun trun) {
		FuncResult<Long> t5 = this.test5();
		
		trun.info("After all FR Tests");

		System.out.println("Log for t5:");
		OperationsWork.dumpLog(t5);

		System.out.println();
		System.out.println("Log for total:");
		OperationsWork.dumpLog(trun);
		
		System.out.println();
		System.out.println("t5 result: " + t5.getResult());
		System.out.println("t5 has errors: " + t5.hasErrors());
		System.out.println("t5 finish code: " + t5.getCode());
		System.out.println("t5 # msgs: " + t5.getMessages().getSize());
		
		System.out.println();
		System.out.println("trun has errors: " + trun.hasErrors());
		System.out.println("trun finish code: " + trun.getCode());
		System.out.println("trun # msgs: " + trun.getMessages().getSize());
		
		this.transition(trun, WorkState.CB);
	}
	
	public void cbTests(TaskRun trun) {
		this.test6(new OperationCallback() {			
			@Override
			public void callback() {
				OperationCallback t6 = this;
				
				OperationsWork.this.test7(new OperationCallback() {					
					@Override
					public void callback() {
						OperationCallback t7 = this;
						
						trun.info("After all CB Tests");

						System.out.println("Log for t6:");
						OperationsWork.dumpLog(t6);

						System.out.println("Log for t7:");
						OperationsWork.dumpLog(t7);

						System.out.println();
						System.out.println("Log for total:");
						OperationsWork.dumpLog(trun);
						
						System.out.println();
						System.out.println("t6 has errors: " + t6.hasErrors());
						System.out.println("t6 finish code: " + t6.getCode());
						System.out.println("t6 has code 75: " + t6.hasCode(75));
						System.out.println("t6 # msgs: " + t6.getMessages().getSize());
						
						System.out.println();
						System.out.println("t7 has errors: " + t7.hasErrors());
						System.out.println("t7 finish code: " + t7.getCode());
						System.out.println("t7 has code 75: " + t7.hasCode(75));
						System.out.println("t7 # msgs: " + t7.getMessages().getSize());
						
						System.out.println();
						System.out.println("trun has errors: " + trun.hasErrors());
						System.out.println("trun finish code: " + trun.getCode());
						System.out.println("trun has code 75: " + trun.hasCode(75));
						System.out.println("trun # msgs: " + trun.getMessages().getSize());
						
						OperationsWork.this.transition(trun, WorkState.FCB);
					}
				});
			}
		});
	}
	
	public void fcbTests(TaskRun trun) {
		this.test8(new FuncCallback<Long>() {			
			@Override
			public void callback() {
				FuncCallback<Long> t8 = this;
				
				OperationsWork.this.test9(new FuncCallback<Long>() {					
					@Override
					public void callback() {
						FuncCallback<Long> t9 = this;
						
						trun.info("After all FCB Tests");

						System.out.println("Log for t8:");
						OperationsWork.dumpLog(t8);

						System.out.println("Log for t9:");
						OperationsWork.dumpLog(t9);

						System.out.println();
						System.out.println("Log for total:");
						OperationsWork.dumpLog(trun);
						
						System.out.println();
						System.out.println("t8 result: " + t8.getResult());
						System.out.println("t8 has errors: " + t8.hasErrors());
						System.out.println("t8 finish code: " + t8.getCode());
						System.out.println("t8 has code 75: " + t8.hasCode(75));
						System.out.println("t8 # msgs: " + t8.getMessages().getSize());
						
						System.out.println();
						System.out.println("t9 result: " + t9.getResult());
						System.out.println("t9 has errors: " + t9.hasErrors());
						System.out.println("t9 finish code: " + t9.getCode());
						System.out.println("t9 has code 75: " + t9.hasCode(75));
						System.out.println("t9 # msgs: " + t9.getMessages().getSize());
						
						System.out.println();
						System.out.println("trun has errors: " + trun.hasErrors());
						System.out.println("trun finish code: " + trun.getCode());
						System.out.println("trun has code 75: " + trun.hasCode(75));
						System.out.println("trun # msgs: " + trun.getMessages().getSize());
						
						OperationsWork.this.transition(trun, WorkState.BUS);
					}
				});
			}
		});
	}
	
	public void busTests(TaskRun trun) {
		Hub.instance.getBus().sendMessage(
				new Message("Status", "Echo", "Test", "Hello from Bus: à¤ à¥€ á‰» âŒ† ï¨¬"), 
				new ServiceResult() {					
					@Override
					public void callback() {
						ServiceResult t10 = this;
						
						trun.info("After all BUS Tests");

						System.out.println("Log for t10:");
						OperationsWork.dumpLog(t10);

						System.out.println();
						System.out.println("Log for total:");
						OperationsWork.dumpLog(trun);
						
						System.out.println();
						System.out.println("t10 result: " + t10.getBodyAsString());
						System.out.println("t10 has errors: " + t10.hasErrors());
						System.out.println("t10 finish code: " + t10.getCode());
						System.out.println("t10 has code 75: " + t10.hasCode(75));
						System.out.println("t10 # msgs: " + t10.getMessages().getSize());
						
						System.out.println();
						System.out.println("trun has errors: " + trun.hasErrors());
						System.out.println("trun finish code: " + trun.getCode());
						System.out.println("trun has code 75: " + trun.hasCode(75));
						System.out.println("trun # msgs: " + trun.getMessages().getSize());
						
						OperationsWork.this.transition(trun, WorkState.LOG);
					}
				}
		);
	}
	
	public void logTests(TaskRun trun) {
		String log = trun.getContext().getLog();
		
		trun.info("After all LOG Tests");

		System.out.println("Log string:");
		System.out.println(log);
		
		OperationsWork.this.transition(trun, WorkState.DONE);
	}

	public OperationResult test1() {
		OperationResult or = new OperationResult();
		
		or.info("test 1 msg 1");
		or.info("test 1 msg 2");
		
		or.markEnd();
		
		return or;
	}

	public OperationResult test2() {
		OperationResult or = new OperationResult();
		
		or.info("test 2 msg 1");
		or.info("test 2 msg 2");
		
		or.markEnd();
		
		return or;
	}

	public OperationResult test3() {
		OperationResult or = new OperationResult();
		
		or.error("test 3 msg 1");
		or.info(55, "test 3 msg 2");
		or.info("test 3 msg 3");
		
		or.markEnd();
		
		return or;
	}

	public OperationResult test4() {
		OperationResult or = new OperationResult();
		
		or.error(95, "test 4 msg 1");
		or.info("test 4 msg 2");
		
		or.markEnd();
		
		return or;
	}

	public FuncResult<Long> test5() {
		FuncResult<Long> fr = new FuncResult<>();
		
		fr.info("test 5 msg 1");
		fr.info("test 5 msg 2");
		
		fr.setResult(4L);
		
		fr.info("test 5 msg 3 - hidden from result");
		
		return fr;
	}

	public void test6(OperationCallback cb) {
		cb.info("test 6 msg 1");
		cb.info("test 6 msg 2");
		
		cb.complete();
	}

	public void test7(OperationCallback cb) {
		cb.info("test 7 msg 1");
		cb.error(75, "test 7 msg 2");
		
		cb.complete();
	}

	public void test8(FuncCallback<Long> cb) {
		cb.info("test 8 msg 1");
		
		cb.setResult(5L);
		cb.complete();
	}

	public void test9(FuncCallback<Long> cb) {
		cb.error(75, "test 9 msg 1");
		
		cb.setResult(7L);
		cb.complete();
	}
	
	static public void dumpLog(OperationResult or) {
		OperationsWork.dumpLog(or.getContext(), or.getMsgStart(), or.getMsgEnd());
	}
	
	static public void dumpLog(OperationContext ctx) {
		OperationsWork.dumpLog(ctx, 0, -1);
	}
	
	static public void dumpLog(OperationContext ctx, int begin, int end) {
		ListStruct msgs = ctx.getMessages();
		
		if (end == -1)
			end = msgs.getSize();
		
		for (int i = begin; i < end; i++) 
			System.out.println(OperationsWork.formatLogEntry(msgs.getItemAsRecord(i)));
	}
	
	static public String formatLogEntry(RecordStruct entry) {
		DateTime occured = entry.getFieldAsDateTime("Occurred");
		
		String lvl = entry.getFieldAsString("Level");
		
		lvl = StringUtil.alignLeft(lvl, ' ', 6);
		
		String msg = entry.getFieldAsString("Message");
		
		// return null if msg was filtered
		if (StringUtil.isEmpty(msg))
			return null;
		
		return occured + " " + lvl + msg;
	}
}
