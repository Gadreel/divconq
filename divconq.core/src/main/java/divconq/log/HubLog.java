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
import java.util.concurrent.locks.ReentrantLock;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import divconq.lang.Memory;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.locale.Tr;
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
public class HubLog {
    static protected DebugLevel globalLevel = DebugLevel.Info;
    static protected boolean debugEnabled = false;
    
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
        return HubLog.globalLevel; 
    }
    
    static public void setGlobalLevel(DebugLevel v) {
        HubLog.globalLevel = v; 
        
        HubLog.debugEnabled = ((v == DebugLevel.Trace) || (v == DebugLevel.Trace));
        
        // keep hub context up to date
		OperationContext.updateHubContext();		// TODO replace with a debug context provider
    }
    
    static public void setLogHandler(ILogHandler v) {
    	HubLog.handler = v;
    }
    
    static public void setToConsole(boolean v) {
    	HubLog.toConsole = v;
    }
    
    /*
     * return true if debugging is even an option on this setup, 
     * if not this saves a lot of overhead on the Logger.debug and Logger.trace calls
     */
    static public boolean getDebugEnabled() {
    	return HubLog.debugEnabled;
    }
   
    /**
     * Called from Hub.start this method configures the logging features.
     * 
     * @param config xml holding the configuration
     */
    static public void init(XElement config) {
    	HubLog.config = config;
    	
    	// TODO return operation result
    	
    	// TODO load levels, path etc
    	// include a setting for startup logging - if present set the TC log level directly
    	
		HubLog.startNewLogFile();
    	
		// set by operation context init 
    	//Logger.locale = LocaleUtil.getDefaultLocale();
		
		// From here on we can use netty and so we need the logger setup
		
		InternalLoggerFactory.setDefaultFactory(new divconq.log.netty.LoggerFactory());
    	
    	if (HubLog.config != null) {
    		// set by operation context init 
    		//if (Logger.config.hasAttribute("Level"))
    	    //	Logger.globalLevel = DebugLevel.parse(Logger.config.getAttribute("Level"));

			if (HubLog.config.hasAttribute("Level")) 
				HubLog.setGlobalLevel(DebugLevel.parse(HubLog.config.getAttribute("Level")));

			if (HubLog.config.hasAttribute("EnableDebugger")) 
				HubLog.debugEnabled = "True".equals(HubLog.config.getAttribute("EnableDebugger"));
    		
    		if (HubLog.config.hasAttribute("NettyLevel")) {
    			ResourceLeakDetector.setLevel(Level.valueOf(HubLog.config.getAttribute("NettyLevel")));
    			
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

    		if (HubLog.logWriter != null) {
    			HubLog.logWriter.flush();
    			HubLog.logWriter.close();
    		}
    		
    		Logger.trace("Opening log file: " + logfile.getCanonicalPath());
    		
			HubLog.logWriter = new PrintWriter(logfile, "utf-8");
			
			HubLog.filestart = System.currentTimeMillis();
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
    	
    	HubLog.toFile = true;		// go back to logging to file
    	
    	// TODO say no to database
    }
    
    /*
     * Insert a (string) translated message into the log
     * 
     * @param ctx context for log settings, null for none
     * @param level message level
     * @param code to translate
     * @param params for the translation
     */
    static public void log(DebugLevel level, long code, Object... params) {
    	HubLog.log(level, Tr.tr("_code_" + code, params), "Code", code + "");
    }
    
    /*
     * Insert a (string) message into the log
     * 
     * @param ctx context for log settings, null for none
     * @param level message level
     * @param message text to store in log
     * @param tags searchable values associated with the message, key-value pairs can be created by putting two tags adjacent
     */
    static public void log(DebugLevel level, String message, String... tags) {
    	// do not log, is being filtered
    	if (HubLog.globalLevel.getCode() < level.getCode())
    		return;
    	
    	HubLog.logWr(null, level, message, tags);
    }

    /*
     * Insert a (string) translated message into the log
     * 
     * @param ctx context for log settings, null for none
     * @param level message level
     * @param code to translate
     * @param params for the translation
     */
    static public void logWr(String opid, DebugLevel level, long code, Object... params) {
    	HubLog.logWr(opid, level, Tr.tr("_code_" + code, params), "Code", code + "");
    }
    
    /*
     * don't check, just write
     *  
     * @param taskid
     * @param level
     * @param message
     * @param tags
     */
    static public void logWr(String opid, DebugLevel level, String message, String... tags) {
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
    	if (HubLog.toFile || HubLog.toConsole) {
    		if (message != null)
    			message = message.replace("\n", "\n\t");		// tab sub-lines
	
	        HubLog.write(opid, indicate, message, tags);
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
    static public void log(String opid, DebugLevel level, Memory data, String... tags) {
    	// do not log, is being filtered
    	if (HubLog.globalLevel.getCode() < level.getCode())
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
    	if (HubLog.toFile || HubLog.toConsole) 
	        HubLog.write(opid, indicate, HexUtil.bufferToHex(data), tags);
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
    static public void boundary(String opid, String... tags) {
    	// do not log, is being filtered
    	if (HubLog.globalLevel.getCode() < DebugLevel.Info.getCode())
    		return;
    	
    	HubLog.boundaryWr(opid, tags);
    }
    
    /*
     * Don't check, just write 
     * 
     * @param taskid
     * @param tags
     */
    static public void boundaryWr(String opid, String... tags) {
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
    	if (HubLog.toFile || HubLog.toConsole) 
	        HubLog.write(opid, "B  ", "", tags);
    }
    
    static protected void write(String opid, String indicator, String message, String... tags) {
    	if (opid == null)
    		opid = "00000_19700101T000000000Z_000000000000000";
    	
    	DateTime occur = new DateTime(DateTimeZone.UTC);
    	String tagvalue = "";
    	
		if ((tags != null) && tags.length > 0) {
	        tagvalue = "|";
	
	        for (String tag : tags) 
	        	tagvalue += tag + "|";
		}
    	
		if (HubLog.handler != null)
			HubLog.handler.write(occur.toString(), opid, indicator, tagvalue, message);
		
		if (tagvalue.length() > 0)
			tagvalue += " ";
		
        HubLog.write(occur  + " " + opid + " " + indicator + " " + tagvalue +  message);
    }
    
    static protected void write(String msg) {
    	if (HubLog.toConsole)
    		System.out.println(msg);
    	
    	if (!HubLog.toFile || (HubLog.logWriter == null))
    		return;
    	
    	HubLog.writeLock.lock();
    	
    	// start a new log file every 24 hours
    	if (System.currentTimeMillis() - HubLog.filestart > 86400000)
    		HubLog.startNewLogFile();
    	
        try {
        	HubLog.logWriter.println(msg);
        	HubLog.logWriter.flush();
        }
        catch (Exception x) {
            // ignore, logger is broken  
        }
        
        HubLog.writeLock.unlock();
    }
}
