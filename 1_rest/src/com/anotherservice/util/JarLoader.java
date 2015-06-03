package com.anotherservice.util;

import java.io.*;
import java.net.URL;
import java.io.IOException;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.lang.ClassLoader;

/**
 * This class is a ClassLoader to which one can assign target jar files.
 *
 * @author  Simon Uyttendaele
 */
public class JarLoader extends URLClassLoader
{
	/**
	 * Default Constructor
	 */
	public JarLoader()
	{
		super(new URL[]{});
	}
	
	/**
	 * Inherit context from a parent ClassLoader
	 * @param	parent	the parent ClassLoader
	 */
	public JarLoader(ClassLoader parent)
	{
		super(new URL[]{}, parent);
	}
	
	/**
	 * Inherit context from a parent ClassLoader and specify a lookup path for resources
	 * @param	parent	the parent ClassLoader
	 * @param	resourceLookupPath	the directory in which to look for resources
	 */
	public JarLoader(ClassLoader parent, String resourceLookupPath)
	{
		super(new URL[]{}, parent);
		this.resourceLookupPath = resourceLookupPath;
	}
	
	/**
	 * Adds a jar file as resource for this instance.
	 * @param	path	the jar file path
	 * @note	if the path is null, empty or does not target a ".jar" file, nothing happens
	 */
	public void addFile(String path) throws MalformedURLException
	{
		if( path == null || path.length() == 0 || !path.endsWith(".jar") || !new File(path).exists() )
			return;

		this.addURL(new URL("jar", "", "file:" + path + "!/"));
	}
	
	private String resourceLookupPath = null;
	/**
	 * Returns an input stream for reading the specified resource.
	 * If the current instance was created using a resource lookup path (see {@link #JarLoader(ClassLoader, String)}), then that directory is
	 * first browsed for the specified resource. If not found, then the normal <code>ClassLoader</code> behavior is applied.
	 * @param name	The resource name
	 * @return	An input stream for reading the resource, or <code>null</code> if the resource could not be found
	 */
	public InputStream getResourceAsStream(String name)
	{
		try
		{
			if( resourceLookupPath == null )
				return super.getResourceAsStream(name);

			if( new File(resourceLookupPath + File.separator + name).exists() )
				return new FileInputStream(resourceLookupPath + File.separator + name);
			else
				return super.getResourceAsStream(name);
		}
		catch(FileNotFoundException fnfe)
		{
			return null;
		}
	}
}