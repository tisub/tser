package com.busit.broker;

import com.busit.*;
import java.util.*;
import com.anotherservice.io.*;
import com.anotherservice.util.*;
import com.busit.security.*;

public class FactoryImpl implements IFactory
{
	public IContent content(int id)
	{
		if( id < 0 )
			return new Content();
		if( id == 0 )
			return new Content((String)null);
		
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put("id", "" + id);
		try { return new Content(RestApi.request("type/select", params, true).get(0).<String>value("knowntype_data")); }
		catch(Exception e) { return new Content(); }
	}
	
	public IMessage message()
	{
		if( _template == null )
			throw new RuntimeException("template is not yet defined");
		return new UncheckedMessage(_template.from(), _template.to());
	}
	
	public IMessageList messageList()
	{
		return new MessageList();
	}
	
	private static IMessage _template = null;
	public void template(IMessage message)
	{
		if( _template != null )
			throw new RuntimeException("template is already defined");
		_template = message;
	}
}