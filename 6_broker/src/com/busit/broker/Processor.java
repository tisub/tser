package com.busit.broker;

import com.busit.*;
import com.anotherservice.io.*;
import com.anotherservice.util.*;
import java.util.*;
import com.busit.security.*;
import com.busit.routing.*;

public class Processor extends ClassLoader implements Runnable
{
	/**
	 * The relative URL of the REST API endpoint that can return the connector's code (jar).
	 * The connector ID will be appended to this URL
	 */
	public static String codeRetrievalRestAction = "connector/download?active=true&encoded=true&connector=";
	
	/**
	 * The relative URL of the REST API endpoint that can return the instance data including outgoing links.
	 * The instance ID will be appended to this URL
	 */
	public static String instanceRetrievalRestAction = "instance/select?extended=1&id=";
	
	/**
	 * The relative URL of the REST API endpoint to forward messages to.
	 */
	public static String messageForwardRestAction = "message/forward";
	
	/**
	 * The relative URL of the REST API endpoint to acknowledge a payment.
	 * The transaction ID will be appended to this URL
	 */
	public static String ackPaymentRestAction = "credit/ack?id=";
	
	/**
	 * The relative URL of the REST API endpoint to confirm payment.
	 * The transaction ID will be appended to this URL
	 */
	public static String confirmPaymentRestAction = "credit/confirm?id=";
	
	/**
	 * The relative URL of the REST API endpoint to refund payment.
	 * The transaction ID will be appended to this URL
	 */
	public static String refundPaymentRestAction = "credit/refund?id=";

	/**
	 * The relative URL of the REST API endpoint to insert errors.
	 */
	public static String insertErrorRestAction = "instance/error/insert";
	
	private IConnector connector;
	private Any instance;
	private String task;
	private Path path;
	
	/**
	 * Creates a new instance of a task that will be handled asynchronousely. Technically speaking,
	 * this instance will be threaded and all processing will occur only at that time. Hence this
	 * constructor only stores the required input data for later on processing.
	 * @param	json	The JSON-serialized task to perform at a later point in time.
	 * @see #run()
	 * @note See {@link com.busit.broker} for details about the JSON format required.
	 */
	public Processor(String task)
	{
		this.task = task;
	}
	
	// ====================================
	// RUNNABLE
	// ====================================
	
	public void run()
	{
		String className = null;
		
		try { className = initialize(); }
		catch(Exception e)
		{
			// This is a setup error = should not happen
			Logger.severe(e);
			return;
		}
		
		if( path.getLast().shareTransaction != null )
		{
			try
			{
				RestApi.request(ackPaymentRestAction + path.getLast().shareTransaction);
			}
			catch(Exception e)
			{
				// The transaction could not be ACKd because it is timeout or already ACK or it is an invalid transaction
				Logger.warning("Transaction ACK failure: " + path.getLast().shareTransaction);
				return;
			}
		}
		
		try
		{
			RestApi.request(ackPaymentRestAction + path.getLast().transaction);
		}
		catch(Exception e)
		{
			// The transaction could not be ACKd because it is timeout or already ACK or it is an invalid transaction
			Logger.warning("Transaction ACK failure: " + path.getLast().transaction);
			return;
		}
		
		try
		{
			path.getLast().handledTime = new java.util.Date().getTime();
			process(className);
			if( path.getLast().shareTransaction != null )
				RestApi.request(confirmPaymentRestAction + path.getLast().shareTransaction);
			RestApi.request(confirmPaymentRestAction + path.getLast().transaction);
		}
		catch(Exception e)
		{
			// This is a runtime error = may happen
			// @see com.busit.rest.Credit#INTERNAL_REFUND(String)
			try
			{
				// TODO notify developer on the AMQP QueuedEvents
			}
			catch(Exception ex)
			{
				Logger.severe("Could not insert error " + path.getLast().identifier); Logger.warning(ex);
			}
			
			Logger.info(e);
			
			if( path.getLast().shareTransaction != null )
			{
				try { RestApi.request(refundPaymentRestAction + path.getLast().shareTransaction); }
				catch(Exception ex) { Logger.severe("Could not refund share transaction " + path.getLast().shareTransaction); Logger.warning(ex); }
			}
			try { RestApi.request(refundPaymentRestAction + path.getLast().transaction); }
			catch(Exception ex) { Logger.severe("Could not refund transaction " + path.getLast().transaction); Logger.warning(ex); }
			return;
		}
	}
	
	private String initialize() throws Exception
	{
		Logger.finest("Parsing task");

		Any json = Json.decode(this.task);
		if( !json.containsKey("path") || !json.get("path").isList() )
			throw new IllegalArgumentException("Missing 'path' in the input task");
		if( !json.containsKey("message") || !(json.get("message").<String>value() instanceof String) )
			throw new IllegalArgumentException("Missing 'message' in the input task");
		
		// CAUTION : we replace the original task with only its message content
		this.task = json.<String>value("message");
		Logger.finest("Parsing task complete");

		Logger.finest("Building path");
		path = new Path(json.get("path"));
		Logger.finest("Building path complete");
		
		Logger.finest("Retreiving instance");
		instance = RestApi.request(instanceRetrievalRestAction + path.getLast().toInstance).get(0);
		Logger.finest("Retreiving instance complete");
		
		setClassLoader(instance.get("instance_connector").toString());
		
		Logger.finest("Loading connector config");
		Config.ConfigImpl c = new Config.ConfigImpl();
		c.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.xml"));
		String className = c.gets("className");
		
		if( className == null || className.length() == 0 )
			throw new IllegalArgumentException("Invalid or missing 'className' in connector configuration");
		Logger.finest("Loading connector config complete");
		
		return className;
	}
	
	private void setClassLoader(String connectorId) throws Exception
	{
		Logger.finest("Retreiving code of connector " + connectorId);
		Any code = RestApi.request(codeRetrievalRestAction + connectorId);
		Logger.finest("Retreiving code complete");

		Logger.finest("Setting class loader of connector " + connectorId);
		Zip source = new Zip(new Base64InputStream(code.<String>value("code"), true));
		ClassLoader c = new Loader(source);
		
		Context.change(c);
		Logger.finest("Setting class complete for connector " + connectorId);
	}
	
	private void process(String className) throws Exception
	{
		Logger.finest("Configuring instance of class " + className);
		connector = (IConnector) Class.forName(className, true, Thread.currentThread().getContextClassLoader()).newInstance();
		
		// INTERFACES
		Any tmp = instance.get("interfaces");
		Map<String, Map<String, String>> inputs = new HashMap<String, Map<String, String>>();
		Map<String, Map<String, String>> outputs = new HashMap<String, Map<String, String>>();
		for( Any t : tmp )
		{
			Map<String, String> i = new HashMap<String, String>();
			i.put("key", t.<String>value("interface_key"));
			i.put("value", t.<String>value("interface_dynamic_value"));
			if( t.<String>value("interface_type").equals("1") )
				inputs.put(t.<String>value("interface_name"), i);
			else
				outputs.put(t.<String>value("interface_name"), i);
		}
		
		// CONFIGS
		tmp = instance.get("configs");
		Map<String, String> config = new HashMap<String, String>();
		for( Any t : tmp )
			config.put(t.<String>value("config_key"), t.<String>value("config_value"));
		config.put("uid", SHA1.hash(className + path.getLast().toInstance));
		config.put("instance", path.getLast().toInstance);
		
		connector.init(config, inputs, outputs);
		Logger.finest("Configuring instance complete");

		// ==============================
		// PUSH TO INSTANCE
		// ==============================
		Message m = new Message(this.task);
		boolean isCron = Config.gets("com.busit.rest.cron").equals(m.content().toText());
		
		long push_time = new java.util.Date().getTime();
		if( isCron )
		{
			Logger.finest("Cron on interface");
			connector.cron(m, path.getLast().toInterface);
			push_time = new java.util.Date().getTime() - push_time;
			Logger.finest("Cron on interface complete");
		}
		else
		{
			Logger.finest("Pushing on interface");
			connector.setInput(m, path.getLast().toInterface);
			push_time = new java.util.Date().getTime() - push_time;
			Logger.finest("Pushing on interface complete");
		}
		
		// ==============================
		// GET LINKS
		// ==============================
		Any links = Any.empty();
		for( Any l : instance.get("links") )
		{
			if( l.<String>value("link_active").equals("0") )
				continue;
				
			String from = l.<String>value("interface_from");
			
			// when CRON, pull only the links from that interface
			if( isCron && !from.equals(path.getLast().toInterface) )
				continue;
			
			if( !links.containsKey(from) )
				links.put(from, new ArrayList<Map<String, String>>());
			links.get(from).add(l);
		}
		
		for( String interfaceId : links.keySet() )
		{
			Any l = links.get(interfaceId);
			
			// ==============================
			// PULL FROM INSTANCE
			// ==============================
			long pull_time = new java.util.Date().getTime();
			MessageList ml = new MessageList();
			IMessage im = connector.getOutput(interfaceId);
			if( im instanceof MessageList )
				ml = (MessageList) im;
			else
				ml.add(im);
			
			if( ml == null || ml.size() == 0 )
			{
				Logger.finest("Empty message from interface " + interfaceId + ". Ignoring.");
				continue;
			}
			
			for( int i = 0; i < ml.size(); i++ )
			{
				im = ml.get(i);
				if( im == null || !(im instanceof Message) )
				{
					Logger.finest("Empty message from interface " + interfaceId + ". Ignoring.");
					continue;
				}
				
				m = (Message) im;
				if( Config.gets("com.busit.rest.cron").equals(m.content().toText()) )
				{
					Logger.finest("Connector cannot send CRON messages. Ignoring.");
					continue;
				}
				
				pull_time = new java.util.Date().getTime() - pull_time;
				
				for( Any link : l )
				{
					try
					{
						String message = m.toString(m.fromIdentity(), m.toIdentity());
						int size = message.length();
						String hash = MD5.hash(message);
						String steps = path.toString(push_time + pull_time, size, hash);

						Step next = new Step();
						next.fromInstance = instance.<String>value("instance_id");
						next.fromInterface = interfaceId;
						next.toInstance = link.<String>value("instance_to");
						next.toInterface = link.<String>value("interface_to");
						next.inputSize = size;
						next.inputHash = hash;
						// costPrice, costTax, sentTime, transaction will be filled by the REST API
						// handledTime, processingTime, outputSize, outputHash will be filled by THIS in next round
						
						// ==============================
						// FORWARD TO LINK
						// ==============================
						forward(message, steps, next.toString());
					}
					catch(Exception e)
					{
						// Any "normal" error in the forward will be logged by the REST API
						// so we can ignore those REST API errors here
						Logger.finer(e);
					}
				}
			}
		}
	}
	
	private void forward(String message, String path, String destination) throws Exception
	{
		Logger.finest("Forwarding message");

		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put("message", message);
		params.put("path", path);
		params.put("destination", destination);
		
		RestApi.request(messageForwardRestAction, params);
	}
}