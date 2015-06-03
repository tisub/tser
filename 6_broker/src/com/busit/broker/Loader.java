package com.busit.broker;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import com.anotherservice.io.*;
import com.anotherservice.util.*;

public class Loader extends ClassLoader
{
	private Zip source;
	
	private static JarLoader initializeParent() throws Exception
	{
		JarLoader context = new JarLoader(Thread.currentThread().getContextClassLoader());
		
		String jarList = Config.gets("com.busit.broker.jars");
		if( jarList != null && jarList.length() > 0 )
		{
			String[] jars = jarList.split("(?s)\\s*,\\s*");
			for( String jar : I.iterable(jars) )
				context.addFile(jar);
		}
		
		return context;
	}
	
	public Loader(Zip source) throws Exception
	{
		super(initializeParent());
		this.source = source;
	}
	
	// ====================================
	// CLASS LOADER
	// ====================================
	
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
	{
		checkSecurity(name);
		
		// will call findClass if parent does not know it
		return super.loadClass(name, resolve);
	}
	
	public Class<?> get(String className) throws ClassNotFoundException 
	{
		return findClass(className);
	}
	
	protected Class<?> findClass(String name) throws ClassNotFoundException 
	{
		Logger.finest("Looking up class " + name);
		byte[] b = loadData(name.replaceAll("\\.", "/") + ".class");
		if( b == null || b.length == 0 )
			throw new ClassNotFoundException();

		return defineClass(name, b, 0, b.length);
	}

	public InputStream getResourceAsStream(String name)
	{
		Logger.finest("Getting resource " + name);
		byte[] data = loadData(name);
		return (data == null ? null : new ByteArrayInputStream(data));
	}
	
	private byte[] loadData(String name)
	{
		return source.get(name);
	}
	
	private void checkSecurity(String name)
	{
		// TODO : throw new SecurityException("Illegal package access");
	}
}