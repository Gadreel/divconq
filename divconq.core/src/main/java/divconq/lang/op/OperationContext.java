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
package divconq.lang.op;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.log.DebugLevel;
import divconq.log.Logger;
import divconq.session.Session;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;
import divconq.work.TaskRun;
import divconq.xml.XElement;

/**
 * Almost all code that executes after Hub.start should have a context.  The context
 * tells the code who the user responsible for the task is, what their access levels
 * are (at a high level), what language/locale/chronology(timezone) they use, how to log the 
 * debug messages for the task, and whether or not the user has been authenticated or not.
 * 
 * Although the task context is associated with the current thread, it is the task that the
 * context belongs to, not the thread.  If a task splits into multiple threads there is still
 * one TaskContext, even if the task makes a remote call on DivConq's bus that remote call
 * executes on the TaskContext.
 * 
 * As long as you use the built-in features - work pool, scheduler, bus, database - the task context
 * will smoothly come along with no effort from the app developer.
 *   
 * A quick guide to what context to use where:
 * 
 * Hub Context - useHubContext()
 * 
 * 		The code is running as part of the Hub core.
 * 
 * Root Context - useNewRoot()
 * 
 * 		Root context is the same identity as Hub, but with useNewRoot() you get a new log id.  
 * 		Use this with code running batch tasks that belong to the system rather than a specific
 * 		user.
 * 
 * Guest Context - useNewGuest()
 * 
 * 		Guest context is for use by an anonymous user.  For example a user through the interchange
 * 		(HTTP, FTP, SFTP, EDI, etc).
 * 
 *  User Context - new TaskContext + set(tc)
 *  
 *  	When a user signs-in create and set a new context.  No need to authenticate against the database
 *  	that will happen automatically (as long as you follow DivConq development guidelines) so think
 *  	of creating the user Task Context as information gathering not authentication. 
 * 
 * @author Andy
 *
 */
public class OperationContext {
	static protected String runid = null;
	static protected String hubid = "00001";
	static protected OperationContext hubcontext = null;
	static protected OperationContext defaultcontext = null;
	
	static protected ThreadLocal<OperationContext> context = new ThreadLocal<OperationContext>();
	static protected AtomicLong nextid = new AtomicLong();
	
	static {
		OperationContext.context.set(new OperationContext());
		
		OperationContext.runid = TimeUtil.stampFmt.print(new DateTime(DateTimeZone.UTC));
		OperationContext.hubcontext = OperationContext.useNewRoot();
		OperationContext.defaultcontext = OperationContext.useNewGuest();		 
		
		OperationContext.context.set(OperationContext.hubcontext);
	}
	
	static public String getHubId() {
		return OperationContext.hubid;
	}
	
	static public void setHubId(String v) {
		if (StringUtil.isEmpty(v))
			return;
		
		// only set once
		if (v.equals(OperationContext.hubid))
			return;
		
		OperationContext.hubid = v;
		OperationContext.hubcontext = OperationContext.useNewRoot();		// reset the hub context 
		OperationContext.defaultcontext = OperationContext.useNewGuest();		 
		
		OperationContext.context.set(OperationContext.hubcontext);
	}
	
	static public String getRunId() {
		return OperationContext.runid;
	}
	
	/**
	 * @return the hub context, use by code running in the "core" code of Hub (e.g. main thread)
	 */
	static public OperationContext getHubContext() {
		return OperationContext.hubcontext;
	}
	
	/**
	 * Sets the current thread to use the hub context
	 * 
	 * @return the hub context, use by code running in the "core" code of Hub (e.g. main thread)
	 */
	static public OperationContext useHubContext() {
		OperationContext.context.set(OperationContext.hubcontext);
		return OperationContext.hubcontext;
	}

	public static void startHubContext(XElement config) {
		// TODO load up info from config - this is the only time TC or UC
		// should be mutated, only internally and only the special root
		// instances
		
		OperationContext.updateHubContext();
	}

	public static void updateHubContext() {
		// this is the only time TC or UC should be mutated, only internally
		//  and only the special root instances
		
		OperationContext.hubcontext.level = Logger.getGlobalLevel(); 
		OperationContext.hubcontext.userctx.context.setField("Locale", Logger.getLocale());
	}

	// make sure messages size never gets too large, since server could run for months
	// without a reset - this could throw off the markers in any OR pointing to hub/default
	// but generally that is such a small problem...not worried about it.
	// Hub startup and shutdown are exempt from this, which is when they are used most
	/*
	public static void cleanUp() {
		while (OperationContext.hubcontext.messages.size() > 1000)
			OperationContext.hubcontext.messages.remove(0);
		
		while (OperationContext.defaultcontext.messages.size() > 1000)
			OperationContext.defaultcontext.messages.remove(0);
	}
	*/
	
	/**
	 * @return context of the current thread, if any
	 *         otherwise the guest context
	 */
	static public OperationContext get() {
		OperationContext tc = OperationContext.context.get();
		
		if (tc == null) {
			// TODO someday monitor how often/where this happens
			//System.out.println("someplace without a context");
			
			tc = OperationContext.defaultcontext;
		}
		
		return tc;
	}
	
	// does the current thread have a context?
	static public boolean hasContext() {
		return (OperationContext.context.get() != null);
	}
	
	/**
	 * @param v context for current thread to use
	 */
	static public void set(OperationContext v) {
		if (v != null)
			OperationContext.context.set(v);
	}
	
	/**
	 * @return context of the current thread, if any, otherwise the hub context
	 */
	static public OperationContext getOrHub() {
		OperationContext tc = OperationContext.context.get();
		
		if (tc == null)
			tc = OperationContext.hubcontext;
		
		return tc;
	}
	
	/**
	 * @return create a new guest context
	 */
	static public OperationContext allocateGuest() {
		// for occasions where no context is set when calling allocate - we need some context
		if (!OperationContext.hasContext())
			OperationContext.context.set(OperationContext.defaultcontext);

		return new OperationContextBuilder().withGuestTaskTemplate().toOperationContext();
	}
	
	/**
	 * Sets the current thread to use a new guest context
	 * 
	 * @return create a new guest context
	 */
	static public OperationContext useNewGuest() {
		OperationContext tc = OperationContext.allocateGuest();
		OperationContext.context.set(tc);
		return tc;
	}
	
	/**
	 * @return create a new root context
	 */
	static public OperationContext allocateRoot() {
		// for occasions where no context is set when calling allocate - we need some context
		if (!OperationContext.hasContext())
			OperationContext.context.set(OperationContext.defaultcontext);

		return new OperationContextBuilder().withRootTaskTemplate().toOperationContext();
	}
	
	/**
	 * Sets the current thread to use a new root context
	 * 
	 * @return create a new root context
	 */
	static public OperationContext useNewRoot() {
		OperationContext tc = OperationContext.allocateRoot();
		OperationContext.context.set(tc);
		return tc;
	}
	
	/*
	 * Sets the current thread to use a new context
	 * 
	 * @return create a new context
	 */
	static public OperationContext use(OperationContextBuilder tcb) {
		OperationContext tc = OperationContext.allocate(tcb);
		OperationContext.context.set(tc);
		return tc;
	}
	
	static public OperationContext use(UserContext ctx, OperationContextBuilder tcb) {
		OperationContext tc = OperationContext.allocate(ctx, tcb);
		OperationContext.context.set(tc);
		return tc;
	}
	
	/*
	 * @param m create a task context from a message (RPC calls to dcBus), keep in mind
	 * this is info gathering only, message must not be allowed to force an 
	 * authenticated/elevated state inappropriately - from RPC clear "Elevated"
	 * field before calling this
	 */
	static public OperationContext allocate(Message m) {
		// for occasions where no context is set when calling allocate - we need some context
		if (!OperationContext.hasContext())
			OperationContext.context.set(OperationContext.defaultcontext);

		return new OperationContext(m.getFieldAsRecord("Context")); 
	}
		
	static public OperationContext allocate(RecordStruct ctx) {
		// for occasions where no context is set when calling allocate - we need some context
		if (!OperationContext.hasContext())
			OperationContext.context.set(OperationContext.defaultcontext);

		return new OperationContext(ctx); 
	}
	
	static public OperationContext allocate(UserContext usr, RecordStruct ctx) {
		// for occasions where no context is set when calling allocate - we need some context
		if (!OperationContext.hasContext())
			OperationContext.context.set(OperationContext.defaultcontext);

		return new OperationContext(usr, ctx); 
	}
	
	static public OperationContext allocate(OperationContextBuilder tcb) {
		// for occasions where no context is set when calling allocate - we need some context
		if (!OperationContext.hasContext())
			OperationContext.context.set(OperationContext.defaultcontext);

		return new OperationContext(tcb.values); 
	}
		
	static public OperationContext allocate(UserContext usr, OperationContextBuilder tcb) {
		// for occasions where no context is set when calling allocate - we need some context
		if (!OperationContext.hasContext())
			OperationContext.context.set(OperationContext.defaultcontext);

		return new OperationContext(usr, tcb.values); 
	}	
	
	static protected String allocateOpId() {
		long num = OperationContext.nextid.getAndIncrement();
		
		// TODO confirm this really does work
		/*
		if (num > 999999999999999L) {
			synchronized (TaskContext.nextid) {
				if (TaskContext.nextid.get()> 999999999999999L)
					TaskContext.nextid.set(0);				
			}
			
			num = TaskContext.nextid.getAndIncrement();
		}
		*/
		
		return OperationContext.getHubId() 
			+ "_" + OperationContext.getRunId() 
			+ "_" + StringUtil.leftPad(num + "", 15, '0');
	}
	
	/*
	 * set current thread context to null 
	 */
	static public void clear() {
		OperationContext.context.set(null);
	}
	
	static public void isGuest(final FuncCallback<Boolean> cb) {
		OperationContext.isGuest(OperationContext.get(), cb);
	}
	
	/*
	 * @return check to see if the task is really no more than a guest access.  does not change task context
	 */
	static public void isGuest(OperationContext ctx, final FuncCallback<Boolean> cb) {
		if ((ctx == null) || (ctx.userctx == null)) {
			cb.setResult(false);
			cb.complete();
			return;
		}
			
		if (ctx.userctx.looksLikeGuest()) {
			cb.setResult(true);
			cb.complete();
			return;
		}
		
		ctx.verify(new FuncCallback<UserContext>() {
			@Override
			public void callback() {
				cb.setResult(this.getResult().looksLikeGuest());
				cb.complete();
			}
		});
	}

	// generally don't mutate a context, but for sign-in support we need to
	public static void switchUser(OperationContext ctx, UserContext usr) {
		// TODO change only if usr != this.opcontext.getUser  
		ctx.userctx = usr;
	}

	// generally don't mutate a context, but sometimes this is fine - use internally only
	public static void elevate(OperationContext ctx) {
		ctx.opcontext.withField("Elevated", true);
	}
	
	// instance code

	// ======================================================
	// these vars travel with calls to bus
	// ======================================================
	
	protected RecordStruct opcontext = null;
	protected UserContext userctx = null;
	protected DebugLevel level = Logger.getGlobalLevel();
	
	protected boolean limitLog = true;
	protected int logOffset = 0;
	
	protected List<RecordStruct> messages = new ArrayList<>();
	
	// ======================================================
	// these vars used only locally, not included in bus calls
	// nor in any workqueue calls, these work locally only
	// ======================================================
	
	// the current task run, if any
	protected WeakReference<TaskRun> taskrun = null;
	
	protected OperationContext parent = null;
	protected List<WeakReference<OperationContext>> children = new ArrayList<>();

	// progress tracking
    protected int progTotalSteps = 0;
    protected int progCurrStep = 0;
    protected String progStepName = null;
    protected int progComplete = 0;	
    protected String progMessage = null;
    
	protected List<IOperationObserver> observers = new CopyOnWriteArrayList<>();
    protected IOperationLogger logger = null;
    
    // this tracks time stamp of signs of life from the job writing to the log/progress tracks
    // volatile helps keep threads on same page - issue found in code testing and this MAY have helped 
    volatile protected long lastactivity = System.currentTimeMillis();
	
    public void touch() {
    	this.lastactivity = System.currentTimeMillis();
    }

    // touch parent context too 
    public void deepTouch() {
		this.fireEvent(OperationEvents.PROGRESS, OperationEvents.PROGRESS_AMOUNT);
    }
    
    public long getLastActivity() {
		return this.lastactivity;
	}
	
    public void setLimitLog(boolean v) {
		this.limitLog = v;
	}
    
    public boolean isLimitLog() {
    	return this.limitLog;
    }
    
    public int logMarker() {
    	return this.logOffset + this.messages.size();
    }
    
	// once elevated we can call any service we want, but first we must
	// call a service we are allowed to call
	
	/*
	 * Elevated tasks have been a) authenticated and b) passed successfully into
	 * a service.  Once elevated all subsequent calls with the task no longer need
	 * to be authenticated or authorized by DivConq framework (individual services/modules
	 * may require it).  Meaning that "guest" cannot call "SendMail" unless it first 
	 * goes through a service that is open to guests, such as password recovery.  
	 * 
	 * Mark the task context as elevated - typically app code does not need to call
	 * this because the services and scheduler handlers decide when a task has met
	 * the desired state. 
	 */

	/**
	 * @return a unique task id - unique across all deployed hub, across runs of a hub
	 */
	public String getOpId() {
		return this.opcontext.getFieldAsString("OpId");
	}

	/**
	 * @return the the user context for this task (user context may be shared with other tasks)
	 */
	public UserContext getUserContext() {
		return this.userctx;
	}

	/**
	 * not all tasks will have a session, but if there is a session here it is. 
	 * 
	 * id is in the format of hubid_sessionid
	 * 
	 * @return the id of the session that spawned this task
	 * 
	 */
	public String getSessionId() {
		return this.opcontext.getFieldAsString("SessionId");
	}

	/**
	 * not all tasks will have a session, but if there is a session here it is.  sessions are local 
	 * to a hub and are not transfered to another hub with the rest of the task info when calling
	 * a remote service.
	 * 
	 * @return the session for this task (user context may be shared with other tasks)
	 */
	public Session getSession() {
		return Hub.instance.getSessions().lookup(this.getSessionId());
	}
	
	public TaskRun getTaskRun() {
		WeakReference<TaskRun> trr = this.taskrun;
		
		if (trr != null) 
			return trr.get();
		
		return null;
	}
	
	public void setTaskRun(TaskRun v) {
		this.taskrun = new WeakReference<TaskRun>(v);
	}
	
	/**
	 * @return logging level to use with this task
	 */
	public DebugLevel getLevel() {
		return this.level;
	}
	
	public void setLevel(DebugLevel v) {
		this.level = v;
	}
	
	/**
	 * Origin indicates where this task originated from.  "hub:" means it was started by
	 * the a hub (task id gives away which hub).  "http:[ip address]" means the task
	 * was started in response to a web request.  "ws:[ip address]" means the task
	 * was started in response to a web scoket request.  "ftp:[ip address]" means the task
	 * was started in response to a ftp request.  Etc.
	 * 
	 * @return origin string 
	 */
	public String getOrigin() {
		return this.opcontext.getFieldAsString("Origin");
	}

	/**
	 * Elevated tasks have been a) authenticated and b) passed successfully into
	 * a service.  Once elevated all subsequent calls with the task no longer need
	 * to be authenticated or authorized by DivConq framework (individual services/modules
	 * may require it).  Meaning that "guest" cannot call "SendMail" unless it first 
	 * goes through a service that is open to guests, such as password recovery.  
	 * 
	 * @return true if task has been elevated
	 */
	public boolean isElevated() {
		return this.opcontext.getFieldAsBooleanOrFalse("Elevated");
	}

	public boolean isGateway() {
		return this.opcontext.getFieldAsBooleanOrFalse("Gateway");
	}
	
	// only use during hub booting
	protected OperationContext() {
		this.opcontext = new RecordStruct();

		this.userctx = new UserContext();
	}
	
	/**
	 * @param ctx create a task context from a RecordStruct, keep in mind
	 * this is info gathering only, call must set 
	 * authenticated/elevated state inappropriately
	 */
	protected OperationContext(RecordStruct ctx) {
		this.opcontext = ctx;

		if (ctx.isFieldEmpty("OpId"))
			ctx.setField("OpId", OperationContext.allocateOpId());		
		
		if (!ctx.isFieldEmpty("DebugLevel"))
			this.level = DebugLevel.valueOf(ctx.getFieldAsString("DebugLevel"));	
		
		this.userctx = UserContext.allocateFromTask(ctx);
	}
	
	protected OperationContext(UserContext usr, RecordStruct ctx) {
		this.opcontext = ctx;

		if (ctx.isFieldEmpty("OpId"))
			ctx.setField("OpId", OperationContext.allocateOpId());		
		
		if (!ctx.isFieldEmpty("DebugLevel"))
			this.level = DebugLevel.valueOf(ctx.getFieldAsString("DebugLevel"));	
		
		this.userctx = usr;
	}
	
	public OperationContextBuilder toBuilder() {
		return new OperationContextBuilder(this.freezeToRecord());
	}
	
	/**
	 * @param m store task context into a message - for context transfer over bus
	 */
	public void freeze(Message m) {
		m.setField("Context", this.freezeToRecord());
	}
	
	public RecordStruct freezeToRecord() {
		RecordStruct clone = (RecordStruct) this.opcontext.deepCopy();
		
		this.userctx.freeze(clone);
		
		clone.setField("DebugLevel", this.level.toString());
		
		return clone;
	}
	
	public RecordStruct freezeToSafeRecord() {
		RecordStruct clone = (RecordStruct) this.opcontext.deepCopy();
		
		this.userctx.freezeSafe(clone);
		
		clone.setField("DebugLevel", this.level.toString());
		
		return clone;
	}
	
	// return an approved/verified user context (guest if nothing else)
	// verify says - the given auth token, if any, is valid - if there is none then you are a guest and that is valid
	// 
	public void verify(FuncCallback<UserContext> cb) {
		if (this.userctx == null) {
			cb.errorTr(444);
			cb.setResult(UserContext.allocateGuest());
			cb.complete();
			return;
		}
			
		if (this.userctx.isVerified() || this.isElevated()) {
			cb.setResult(this.userctx);
			cb.complete();
			return;
		}
		
		Message msg = new Message("dcAuth", "Authentication", "Verify");
    	
		Hub.instance.getBus().sendMessage(msg, r ->	{		
			if (r.hasErrors()) 
				cb.setResult(UserContext.allocateGuest());
			else 
				cb.setResult(r.getContext().getUserContext());
			
			cb.complete();
		});
	}
	
	/**
	 * @param tags to search for with this user
	 * @return true if this user has one of the requested authorization tags  (does not check authentication)
	 */
	public boolean isAuthorized(String... tags) {
		if (this.isElevated())
			return true;		// always ok
		
		if (!this.userctx.isVerified())
			return false;
		
		return this.userctx.isTagged(tags);
	}
	
	@Override
	public String toString() {
		// capture both this and the user
		return this.freezeToRecord().toPrettyString(); 
	}
	
	/**
	 * Overrides any previous return codes and messages
	 * 
	 * @param code code for message
	 * @param msg message
	 */
	public void exit(long code, String msg) {
		if (StringUtil.isNotEmpty(msg))
			this.log(DebugLevel.Info, code, msg, "Exit");
		else 
			this.boundary("Code", code + "", "Exit");
	}

	public void clearExitCode() {
		this.exit(0, null);
	}

	// search backward through log to find an error, if we hit a message with an Exit tag then
	// stop, as Exit resets Error (unless it is an error itself)
	// similar to findExitEntry but stops after last Error as we don't need to loop through all
	public boolean hasErrors() {
		for (int i = this.messages.size() - 1; i >= 0; i--) {
			RecordStruct msg =  this.messages.get(i);
			
			if ("Error".equals(msg.getFieldAsString("Level")))
				return true;
		
			if (msg.hasField("Tags")) {
				ListStruct tags = msg.getFieldAsList("Tags");
				
				if (tags.stringStream().anyMatch(tag -> tag.equals("Exit")))
					break;
			}
		}
		
		return false;
	}

	public long getCode() {
		RecordStruct entry = this.findExitEntry();
		
		if (entry == null)
			return 0;
		
		return entry.getFieldAsInteger("Code", 0);
	}

	public String getMessage() {
		RecordStruct entry = this.findExitEntry();
		
		if (entry == null)
			return null;
		
		return entry.getFieldAsString("Message");
	}

	public RecordStruct findExitEntry() {
		return this.findExitEntry(0, -1);
	}

	// search backward through log to find an exit, if we hit a message with an Exit tag then
	// stop, as Exit resets Error.  now return the first error after Exit.  if no errors after
	// then return Exit
	public RecordStruct findExitEntry(int msgStart, int msgEnd) {
		msgStart -= this.logOffset;		// adjust so the markers are relative to the current collection of messages, assuming some may have been purged
		
		if (msgEnd == -1)
			msgEnd = this.messages.size();
		else
			msgEnd -= this.logOffset;
			
		RecordStruct firsterror = null;
		
		for (int i = msgEnd - 1; i >= msgStart; i--) {
			RecordStruct msg =  this.messages.get(i);
			
			if ("Error".equals(msg.getFieldAsString("Level")))
				firsterror = msg;
		
			if (msg.hasField("Tags")) {
				ListStruct tags = msg.getFieldAsList("Tags");
				
				if (tags.stringStream().anyMatch(tag -> tag.equals("Exit")))
					return (firsterror != null) ? firsterror : msg;
			}
		}
		
		return firsterror;
	}

	public ListStruct getMessages() {
		return new ListStruct(this.messages.toArray());	
	}

	public ListStruct getMessages(int msgStart, int msgEnd) {
		msgStart -= this.logOffset;		// adjust so the markers are relative to the current collection of messages, assuming some may have been purged
		
		if (msgEnd == -1)
			msgEnd = this.messages.size();
		else
			msgEnd -= this.logOffset;
		
		return new ListStruct(this.messages.subList(msgStart, msgEnd).toArray());	
	}

	/**
	 * @param code to search for
	 * @return true if an error code is present
	 */
	public boolean hasCode(long code) {
		return this.hasCode(code, 0, -1);
	}

	public boolean hasCode(long code, int msgStart, int msgEnd) {
		msgStart -= this.logOffset;		// adjust so the markers are relative to the current collection of messages, assuming some may have been purged
		
		if (msgEnd == -1)
			msgEnd = this.messages.size();
		else
			msgEnd -= this.logOffset;
		
		for (int i = msgStart; i < msgEnd; i++) {
			RecordStruct msg =  this.messages.get(i); 
		
			if (msg.getFieldAsInteger("Code") == code)
				return true;
		}
		
		return false;
	}

	// search backward through log to find an error, if we hit a message with an Exit tag then
	// stop, as Exit resets Error (unless it is an error itself)
	// similar to findExitEntry but stops after last Error as we don't need to loop through all
	public boolean hasLevel(int msgStart, int msgEnd, DebugLevel lvl) {
		msgStart -= this.logOffset;		// adjust so the markers are relative to the current collection of messages, assuming some may have been purged
		
		if (msgEnd == -1)
			msgEnd = this.messages.size();
		else
			msgEnd -= this.logOffset;

		String slvl = lvl.toString();
		
		for (int i = msgStart; i < msgEnd; i++) {
			RecordStruct msg =  this.messages.get(i);
			
			if (slvl.equals(msg.getFieldAsString("Level")))
				return true;
		}
		
		return false;
	}

	public IOperationLogger getLogger() {
		return this.logger;
	}
	
	public String getLog() {
		IOperationLogger logger = this.logger;

		if (logger != null)
				return logger.logToString();	
				
		// TODO reformat these as log entries not as JSON
		return this.getMessages().toString();
	}
	
    public void error(String message, String... tags) {
		this.log(DebugLevel.Error, 1, message, tags);
    }
    
    public void error(long code, String message, String... tags) {
		this.log(DebugLevel.Error, code, message, tags);
    }
    
    public void warn(String message, String... tags) {
		this.log(DebugLevel.Warn, 2, message, tags);
    }
    
    public void warn(long code, String message, String... tags) {
		this.log(DebugLevel.Warn, code, message, tags);
    }
    
    public void info(String message, String... tags) {
		this.log(DebugLevel.Info, 0, message, tags);
    }
    
    public void info(long code, String message, String... tags) {
		this.log(DebugLevel.Info, code, message, tags);
    }
    
    public void debug(String message, String... tags) {
		this.log(DebugLevel.Debug, 0, message, tags);
    }
    
    public void debug(long code, String message, String... tags) {
		this.log(DebugLevel.Debug, code, message, tags);
    }
    
    public void trace(String message, String... tags) {
		this.log(DebugLevel.Trace, 0, message, tags);
    }
    
    public void trace(long code, String message, String... tags) {
		this.log(DebugLevel.Trace, code, message, tags);
    }
	
    // let Logger translate to the language of the log file - let tasks translate to their own logs in
    // the language of the context 
    
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void traceTr(long code, Object... params) {
		this.logTr(DebugLevel.Trace, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void debugTr(long code, Object... params) {
		this.logTr(DebugLevel.Debug, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void infoTr(long code, Object... params) {		
		this.logTr(DebugLevel.Info, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void warnTr(long code, Object... params) {
		this.logTr(DebugLevel.Warn, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void errorTr(long code, Object... params) {
		this.logTr(DebugLevel.Error, code, params);
	}
	
	public void exitTr(long code, Object... params) {
		String msg = this.tr("_code_" + code, params);

		this.exit(code, msg);
	}
	
	/**
	 * @param lvl level of message
	 * @param code for message
	 * @param msg text of message
	 * @param tags of message
	 */
	public void log(DebugLevel lvl, long code, String msg, String... tags) {
		// must be some sort of message
		if (StringUtil.isEmpty(msg))
			return;
		
		RecordStruct entry = new RecordStruct(
				new FieldStruct("Occurred", new DateTime(DateTimeZone.UTC)),
				new FieldStruct("Level", lvl.toString()),
				new FieldStruct("Code", code),
				new FieldStruct("Message", msg)
		);
		
		if (tags.length > 0)
			entry.setField("Tags", new ListStruct((Object[])tags));
		
		this.log(entry, lvl);
		
		// pass the message to logger 
		if (this.getLevel().getCode() >= lvl.getCode()) {
			// don't record 0, 1 or 2 - no generic codes
			if (code > 2) {
				tags = Arrays.copyOf(tags, tags.length + 2);
				tags[tags.length - 2] = "Code";
				tags[tags.length - 1] = code + "";
			}

			Logger.logWr(this.getOpId(), lvl, msg, tags);
		}
	}
	
	/**
	 * @param lvl level of message
	 * @param code for message
	 * @param params parameters to the message string
	 */
	public void logTr(DebugLevel lvl, long code, Object... params) {
		String msg = this.tr("_code_" + code, params);
		
		RecordStruct entry = new RecordStruct(
				new FieldStruct("Occurred", new DateTime(DateTimeZone.UTC)),
				new FieldStruct("Level", lvl.toString()),
				new FieldStruct("Code", code),
				new FieldStruct("Message", msg)
		);
		
		this.log(entry, lvl);
	
		// pass the code to logger 
		if (this.getLevel().getCode() >= lvl.getCode()) 
			Logger.logWr(this.getOpId(), lvl, code, params);
	}
    
    /**
     * Add a logging boundary, delineating a new section of work for this task
     * 
     * @param tags identity of this boundary
     */
    public void boundary(String... tags) {
		RecordStruct entry = new RecordStruct(
				new FieldStruct("Occurred", new DateTime(DateTimeZone.UTC)),
				new FieldStruct("Level", DebugLevel.Info.toString()),
				new FieldStruct("Code", 0),
				new FieldStruct("Tags", new ListStruct((Object[])tags))
		);
		
		this.log(entry, DebugLevel.Info);
		
		// pass the code to logger 
		if (this.getLevel().getCode() >= DebugLevel.Info.getCode())
			Logger.boundaryWr(this.getOpId(), tags);
    }
	
	// logging is hard on heap and GC - so only do it if necessary
	// not generally called by code, internal use mostly
    // call this to bypass the Hub logger - for example a bus callback 
	public void log(RecordStruct entry) {
		this.log(entry, DebugLevel.parse(entry.getFieldAsString("Level")));
	}
	
	public void log(RecordStruct entry, DebugLevel lvl) {
		// think twice about logging debug or trace so we don't overflow the OC log
		// always log Info, Error, Warn so it bubbles up and so "hasCode" is relable at those levels
		if ((lvl == DebugLevel.Debug) || (lvl == DebugLevel.Trace)) {
			if (this.getLevel().getCode() < lvl.getCode()) 
				return;
		}
		
		if (this.limitLog) {
			while (this.messages.size() > 999) {		// no more than 1000 messages when limit is on
				this.messages.remove(0);
				this.logOffset++;
			}
		}
		
		// this isn't thread safe, and much of the time it won't be much of an issue
		// but could consider Stamp Lock approach to accessing messages array
		this.messages.add(entry);
		
		this.fireEvent(OperationEvents.LOG, entry);
	}
	
	public void logResult(RecordStruct v) {
		ListStruct h = v.getFieldAsList("Messages");
		
		if (h != null) {
			for (Struct st : h.getItems()) 
				this.log((RecordStruct) st);
		}
	}

	public boolean isLevel(DebugLevel debug) {
		return (this.getLevel().getCode() >= debug.getCode());
	}

	// progress methods
	
	/**
	 * @return units/percentage of task completed
	 */
	public int getAmountCompleted() {
		return this.progComplete; 
	}
	
	/**
	 * @param v units/percentage of task completed
	 */
	public void setAmountCompleted(int v) { 
		this.progComplete = v; 
		
		this.fireEvent(OperationEvents.PROGRESS, OperationEvents.PROGRESS_AMOUNT);
	}

	/**
	 * @return status message about task progress
	 */
	public String getProgressMessage() {
		return this.progMessage; 
	}
	
	/**
	 * @param v status message about task progress
	 */
	public void setProgressMessage(String v) { 
		this.progMessage = v; 
		
		this.fireEvent(OperationEvents.PROGRESS, OperationEvents.PROGRESS_MESSAGE);
	}
	
	/**
	 * @param code message translation code
	 * @param params for the message string
	 */
	public void setProgressMessageTr(int code, Object... params) { 
		this.progMessage = this.tr("_code_" + code, params); 
		
		this.fireEvent(OperationEvents.PROGRESS, OperationEvents.PROGRESS_MESSAGE);
	}

	/**
	 * @return total steps for this specific task
	 */
	public int getSteps() { 
		return this.progTotalSteps; 
	}
	
	/**
	 * @param v total steps for this specific task
	 */
	public void setSteps(int v) { 
		this.progTotalSteps = v; 
	}

	/**
	 * @return current step within this specific task
	 */
	public int getCurrentStep() { 
		return this.progCurrStep; 
	}
	
	/**
	 * Set step name first, this triggers observers
	 * 
	 * @param step current step number within this specific task
	 * @param name current step name within this specific task
	 */
	public void setCurrentStep(int step, String name) { 
		this.progCurrStep = step; 
		this.progStepName = name; 
		
		this.fireEvent(OperationEvents.PROGRESS, OperationEvents.PROGRESS_STEP);
	}
	
	/**
	 * Set step name first, this triggers observers
	 * 
	 * @param name current step name within this specific task
	 */
	public void nextStep(String name) { 
		this.progCurrStep++; 
		this.progStepName = name; 
		
		this.fireEvent(OperationEvents.PROGRESS, OperationEvents.PROGRESS_STEP);
	}

	/**
	 * @return name of current step
	 */
	public String getCurrentStepName() { 
		return this.progStepName; 
	}
	
	/**
	 * @param step number of current step
	 * @param code message translation code
	 * @param params for the message string
	 */
	public void setCurrentStepNameTr(int step, int code, Object... params) {
		String name = this.tr("_code_" + code, params);
				
		this.progCurrStep = step; 
		this.progStepName = name; 
		
		this.fireEvent(OperationEvents.PROGRESS, OperationEvents.PROGRESS_STEP);
	}    
	
	/**
	 * @param code message translation code
	 * @param params for the message string
	 */
	public void nextStepTr(int code, Object... params) {
		String name = this.tr("_code_" + code, params);
				
		this.progCurrStep++; 
		this.progStepName = name; 
		
		this.fireEvent(OperationEvents.PROGRESS, OperationEvents.PROGRESS_STEP);
	}    
	
	public void addObserver(IOperationObserver oo) {
		// the idea is that we want to unwind the callbacks in LILO order
		if (!this.observers.contains(oo))
			this.observers.add(0, oo);
		
		if ((oo instanceof IOperationLogger) && (this.logger == null))
			this.logger = (IOperationLogger) oo;
	}
	
	public int countObservers() {
		return this.observers.size();
	}
	
	public void removeObserver(IOperationObserver o) {
		this.observers.remove(o);
	}	

	public OperationContext subContext() {
		OperationContext sub = this.toBuilder().toOperationContext();
		
		sub.setParent(this);
		//sub.addObserver(new ParentLogger(this));
		
		this.children.add(new WeakReference<OperationContext>(sub));
		
		return sub;
	}

	protected void setParent(OperationContext v) {
		this.parent = v;
	}
    
    // events might fire from external context, keep this in mind
    public void fireEvent(OperationEvent event, Object detail) {
    	this.fireEvent(this, event, detail);
	}
    
    // events might fire from external context, keep this in mind
    public void fireEvent(OperationContext src, OperationEvent event, Object detail) {
		OperationContext curr = OperationContext.get();
		
		try {
			this.touch();
			
			for (IOperationObserver ob : this.observers) {
		    	OperationContext.set(this);
		    	
				ob.fireEvent(event, src, detail);
			}
			
			if (this.parent != null) {
				if (event == OperationEvents.LOG)
					this.parent.log((RecordStruct) detail);
				else if (event == OperationEvents.PROGRESS)
					this.parent.fireEvent(src, event, detail);
			}
			
			// TODO missing concept here
			// how do we capture a COMPLETED event after it has passed
			// some events need to be flagged as enduring and then kept in a list
			// when an observer is added it should be handed the enduring list
			
			// do not use locks here or in addObserver - too expensive - officially 
			// we only support listeners to COMPLETE, START, PREP, START that
			// are added before we start - so we may never want to add any more support
			// here, however, if we do, do not include a lock here...that is too expensive
			// relative to benefits
		}
		finally {
	    	OperationContext.set(curr);
		}
    }
	
	public String tr(String token, Object... params) {
		return this.userctx.tr(token, params);
	}
		
	public String trp(String pluraltoken, String singulartoken, Object... params) {
		return this.userctx.trp(pluraltoken, singulartoken, params);
	}
}
