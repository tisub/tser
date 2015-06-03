package com.busit.broker;

import com.busit.*;
import com.anotherservice.io.*;
import com.anotherservice.util.*;
import java.util.*;
import com.busit.security.*;
import com.busit.routing.*;
import java.io.*;

public class RemoteProcessorServer implements Runnable
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
	
	private Any instance;
	private String task;
	private Path path;
	private Process client;
	private StdIOIPC io;
	private String command;
	private Identity originalIdentityFrom;
	private Identity originalIdentityTo;
	private String connectorName;
	private String connectorId;
	private String instanceId;
	
	/**
	 * Creates a new instance of a task that will be handled asynchronousely. Technically speaking,
	 * this instance will be threaded and all processing will occur only at that time. Hence this
	 * constructor only stores the required input data for later on processing.
	 * @param	json	The JSON-serialized task to perform at a later point in time.
	 * @param	command	The command to spawn the remote processor client
	 * @see #run()
	 * @note See {@link com.busit.broker} for details about the JSON format required.
	 */
	public RemoteProcessorServer(String task, String command)
	{
		this.task = task;
		this.command = command;
	}
	
	public void run()
	{
		try
		{
			this.client = Runtime.getRuntime().exec(command);
			this.io = new StdIOIPC(client);
			this.startErrorHandler(client.getErrorStream());
			
			try { wrapRun(); }
			finally
			{
				this.io.close();
				this.stopErrorHandler();
				client.destroy();
			}
		}
		catch(Exception e)
		{
			Logger.warning(new Exception("[" + connectorName + "][c:" + connectorId + "][i:" + instanceId + "]", e));
		}
		finally
		{
		}
	}
	
	private void wrapRun() throws Exception
	{
		// 1) wait for the client to complete the startup phase
		if( !"started".equals(getRemote("status").<String>value()) )
			throw new IllegalStateException("Unexpected client reply at startup phase");
		
		// 2) send the connector code to the client
		sendRemote(this.getCode());
		
		// 3) wait for the client to complete the creation phase
		if( !"created".equals(getRemote("status").<String>value()) )
			throw new IllegalStateException("Unexpected client reply at creation phase");
		
		if( !this.ackTransaction() )
			return;
		
		try
		{
			path.getLast().handledTime = new java.util.Date().getTime();
			
			// 4) send the init data to the client
			sendRemote(this.getInitData());
			
			// 5) wait for the client to complete the init phase
			if( !"initialized".equals(getRemote("status").<String>value()) )
				throw new IllegalStateException("Unexpected client reply at init phase");
			
			Message m = new Message(this.task);
			this.originalIdentityFrom = (Identity) m.fromIdentity();
			this.originalIdentityTo = (Identity) m.toIdentity();
			boolean cron = isCron(m.content());
			long push_time = new java.util.Date().getTime();
			
			// 6) send the push data to the client
			sendRemote(Any.empty().pipe((cron?"cron":"input"), new UncheckedMessage(m).toString()).pipe("interfaceId", path.getLast().toInterface));
			
			// 7) wait for the client to complete the push phase
			if( !"pushed".equals(getRemote("status").<String>value()) )
				throw new IllegalStateException("Unexpected client reply at push phase");
			
			push_time = new java.util.Date().getTime() - push_time;
			
			Any links = this.getLinks(cron);
			
			for( String interfaceId : links.keySet() )
			{
				Logger.finest("Pulling interface [" + interfaceId + "] of connector [" + connectorName + "][c:" + connectorId + "][i:" + instanceId + "]");
				long pull_time = new java.util.Date().getTime();
				
				// 8) send the pull data to the client
				sendRemote(Any.empty().pipe("interfaceId", interfaceId));
				
				// 8.b) wait for the client to complete the pull operation
				Any a = getRemote("message");
				MessageList ml = new MessageList();
				if( !a.isList() )
				{
					Any tmp = Any.empty();
					tmp.add(a);
					a = tmp;
				}
				for( Any b : a )
				{
					if( !b.isNull() )
						ml.add(new UncheckedMessage(b.<String>value()));
				}
				
				this.processLink(links.get(interfaceId), interfaceId, ml, push_time, pull_time);
			}

			// 8) send the stop command to the client
			sendRemote(Any.empty().pipe("stop", true));
			
			// 8.a) wait for the client to complete the pull phase
			if( !"pulled".equals(getRemote("status").<String>value()) )
				throw new IllegalStateException("Unexpected client reply at push phase");
		
			if( path.getLast().shareTransaction != null )
				RestApi.request(confirmPaymentRestAction + path.getLast().shareTransaction, false);
			RestApi.request(confirmPaymentRestAction + path.getLast().transaction, false);
			Logger.finer("RemoteProcessor processing complete for [" + connectorName + "][c:" + connectorId + "][i:" + instanceId + "].");
		}
		catch(Exception e)
		{
			// This is a runtime error = may happen
			// @see com.busit.rest.Credit#INTERNAL_REFUND(String)
			
			if( e instanceof InterruptedException )
				Logger.info("Connector [" + connectorName + "][c:" + connectorId + "][i:" + instanceId + "] aborted processing explicitely");
			else
				Logger.info(new Exception("[" + connectorName + "][c:" + connectorId + "][i:" + instanceId + "]", e));
			
			if( path.getLast().shareTransaction != null )
			{
				try { RestApi.request(refundPaymentRestAction + path.getLast().shareTransaction, false); }
				catch(Exception ex) { Logger.severe("Could not refund share transaction " + path.getLast().shareTransaction); Logger.warning(ex); }
			}
			try { RestApi.request(refundPaymentRestAction + path.getLast().transaction, false); }
			catch(Exception ex) { Logger.severe("Could not refund transaction " + path.getLast().transaction); Logger.warning(ex); }
			return;
		}
	}
	
	private void sendRemote(Any data) throws Exception
	{
		boolean terminated = false;
		try { client.exitValue(); terminated = true; }
		catch(Exception e) { } // the client has not terminated yet
		
		if( terminated )
			throw new IllegalStateException("Subprocess terminated abnormally");
		
		io.sendAny(data);
	}
	
	private Any getRemote(String value) throws Exception
	{
		Any data = io.receiveAny();
		if( data.containsKey("abort") )
			throw new InterruptedException();
		if( !data.containsKey(value) )
			throw new Exception("Unexpected client response. Expected " + value + " but got " + data.toJson());
		return data.get(value);
	}
	
	private Any getCode() throws Exception
	{
		Any json = Json.decode(this.task);
		if( !json.containsKey("path") || !json.get("path").isList() )
			throw new IllegalArgumentException("Missing 'path' in the input task");
		if( !json.containsKey("message") || !(json.get("message").<String>value() instanceof String) )
			throw new IllegalArgumentException("Missing 'message' in the input task");
		
		// CAUTION : we replace the original task with only its message content
		this.task = json.<String>value("message");
		
		path = new Path(json.get("path"));
		connectorName = "INSTANCE ID " + path.getLast().toInstance;
		
		instance = RestApi.request(instanceRetrievalRestAction + path.getLast().toInstance, false).get(0);
		instanceId = instance.<String>value("instance_id");
		checker.instanceId = instanceId;
		connectorId = instance.<String>value("instance_connector");
		checker.connectorId = connectorId;
		Any code = RestApi.request(codeRetrievalRestAction + connectorId, true);
		
		if( !code.isNull("name") )
		{
			connectorName = code.<String>value("name");
			checker.connectorName = connectorName;
		}
		return Any.empty().pipe("code", code.<String>value("code"));
	}
	
	private boolean ackTransaction()
	{
		if( path.getLast().shareTransaction != null )
		{
			try
			{
				RestApi.request(ackPaymentRestAction + path.getLast().shareTransaction, false);
			}
			catch(Exception e)
			{
				// The transaction could not be ACKd because it is timeout or already ACK or it is an invalid transaction
				Logger.warning("Transaction ACK failure: " + path.getLast().shareTransaction);
				return false;
			}
		}
		
		try
		{
			RestApi.request(ackPaymentRestAction + path.getLast().transaction, false);
		}
		catch(Exception e)
		{
			// The transaction could not be ACKd because it is timeout or already ACK or it is an invalid transaction
			Logger.warning("Transaction ACK failure: " + path.getLast().transaction);
			return false;
		}
		
		return true;
	}
	
	private Any getInitData() throws Exception
	{
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
		config.put("__uid", SHA1.hash(path.getLast().toInstance));
		config.put("__instance", path.getLast().toInstance);
		config.put("__instance_name", path.getLast().toInstance); // TODO : get the instance name
		config.put("__locale", "en"); // TODO : get user locale
		
		return Any.empty().pipe("config", config).pipe("inputs", inputs).pipe("outputs", outputs);
	}
	
	private Any getLinks(boolean isCron)
	{
		Any links = Any.empty();
		boolean isOutput = !isInputInterface(path.getLast().toInterface);
		for( Any l : instance.get("links") )
		{
			if( l.<String>value("link_active").equals("0") )
				continue;
				
			String from = l.<String>value("interface_from");
			
			// when CRON an OUTPUT, pull only the links from that interface
			// when CRON an INPUT, pull all
			if( isCron && isOutput && !from.equals(path.getLast().toInterface) )
				continue;
			
			if( !links.containsKey(from) )
				links.put(from, new ArrayList<Map<String, String>>());
			links.get(from).add(l);
		}
		
		return links;
	}
	
	private void processLink(Any l, String interfaceId, MessageList ml, long push_time, long pull_time)
	{
		if( ml == null || ml.size() == 0 )
		{
			Logger.finest("Empty message from interface " + interfaceId + ". Ignoring.");
			return;
		}
		
		for( int i = 0; i < ml.size(); i++ )
		{
			IMessage im = ml.get(i);
			if( im == null )
			{
				Logger.finest("Empty message from interface " + interfaceId + ". Ignoring.");
				continue;
			}
			if( im instanceof UncheckedMessage )
			{
				try { im = ((UncheckedMessage)im).securize(originalIdentityFrom, originalIdentityTo); }
				catch(Exception e)
				{
					Logger.warning(e);
					continue;
				}
			}
			
			Message m = (Message) im;
			if( isCron(m.content()) )
			{
				Logger.finest("Connector cannot send CRON messages. Ignoring.");
				continue;
			}
			
			pull_time = new java.util.Date().getTime() - pull_time;
			
			for( Any link : l )
			{
				try
				{
					String message = m.toString();
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
					this.forward(message, steps, next.toString());
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
	
	private void forward(String message, String path, String destination) throws Exception
	{
		Logger.fine("Forwarding message");
		
		Hashtable<String, String> params = new Hashtable<String, String>();
		params.put("message", message);
		params.put("path", path);
		params.put("destination", destination);
		
		RestApi.request(messageForwardRestAction, params, false);
	}
	
	private boolean isCron(IContent c)
	{
		return ( c != null && c.id() == 0 && Config.gets("com.busit.rest.cron").equals(c.get("data")) );
	}
	
	private boolean isInputInterface(String interfaceId)
	{
		for( Any t : instance.get("interfaces") )
			if( t.<String>value("interface_name").equals(interfaceId) )
				return t.<String>value("interface_type").equals("1");
		
		return false;
	}
	
	// ==================================
	// ERROR HANDLER THREAD
	// ==================================
	private ErrorChecker checker;
	private void startErrorHandler(InputStream err)
	{
		if( checker != null ) return;
		checker = new ErrorChecker(err);
		checker.start();
	}
	
	private void stopErrorHandler()
	{
		checker.shutdown();
		// block current thread until the checker completes
		try { checker.join(); } catch (InterruptedException ie) {}
	}
	
	private static class ErrorChecker extends Thread
	{
		private StdIOIPC io;
		private boolean stopping = false;
		public String connectorId = null;
		public String instanceId = null;
		public String connectorName = null;
		
		public ErrorChecker(InputStream err)
		{
			io = new StdIOIPC(err, null); // one way stdioipc listenner
			this.setDaemon(true);
		}
		
		public void run()
		{
			while(!stopping && !isInterrupted())
			{
				try
				{
					Thread.sleep(10);
					//debug : System.out.println("=====" + new String(StreamReader.readAtMost(err, 1000), "UTF-8"));
					
					Any a = io.receiveAny();
					if( a.containsKey("notifyUser") )
						NotificationManager.notifyUser(instanceId, a.get("notifyUser"));
					else if( a.containsKey("notifyOwner") )
						NotificationManager.notifyOwner(connectorId, a.get("notifyOwner"));
					else if( a.containsKey("notifyBusit") )
						Logger.info("[" + connectorName + "][c:" + connectorId + "][i:" + instanceId + "]\nConnector Notification:\n" + a.get("notifyBusit").toJson());
					else if( !a.isEmpty() )
						NotificationManager.notifyOwner(connectorId, Any.empty().pipe("message", a));
				}
				catch(InterruptedException ie)
				{
					break;
				}
				catch(StreamReader.IllegalSizeException ise)
				{
					try
					{
						String message = new String(ise.data, "UTF-8") + new String(StreamReader.readAll(io.in), "UTF-8");
						NotificationManager.notifyOwner(connectorId, Any.empty().pipe("message", message));
					}
					catch(Exception ex)
					{
						// we did what we could
					}
				}
				catch(StdIOIPC.IllegalControlSequence ice)
				{
					try
					{
						String message = ice.sequence + new String(StreamReader.readAll(io.in), "UTF-8");
						NotificationManager.notifyOwner(connectorId, Any.empty().pipe("message", message));
					}
					catch(Exception ex)
					{
						// we did what we could
					}
				}
				catch(EOFException eof)
				{
					// nothing to read
				}
				catch(Exception e)
				{
					// ignore io exceptions (end of stream ?)
					Logger.finest(e);
				}
			}
		}
		
		public void shutdown()
		{
			stopping = true;
		}
	}
}