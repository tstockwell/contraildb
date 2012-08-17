package com.googlecode.contraildb.core.utils;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Logging {

	static abstract public class Task 
	{
		abstract public void log(Logger logger);
	}
	public static Logger getLogger()
	{
		return getLogger(getLogRecord(1, Level.ALL, null, null));
	}
	private static Logger getLogger(LogRecord logRecord) {
		Logger logger= Logger.getLogger(logRecord.getLoggerName());
		if (logger == null)
			logger= Logger.getAnonymousLogger();
		return logger;
	}
	private static LogRecord getLogRecord(int depth, Level level, String msg, Throwable t)
	{
		LogRecord logRecord= new LogRecord(level, msg);
		Throwable throwable= new Throwable();
		StackTraceElement[] stackTraceElements= throwable.getStackTrace();
		String loggerName= null;
		if (stackTraceElements != null && 1 <= stackTraceElements.length)
		{
			StackTraceElement traceElement= stackTraceElements[3];
			loggerName= traceElement.getClassName();
			logRecord.setSourceClassName(loggerName);
			logRecord.setSourceMethodName(traceElement.getMethodName());
			int i= loggerName.lastIndexOf('.');
			if (0 < i)
				loggerName= loggerName.substring(0, i);
		}
		if (loggerName == null)
			loggerName= Logging.class.getPackage().getName();
		
		logRecord.setLoggerName(loggerName);
		logRecord.setThrown(t);
		
		return logRecord;
	}
	private static void log(int depth, Level level, String msg, Throwable t)
	{
		LogRecord logRecord= getLogRecord(depth, level, msg, t);
		Logger logger= getLogger(logRecord);
		logger.log(logRecord);
	}
	
	static public void entering() {
    	log(2, Level.FINER, "ENTRY", null);
	}
	static public void exiting() {
    	log(2, Level.FINER, "RETURN", null);
	}
	
    static public void log(Level level, String msg) {
    	log(2, level, msg, null);
    }
	
    static public void log(Level level, String msg, Throwable t) {
    	log(2, level, msg, t);
    }
	
    static public void severe(String msg) {
    	log(2, Level.SEVERE, msg, null);
    }
	
    static public void severe(String msg, Throwable t) {
    	log(2, Level.SEVERE, msg, t);
    }
	
    static public void warning(String msg) {
    	log(2, Level.WARNING, msg, null);
    }
	
    static public void warning(Throwable t) {
    	log(2, Level.WARNING, null, t);
    }
	
    static public void warning(String msg, Throwable t) {
    	log(2, Level.WARNING, msg, t);
    }
	public static void info(String msg) {
    	log(2, Level.INFO, msg, null);
	}
	public static void fine(String msg)
	{
    	log(2, Level.FINE, msg, null);
	}
	public static void fine(String msg, Throwable t)
	{
    	log(2, Level.FINE, msg, t);
	}
	public static void finer(String msg)
	{
    	log(2, Level.FINER, msg, null);
	}
	public static void finer(String msg, Throwable t)
	{
    	log(2, Level.FINER, msg, t);
	}
	/**
	 * For performing an intensive logging task.
	 * The task is only invoked if the specified logging level is enabled. 
	 */
	public static void fine(Task task)
	{
    	Logger logger= getLogger(getLogRecord(2, Level.FINE, null, null));
    	if (logger.isLoggable(Level.FINE)) {
    		task.log(logger);
    	}
	}

}
