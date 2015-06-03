package com.anotherservice.util;

import java.util.*;
import java.lang.*;

/**
 * This class stores different ClassLoader contexts. Contexts can be registered with any <em>key</em>, ideally: an object instance with the ClassLoader it belongs to.
 * This class intends to be used in multi-threaded environments using multiple ClassLoaders; thus, all methods are synchronized.
 *
 * @author  Simon Uyttendaele
 */
public class Context
{
	private Context() { }
	
	private static Hashtable<Object, ClassLoader> contexts = new Hashtable<Object, ClassLoader>();
	
	/**
	 * Returns the ClassLoader associated with the provided key
	 * @param	key	the key that was used to register the ClassLoader. Keys behavior is identical to the <code>Map</code> interface keys behavior.
	 * @return	the associated ClassLoader or <code>null</code> if no match is found
	 */
	synchronized public static ClassLoader get(Object key)
	{
		if( key == null )
			return null;

		return contexts.get(key);
	}
	
	/**
	 * Registers the current Thread's ClassLoader with the provided key
	 * @param	key	the registration key
	 */
	synchronized public static void add(Object key)
	{
		if( key == null )
			throw new IllegalArgumentException("key cannot be null");

		contexts.put(key, Thread.currentThread().getContextClassLoader());
	}
	
	/**
	 * Registers the provided ClassLoader with the provided key
	 * @param	key	the registration key
	 * @param	context	the ClassLoader
	 */
	synchronized public static void add(Object key, ClassLoader context)
	{
		if( key == null || context == null )
			throw new IllegalArgumentException("key and context cannot be null");

		contexts.put(key, context);
	}
	
	/**
	 * Removes the association between the provided key and the ClassLoader context
	 * @note	<ul><li>The ClassLoader may not be completely released if it is still registered with another key</li>
	 * <li>If the key is not found, nothing happens</li></ul>
	 * @param	key	the key that was used to register the ClassLoader
	 */
	synchronized public static void remove(Object key)
	{
		if( key != null )
			contexts.remove(key);
	}
	
	/**
	 * Checks if the key's <code>Type</code> is is defined in the current Thread's ClassLoader.
	 * @param	key	an object instance. The ClassLoader of the key is retrieved by <code>key.getClass().getClassLoader()</code>
	 * @return	<code>true</code> if the ClassLoader of the key is the same as the current Thread's. <code>false</code> otherwise
	 */
	synchronized public static boolean defined(Object key)
	{
		if( key == null )
			return false;
			
		return (key.getClass().getClassLoader() == Thread.currentThread().getContextClassLoader());
	}
	
	/**
	 * Checks if the key's <code>Type</code> is the provided ClassLoader.
	 * @param	key	key	an object instance. The ClassLoader of the key is retrieved by <code>key.getClass().getClassLoader()</code>
	 * @param	context	the ClassLoader to check against
	 * @return	<code>true</code> if the ClassLoader of the key is the same as the provided context. <code>false</code> otherwise
	 */
	synchronized public static boolean defined(Object key, ClassLoader context)
	{
		if( key == null || context == null )
			return false;

		return (key.getClass().getClassLoader() == context);
	}
	
	/**
	 * Checks if the provided key is registered to a ClassLoader
	 * @param	key	the registration key
	 * @return	<code>true</code> if a ClassLoader is registered with this key. <code>false</code> otherwise
	 */
	synchronized public static boolean registered(Object key)
	{
		if( key == null )
			return false;

		return contexts.containsKey(key);
	}
	
	/**
	 * Checks if the ClassLoader registered with this key is the same as the current Thread's
	 * @param	key	the registration key
	 * @return	<code>true</code> if the ClassLoader registered with this key is the same as the current Thread's. <code>false</code> otherwise
	 */
	synchronized public static boolean created(Object key)
	{
		if( key == null )
			return false;

		return (get(key) == Thread.currentThread().getContextClassLoader());
	}
	
	/**
	 * Checks if the ClassLoader registered with this key is the same as the provided one
	 * @param	key	the registration key
	 * @param	context	the ClassLoader to check against
	 * @return	<code>true</code> if the ClassLoader registered with this key is the same as the provided context. <code>false</code> otherwise
	 */
	synchronized public static boolean created(Object key, ClassLoader context)
	{
		return (get(key) == context);
	}
	
	/**
	 * Changes the current Thread's ClassLoader to the one registered with the target key
	 * @param	key	the registration key
	 * @throws	RuntimeException	if no ClassLoader is registered with that key
	 */
	synchronized public static void wrap(Object key)
	{
		if( key == null )
			throw new RuntimeException("Cannot wrap an undefined key");

		ClassLoader c = get(key);
		if( c == null )
			throw new RuntimeException("Cannot wrap an undefined context");
			
		Thread.currentThread().setContextClassLoader(c);
	}
	
	/**
	 * Changes the current Thread's ClassLoader to the one provided
	 * @throws	RuntimeException	if context is null
	 */
	public static void change(ClassLoader context)
	{
		if( context == null )
			throw new RuntimeException("Cannot wrap an undefined context");

		Thread.currentThread().setContextClassLoader(context);
	}
}