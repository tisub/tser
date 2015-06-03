package com.anotherservice.util;

import java.util.*;
import java.text.*;
import java.lang.*;
import java.io.*;
import java.util.logging.*;
import java.util.concurrent.*;

/**
 * This class relies on the <em>java.util.logging</em> framework but provides a bit more flexibility.
 * For instance, if you are not using the logging framework, you can set a <code>PrintStream</code> to redirect the output.
 * You can also customize a bit what components will be logged (time, class, method).
 *
 * @author  Simon Uyttendaele
 */
public class Logger
{
	public static final String DEFAULT = "com.anotherservice";
	
	public static class Log implements Runnable
	{
		public Log(String name) { this.name = name; }

		/**
		 * Specifies whether or not to prepend the current date to the log message. Default <code>true</code>.
		 */
		public boolean logDate = true;
		/**
		 * Specifies whether or not to prepend the calling class name to the log message. Default <code>true</code>.
		 */
		public boolean logClass = true;
		/**
		 * Specifies whether or not to prepend the calling method name to the log message. Default <code>false</code>.
		 */
		public boolean logMethod = false;
		/**
		 * Specifies whether or not to prepend the calling method name to the log message. Default <code>false</code>.
		 */
		public boolean logLevel = true;
		/**
		 * Specifies the log level above which messages are considered. Default <code>Level.INFO</code>.
		 * @see java.util.logging.Level
		 */
		public int level = 800; // Level.INFO
		/**
		 * Specifies the logger name when not using a custom {#link #stream). Default <code>"com.anotherservice"</code>.
		 */
		public String name = "com.anotherservice";
		/**
		 * When set, all messages are sent directly to this stream. Default <code>null</code>.
		 */
		public PrintStream stream = null;
		
		private String format(String message, Date date, Level l)
		{
			if( logClass )
			{
				StackTraceElement s = Thread.currentThread().getStackTrace()[4];
				String canonicalName = s.getClassName();
				canonicalName = canonicalName.substring(canonicalName.lastIndexOf('.') + 1, canonicalName.length());
			
				message = "[" + canonicalName + (logMethod ? "." + s.getMethodName() + "()" : "") + "] " + message;
			}
			
			if( logDate )
				message = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS").format(date) + " " + message;

			if( logLevel )
				message = l.getName() + ": " + message;

			return message;
		}
		
		/**
		 * Logs a <code>Level.SEVERE</code> message which is the <code>Throwable</code> stacktrace.
		 * @param	t	the exception to log
		 */
		public void severe(Throwable t) { realLog(Level.SEVERE, t); }
		/**
		 * Logs a <code>Level.SEVERE</code> message.
		 * @param	message	the message to log
		 */
		public void severe(String message) { realLog(Level.SEVERE, message); }
		/**
		 * Logs a <code>Level.WARNING</code> message which is the <code>Throwable</code> stacktrace.
		 * @param	t	the exception to log
		 */
		public void warning(Throwable t) { realLog(Level.WARNING, t); }
		/**
		 * Logs a <code>Level.WARNING</code> message.
		 * @param	message	the message to log
		 */
		public void warning(String message) { realLog(Level.WARNING, message); }
		/**
		 * Logs a <code>Level.INFO</code> message which is the <code>Throwable</code> stacktrace.
		 * @param	t	the exception to log
		 */
		public void info(Throwable t) { realLog(Level.INFO, t); }
		/**
		 * Logs a <code>Level.INFO</code> message.
		 * @param	message	the message to log
		 */
		public void info(String message) { realLog(Level.INFO, message); }
		/**
		 * Logs a <code>Level.CONFIG</code> message which is the <code>Throwable</code> stacktrace.
		 * @param	t	the exception to log
		 */
		public void config(Throwable t) { realLog(Level.CONFIG, t); }
		/**
		 * Logs a <code>Level.CONFIG</code> message.
		 * @param	message	the message to log
		 */
		public void config(String message) { realLog(Level.CONFIG, message); }
		/**
		 * Logs a <code>Level.FINE</code> message which is the <code>Throwable</code> stacktrace.
		 * @param	t	the exception to log
		 */
		public void fine(Throwable t) { realLog(Level.FINE, t); }
		/**
		 * Logs a <code>Level.FINE</code> message.
		 * @param	message	the message to log
		 */
		public void fine(String message) { realLog(Level.FINE, message); }
		/**
		 * Logs a <code>Level.FINER</code> message which is the <code>Throwable</code> stacktrace.
		 * @param	t	the exception to log
		 */
		public void finer(Throwable t) { realLog(Level.FINER, t); }
		/**
		 * Logs a <code>Level.FINER</code> message.
		 * @param	message	the message to log
		 */
		public void finer(String message) { realLog(Level.FINER, message); }
		/**
		 * Logs a <code>Level.FINEST</code> message which is the <code>Throwable</code> stacktrace.
		 * @param	t	the exception to log
		 */
		public void finest(Throwable t) { realLog(Level.FINEST, t); }
		/**
		 * Logs a <code>Level.FINEST</code> message.
		 * @param	message	the message to log
		 */
		public void finest(String message) { realLog(Level.FINEST, message); }
		/**
		 * Logs a message which is the <code>Throwable</code> stacktrace with a custom level.
		 * @param	level	the log level
		 * @param	t	the exception to log
		 */
		public void log(int level, Throwable t) { realLog(Level.parse(level + ""), t); }
		/**
		 * Logs a message with a custom level.
		 * @param	level	the log level
		 * @param	message	the message to log
		 */
		public void log(int level, String message) { realLog(Level.parse(level + ""), message); }
		
		private void realLog(Level level, String message)
		{
			if( level == null )
				level = Level.INFO;
			if( message == null )
				message = "";

			if( level.intValue() >= this.level )
			{
				if( async )
					queue.offer(new Entry(level, message));
				else
					realLog(new Entry(level, message));
			}
		}
		
		private void realLog(Level level, Throwable t)
		{
			if( level == null )
				level = Level.INFO;
			if( t == null )
				t = new Exception("");

			if( level.intValue() >= this.level )
			{
				if( async )
					queue.offer(new Entry(level, t));
				else
					realLog(new Entry(level, t));
			}
		}
		
		private synchronized void realLog(Entry e)
		{
			if( e.s != null )
			{
				if( stream == null )
					java.util.logging.Logger.getLogger(name).log(e.l, format(e.s, e.d, e.l));
				else
					stream.println(format(e.s, e.d, e.l));
			}
			else if( e.t != null )
			{
				if( stream == null )
					java.util.logging.Logger.getLogger(name).log(e.l, e.t.getMessage(), e.t);
				else
					e.t.printStackTrace(stream);
			}
		}
	
		// ============================
		// ASYNC
		// ============================
		private boolean async = false;
		private Thread thread = null;
		
		/**
		 * Set the logging mechanism to be processed (a)synchronousely.
		 * @param value whether logging should be async
		 */
		public synchronized void async(boolean value)
		{
			if( async == value )
				return;

			async = value;
			
			if( async && thread == null )
			{
				thread = new Thread(this);
				thread.start();
			}
		}
		
		private class Entry
		{
			public Level l = null;
			public String s = null;
			public Throwable t = null;
			public Date d = new Date();
			public Entry(Level l, String s) { this.l = l; this.s = s; }
			public Entry(Level l, Throwable t) { this.l = l; this.t = t; }
		}
		
		private ConcurrentLinkedQueue<Entry> queue = new ConcurrentLinkedQueue<Entry>();
		private LinkedList<Entry> duplicate()
		{
			// to prevent infinite gathering with spamming threads
			ConcurrentLinkedQueue<Entry> queue = this.queue;
			this.queue = new ConcurrentLinkedQueue<Entry>();
			
			LinkedList<Entry> q = new LinkedList<Entry>();
			Entry e;
			while( (e = queue.poll()) != null )
				q.add(e);
			return q;
		}
		
		/**
		 * The interval (ms) at which log entries are written when {@link #async(boolean)} is <code>true</code>.
		 */
		public long asyncInterval = 1000L;
		public void run()
		{
			while( async )
			{
				try
				{
					Thread.sleep(asyncInterval);
					
					LinkedList<Entry> q = duplicate();
					
					for( Entry e : q )
						realLog(e);

					q.clear();
					stream.flush();
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
				}
			}
			
			thread = null;
		}
	}
	
	private Logger() { }
	private static Map<String, Logger.Log> instances = new Hashtable<String, Logger.Log>();
	public static Logger.Log instance() { return instance(DEFAULT); }
	public static Logger.Log instance(String name) { if( !instances.containsKey(name) ) instances.put(name, new Logger.Log(name)); return instances.get(name); }
	public static void instance(String name, Logger.Log log) { instances.put(name, log); }
	
	/**
	 * Logs a <code>Level.SEVERE</code> message which is the <code>Throwable</code> stacktrace.
	 * @param	t	the exception to log
	 */
	public static void severe(Throwable t) { instance().realLog(Level.SEVERE, t); }
	/**
	 * Logs a <code>Level.SEVERE</code> message.
	 * @param	message	the message to log
	 */
	public static void severe(String message) { instance().realLog(Level.SEVERE, message); }
	
	/**
	 * Logs a <code>Level.WARNING</code> message which is the <code>Throwable</code> stacktrace.
	 * @param	t	the exception to log
	 */
	public static void warning(Throwable t) { instance().realLog(Level.WARNING, t); }
	/**
	 * Logs a <code>Level.WARNING</code> message.
	 * @param	message	the message to log
	 */
	public static void warning(String message) { instance().realLog(Level.WARNING, message); }
	
	/**
	 * Logs a <code>Level.INFO</code> message which is the <code>Throwable</code> stacktrace.
	 * @param	t	the exception to log
	 */
	public static void info(Throwable t) { instance().realLog(Level.INFO, t); }
	/**
	 * Logs a <code>Level.INFO</code> message.
	 * @param	message	the message to log
	 */
	public static void info(String message) { instance().realLog(Level.INFO, message); }
	
	/**
	 * Logs a <code>Level.CONFIG</code> message which is the <code>Throwable</code> stacktrace.
	 * @param	t	the exception to log
	 */
	public static void config(Throwable t) { instance().realLog(Level.CONFIG, t); }
	/**
	 * Logs a <code>Level.CONFIG</code> message.
	 * @param	message	the message to log
	 */
	public static void config(String message) { instance().realLog(Level.CONFIG, message); }
	
	/**
	 * Logs a <code>Level.FINE</code> message which is the <code>Throwable</code> stacktrace.
	 * @param	t	the exception to log
	 */
	public static void fine(Throwable t) { instance().realLog(Level.FINE, t); }
	/**
	 * Logs a <code>Level.FINE</code> message.
	 * @param	message	the message to log
	 */
	public static void fine(String message) { instance().realLog(Level.FINE, message); }
	
	/**
	 * Logs a <code>Level.FINER</code> message which is the <code>Throwable</code> stacktrace.
	 * @param	t	the exception to log
	 */
	public static void finer(Throwable t) { instance().realLog(Level.FINER, t); }
	/**
	 * Logs a <code>Level.FINER</code> message.
	 * @param	message	the message to log
	 */
	public static void finer(String message) { instance().realLog(Level.FINER, message); }
	
	/**
	 * Logs a <code>Level.FINEST</code> message which is the <code>Throwable</code> stacktrace.
	 * @param	t	the exception to log
	 */
	public static void finest(Throwable t) { instance().realLog(Level.FINEST, t); }
	/**
	 * Logs a <code>Level.FINEST</code> message.
	 * @param	message	the message to log
	 */
	public static void finest(String message) { instance().realLog(Level.FINEST, message); }
	
	/**
	 * Logs a message which is the <code>Throwable</code> stacktrace with a custom level.
	 * @param	level	the log level
	 * @param	t	the exception to log
	 */
	public static void log(int level, Throwable t) { instance().realLog(Level.parse(level + ""), t); }
	/**
	 * Logs a message with a custom level.
	 * @param	level	the log level
	 * @param	message	the message to log
	 */
	public static void log(int level, String message) { instance().realLog(Level.parse(level + ""), message); }
	
	// ============================
	// ASYNC
	// ============================
	
	/**
	 * Set the logging mechanism to be processed (a)synchronousely.
	 * @param value whether logging should be async
	 */
	public static void async(boolean value) { instance().async(value); }
}