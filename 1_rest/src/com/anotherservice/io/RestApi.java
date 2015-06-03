package com.anotherservice.io;

import com.anotherservice.util.*;
import java.util.*;

public class RestApi
{
	private static String auth = "";
	private static String url = "";
	
	public static void initialize(String url, String auth)
	{
		initialize(url, auth, true);
	}
	
	public static void initialize(String url, String auth, boolean enableCache)
	{
		RestApi.url = url;
		RestApi.auth = auth;
		if( enableCache )
			cache = new Cache(cacheTimeout);
	}
	
	public static Any request(String action, boolean cacheable) throws Exception
	{
		return request(action, null, cacheable);
	}
	
	public static Any request(String action) throws Exception
	{
		return request(action, null, false);
	}
	
	public static Any request(String action, Hashtable<String, String> params) throws Exception
	{
		return request(action, params, false);
	}
	
	public static Any request(String action, Hashtable<String, String> params, boolean cacheable) throws Exception
	{
		if( params == null )
			params = new Hashtable<String, String>();
		if( !params.containsKey("f") )
			params.put("f", "json");
		if( !params.containsKey("auth") && auth != null && auth.length() > 0 )
			params.put("auth", auth);
		if( action == null )
			action = "";
		
		String key = action + params.toString();
		if( cacheable && cache != null )
		{
			Any a = cache.cache(key);
			if( a != null )
				return a;
		}
		
		String r = UrlReader.readUrl(action.matches("^https?://.*") || url == null ? action : url + action, params);

		Any response = Json.decode(r);
		if( response.isMap() )
		{
			if( response.containsKey("response") )
			{
				if( cacheable && cache != null ) cache.cache(key, response.get("response"));
				return response.get("response");
			}
			else if( response.containsKey("error") )
			{
				response = response.get("error");
				Exception e = new Exception(response.<String>value("message"));
				Any trace = response.get("trace");
				if( trace == null )
					trace = Any.empty();
					
				StackTraceElement[] stackTrace = new StackTraceElement[trace.size()];
				for( int i = 0; i < trace.size(); i++ )
				{
					Any stack = trace.get(i);
					stackTrace[i] = new StackTraceElement(
						stack.<String>value("class"), 
						stack.<String>value("method"), 
						stack.<String>value("file"), 
						stack.<Double>value("line").intValue());
				}
				e.setStackTrace(stackTrace);
				
				throw e;
			}
			else
			{
				if( cacheable && cache != null ) cache.cache(key, response);
				return response;
			}
		}
		else
		{
			if( cacheable && cache != null ) cache.cache(key, response);
			return response;
		}
	}
	
	public static long cacheTimeout = 1800000; // 30 minutes
	private static Cache cache = null;
	private static class Cache extends Thread
	{
		public Cache(long timeout)
		{
			this.timeout = timeout;
			this.setDaemon(true);
			this.start();
		}
		
		private long timeout = 1800000; // 30 minutes
		private Map<String, CacheEntry> _cache = new HashMap<String, CacheEntry>();
		private class CacheEntry
		{
			Any value = null;
			long timeout = 0;
			public CacheEntry(Any v, long t) { value = v; timeout = System.currentTimeMillis() + t; }
		}
		
		private Any cache(String key)
		{
			synchronized(_cache)
			{
				CacheEntry ce = _cache.get(key);
				if( ce != null )
				{
					if( ce.timeout < System.currentTimeMillis() )
					{
						_cache.remove(key);
						return null;
					}
					else
					{
						//Logger.finer("RestApi cache hit");
						return ce.value;
					}
				}
				else
					return null;
			}
		}
	
		private void cache(String key, Any value)
		{
			synchronized(_cache) { _cache.put(key, new CacheEntry(value, timeout)); }
		}
		
		public void run()
		{
			while(true)
			{
				try
				{
					Thread.sleep(60000); // 1 min
					
					synchronized(_cache)
					{ 
						Map.Entry<String, CacheEntry> entry = null;
						Iterator<Map.Entry<String, CacheEntry>> i = _cache.entrySet().iterator();
						while( i.hasNext() )
						{
							entry = i.next();
							if( entry.getValue().timeout < System.currentTimeMillis() )
								i.remove();
						}
					}
				}
				catch(Exception e)
				{
				}
			}
		}
	}
}