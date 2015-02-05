package squiddev.cctweaks.core.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import squiddev.cctweaks.core.reference.Config;

public class DebugLogger {
	private static Logger logger;

	public static void init(Logger log) {
		logger = log;
	}

	public static void debug(String message, Object... args) {
		if (Config.config.debug) {
			info(String.format(message, args));
		}
	}

	public static void info(String message) {
		logger.log(Level.INFO, message);
	}

	public static void warning(String message) {
		logger.log(Level.WARN, message);
	}

	public static void error(String message) {
		logger.log(Level.ERROR, message);
	}
}
