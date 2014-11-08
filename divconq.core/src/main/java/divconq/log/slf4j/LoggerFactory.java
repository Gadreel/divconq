package divconq.log.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class LoggerFactory implements ILoggerFactory {
	static public final Slf4jLogger instance = new Slf4jLogger();

	@Override
	public Logger getLogger(String name) {
		return LoggerFactory.instance;
	}
}
