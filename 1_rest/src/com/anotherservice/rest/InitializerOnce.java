package com.anotherservice.rest;

import java.util.*;
import java.io.InputStream;

/**
 * This class is ensured that the {@link com.anotherservice.rest.Initializer#initialize()} method gets called only once in a Thread safe way.
 * 
 * @note	to be exact, the <code>initialize()</code> method may be called once per ClassLoader context.
 * @author  Simon Uyttendaele
 */
public abstract class InitializerOnce implements Initializer
{
	private static final int ERROR_CLASS_ID = 0x4000;
	
	private static final Hashtable<Class, Boolean> locks = new Hashtable<Class, Boolean>();
		
	/**
	 * Subclasses <strong>should override</strong> this method 
	 * and wrap a <code>super.initialize()</code> statement in a <code>try/catch</code> block
	 * @throws	RestException	if the method gets called more than once
	 */
	public synchronized void initialize()
	{
		if( locks.get(this.getClass()) == true )
			throw new RestException("Already initialized", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_CONFIG | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x1);
		else
			locks.put(this.getClass(), true);
	}
	
	/**
	 * Returns an input stream for reading the specified resource.
	 * <br />This method is a shorthand for <code>Thread.currentThread().getContextClassLoader().getResourceAsStream()</code>.
	 * <br />Since all initializers are instanciated using their own {@link com.anotherservice.util.JarLoader}, they have the ability to
	 * load plain file resources from the deployment directory of the service. If the file is not found in the service directory, the default
	 * parent <code>ClassLoader</code> behavior is applied.
	 * @param name	The resource name
	 * @return	An input stream for reading the resource, or <code>null</code> if the resource could not be found
	 */
	public InputStream getResourceAsStream(String name)
	{
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	}
}