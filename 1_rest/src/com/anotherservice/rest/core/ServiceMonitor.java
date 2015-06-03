package com.anotherservice.rest.core;

import java.lang.*;
import java.util.*;
import java.util.regex.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.*;
import java.nio.file.attribute.*;
import com.anotherservice.util.*;
import com.anotherservice.io.*;
import com.anotherservice.rest.*;

/**
 * This class is the base loader for REST services. It watchs over the specified services directory for changes.
 * If a service folder is created, the <code>ServiceMonitor</code> {@link com.anotherservice.rest.Initializer#initialize() initializes} 
 * it in a new isolated {@link com.anotherservice.util.Context Context}.
 * If a service folder is removed, the <code>ServiceMonitor</code> tries to unload all {@link com.anotherservice.rest.Handler Handlers} 
 * and to free the <code>Context</code>.
 * <p><br />Initializing a service follows these steps :<ol>
 * <li>Browse the service directory specified by {@link #setDirectory(String)}</li>
 * <li>Find the XML service configuration file specified by {@link #setConfigFileName(String)}</li>
 * <li>Read the value of the initializer XML tag specified by {@link #setInitializerSection(String)}</li>
 * <li>Load all jar files in the service directory in a common {@link com.anotherservice.util.JarLoader} context</li>
 * <li>Create a new instance of the <code>Initializer</code> specified in step 3 in the context created at step 4</li>
 * <li>Call the <code>initialize()</code> method against it</li></ol></p>
 *
 * @note	Unloading a <code>Initializer</code> context highly depends on garbage collection and existing references. Thus, even though visible references are removed, some
 * may remain. Hence, if over-expoiting this mechanism, the memory usage may suffer from this.
 * @see	com.anotherservice.rest.Handler
 * @see	com.anotherservice.util.JarLoader
 * @see	com.anotherservice.rest.Initializer
 * @author  Simon Uyttendaele
 */
public class ServiceMonitor extends Thread
{
	private static final int ERROR_CLASS_ID = 0x8000;
	
	private String servicesDirectory = "/WEB-INF/services/";
	private String serviceConfigFileName = "config.xml";
	private String serviceInitializerSection = "initializer";
	private WatchService watcher;
	private Path rootPath;
	private boolean started = false;
	
	/**
	 * Default constructor
	 */
	public ServiceMonitor() { }
	
	/**
	 * Constructor with a service directory, a config file name and an initializer XML tag name
	 */
	public ServiceMonitor(String directory, String configFileName, String initializerSection)
	{
		setDirectory(directory);
		setConfigFileName(configFileName);
		setInitializerSection(initializerSection);
	}
	
	/**
	 * Sets the service directory to watch for changes
	 * @param	directory	the path to the directory to watch
	 * @throws	RestException	if the <code>ServiceMonitor</code> is already started
	 */
	public void setDirectory(String directory)
	{
		if( this.started )
			throw new RestException("Cannot set the directory because the Service Monitor is already started", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_RUNTIME | ERROR_CLASS_ID | 0x1);
		if( directory != null && directory.length() > 0 )
		{
			Logger.config("Setting the services directory to : " + directory);
			this.servicesDirectory = directory;
		}
	}
	
	/**
	 * Sets the XML service configuration file name to load when initializing a service
	 * @param	configFileName	the name of the config file to load
	 * @throws	RestException	if the <code>ServiceMonitor</code> is already started
	 */
	public void setConfigFileName(String configFileName)
	{
		if( this.started )
			throw new RestException("Cannot set the config file name because the Service Monitor is already started", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_RUNTIME | ERROR_CLASS_ID | 0x2);
		if( configFileName != null && configFileName.length() > 0 )
		{
			Logger.config("Setting the services config file name to : " + configFileName);
			this.serviceConfigFileName = configFileName;
		}
	}
	
	/**
	 * Sets the initializer XML tag name which contains the canonical name of the <code>Initializer</code> class.
	 * @param	initializerSection	the name of the XML tag
	 * @throws	RestException	if the <code>ServiceMonitor</code> is already started
	 */
	public void setInitializerSection(String initializerSection)
	{
		if( this.started )
			throw new RestException("Cannot set the initializer section because the Service Monitor is already started", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_RUNTIME | ERROR_CLASS_ID | 0x3);
		if( initializerSection != null && initializerSection.length() > 0 )
		{
			Logger.config("Setting the services config initializer section to : " + initializerSection);
			this.serviceInitializerSection = initializerSection;
		}
	}
	
	private void initialize() throws Exception
	{
		Logger.info("Starting the Service Monitor to watch over " + servicesDirectory);
		
		// first of all : save our context
		Context.add(this);
		
		rootPath = Paths.get(servicesDirectory);
		this.watcher = FileSystems.getDefault().newWatchService();
		
		// Register all existing services
		Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>()
		{
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
				if( dir == rootPath )
					return FileVisitResult.CONTINUE;
                addService(dir);
                return FileVisitResult.SKIP_SUBTREE;
            }
        });
		
		// Watch main directory
		rootPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
	}
	
	/**
	 * Starts this <code>ServiceMonitor</code> instance.
	 */
	public void run()
	{
		try
		{
			if( this.started ) return;
			this.started = true;
			this.initialize();
			
			// Watch loop
			while(true)
			{
				WatchKey key;
				try { key = watcher.take(); } // wait for changes
				catch (InterruptedException e) { return; }
				
				// Wait AFTER the take() so that eventual other changes may occur then
				Thread.sleep(2500);
				
				for( WatchEvent<?> event : key.pollEvents() )
				{
					WatchEvent.Kind kind = event.kind();
					if( kind == StandardWatchEventKinds.OVERFLOW ) continue;
					
					Path name = (Path) event.context();
					Logger.finest("Watcher update " + kind + " over " + name);
					
					if( kind == StandardWatchEventKinds.ENTRY_CREATE )
						addService(rootPath.resolve(name).toAbsolutePath());
					else if( kind == StandardWatchEventKinds.ENTRY_DELETE )
						removeService(rootPath.resolve(name).toAbsolutePath());
					else if( kind == StandardWatchEventKinds.ENTRY_MODIFY )
						updateService(rootPath.resolve(name).toAbsolutePath());
					break;
				}
				
				key.reset();
			}
		}
		catch(Exception e)
		{
			Logger.severe("Service Monitor interrupted because of an error : " + e.getMessage());
			Logger.fine(e);
		}
		finally
		{
			this.started = false;
			try { this.watcher.close(); }
			catch(Exception e) { }
		}
	}
	
	public static String compatibility(Charset c)
	{
		try { return SHA1.hash(c.name()).substring(7, 043); }
		catch(Exception e) { return null; }
	}
	
	private void addService(final Path dir)
	{
		try
		{
			Logger.info("Initializing service " + dir.getFileName());

			// check if the config file exists
			Path configPath = dir.resolve(this.serviceConfigFileName);
			if( !Files.exists(configPath) )
			{
				Logger.warning("The configuration file '" + this.serviceConfigFileName + "' is not found for service " + dir.getFileName());
				return;
			}
			
			// load config file
			String config = FileReader.readFile(configPath.toString());
			Matcher m = Pattern.compile("<" + this.serviceInitializerSection + ">\\s*([a-z0-9_\\-\\$\\.]*)\\s*</" + this.serviceInitializerSection + ">", Pattern.CASE_INSENSITIVE).matcher(config);
			
			// check if the initializer is set
			if( !m.find() )
			{
				Logger.warning("The service initializer section '" + this.serviceInitializerSection + "' was not found in the config file for service " + dir.getFileName());
				return;
			}
			String className = m.group(1);
			
			// add jar files to the loader
			final JarLoader loader = new JarLoader(this.getClass().getClassLoader(), dir.toString());
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>()
			{
				public FileVisitResult preVisitDirectory(Path dir2, BasicFileAttributes attrs) throws IOException
				{
					if( dir == dir2 )
						return FileVisitResult.CONTINUE;
					return FileVisitResult.SKIP_SUBTREE;
				}
				
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
				{
					if( file.toString().endsWith(".jar") )
					{
						Logger.info("Adding external library " + file.getFileName());
						loader.addFile(file.toString());
					}
					return FileVisitResult.CONTINUE;
				}
			});
			
			// register the context with the path
			Context.add(dir, loader);
			Logger.finest("Adding context for directory " + dir);
			
			// start service initialization
			new ServiceInitializer(loader, className).start();
		}
		catch(Exception e)
		{
			Logger.warning("Service " + dir.getFileName() + " initialization failed due to : " + e.getMessage());
			Logger.fine(e);
		}
	}
	
	private void removeService(Path dir)
	{
		try
		{
			Logger.info("Unloading service " + dir.getFileName());
			
			_recurseRemoveService(Handler.getRoot(), dir);
			Context.remove(dir);
			Logger.finest("Removing context for directory " + dir);
		}
		catch(Exception e)
		{
			Logger.warning("Service " + dir.getFileName() + " removal failed due to : " + e.getMessage());
			Logger.fine(e);
		}
	}
	
	private void _recurseRemoveService(Index i, Path dir)
	{
		ArrayList<Handler> matches = new ArrayList<Handler>();
		
		for( Handler h : i.getOwnHandlers() )
		{
			if( h instanceof Index )
				_recurseRemoveService((Index)h, dir);
			if( Context.get(dir) == Context.get(h) )
				matches.add(h);
		}
		
		for( Handler match : matches )
		{
			i.removeOwnHandler(match);
			
			// detach all sub-handlers that might remain
			if( match instanceof Index )
				((Index)match).removeAllOwnHandlersRecursively();
		}
	}
	
	private void updateService(Path dir)
	{
		Logger.info("Reloading service " + dir.getFileName());
		removeService(dir);
		addService(dir);
	}
	
	private class ServiceInitializer extends Thread
	{
		private ClassLoader loader;
		private String className;
		
		public ServiceInitializer(ClassLoader l, String c)
		{
			this.loader = l;
			this.className = c;
		}
		
		public void run()
		{
			try
			{
				Context.change(loader);
				Class c = Class.forName(className, true, loader);
				if( !this.isInitializer(c) )
				{
					Logger.warning("The service initializer '" + className + "' does not implement the required interface 'Initializer'");
					return;
				}
				
				Initializer i = (Initializer) c.newInstance();
				i.initialize();
			}
			catch(Exception e)
			{
				Logger.warning("The service initializer '" + className + "' raised an error : " + e.getMessage());
				Logger.fine(e);
			}
		}
		
		private boolean isInitializer(Class c)
		{
			// we should get all the interfaces of all superclasses in case the class does not implement the interface directly
			for( Class cl = c; cl != null; cl = cl.getSuperclass() )
			{
				Class[] interfaces = cl.getInterfaces();
				for( int i = 0; i < interfaces.length; i++ )
					if( interfaces[i].getCanonicalName().equals(Initializer.class.getCanonicalName()) )
						return true;
			}
			
			return false;
		}
	}
}