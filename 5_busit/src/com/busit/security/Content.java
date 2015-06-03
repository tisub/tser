package com.busit.security;

import com.busit.*;
import com.anotherservice.util.*;
import java.util.regex.*;
import java.lang.*;
import java.util.*;

public class Content extends HashMap<String, Object> implements IContent
{
	private int _id = -1;
	public int id() { return _id; }
	public void id(int id) { _id = id; }
	
	private String _name = "";
	public String name() { return _name; }
	public void name(String name) { _name = name; }
	
	private List<Integer> _compat = new ArrayList<Integer>();
	public boolean compatible(int id) { return _id == id || _compat.contains(id); }
	public void compatible(int id, boolean value) { if( !value ) _compat.remove((Object)id); else if( !compatible(id) ) _compat.add(id); }
	public List<Integer> compatibility() { return _compat; }
	public void compatibility(List<Integer> compat) { _compat = compat; }

	private String _textFormat = "";
	public String textFormat() { return _textFormat; }
	public void textFormat(String format) { _textFormat = format; }
	
	private String _htmlFormat = "";
	public String htmlFormat() { return _htmlFormat; }
	public void htmlFormat(String format) { _htmlFormat = format; }
	
	public String toText() { return toText(textFormat()); }
	public String toText(String format)
	{
		if( format == null || format.length() == 0 )
			return toJson();

		Matcher m = Pattern.compile("\\{\\{(.+?)\\}\\}", Pattern.DOTALL).matcher(format);
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			if( m.group(1) == null || m.group(1).length() == 0 )
				m.appendReplacement(sb, "");
			else
				m.appendReplacement(sb, getReplacementValue(m.group(1), this));
		}
		m.appendTail(sb);
		
		return noscriptHtml(sb.toString());
	}
	
	public String toHtml() { return toHtml(htmlFormat()); }
	public String toHtml(String format)
	{
		if( format == null || format.length() == 0 )
			return "<pre>"+toText().replaceAll("&", "&amp;").replaceAll("<", "&lt;")+"</pre>";
	
		Matcher m = Pattern.compile("\\{\\{(.+?)\\}\\}", Pattern.DOTALL).matcher(format);
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			if( m.group(1) == null || m.group(1).length() == 0 )
				m.appendReplacement(sb, "");
			else
				m.appendReplacement(sb, getReplacementValue(m.group(1), this).replaceAll("&", "&amp;").replaceAll("<", "&lt;"));
		}
		m.appendTail(sb);
		
		return noscriptHtml(sb.toString());
	}
	
	public String toJson()
	{
		Map<String, Object> json = new HashMap<String, Object>();
		json.put("id", id());
		json.put("name", name());
		json.put("compatibility", compatibility());
		json.put("textFormat", textFormat());
		json.put("htmlFormat", htmlFormat());
		json.put("data", this);
		
		return Json.encode(json);
	}
	
	public Content() { }
	
	public Content(IContent c)
	{
		super();
		if( c == null )
		{
			this._id = 0;
			this._name = "Data";
			this.put("data", null);
			this._textFormat = "{{data}}";
			this._htmlFormat = "<pre>{{data}}</pre>";
		}
		else
		{
			this.id(c.id());
			this.name(c.name());
			this.textFormat(c.textFormat());
			this.htmlFormat(c.htmlFormat());
			this.compatibility(c.compatibility());
			
			for( Map.Entry<String, Object> entry : c.entrySet() )
				this.put(entry.getKey(), entry.getValue());
		}
	}
	
	public Content(String s)
	{
		super();
		try
		{
			if( s == null )
				throw new IllegalArgumentException("input string cannot be null");
			
			Any json = Json.decode(s);
			
			importFrom(json);
		}
		catch(Exception e)
		{
			this._id = 0;
			this._name = "Data";
			this.put("data", s);
			this._textFormat = "{{data}}";
			this._htmlFormat = "<pre>{{data}}</pre>";
		}
	}
	
	public void importFrom(Any json)
	{
		if( json == null )
			throw new IllegalArgumentException("input parameter cannot be null");
		
		if( !json.exists("data") || !json.get("data").isMap() )
			throw new IllegalArgumentException("Minimum requirement for Content is {\"data\": {}}");
		
		if( !json.isNull("id") )
			this.id(json.<Double>value("id").intValue());
		if( !json.isNull("name") )
			this.name(json.<String>value("name"));
		if( !json.isNull("textFormat") )
			this.textFormat(json.<String>value("textFormat"));
		if( !json.isNull("htmlFormat") )
			this.htmlFormat(json.<String>value("htmlFormat"));
		
		List<Integer> compat = new ArrayList<Integer>();
		if( json.exists("compatibility") && json.get("compatibility").isList() )
			for( Any c : json.get("compatibility") )
				compat.add(c.<Double>value().intValue());
		this.compatibility(compat);
		
		for( String key : json.get("data").keys() )
			this.put(key, json.get("data").value(key));
	}
	
	public void merge(IContent c)
	{
		for( Integer i : c.compatibility() )
			this.compatible(i, true);
		this.compatible(c.id(), true);
		for( Map.Entry<String, Object> entry : c.entrySet() )
			this.put(entry.getKey(), entry.getValue());
	}
	
	private String getReplacementValue(String key, Object root)
	{
		try
		{
			if( key == null || key.length() == 0 )
				return (root == null ? "" : "" + root);
			
			if( root instanceof Map )
			{
				if( ((Map)root).containsKey(key) )
				{
					Object o = ((Map)root).get(key);
					return (o != null ? o.toString() : "");
				}
				int i = key.indexOf(".");
				if( i < 0 ) return "";
				if( ((Map)root).containsKey(key.substring(0, i)) )
					return getReplacementValue(key.substring(i+1), ((Map)root).get(key.substring(0, i)));
				return "";
			}
			
			if( root instanceof Object[] )
				root = Arrays.asList((Object[])root);
			if( root instanceof List )
			{
				int index = -1;
				try { index = Integer.parseInt(key); } catch(NumberFormatException e) {}
				
				if( index >= 0 && index < ((List)root).size() )
				{
					Object o = ((List)root).get(index);
					return (o != null ? o.toString() : "");
				}
				int i = key.indexOf(".");
				if( i < 0 ) return "";
				try { index = Integer.parseInt(key.substring(0, i)); } catch(NumberFormatException e) {}
				
				if( index >= 0 && index < ((List)root).size() )
					return getReplacementValue(key.substring(i+1), ((List)root).get(index));
				return "";
			}
			
			return "";
		}
		catch(Exception e)
		{
			return "";
		}
	}
	
	private String noscriptHtml(String html)
	{
		if( html == null ) return "";
		
		return html
			.replaceAll("(?is)<\\s*script(:?.*?<\\s*/\\s*script\\s*>|[^>]*?/\\s*>)", "")
			.replaceAll("(?is)<\\s*object(:?.*?<\\s*/\\s*object\\s*>|[^>]*?/\\s*>)", "")
			.replaceAll("(?is)<\\s*embed(:?.*?<\\s*/\\s*embed\\s*>|[^>]*?/\\s*>)", "");
	}
}