package divconq.lang.op;

public class OperationEvents {
	static public OperationEvent COMPLETED = new OperationEvent();  
	static public OperationEvent LOG = new OperationEvent();  
	static public OperationEvent PROGRESS = new OperationEvent();  

	static public Object PROGRESS_AMOUNT = new Object();
	static public Object PROGRESS_STEP = new Object();
	static public Object PROGRESS_MESSAGE = new Object();
	
	static public OperationEvent PREP_TASK = new OperationEvent();  
	static public OperationEvent START_TASK = new OperationEvent();  
	static public OperationEvent STOP_TASK = new OperationEvent();
}
