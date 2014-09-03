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
package divconq.script;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.lang.OperationCallback;
import divconq.lang.OperationResult;
import divconq.struct.Struct;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.work.ISmartWork;
import divconq.work.TaskRun;
import divconq.work.Task;
import divconq.xml.XElement;

// TODO global variables need to be pushed by Collab to all activities, those vars get stored in Activity
// level variables (so be sure to use a good naming convention for globals - all start with "gbl"

// note that Activity, not Main is the root function block, little different but means exit codes are with Activity
public class Activity {
    protected String id = null;
    protected ActivityManager manager = null;
    protected boolean suspended = false;
	protected Script script = null;
	protected StackFunctionEntry stack = null;
	protected Instruction inst = null;
	protected long runtime = 0;
	
	protected Map<String, Struct> globals = new HashMap<String, Struct>();
	
    // what we send back - errors/state  and Return variables
    protected OperationResult log = new OperationResult();
	protected Struct args = null;
	protected TaskRun taskRun = null;

    public Activity(ActivityManager man) {
    	this.manager = man;
    }

    public Activity(Script script) {
    	this.manager = script.manager;
    	this.script = script;
    	this.inst = script.getMain();
    }
	
	public Activity(Script script, Object args) {
		this(script);
		
		this.args = Struct.objectToStruct(args);
	}

	public OperationResult getLog() {
		return this.log;
	}

	public void setLog(OperationResult v) {
		this.log = v;
	}

    public ExecuteState getState() {
        return (this.stack != null) ? this.stack.getState() : ExecuteState.Ready;
    }
    
    public Long getExitCode() {
        return (this.stack != null) ? this.stack.getLastCode() : 0;
    }
    
    public Struct getExitResult() {
        return (this.stack != null) ? this.stack.getLastResult() : null;
    }
    
    public boolean isSuspended() {
    	return this.suspended;
    }
    
    public void setSuspended(boolean v) {
    	this.suspended = v;
    }

    public String getId() { 
    	return this.id; 
    }
    
    public void setId(String v) { 
    	this.id = v; 
    }

	public String getTitle() {
		if (this.script == null)
			return null;
		
		return this.script.getTitle(); 
	}
	
	public long getRuntime() {
		return this.runtime;
	}

	public ActivityManager getManager() {
		return this.manager;
	}
	
	public TaskRun getTaskRun() {
		return this.taskRun;
	}
	
    public void run(final OperationCallback cb, Task parent) {
    	this.suspended = false;
		final long st = System.currentTimeMillis();
		
    	final AtomicReference<Task> runref = new AtomicReference<>();
    	
    	Task tb = new Task();
    	runref.set(tb);
    	
    	if (parent != null) {
	    	tb.withContext(parent.getContext());
	    	tb.withDeadline(parent.getDeadline());
	    	tb.withTimeout(parent.getTimeout());
	    	tb.withTitle(parent.getTitle());
    	}
    	else {
    		tb.withTimeout(1);
    	}
    	
    	tb.withWork(new ISmartWork() {			
			@Override
			public void run(final TaskRun run) {
				//System.out.println("run on: " + Thread.currentThread().getName());
				
				if (Activity.this.suspended || (Activity.this.getState() == ExecuteState.Exit)) {
					Activity.this.runtime = (System.currentTimeMillis() - st);
					
					run.complete();
					
					try {
						if (cb != null)
							cb.completed();
					}
					catch (Exception x) {
						// not our problem
					}
					
					return;
				}
				
				Activity.this.setTaskRun(run);
				//run.getTask().withTimeout(1);		// default to 1 minute for all instructions - instructions may increase the number if they need to
				
				Activity.this.runSingleInstruction(new IInstructionCallback() {					
					@Override
					public void resume() {
						//System.out.println("submit next: " + Thread.currentThread().getName());
				    	Hub.instance.getWorkPool().submit(runref.get());
						run.complete();
					}
				});
			}

			@Override
			public void cancel(TaskRun run) {
				System.out.println("script task run cancelled");
				
				if (Activity.this.stack != null)
					Activity.this.stack.cancel();
			}

			@Override
			public void completed(TaskRun run) {
			}
		});
    	
    	Hub.instance.getWorkPool().submit(runref.get());
    }
    
    protected void setTaskRun(TaskRun v) {
		this.taskRun  = v;
	}

	public void run() {
		final Semaphore s = new Semaphore(0);
		
    	this.run(new OperationCallback() {			
			@Override
			public void callback() {
				s.release();
			}
		}, null);
		
		try {
			s.acquire();
		}
		catch (InterruptedException x) {
		}
    }

    public void runSingleInstruction(IInstructionCallback cb) {
    	if (this.inst == null) { 
    		cb.resume();
    		return;
    	}
    	
    	if (this.stack == null) {
       		this.stack = (StackFunctionEntry)this.inst.createStack(this, null);
       		this.stack.setParameter(this.args);
    	}
    	
        if (this.getState() != ExecuteState.Exit) 
        	this.stack.run(cb);
        else
        	cb.resume();            
	}

    public Struct createStruct(String type) {
        return this.manager.createVariable(this.log, type);
    }

    public RecordStruct getDebugInfo() {
    	RecordStruct info = new RecordStruct();		// TODO type this
    	
    	info.setField("Log", this.log.getMessages());
    	
    	ListStruct list = new ListStruct();
    	
    	if (this.stack != null)
    		this.stack.debugStack(list);
    	
        info.setField("Stack", list);
        
        return info;
    }

    public OperationResult compile(XElement doc) {
        this.script = new Script(this.manager);        
        
        OperationResult res = this.script.compile(doc);
        
        if (!res.hasErrors())
        	this.inst = this.script.getMain();
        
        return res;
    }

    // global variables
    public Struct queryVariable(String name) {
		if (StringUtil.isEmpty(name))
			return null;

        // do not call super - that would expose vars outside of the function
        int dotpos = name.indexOf(".");

        if (dotpos > -1) {
            String oname = name.substring(0, dotpos);

            Struct ov = this.globals.containsKey(oname) ? this.globals.get(oname) : null;

            if (ov == null) {
            	this.log.errorTr(507, oname);
            	return null;
            }
            
            if (!(ov instanceof CompositeStruct)){
            	this.log.errorTr(508, oname);
            	return null;
            }
            
            FuncResult<Struct> sres = ((CompositeStruct)ov).select(name.substring(dotpos + 1)); 

            this.log.copyMessages(sres);
            
            return sres.getResult();
        }
        else if (this.globals.containsKey(name)) {
            return this.globals.get(name);
        }
        
        return null;
    }

	public Instruction queryFunction(String name) {
		return this.script.getFunction(name);
	}

	public void addVariable(String name, Struct var) {
    	this.globals.put(name, var);
	}

	public void dispose() {
    	if (this.stack != null)
    		this.stack.dispose();
	}

	public void cancel() {
		// stop from doing any more instructions
		this.setSuspended(true);
		
		System.out.println("activity cancelled");
		
		// if in work pool, kill it
		if (this.taskRun != null)
			this.taskRun.kill();
	}
}
