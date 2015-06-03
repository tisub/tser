package com.anotherservice.rest.formatter;

import com.anotherservice.rest.core.*;
import com.anotherservice.rest.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;

public class JsonFormatter implements IResponseFormatter
{
	public byte[] format(Object o)
	{
		Hashtable<String, Object> r = new Hashtable<String, Object>();
		r.put("response", o);
		return Hex.getBytes(Json.encode(r));
	}
	
	public byte[] format(Throwable t)
	{
		if( t == null )
			throw new IllegalArgumentException("t cannot be null");
		if( t instanceof Jsonizable )
		{
			Any a = Any.empty();
			a.put("error", t);
			return Hex.getBytes(a.toJson());
		}
		else
		{
			Hashtable<String, Object> error = new Hashtable<String, Object>();
			error.put("message", (t.getMessage() == null ? "null" : t.getMessage()));
			
			if( Logger.instance().level <= 500 /* Level.FINE */ )
			{
				StackTrace.collapse(t);
				StackTrace.removeLastUntil(t, Handler.class.getCanonicalName()); // do not show tomcat and routing stuff
				StackTrace.keepLastUntil(t, Database.class.getCanonicalName()); // do not show internal database driver stuff
			
				ArrayList<Hashtable<String, Object>> trace = new ArrayList<Hashtable<String, Object>>();
				for( StackTraceElement s : t.getStackTrace() )
				{
					Hashtable<String, Object> line = new Hashtable<String, Object>();
					line.put("file", (s.getFileName() == null ? "unknown" : s.getFileName()));
					line.put("line", s.getLineNumber());
					line.put("class", (s.getClassName() == null ? "unknown" : s.getClassName()));
					line.put("method", (s.getMethodName() == null ? "unknown" : s.getMethodName()));
					trace.add(line);
				}
				error.put("trace", trace);
			}

			Hashtable<String, Object> r = new Hashtable<String, Object>();
			r.put("error", error);
			return Hex.getBytes(Json.encode(r));
		}
	}
	
	public byte[] format(Handler h)
	{
		Hashtable<String, Object> help = new Hashtable<String, Object>();
		help.put("alias", h.getMappings());
		help.put("description", h.getDescription());
		help.put("grants", h.getGrants());
		
		if( h instanceof Index )
		{
			ArrayList<Collection<String>> entries = new ArrayList<Collection<String>>();
			for( Handler e : ((Index)h).getOwnHandlers() )
				entries.add(e.getMappings());
			help.put("entries", entries);
		}
		else if( h instanceof Action )
		{
			ArrayList<Hashtable<String, Object>> params = new ArrayList<Hashtable<String, Object>>();
			for( Parameter p : ((Action)h).getParameters() )
			{
				Hashtable<String, Object> param = new Hashtable<String, Object>();
				param.put("alias", p.getAliases());
				param.put("optional", p.isOptional);
				if( p.isMultipleValues )
					param.put("multiple", p.isMultipleValues);
				if( p.allowInUrl )
					param.put("urlizable", p.allowInUrl);
				if( p.minLength > 0 )
					param.put("minlength", p.minLength);
				if( p.maxLength > 0 )
					param.put("maxlength", p.maxLength);
				if( p.mustMatch != null )
					param.put("match", p.mustMatch);
				params.add(param);
			}
			help.put("parameters", params);
		}
		
		return format(help);
	}
}