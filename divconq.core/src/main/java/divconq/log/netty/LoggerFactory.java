package divconq.log.netty;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class LoggerFactory extends InternalLoggerFactory {
	static public final NettyLogger instance = new NettyLogger();
	
	@Override
	protected InternalLogger newInstance(String arg0) {
		return LoggerFactory.instance;
	}
}
