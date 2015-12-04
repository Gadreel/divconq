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
package divconq.log;

import divconq.lang.op.OperationContext;

/**
 * When logging messages to the debug log each message has a debug level.
 * The logger has a filter level and messages of lower priority than the 
 * current debug level will not be logged.
 * 
 * Note that 99% of the time the "current" debug level is determined by
 * the current TaskContext.  The preferred way to log messages is through 
 * the TaskContext or through an OperationResult.  Ultimately a filter
 * is used to determine what should go in the log.  
 * 
 * In fact, when you call "void error(String message, String... tags)"
 * and other logging methods, theses methods will lookup the current
 * task context.  So it is more efficient to work directly with task
 * context, however, occasional calls to these global logger methods
 * are fine.
 * 
 * @author Andy
 *
 */
public class Logger {
    static public boolean isDebug() {
    	// fast fail if debugging not enabled on Hub
    	if (!HubLog.debugEnabled)
    		return false;
    	
    	OperationContext ctx = OperationContext.get();
    	
    	DebugLevel setlevel = (ctx != null) ? ctx.getLevel() : HubLog.globalLevel;
    	
    	return (setlevel.getCode() >= DebugLevel.Debug.getCode());
    }

    static public boolean isTrace() {
    	// fast fail if debugging not enabled on Hub
    	if (!HubLog.debugEnabled)
    		return false;
    	
    	OperationContext ctx = OperationContext.get();
    	
    	DebugLevel setlevel = (ctx != null) ? ctx.getLevel() : HubLog.globalLevel;
    	
    	return (setlevel.getCode() >= DebugLevel.Trace.getCode());
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param message error text
     * @param tags searchable values associated with the message, key-value pairs can be created by putting two tags adjacent
     */
    static public void error(String message, String... tags) {
    	OperationContext ctx = OperationContext.get();
    	
    	if (ctx != null)
    		ctx.error(message, tags);
    	else    	
    		HubLog.log(DebugLevel.Error, message, tags);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param message warning text
     * @param tags searchable values associated with the message, key-value pairs can be created by putting two tags adjacent
     */
    static public void warn(String message, String... tags) {
    	OperationContext ctx = OperationContext.get();
    	
    	if (ctx != null)
    		ctx.warn(message, tags);
    	else    	
    		HubLog.log(DebugLevel.Warn, message, tags);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param message info text
     * @param tags searchable values associated with the message, key-value pairs can be created by putting two tags adjacent
     */
    static public void info(String message, String... tags) {
    	OperationContext ctx = OperationContext.get();
    	
    	if (ctx != null)
    		ctx.info(message, tags);
    	else    	
    		HubLog.log(DebugLevel.Info, message, tags);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param accessCode to translate
     * @param locals for the translation
     */
    static public void debug(String message, String... tags) {
    	// fast fail if debugging not enabled on Hub
    	if (!HubLog.debugEnabled)
    		return;
    	
    	OperationContext ctx = OperationContext.get();
    	
    	if (ctx != null)
    		ctx.debug(message, tags);
    	else    	
    		HubLog.log(DebugLevel.Debug, message, tags);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param accessCode to translate
     * @param locals for the translation
     */
    static public void trace(String message, String... tags) {
    	// fast fail if debugging not enabled on Hub
    	if (!HubLog.debugEnabled)
    		return;
    	
    	OperationContext ctx = OperationContext.get();
    	
    	if (ctx != null)
    		ctx.trace(message, tags);
    	else    	
    		HubLog.log(DebugLevel.Trace, message, tags);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param code to translate
     * @param params for the translation
     */
    static public void errorTr(long code, Object... params) {
    	OperationContext ctx = OperationContext.get();
    	
    	if (ctx != null)
    		ctx.errorTr(code, params);
    	else    	
    		HubLog.log(DebugLevel.Error, code, params);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param code to translate
     * @param params for the translation
     */
    static public void warnTr(long code, Object... params) {
    	OperationContext ctx = OperationContext.get();
    	
    	if (ctx != null)
    		ctx.warnTr(code, params);
    	else    	
    		HubLog.log(DebugLevel.Warn, code, params);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param code to translate
     * @param params for the translation
     */
    static public void infoTr(long code, Object... params) {
    	OperationContext ctx = OperationContext.get();
    	
    	if (ctx != null)
    		ctx.infoTr(code, params);
    	else    	
    		HubLog.log(DebugLevel.Info, code, params);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param code to translate
     * @param params for the translation
     */
    static public void debugTr(long code, Object... params) {
    	// fast fail if debugging not enabled on Hub
    	if (!HubLog.debugEnabled)
    		return;
    	
    	OperationContext ctx = OperationContext.get();
    	
    	if (ctx != null)
    		ctx.debugTr(code, params);
    	else    	
    		HubLog.log(DebugLevel.Debug, code, params);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param code to translate
     * @param params for the translation
     */
    static public void traceTr(long code, Object... params) {
    	// fast fail if debugging not enabled on Hub
    	if (!HubLog.debugEnabled)
    		return;
    	
    	OperationContext ctx = OperationContext.get();
    	
    	if (ctx != null)
    		ctx.traceTr(code, params);
    	else    	
    		HubLog.log(DebugLevel.Trace, code, params);
    }
}
