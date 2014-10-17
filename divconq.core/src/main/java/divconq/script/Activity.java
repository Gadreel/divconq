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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

import org.joda.time.DateTime;

import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.lang.IOperationObserver;
import divconq.lang.OperationObserver;
import divconq.lang.OperationResult;
import divconq.struct.Struct;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.scalar.BooleanStruct;
import divconq.struct.scalar.DateTimeStruct;
import divconq.struct.scalar.IntegerStruct;
import divconq.struct.scalar.NullStruct;
import divconq.util.IOUtil;
import divconq.util.StringUtil;
import divconq.work.ISmartWork;
import divconq.work.TaskRun;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

// TODO global variables need to be pushed by Collab to all activities, those vars get stored in Activity
// level variables (so be sure to use a good naming convention for globals - all start with "gbl"

// note that Activity, not Main is the root function block, little different but means exit codes are with Activity
public class Activity implements ISmartWork, IInstructionCallback {
    protected ActivityManager manager = null;
    protected boolean debugmode = false;
    protected boolean inDebugger = false;
    protected boolean exitFlag = false;
    protected IDebugger debugger = null;
    
    // error handler
    protected ErrorMode errorMode = ErrorMode.Resume;
    protected long errorCode = 0;
    protected String errorMessage = null;
    
	protected Script script = null;
	protected StackFunctionEntry stack = null;
	protected Instruction inst = null;
	protected long starttime = 0;
	protected long runtime = 0;
	protected AtomicLong runCount = new AtomicLong();		// useful flag to let us know that another instruction has completed
	protected AtomicLong varnames = new AtomicLong();
	
	protected Map<String, Struct> globals = new HashMap<String, Struct>();
	
	protected TaskRun taskRun = null;
	protected IOperationObserver taskObserver = null;
	protected boolean hasErrored = false;

    public Activity() {
    	this.manager = Hub.instance.getActivityManager();
    }

	public OperationResult getLog() {
		return this.taskRun;
	}

    public ExecuteState getState() {
        return (this.stack != null) ? this.stack.getState() : ExecuteState.Ready;
    }
    
    public void setState(ExecuteState v) {
    	if (this.stack != null) 
    		this.stack.setState(v);
    }
    
    public boolean hasErrored() {
		return this.hasErrored;
	}
    
    public void clearErrored() {
		this.hasErrored = false;
	}
    
    public Long getExitCode() {
        return (this.stack != null) ? this.stack.getLastCode() : 0;
    }
    
    public Struct getExitResult() {
        return (this.stack != null) ? this.stack.getLastResult() : null;
    }

    public void setExitFlag(boolean v) {
		this.exitFlag = v;
	}
    
    public boolean isExitFlag() {
		return this.exitFlag;
	}
    
    public void setDebugMode(boolean v) {
		this.debugmode = v;
	}
    
    /*
     * has the code signaled that it wants to debug?
     */
    public boolean isDebugMode() {
		return this.debugmode;
	}
    
    public void setInDebugger(boolean v) {
		this.inDebugger = v;
	}
    
    public void setDebugger(IDebugger v) {
		this.debugger = v;
	}
    
    public IDebugger getDebugger() {
		return this.debugger;
	}
    
    public void setErrorMode(ErrorMode errorMode, long errorCode, String errorMessage) {
		this.errorMode = errorMode;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
    
    /*
     * is the code already managed by a debugger
     */
    public boolean isInDebugger() {
		return this.inDebugger;
	}
    
	public String getTitle() {
		if (this.script == null)
			return null;
		
		return this.script.getTitle(); 
	}
	
	public long getRuntime() {
		return this.runtime;
	}

	public long getRunCount() {
		return this.runCount.get();
	}
	
	public ActivityManager getManager() {
		return this.manager;
	}
	
	public TaskRun getTaskRun() {
		return this.taskRun;
	}
    
	@Override
	public void run(TaskRun scriptrun) {
		// TODO not cool - circular refs, try to cleanup someday by passing trun to everyone in stack?
		if (this.taskRun == null) {
			this.taskRun = scriptrun;
			
			this.taskObserver = new OperationObserver() {
				// lock is too expensive, this is a flag just too keep us from calling ourself again, not for thread safety
				// worse case is we get a few more log messages than we wanted, should be very rare (task killed same time script makes error)
				protected boolean inHandler = false;
				
				@Override
				public void log(OperationResult or, RecordStruct entry) {
					if ("Error".equals(entry.getFieldAsString("Level"))) {
						if (this.inHandler)
							return;
						
						this.inHandler = true;
						
						Activity.this.hasErrored = true;
						
						long lcode = entry.getFieldAsInteger("Code", 1);
						
						if (lcode > 0) {
							StackEntry se = Activity.this.stack.getExecutingStack();
							
							if (se != null)
								se.setLastCode(lcode);
						}
						
						TaskRun r = Activity.this.taskRun;
						
						if (r != null) {
							if (StringUtil.isNotEmpty(Activity.this.errorMessage) && (Activity.this.errorCode > 0)) 
								r.error(Activity.this.errorCode, Activity.this.errorMessage);
							else if (StringUtil.isNotEmpty(Activity.this.errorMessage)) 
								r.error(Activity.this.errorMessage);
							else if (Activity.this.errorCode > 0) 
								r.errorTr(Activity.this.errorCode);
						}
						
						if (Activity.this.errorMode == ErrorMode.Debug) {
							if (r != null)
								r.clearExitCode();
							
							Activity.this.engageDebugger();							
						}
						else if (Activity.this.errorMode == ErrorMode.Exit) {
							Activity.this.setExitFlag(true);
						}
						else {
							if (r != null)
								r.clearExitCode();
						}
						
						this.inHandler = false;
					}					
				}
			};
			
			this.taskRun.addObserver(this.taskObserver);
		}
		
    	if (this.inst == null) { 
			this.exitFlag = true;
    	}
    	else if (this.stack == null) {
			this.starttime = System.currentTimeMillis();
       		this.stack = (StackFunctionEntry)this.inst.createStack(this, null);
       		this.stack.setParameter(scriptrun.getTask().getParams());
    	}
		
		if (this.exitFlag) {
			scriptrun.complete();
			return;
		}
		
       	this.stack.run(this);
	}
	
	@Override
	public void resume() {
		TaskRun run = this.taskRun;
		
		if (run == null) {
			System.out.println("Resume with no run!!!");
			return;
		}
		
		if (this.exitFlag) 
			run.complete();
		else if (this.debugmode) {
			IDebugger d = this.debugger;
			
			if (d != null)
				d.stepped();
		}
		else
			Hub.instance.getWorkPool().submit(run);
		
		this.runCount.incrementAndGet();
	}

	@Override
	public void cancel(TaskRun scriptrun) {
		scriptrun.error("script task run canceled");
		
		if (this.stack != null)
			this.stack.cancel();
		
		System.out.println("activity canceled");
	}

	@Override
	public void completed(TaskRun scriptrun) {
		this.runtime = (System.currentTimeMillis() - this.starttime);
		
		// try to clean up circular ref
		this.taskRun = null;
	}

	public void engageDebugger() {		
		this.taskRun.trace("Debugger requested");
		
		this.debugmode = true;
		
		if (this.inDebugger) 
			return;

		// need a task run to do debugging
		if (this.taskRun != null) {
			IDebuggerHandler debugger = this.manager.getDebugger();
			
			if (debugger == null) {
				this.taskRun.error("Unable to debug script, no debugger registered.");
				this.taskRun.kill();
			}
			else {
				// so debugging don't timeout
				this.taskRun.getTask().withTimeout(0).withDeadline(0);
			
				debugger.startDebugger(this.taskRun);
			}
		}
	}

    public Struct createStruct(String type) {
    	if (this.taskRun != null)
    		return this.manager.createVariable(this.taskRun, type);
    	
    	return NullStruct.instance;
    }

    public RecordStruct getDebugInfo() {
    	RecordStruct info = new RecordStruct();		// TODO type this
    	
    	if (this.taskRun != null)
    		info.setField("Log", this.taskRun.getMessages());
    	
    	ListStruct list = new ListStruct();

    	// global level
    	RecordStruct dumpRec = new RecordStruct();
    	list.addItem(dumpRec);
    	
    	dumpRec.setField("Line", 1);
    	dumpRec.setField("Column", 1);		
    	dumpRec.setField("Command", ">Global<");

    	RecordStruct dumpVariables = new RecordStruct();
    	dumpRec.setField("Variables", dumpVariables);
        
        for (Entry<String, Struct> var : this.globals.entrySet()) 
            dumpVariables.setField(var.getKey(), var.getValue());
        
        dumpVariables.setField("_Errored", new BooleanStruct(this.hasErrored));        
        
    	if (this.taskRun != null)
    		dumpVariables.setField("_ExitCode", new IntegerStruct(this.taskRun.getCode()));

        // add the rest of the stack
    	if (this.stack != null)
    		this.stack.debugStack(list);
    	
        info.setField("Stack", list);
        
        return info;
    }

    public OperationResult compile(String source) {
    	OperationResult res = new OperationResult();
		
		boolean checkmatches = true;
		Set<String> includeonce = new HashSet<>();

		while (checkmatches) {
			checkmatches = false;
	    	
			Matcher m = Script.includepattern.matcher(source);
			
			while (m.find()) {
				String grp = m.group();
				String path = grp.trim();

				path = path.substring(10, path.length() - 3);

				String lib = "\n";
				
				if (! includeonce.contains(path)) {
					System.out.println("Including: " + path);
					
					// set lib from file content
					lib = IOUtil.readEntireFile(new File("." + path));
					
					if (StringUtil.isEmpty(lib))
						lib = "\n";
					else
						lib = "\n" + lib;
					
					includeonce.add(path);
				}
				
				source = source.replace(grp, lib);
				checkmatches = true;
			}
		}
    	
		FuncResult<XElement> xres = XmlReader.parse(source, true); 
		
		res.copyMessages(xres);
		
		if (res.hasErrors()) {
			res.error("Unable to parse script");
			return res;
		}
    	
        this.script = new Script(this.manager);        
        
        res.copyMessages(this.script.compile(xres.getResult(), source));
        
        if (res.hasErrors()) {
			res.error("Unable to compile script");
	        return res;
        }
        
       	this.inst = this.script.getMain();        
        return res;
    }
    
    public Script getScript() {
		return this.script;
	}

    // global variables
    public Struct queryVariable(String name) {
		if (StringUtil.isEmpty(name))
			return null;
        
        if ("_Errored".equals(name)) 
        	return new BooleanStruct(this.hasErrored);
        
        if ("_ExitCode".equals(name)) 
        	return new IntegerStruct(this.taskRun.getCode());
        
        if ("_Log".equals(name)) 
        	return this.taskRun.getMessages();

        if ("_Now".equals(name))
        	return new DateTimeStruct(new DateTime());

        // do not call super - that would expose vars outside of the function
        int dotpos = name.indexOf(".");

        if (dotpos > -1) {
            String oname = name.substring(0, dotpos);

            Struct ov = this.globals.containsKey(oname) ? this.globals.get(oname) : null;

            if (ov == null) {
            	if (this.taskRun != null)
            		this.taskRun.errorTr(507, oname);
            	
            	return null;
            }
            
            if (!(ov instanceof CompositeStruct)){
            	if (this.taskRun != null)
            		this.taskRun.errorTr(508, oname);
            	
            	return null;
            }
            
            FuncResult<Struct> sres = ((CompositeStruct)ov).select(name.substring(dotpos + 1)); 

        	if (this.taskRun != null)
        		this.taskRun.copyMessages(sres);
            
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

	public String tempVarName() {
		return this.varnames.incrementAndGet() + "";
	}
}
