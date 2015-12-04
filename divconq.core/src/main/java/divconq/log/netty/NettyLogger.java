package divconq.log.netty;

import io.netty.util.internal.logging.AbstractInternalLogger;
import divconq.hub.Hub;
import divconq.log.DebugLevel;
import divconq.log.HubLog;
import divconq.log.Logger;
import divconq.log.slf4j.FormattingTuple;
import divconq.log.slf4j.MessageFormatter;

public class NettyLogger extends AbstractInternalLogger {
    	/*
		 * 
		 */
		private static final long serialVersionUID = 8748850640482801893L;

		public NettyLogger() {
    		super("dcLogger");
    	}
    	

        /*
         * Is this logger instance enabled for the FINEST level?
         *
         * @return True if this Logger is enabled for level FINEST, false otherwise.
         */
        @Override
        public boolean isTraceEnabled() {
            return HubLog.getGlobalLevel().getCode() <= DebugLevel.Trace.getCode();
        }

        /*
         * Log a message object at level FINEST.
         *
         * @param msg
         *          - the message object to be logged
         */
        @Override
        public void trace(String msg) {
        	Logger.trace(msg, "Netty");
        }

        /*
         * Log a message at level FINEST according to the specified format and
         * argument.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for level FINEST.
         * </p>
         *
         * @param format
         *          the format string
         * @param arg
         *          the argument
         */
        @Override
        public void trace(String format, Object arg) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
            	Logger.trace(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log a message at level FINEST according to the specified format and
         * arguments.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the FINEST level.
         * </p>
         *
         * @param format
         *          the format string
         * @param argA
         *          the first argument
         * @param argB
         *          the second argument
         */
        @Override
        public void trace(String format, Object argA, Object argB) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            	Logger.trace(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log a message at level FINEST according to the specified format and
         * arguments.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the FINEST level.
         * </p>
         *
         * @param format
         *          the format string
         * @param argArray
         *          an array of arguments
         */
        @Override
        public void trace(String format, Object... argArray) {
                FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            	Logger.trace(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log an exception (throwable) at level FINEST with an accompanying message.
         *
         * @param msg
         *          the message accompanying the exception
         * @param t
         *          the exception (throwable) to log
         */
        @Override
        public void trace(String msg, Throwable t) {
        	Logger.trace(msg + " - " + t);
        }

        /*
         * Is this logger instance enabled for the FINE level?
         *
         * @return True if this Logger is enabled for level FINE, false otherwise.
         */
        @Override
        public boolean isDebugEnabled() {
            return HubLog.getGlobalLevel().getCode() <= DebugLevel.Debug.getCode();
        }

        /*
         * Log a message object at level FINE.
         *
         * @param msg
         *          - the message object to be logged
         */
        @Override
        public void debug(String msg) {
        	Logger.debug(msg, "Netty");
        }

        /*
         * Log a message at level FINE according to the specified format and argument.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for level FINE.
         * </p>
         *
         * @param format
         *          the format string
         * @param arg
         *          the argument
         */
        @Override
        public void debug(String format, Object arg) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                this.debug(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log a message at level FINE according to the specified format and
         * arguments.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the FINE level.
         * </p>
         *
         * @param format
         *          the format string
         * @param argA
         *          the first argument
         * @param argB
         *          the second argument
         */
        @Override
        public void debug(String format, Object argA, Object argB) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                this.debug(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log a message at level FINE according to the specified format and
         * arguments.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the FINE level.
         * </p>
         *
         * @param format
         *          the format string
         * @param argArray
         *          an array of arguments
         */
        @Override
        public void debug(String format, Object... argArray) {
                FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
                this.debug(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log an exception (throwable) at level FINE with an accompanying message.
         *
         * @param msg
         *          the message accompanying the exception
         * @param t
         *          the exception (throwable) to log
         */
        @Override
        public void debug(String msg, Throwable t) {
        	this.debug(msg + " - " + t);
        }

        /*
         * Is this logger instance enabled for the INFO level?
         *
         * @return True if this Logger is enabled for the INFO level, false otherwise.
         */
        @Override
        public boolean isInfoEnabled() {
            return HubLog.getGlobalLevel().getCode() <= DebugLevel.Info.getCode();
        }

        /*
         * Log a message object at the INFO level.
         *
         * @param msg
         *          - the message object to be logged
         */
        @Override
        public void info(String msg) {
        	Logger.info(msg, "Netty");
        }

        /*
         * Log a message at level INFO according to the specified format and argument.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the INFO level.
         * </p>
         *
         * @param format
         *          the format string
         * @param arg
         *          the argument
         */
        @Override
        public void info(String format, Object arg) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                this.info(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log a message at the INFO level according to the specified format and
         * arguments.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the INFO level.
         * </p>
         *
         * @param format
         *          the format string
         * @param argA
         *          the first argument
         * @param argB
         *          the second argument
         */
        @Override
        public void info(String format, Object argA, Object argB) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                this.info(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log a message at level INFO according to the specified format and
         * arguments.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the INFO level.
         * </p>
         *
         * @param format
         *          the format string
         * @param argArray
         *          an array of arguments
         */
        @Override
        public void info(String format, Object... argArray) {
                FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
                this.info(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log an exception (throwable) at the INFO level with an accompanying
         * message.
         *
         * @param msg
         *          the message accompanying the exception
         * @param t
         *          the exception (throwable) to log
         */
        @Override
        public void info(String msg, Throwable t) {
        	this.info(msg + " - " + t);
        }

        /*
         * Is this logger instance enabled for the WARNING level?
         *
         * @return True if this Logger is enabled for the WARNING level, false
         *         otherwise.
         */
        @Override
        public boolean isWarnEnabled() {
            return HubLog.getGlobalLevel().getCode() <= DebugLevel.Warn.getCode();
        }

        /*
         * Log a message object at the WARNING level.
         *
         * @param msg
         *          - the message object to be logged
         */
        @Override
        public void warn(String msg) {
    		Hub.instance.getCountManager().countObjects("nettyWarn", msg);
    		
        	Logger.warn(msg, "Netty");
        }

        /*
         * Log a message at the WARNING level according to the specified format and
         * argument.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the WARNING level.
         * </p>
         *
         * @param format
         *          the format string
         * @param arg
         *          the argument
         */
        @Override
        public void warn(String format, Object arg) {
                FormattingTuple ft = MessageFormatter.format(format, arg);
                this.warn(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log a message at the WARNING level according to the specified format and
         * arguments.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the WARNING level.
         * </p>
         *
         * @param format
         *          the format string
         * @param argA
         *          the first argument
         * @param argB
         *          the second argument
         */
        @Override
        public void warn(String format, Object argA, Object argB) {
                FormattingTuple ft = MessageFormatter.format(format, argA, argB);
                this.warn(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log a message at level WARNING according to the specified format and
         * arguments.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the WARNING level.
         * </p>
         *
         * @param format
         *          the format string
         * @param argArray
         *          an array of arguments
         */
        @Override
        public void warn(String format, Object... argArray) {
                FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
                this.warn(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log an exception (throwable) at the WARNING level with an accompanying
         * message.
         *
         * @param msg
         *          the message accompanying the exception
         * @param t
         *          the exception (throwable) to log
         */
        @Override
        public void warn(String msg, Throwable t) {
        	this.warn(msg + " - " + t);
        }

        /*
         * Is this logger instance enabled for level SEVERE?
         *
         * @return True if this Logger is enabled for level SEVERE, false otherwise.
         */
        @Override
        public boolean isErrorEnabled() {
            return HubLog.getGlobalLevel().getCode() <= DebugLevel.Error.getCode();
        }

        /*
         * Log a message object at the SEVERE level.
         *
         * @param msg
         *          - the message object to be logged
         */
        @Override
        public void error(String msg) {
    		Hub.instance.getCountManager().countObjects("nettyError", msg);
    		
    		if (msg.contains("LEAK:"))
        		Hub.instance.getCountManager().countObjects("nettyLeakError", msg);
        	
        	Logger.error(msg, "Netty");
        }

        /*
         * Log a message at the SEVERE level according to the specified format and
         * argument.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the SEVERE level.
         * </p>
         *
         * @param format
         *          the format string
         * @param arg
         *          the argument
         */
        @Override
        public void error(String format, Object arg) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            this.error(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log a message at the SEVERE level according to the specified format and
         * arguments.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the SEVERE level.
         * </p>
         *
         * @param format
         *          the format string
         * @param argA
         *          the first argument
         * @param argB
         *          the second argument
         */
        @Override
        public void error(String format, Object argA, Object argB) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            this.error(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log a message at level SEVERE according to the specified format and
         * arguments.
         *
         * <p>
         * This form avoids superfluous object creation when the logger is disabled
         * for the SEVERE level.
         * </p>
         *
         * @param format
         *          the format string
         * @param arguments
         *          an array of arguments
         */
        @Override
        public void error(String format, Object... arguments) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            this.error(ft.getMessage() + " - " + ft.getThrowable());
        }

        /*
         * Log an exception (throwable) at the SEVERE level with an accompanying
         * message.
         *
         * @param msg
         *          the message accompanying the exception
         * @param t
         *          the exception (throwable) to log
         */
        @Override
        public void error(String msg, Throwable t) {
        	this.error(msg + " - " + t);
        }
   }