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

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import divconq.lang.Memory;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.locale.LocaleUtil;
import divconq.util.HexUtil;
import divconq.xml.XElement;

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
    static protected DebugLevel globalLevel = DebugLevel.Info;
	static protected String locale = Locale.getDefault().toString();
    
    // typically task logging is handled by a service on the bus, but on occasions
    // we want it to log to the file as well, from settings change this to 'true' 
    static protected boolean toFile = true;
    static protected boolean toConsole = true;
    
    static protected PrintWriter logWriter = null;
    static protected ReentrantLock writeLock = new ReentrantLock();  
    static protected long filestart = 0;
    
    static protected ILogHandler handler = null;
    
    static protected XElement config = null;
    
    static public DebugLevel getGlobalLevel() {
        return Logger.globalLevel; 
    }
    
    static public void setGlobalLevel(DebugLevel v) {
        Logger.globalLevel = v; 
        
        // keep hub context up to date
		OperationContext.updateHubContext();
    }
    
    static public String getLocale() {
        return Logger.locale; 
    }
    
    static public void setLocale(String v) {
        Logger.locale = v; 
        
        // keep hub context up to date
		OperationContext.updateHubContext();
    }
    
    static public void setLogHandler(ILogHandler v) {
    	Logger.handler = v;
    }
    
    static public void setToConsole(boolean v) {
    	Logger.toConsole = v;
    }
   
    /**
     * Called from Hub.start this method configures the logging features.
     * 
     * @param config xml holding the configuration
     */
    static public void init(XElement config) {
    	Logger.config = config;
    	
    	// TODO return operation result
    	
    	// TODO load levels, path etc
    	// include a setting for startup logging - if present set the TC log level directly
    	
		Logger.startNewLogFile();
    	
		// set by operation context init 
    	//Logger.locale = LocaleUtil.getDefaultLocale();
		
		// From here on we can use netty and so we need the logger setup
		
		InternalLoggerFactory.setDefaultFactory(new divconq.log.netty.LoggerFactory());
    	
    	if (Logger.config != null) {
    		// set by operation context init 
    		//if (Logger.config.hasAttribute("Level"))
    	    //	Logger.globalLevel = DebugLevel.parse(Logger.config.getAttribute("Level"));
		
    		if (Logger.config.hasAttribute("NettyLevel")) {
    			ResourceLeakDetector.setLevel(Level.valueOf(Logger.config.getAttribute("NettyLevel")));
    			
    			Logger.debug("Netty Level set to: " + ResourceLeakDetector.getLevel());    			
    		}
    		else if (!"none".equals(System.getenv("dcnet"))) {
    			// TODO anything more we should do here?  maybe paranoid isn't helpful?
    		}
    		
    		// set by operation context init 
    		//if (Logger.config.hasAttribute("Locale"))
    	    //	Logger.locale = Logger.config.getAttribute("Locale");
    	}
    }
    
    static protected void startNewLogFile() {
    	try {
    		File logfile = new File("./logs/" 
					+ DateTimeFormat.forPattern("yyyyMMdd'_'HHmmss").print(new DateTime(DateTimeZone.UTC))
					+ ".log"); 
    		
    		if (!logfile.getParentFile().exists())
    			if (!logfile.getParentFile().mkdirs())
    				Logger.error("Unable to create logs folder.");
    		
    		logfile.createNewFile();

    		if (Logger.logWriter != null) {
    			Logger.logWriter.flush();
    			Logger.logWriter.close();
    		}
    		
    		Logger.trace("Opening log file: " + logfile.getCanonicalPath());
    		
			Logger.logWriter = new PrintWriter(logfile, "utf-8");
			
			Logger.filestart = System.currentTimeMillis();
		} 
    	catch (Exception x) {
			Logger.error("Unable to create log file: " + x);
		}
    }
    
    /*
     *  In a distributed setup, DivConq may route logging to certain Hubs and
     *  bypass the local log file.  During shutdown logging returns to local
     *  log file so that the dcBus can shutdown and stop routing the messages.
     * @param or 
     */
    static public void stop(OperationResult or) {
    	// TODO return operation result
    	
    	Logger.toFile = true;		// go back to logging to file
    	
    	// TODO say no to database
    }

    static public boolean isDebug() {
    	OperationContext ctx = OperationContext.get();
    	
    	DebugLevel setlevel = (ctx != null) ? ctx.getLevel() : Logger.globalLevel;
    	
    	return (setlevel.getCode() >= DebugLevel.Debug.getCode());
    }

    static public boolean isTrace() {
    	OperationContext ctx = OperationContext.get();
    	
    	DebugLevel setlevel = (ctx != null) ? ctx.getLevel() : Logger.globalLevel;
    	
    	return (setlevel.getCode() >= DebugLevel.Trace.getCode());
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param message error text
     * @param tags searchable values associated with the message, key-value pairs can be created by putting two tags adjacent
     */
    static public void error(String message, String... tags) {
    	Logger.log(OperationContext.get(), DebugLevel.Error, message, tags);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param message warning text
     * @param tags searchable values associated with the message, key-value pairs can be created by putting two tags adjacent
     */
    static public void warn(String message, String... tags) {
    	Logger.log(OperationContext.get(), DebugLevel.Warn, message, tags);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param message info text
     * @param tags searchable values associated with the message, key-value pairs can be created by putting two tags adjacent
     */
    static public void info(String message, String... tags) {
    	Logger.log(OperationContext.get(), DebugLevel.Info, message, tags);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param accessCode to translate
     * @param locals for the translation
     */
    static public void debug(String message, String... tags) {
    	Logger.log(OperationContext.get(), DebugLevel.Debug, message, tags);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param accessCode to translate
     * @param locals for the translation
     */
    static public void trace(String message, String... tags) {
    	Logger.log(OperationContext.get(), DebugLevel.Trace, message, tags);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param code to translate
     * @param params for the translation
     */
    static public void errorTr(long code, Object... params) {
    	Logger.log(OperationContext.get(), DebugLevel.Error, code, params);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param code to translate
     * @param params for the translation
     */
    static public void warnTr(long code, Object... params) {
    	Logger.log(OperationContext.get(), DebugLevel.Warn, code, params);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param code to translate
     * @param params for the translation
     */
    static public void infoTr(long code, Object... params) {
    	Logger.log(OperationContext.get(), DebugLevel.Info, code, params);
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param code to translate
     * @param params for the translation
     */
    static public void traceTr(long code, Object... params) {
    	Logger.log(OperationContext.get(), DebugLevel.Trace, code, params);
    }
    
    /*
     * Insert a (string) translated message into the log
     * 
     * @param ctx context for log settings, null for none
     * @param level message level
     * @param code to translate
     * @param params for the translation
     */
    static public void log(OperationContext ctx, DebugLevel level, long code, Object... params) {
    	Logger.log(ctx, level, LocaleUtil.tr(Logger.locale, "_code_" + code, params), "Code", code + "");
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param ctx context for log settings, null for none
     * @param level message level
     * @param message text to store in log
     * @param tags searchable values associated with the message, key-value pairs can be created by putting two tags adjacent
     */
    static public void log(OperationContext ctx, DebugLevel level, String message, String... tags) {
    	DebugLevel setlevel = (ctx != null) ? ctx.getLevel() : Logger.globalLevel;
    	
    	// do not log, is being filtered
    	if (setlevel.getCode() < level.getCode())
    		return;
    	
    	Logger.logWr((ctx != null) ? ctx.getOpId() : null, level, message, tags);
    }
    
    /*
     * Insert a (string) translated message into the log
     * 
     * @param ctx context for log settings, null for none
     * @param level message level
     * @param code to translate
     * @param params for the translation
     */
    static public void logWr(String taskid, DebugLevel level, long code, Object... params) {
    	Logger.logWr(taskid, level, LocaleUtil.tr(Logger.locale, "_code_" + code, params), "Code", code + "");
    }
    
    /*
     * don't check, just write
     *  
     * @param taskid
     * @param level
     * @param message
     * @param tags
     */
    static public void logWr(String taskid, DebugLevel level, String message, String... tags) {
    	String indicate = "M" + level.getIndicator();
    	
		/* TODO
    	if (Logger.toDatabase) {
			Message lmsg = new Message("Logger");
			lmsg.addHeader("Op", "Log");
			lmsg.addHeader("Indicator", indicate);
			lmsg.addHeader("Occurred", occur);
			lmsg.addHeader("Tags", tagvalue);			
			lmsg.addStringAttachment(message);
	        Hub.instance.getBus().sendMessage(lmsg);
    	}
    	*/

    	// write to file if not a Task or if File Tasks is flagged
    	if (Logger.toFile || Logger.toConsole) {
    		if (message != null)
    			message = message.replace("\n", "\n\t");		// tab sub-lines
	
	        Logger.write(taskid, indicate, message, tags);
    	}
    }

    /*
     * Insert a chunk of hex encoded memory into the log
     * 
     * @param ctx context for log settings, null for none
     * @param level message level
     * @param data memory to hex encode and store
     * @param tags searchable values associated with the message, key-value pairs can be created by putting two tags adjacent
     */
    static public void log(OperationContext ctx, DebugLevel level, Memory data, String... tags) {
    	DebugLevel setlevel = (ctx != null) ? ctx.getLevel() : Logger.globalLevel;
    	
    	// do not log, is being filtered
    	if (setlevel.getCode() < level.getCode())
    		return;
    	
    	String indicate = "H" + level.getIndicator();
    	
		/* TODO
    	if (tc != null) {
			Message lmsg = new Message("Logger");
			lmsg.addHeader("Op", "Log");
			lmsg.addHeader("Indicator", indicate);
			lmsg.addHeader("Occurred", occur);
			lmsg.addHeader("Tags", tagvalue);			
			lmsg.addAttachment(data);
	        Hub.instance.getBus().sendMessage(lmsg);
    	}
    	*/

    	// write to file if not a Task or if File Tasks is flagged
    	if (Logger.toFile || Logger.toConsole) 
	        Logger.write((ctx != null) ? ctx.getOpId() : null, indicate, HexUtil.bufferToHex(data), tags);
    }

    /*
     * A boundary delineates in section of a task log from another, making it
     * easier for a log viewer to organize the content.  Boundary's are treated
     * like "info" messages, if only errors or warnings are being logged then 
     * the boundary entry will be skipped.
     *  
     * @param ctx context for log settings, null for none
     * @param tags searchable values associated with the message, key-value pairs can be created by putting two tags adjacent
     */
    static public void boundary(OperationContext ctx, String... tags) {
    	DebugLevel setlevel = (ctx != null) ? ctx.getLevel() : Logger.globalLevel;
    	
    	// do not log, is being filtered
    	if (setlevel.getCode() < DebugLevel.Info.getCode())
    		return;
    	
    	Logger.boundaryWr((ctx != null) ? ctx.getOpId() : null, tags);
    }
    
    /*
     * Don't check, just write 
     * 
     * @param taskid
     * @param tags
     */
    static public void boundaryWr(String taskid, String... tags) {
		/* TODO
    	if (tc != null) {
			Message lmsg = new Message("Logger");
			lmsg.addHeader("Op", "Log");
			lmsg.addHeader("Indicator", "B");
			lmsg.addHeader("Occurred", occur);
			lmsg.addHeader("Tags", tagvalue);			
	        Hub.instance.getBus().sendMessage(lmsg);
    	}
    	*/

    	// write to file if not a Task or if File Tasks is flagged
    	if (Logger.toFile || Logger.toConsole) 
	        Logger.write(taskid, "B  ", "", tags);
    }
    
    static protected void write(String taskid, String indicator, String message, String... tags) {
    	if (taskid == null)
    		taskid = "00000_19700101T000000000Z_000000000000000";
    	
    	DateTime occur = new DateTime(DateTimeZone.UTC);
    	String tagvalue = "";
    	
		if ((tags != null) && tags.length > 0) {
	        tagvalue = "|";
	
	        for (String tag : tags) 
	        	tagvalue += tag + "|";
		}
    	
		if (Logger.handler != null)
			Logger.handler.write(occur.toString(), taskid, indicator, tagvalue, message);
		
		if (tagvalue.length() > 0)
			tagvalue += " ";
		
        Logger.write(occur  + " " + taskid + " " + indicator + " " + tagvalue +  message);
    }
    
    static protected void write(String msg) {
    	if (Logger.toConsole)
    		System.out.println(msg);
    	
    	if (!Logger.toFile || (Logger.logWriter == null))
    		return;
    	
    	Logger.writeLock.lock();
    	
    	// start a new log file every 24 hours
    	if (System.currentTimeMillis() - Logger.filestart > 86400000)
    		Logger.startNewLogFile();
    	
        try {
        	Logger.logWriter.println(msg);
        	Logger.logWriter.flush();
        }
        catch (Exception x) {
            // ignore, logger is broken  
        }
        
        Logger.writeLock.unlock();
    }
}
