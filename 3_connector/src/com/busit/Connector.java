package com.busit;

import java.util.*;
import com.anotherservice.util.Any;
import com.anotherservice.util.Logger;

/** This class is a simple {@link com.busit.ConnectorHelper} implementation of {@link com.busit.IConnector} which should make life easier.
 *
 * <br /><br />
 * <strong>How to proceed</strong> : extend this class and implement the interface you need among {@link com.busit.Consumer Consumer}, 
 * {@link com.busit.Producer Producer} and {@link com.busit.Transformer Transformer}.<br />
 */
public abstract class Connector implements IConnector, ConnectorHelper
{
	// ===========================================
	// LOCAL
	// ===========================================
	private Any conf = Any.empty();
	private Any in = Any.empty();
	private Any out = Any.empty();
	private IMessage msg = null;
	private Interface _internal_push_in = null;
	
	/** Pushes the message on the given interface.
	 * <br />Implementation : <ol>
	 * <li>Call {@link com.busit.Consumer#consume(IMessage, Interface)} if applicable</li>
	 * <li>Prepare internal state for {@link com.busit.Transformer#transform(IMessage, Interface, Interface)} if applicable (aka: do nothing yet)</li>
	 * <li>Otherwise throw an <code>UnsupportedOperationException</code></li>
	 * </ol>
	 * If you want more control over this method <em>(highly discouraged)</em>, 
	 * override it to define what to do when a message is being <strong>pushed into</strong> your connector (aka: your connector receives data).
	 * @param	message	The message
	 * @param	in	The interface that is being triggered
	 * @throws	UnsupportedOperationException	if the current implementation is not a {@link com.busit.Consumer} or a {@link com.busit.Transformer}.
	 */
	private void push(IMessage message, Interface in)
	{
		this.currentInterface = in;
		if( this instanceof Consumer )
			((Consumer)this).consume(message, in);
		if( this instanceof Transformer )
			_internal_push_in = in;
		if( !(this instanceof Consumer) && !(this instanceof Transformer) )
			throw new UnsupportedOperationException("Push is not supported");
	}
	
	/** Pulls the message from the given interface.
	 * <br />Implementation : <ol>
	 * <li>Call {@link com.busit.Producer#produce(IMessage, Interface)} if applicable. 
	 * If a valid {@link com.busit.IMessage} is returned, return it and do not process the next step</li>
	 * <li>Call {@link com.busit.Transformer#transform(IMessage, Interface, Interface)} if applicable</li>
	 * <li>Otherwise throw an <code>UnsupportedOperationException</code></li>
	 * </ol>
	 * If you want more control over this method <em>(highly discouraged)</em>, 
	 * override it to define what to do when a message is being <strong>pulled out of</strong> your connector (aka: your connector emits data).
	 * You should return a message instance obtained using either {@link #original()}, {@link #copy()} or {@link #empty()}.
	 * @param	out	The interface that is being triggered
	 * @return	The message to transmit to the next connector
	 * @throws	UnsupportedOperationException	if the current implementation is not a {@link com.busit.Producer} or a {@link com.busit.Transformer}.
	 */
	private IMessage pull(Interface out)
	{
		this.currentInterface = out;
		if( this instanceof Producer )
		{
			IMessage result = ((Producer)this).produce(out);
			if( result != null )
				return result;
		}
		if( this instanceof Transformer && _internal_push_in != null )
			return ((Transformer)this).transform(msg.copy(), _internal_push_in, out);
		else if( this instanceof Producer )
			return null; // the producer did return null before but we had to check for Transformer first
		
		// not a Producer and not a Transformer
		throw new UnsupportedOperationException("Pull is not supported");
	}
	
	public abstract boolean test() throws Exception;
	
	// ===========================================
	// CONNECTOR HELPER
	// ===========================================
	public Interface input(String key)
	{
		if( key == null || key.length() == 0 )
			return null;
		
		for( String name : in.keys() )
			if( key.equals(in.get(name).<String>value("key")) )
				return new Interface(name, in.get(name));
		return null;
	}
	
	public List<Interface> input()
	{
		List<Interface> list = new LinkedList<Interface>();
		for( String name : in.keys() )
			list.add(new Interface(name, in.get(name)));
		return list;
	}
	
	public Interface output(String key)
	{
		if( key == null || key.length() == 0 )
			return null;
		
		for( String name : out.keys() )
			if( key.equals(out.get(name).<String>value("key")) )
				return new Interface(name, out.get(name));
		return null;
	}
	
	public List<Interface> output()
	{
		List<Interface> list = new LinkedList<Interface>();
		for( String name : out.keys() )
			list.add(new Interface(name, out.get(name)));
		return list;
	}
	
	public String config(String key)
	{
		return conf.<String>value(key);
	}
	
	public Map<String, String> config()
	{
		Map<String, String> all = new HashMap<String, String>();
		for( String key : conf.keys() )
			all.put(key, conf.<String>value(key));
		return all;
	}
	
	private Interface currentInterface = null;
	public void notifyUser(String message)
	{
		Logger.info(Any.empty()
			.pipe("notifyUser", 
				Any.empty()
				.pipe("interface", (this.currentInterface != null ? this.currentInterface.name : ""))
				.pipe("message", message)
			).toJson()
		);
	}
	
	public void notifyOwner(String message, Map<String, Object> data)
	{
		Logger.info(Any.empty()
			.pipe("notifyOwner", 
				Any.empty()
				.pipe("interface", (this.currentInterface != null ? this.currentInterface.key : ""))
				.pipe("message", message)
				.pipe("data", data)
			).toJson()
		);
	}
	
	public String locale()
	{
		return config("__locale");
	}
	
	public String id()
	{
		return config("__uid");
	}
	
	// ===========================================
	// ICONNECTOR
	// ===========================================
	public void init(Map<String, String> config, Map<String, Map<String, String>> inputs, Map<String, Map<String, String>> outputs)
	{
		conf = Any.wrap(config);
		in = Any.wrap(inputs);
		out = Any.wrap(outputs);
	}
	
	public void cron(IMessage message, String interfaceId)
	{
		msg = message;
		if( in.containsKey(interfaceId) ) 
			push(msg, new Interface(interfaceId, in.get(interfaceId)));
		else if( !out.containsKey(interfaceId) ) 
			throw new UnsupportedOperationException("Invalid cron interface " + interfaceId);
		
		// in case cron output, do nothing... just do it in pull()
	}
	
	public void setInput(IMessage message, String interfaceId)
	{
		if( !in.containsKey(interfaceId) ) throw new UnsupportedOperationException("Invalid input interface " + interfaceId);
		msg = message;
		push(msg, new Interface(interfaceId, in.get(interfaceId)));
	}
	
	public IMessage getOutput(String interfaceId)
	{
		if( !out.containsKey(interfaceId) ) throw new UnsupportedOperationException("Invalid output interface " + interfaceId);
		return pull(new Interface(interfaceId, out.get(interfaceId)));
	}
	
	public IMessage getSampleData(IMessage message, String interfaceId)
	{
		this.currentInterface = new Interface(interfaceId, out.get(interfaceId));
		if( this instanceof Producer )
			return ((Producer)this).sample(this.currentInterface);
		else
			return null;
	}
	
	public boolean isFunctional(Map<String, String> config, Map<String, Map<String, String>> inputs, Map<String, Map<String, String>> outputs) throws Exception
	{
		conf = Any.wrap(config);
		in = Any.wrap(inputs);
		out = Any.wrap(outputs);
		
		return test();
	};
}
