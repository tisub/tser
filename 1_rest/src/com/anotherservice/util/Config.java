package com.anotherservice.util;

import com.anotherservice.io.*;
import com.anotherservice.util.*;
import java.util.*;
import java.io.InputStream;

/**
 * This class acts as a simple config store. It can natively load a simple XML file using the {@link com.anotherservice.util.Xml} class.
 * Data is organized in a logical structure of "."-separated names
 *
 * @author  Simon Uyttendaele
 */
public class Config
{
	private static ConfigImpl _instance = new ConfigImpl();
	private Config() { }
	
	public static class ConfigImpl
	{
	private Map<String, Object> data = new Hashtable<String, Object>();
	
	/**
	 * Retrieves the value associated with the provided key.
	 * The key can be composed of different grouping names separated by a dot ("."). If the key targets a parent group, 
	 * it is returned as a <code>Map&lt;String, Object&gt;</code>
	 * @note	the key is case sensitive
	 * @param	key	the config key 
	 * @return	the associated value or <code>null</code> if the key is not found
	 */
	public Object get(String key)
	{
		if (data == null || key == null)
			return null;

		String[] parts = key.split("\\.");
		Object item = null;
		Map<String, Object> root = data;
		for (int i = 0; i < parts.length - 1; i++)
		{
			item = root.get(parts[i]);
			if( item == null || !(item instanceof Map) )
				return null;
			root = (Map<String, Object>)item;
		}
		
		if( parts.length == 0 )
			return null;
		
		item = root.get(parts[parts.length - 1]);
		return item;
	}
	
	/**
	 * Retrieves the <code>String</code> value associated with the provided key or its <code>.toString()</code> counterpart.
	 * The key can be composed of different grouping names separated by a dot (".").
	 * @note	the key is case sensitive
	 * @param	key	the config key
	 * @return	the associated value or <code>null</code> if the key is not found
	 */
	public String gets(String key)
	{
		Object value = get(key);
		if( value == null )
			return null;
		if( value instanceof String )
			return (String)value;
		else
			return value.toString();
	}
	
	/**
	 * Retrieves the <code>String</code> value associated with the provided key under the grouping name part defined by the provided instance's canonical name.
	 * <br />Example: for <code>gets(new Object(), "foo")</code>, the key searched is <em>java.lang.Object.foo</em>
	 * @param	instance	the key prefix
	 * @param	key	the config key
	 * @return	the associated value or <code>null</code> if the key is not found
	 * @see	#gets(String)
	 */
	public String gets(Object instance, String key)
	{
		return gets(instance.getClass().getCanonicalName() + "." + key);
	}
	
	/**
	 * Sets a config value at the target entry key
	 * The key can be composed of different grouping names separated by a dot ("."). If the key targets a parent group
	 * or an existing key, it is overriden. If the key targets a non-existing key, it is created on the fly.
	 * @note	the key is case sensitive
	 * @param	key	the config key
	 * @param	value	the config value
	 * @throws	RuntimeException	if the ket targets an existing value as a group
	 */
	public void set(String key, Object value)
	{
		if( key == null )
			return;
			
		String[] parts = key.split("\\.");
		Object item = null;
		Map<String, Object> root = data;
		for (int i = 0; i < parts.length - 1; i++)
		{
			item = root.get(parts[i]);
			if (item == null)
			{
				item = new Hashtable<String, Object>();
				root.put(parts[i], item);
			}
			else if( !(item instanceof Map) )
				throw new RuntimeException("Cannot set a config value for a non-container key");
			root = (Map<String, Object>)item;
		}

		root.put(parts[parts.length - 1], value);
	}
	
	/**
	 * Sets a config value at the target entry key under the grouping name part defined by the provided instance's canonical name.
	 * <br />Example: for <code>gets(new Object(), "foo")</code>, the key searched is <em>java.lang.Object.foo</em>
	 * @param	instance	the key prefix
	 * @param	key	the config key
	 * @param	value	the config value
	 * @see	#set(String, Object)
	 */
	public void set(Object instance, String key, Object value)
	{
		set(instance.getClass().getCanonicalName() + "." + key, value);
	}
	
	/**
	 * Loads the target XML file and merge all values.
	 * If a value already exists at that key, it is changed to a Collection and both values exist; if the existing value was already a Collection, the new value is added.
	 * If a group already exists in the key path, all values are merged.
	 * @param	path	the path of the XML file
	 */
	public void load(String path) throws Exception
	{
		String xml = FileReader.readFile(path);
		merge(Xml.decode(xml, true), data);
	}
	
	/**
	 * Loads the target XML stream and merge all values.
	 * If a value already exists at that key, it is changed to a Collection and both values exist; if the existing value was already a Collection, the new value is added.
	 * If a group already exists in the key path, all values are merged.
	 * @param	stream	the stream containing XML data
	 */
	public void load(InputStream stream) throws Exception
	{
		String xml = FileReader.readFile(stream);
		merge(Xml.decode(xml, true), data);
	}
	
	private void merge(Map<String, Object> source, Map<String, Object> target)
	{
		for( String key : source.keySet())
		{
			Object sourceValue = source.get(key);
			Object targetValue = target.get(key);

			if (targetValue == null)
			{
				target.put(key, sourceValue);
				return;
			}
			else if (sourceValue instanceof Map && targetValue instanceof Map)
				merge((Map<String, Object>)sourceValue, (Map<String, Object>)targetValue);
			else if (targetValue instanceof Collection)
			{
				if( sourceValue instanceof Collection )
					((Collection)targetValue).addAll((Collection)sourceValue);
				else
					((Collection)targetValue).add(sourceValue);
			}
			else
			{
				ArrayList<Object> t = new ArrayList<Object>();
				t.add(targetValue);
				if( sourceValue instanceof Collection )
					t.addAll((Collection)sourceValue);
				else
					t.add(sourceValue);
				
				target.put(key, t);
			}
		}
	}
	}
	
	/**
	 * Retrieves the value associated with the provided key in the provided map.
	 * If the key does not exist, then it is initialized with the provided value and the latter is returned.
	 * @param map the map to look into
	 * @param key the config key
	 * @param value the default value if not set
	 * @return the existing or the provided value
	 */
	public static Object get(Map<String, Object> map, String key, Object value)
	{
		if( map == null )
			return null;
		if( !map.containsKey(key) )
			map.put(key, value);
		return map.get(key);
	}
	
	public static Object get(String key) { return _instance.get(key); }
	public static String gets(String key) { return _instance.gets(key); }
	public static String gets(Object instance, String key) { return _instance.gets(instance, key); }
	public static void set(String key, Object value) { _instance.set(key, value); }
	public static void set(Object instance, String key, Object value) { _instance.set(instance, key, value); }
	public static void load(String path) throws Exception { _instance.load(path); }
	public static void load(InputStream stream) throws Exception { _instance.load(stream); }
}