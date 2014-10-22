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
package divconq.lang;

import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.log.DebugLevel;
import divconq.log.Logger;
import divconq.session.Session;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;
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
	
	static protected ThreadLocal<OperationContext> context = new ThreadLocal<OperationContext>();
	static protected AtomicLong nextid = new AtomicLong();
	
	static {
		OperationContext.runid = TimeUtil.stampFmt.print(new DateTime(DateTimeZone.UTC));
		OperationContext.hubcontext = OperationContext.useNewRoot();
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
	
	/**
	 * @return context of the current thread, if any
	 *         otherwise the guest context
	 */
	static public OperationContext get() {
		OperationContext tc = OperationContext.context.get();
		
		if (tc != null)
			return tc;
		
		return OperationContext.allocateGuest();
	}
	
	// does the current thread have a context?
	static public boolean hasContext() {
		return (OperationContext.context.get() != null);
	}
	
	/**
	 * @param v context for current thread to use
	 */
	static public void set(OperationContext v) {
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
		return new OperationContext(m.getFieldAsRecord("Context")); 
	}
		
	static public OperationContext allocate(RecordStruct ctx) {
		return new OperationContext(ctx); 
	}
	
	static public OperationContext allocate(UserContext usr, RecordStruct ctx) {
		return new OperationContext(usr, ctx); 
	}
	
	static public OperationContext allocate(OperationContextBuilder tcb) {
		return new OperationContext(tcb.values); 
	}
		
	static public OperationContext allocate(UserContext usr, OperationContextBuilder tcb) {
		return new OperationContext(usr, tcb.values); 
	}	
	
	protected static String allocateOpId() {
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
				// be sure to call TaskContext.get() because task context can change during verify
				cb.setResult(this.getResult().looksLikeGuest());
				cb.complete();
			}
		});
	}
	
	// instance code
	
	protected RecordStruct opcontext = null;
	protected UserContext userctx = null;
	protected DebugLevel level = Logger.getGlobalLevel();
	
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
	
	/**
	 * @return logging level to use with this task
	 */
	public DebugLevel getLevel() {
		return this.level;
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
			cb.copyMessages(r);
			
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
    
    public void error(String message, String... tags) {
    	Logger.log(this, DebugLevel.Error, message, tags);
    }
    
    public void warn(String message, String... tags) {
    	Logger.log(this, DebugLevel.Warn, message, tags);
    }
    
    public void info(String message, String... tags) {
    	Logger.log(this, DebugLevel.Info, message, tags);
    }
    
    public void trace(String message, String... tags) {
    	Logger.log(this, DebugLevel.Trace, message, tags);
    }
    
    public void debug(String message, String... tags) {
    	Logger.log(this, DebugLevel.Debug, message, tags);
    }
    
    public void logBoundary(String... tags) {
    	Logger.boundary(this, tags);
    }
    
    public void log(DebugLevel level, String message, String... tags) {
    	Logger.log(this, level, message, tags);
    }
	
    // let Logger translate to the language of the log file - let tasks translate to their own logs in
    // the language of the TaskContext - so below does not accomplish the goals
    
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void traceTr(long code, Object... params) {
    	Logger.log(this, DebugLevel.Trace, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void debugTr(long code, Object... params) {
    	Logger.log(this, DebugLevel.Debug, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void infoTr(long code, Object... params) {		
    	Logger.log(this, DebugLevel.Info, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void warnTr(long code, Object... params) {
    	Logger.log(this, DebugLevel.Warn, code, params);
	}
	
	/**
	 * @param code for message translation token
	 * @param params for message translation
	 */
	public void errorTr(long code, Object... params) {
    	Logger.log(this, DebugLevel.Error, code, params);
	}
	
	public String tr(String token, Object... params) {
		return this.userctx.tr(token, params);
	}
		
	public String trp(String pluraltoken, String singulartoken, Object... params) {
		return this.userctx.trp(pluraltoken, singulartoken, params);
	}
}
