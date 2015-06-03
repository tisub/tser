package com.busit.broker;

import com.busit.*;
import com.anotherservice.util.*;
import java.util.*;
import java.io.*;
import com.busit.security.*;
	
public class UncheckedMessage implements IMessage
{
	//=====================================
	// IMessage
	//=====================================
	
	private Hashtable<String, InputStream> _files = new Hashtable<String, InputStream>();;
	
	public InputStream file(String name)
	{
		if( name == null || name.length() == 0 || !_files.containsKey(name) )
			return null;
		
		InputStream is = _files.get(name);
		try { is.reset(); } catch(Exception e) {}
		return is;
	}
	
	public void file(String name, InputStream data)
	{
		if( name != null && name.length() > 0 && data != null )
			_files.put(name, data);
	}
	
	public Map<String, InputStream> files()
	{
		// reset all files streams
		for( Map.Entry<String, InputStream> f : _files.entrySet() )
		{
			try { f.getValue().reset(); } catch(Exception e) {}
		}
		
		return _files;
	}
	
	private IContent _content = null;
	
	public IContent content()
	{
		if( _content == null )
			_content = new Content((String)null);
		return _content;
	}
	
	public void content(IContent data)
	{
		_content = data;
	}
	
	public void clear()
	{
		_content = null;
		_files.clear();
	}
	
	public IMessage copy()
	{
		UncheckedMessage m = new UncheckedMessage(from(), to());
		m.content(content());
		for( Map.Entry<String, InputStream> f : files().entrySet() )
			m.file(f.getKey(), f.getValue());
		return m;
	}
	
	private String _from = null;
	
	public String from()
	{
		return _from;
	}
	
	private String _to = null;
	
	public String to()
	{
		return _to;
	}
	
	//=====================================
	// CONSTRUCTOR
	//=====================================
	
	public UncheckedMessage(String from, String to)
	{
		_from = from;
		_to = to;
	}
	
	public UncheckedMessage(IMessage m)
	{
		content(new Content(m.content().toJson()));
		for( Map.Entry<String, InputStream> f : m.files().entrySet() )
			file(f.getKey(), f.getValue());
		_from = m.from();
		_to = m.to();
	}
	
	public UncheckedMessage(String raw) throws Exception
	{
		this(Json.decode(raw));
	}
	
	public UncheckedMessage(Any a)
	{
		content(new Content(a.<String>value("content")));
		Any b = a.get("files");
		for( String name : b.keys() )
			file(name, new ByteArrayInputStream(Base64.decode(b.<String>value(name))));
		_from = a.<String>value("from");
		_to = a.<String>value("to");
	}
	
	//=====================================
	// BACK TO MESSAGE
	//=====================================
	
	public String toString()
	{
		Any a = Any.empty();
		a.put("from", from());
		a.put("to", to());
		a.put("content", content().toJson());
		
		// reset all files streams
		for( Map.Entry<String, InputStream> f : _files.entrySet() )
		{
			try { f.getValue().reset(); } catch(Exception e) {}
		}
		
		a.put("files", files());
		
		return a.toJson();
	}
	
	public ISecureMessage securize(Identity from, Identity to) throws Exception
	{
		Message m = new Message(from, to);
		m.content(content()); // caution : this is a shallow copy
		
		// reset all files streams
		for( Map.Entry<String, InputStream> f : _files.entrySet() )
		{
			try { f.getValue().reset(); } catch(Exception e) {}
		}
		
		for( Map.Entry<String, InputStream> f : files().entrySet() )
			m.file(f.getKey(), f.getValue());
		return m;
	}
}