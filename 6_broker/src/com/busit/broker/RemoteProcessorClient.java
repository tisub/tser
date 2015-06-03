package com.busit.broker;

import com.busit.*;
import com.anotherservice.io.*;
import com.anotherservice.util.*;
import java.util.*;
import com.busit.security.*;
import com.busit.routing.*;
import java.io.*;

public class RemoteProcessorClient extends ClassLoader implements Runnable
{
	public RemoteProcessorClient()
	{
		try
		{
			Logger.instance(Logger.DEFAULT, new Logger.Log(Logger.DEFAULT)
			{
				private StdIOIPC err = new StdIOIPC(null, System.err);
				public void severe(Throwable t) 			{ try { err.send(Hex.getBytes(t.toString())); } catch (Exception e) { } }
				public void severe(String message) 			{ try { err.send(Hex.getBytes(message)); } catch (Exception e) { } }
				public void warning(Throwable t) 			{ try { err.send(Hex.getBytes(t.toString())); } catch (Exception e) { } }
				public void warning(String message) 		{ try { err.send(Hex.getBytes(message)); } catch (Exception e) { } }
				public void info(Throwable t) 				{ try { err.send(Hex.getBytes(t.toString())); } catch (Exception e) { } }
				public void info(String message) 			{ try { err.send(Hex.getBytes(message)); } catch (Exception e) { } }
				public void config(Throwable t) 			{ try { err.send(Hex.getBytes(t.toString())); } catch (Exception e) { } }
				public void config(String message) 			{ try { err.send(Hex.getBytes(message)); } catch (Exception e) { } }
				public void fine(Throwable t) 				{ try { err.send(Hex.getBytes(t.toString())); } catch (Exception e) { } }
				public void fine(String message) 			{ try { err.send(Hex.getBytes(message)); } catch (Exception e) { } }
				public void finer(Throwable t) 				{ try { err.send(Hex.getBytes(t.toString())); } catch (Exception e) { } }
				public void finer(String message) 			{ try { err.send(Hex.getBytes(message)); } catch (Exception e) { } }
				public void finest(Throwable t) 			{ try { err.send(Hex.getBytes(t.toString())); } catch (Exception e) { } }
				public void finest(String message) 			{ try { err.send(Hex.getBytes(message)); } catch (Exception e) { } }
				public void log(int level, Throwable t) 	{ try { err.send(Hex.getBytes(t.toString())); } catch (Exception e) { } }
				public void log(int level, String message) 	{ try { err.send(Hex.getBytes(message)); } catch (Exception e) { } }
			});
			
			Factory.instance(new FactoryImpl());
			RestApi.initialize("http://api.broker.busit.com/busit/", "", true);
			
			run();
		}
		catch(Exception e)
		{
			Logger.warning(Any.empty().pipe("notifyBusit", e).toJson());
		}
	}
	
	private static class NotifyOwnerFilterOutputStream extends FilterOutputStream
	{
		private ByteArrayOutputStream buffer;
		
		public NotifyOwnerFilterOutputStream(ByteArrayOutputStream out)
		{
			super(out);
			buffer = out;
		}
		
		public void flush() throws IOException
		{
			Logger.info(Any.empty().pipe("notifyOwner", new String(buffer.toByteArray(), "UTF-8")).toJson());
			buffer.reset();
		}
	}
	
	public void run()
	{
		StdIOIPC io = new StdIOIPC(System.in, System.out);
		System.setIn(new ByteArrayInputStream(new byte[0])); // so that the developper does not mess with our System.in
		System.setOut(new PrintStream(new NotifyOwnerFilterOutputStream(new ByteArrayOutputStream()))); // for the developper to use System.out.println
		System.setErr(System.out); // for the developper to use System.err.println
		
		try
		{
			// 1) tell the server the startup phase is complete
			io.sendAny(Any.empty().pipe("status", "started"));

			// 2) get the connector code
			Any code = io.receiveAny();
			this.construct(code.<String>value("code"));

			// 3) tell the server the creation phase is complete
			io.sendAny(Any.empty().pipe("status", "created"));
			
			// 4) get the init data
			Any init = io.receiveAny();
			Map<String, String> config = new HashMap<String, String>();
			for( String key : init.get("config").keys() )
				config.put(key, init.get("config").<String>value(key));
			Map<String, Map<String, String>> inputs = new HashMap<String, Map<String, String>>();
			for( String key : init.get("inputs").keys() )
			{
				Map<String, String> i = new HashMap<String, String>();
				for( String key2 : init.get("inputs").get(key).keys() )
					i.put(key2, init.get("inputs").get(key).<String>value(key2));
				inputs.put(key, i);
			}
			Map<String, Map<String, String>> outputs = new HashMap<String, Map<String, String>>();
			for( String key : init.get("outputs").keys() )
			{
				Map<String, String> o = new HashMap<String, String>();
				for( String key2 : init.get("outputs").get(key).keys() )
					o.put(key2, init.get("outputs").get(key).<String>value(key2));
				outputs.put(key, o);
			}
			this.connector.init(config, inputs, outputs);

			// 5) tell the server the init phase is complete
			io.sendAny(Any.empty().pipe("status", "initialized"));
			
			// 6) get the message to push
			IMessage sample = null;
			Any push = io.receiveAny();
			if( push.containsKey("cron") )
			{
				IMessage m_cron = new UncheckedMessage(push.<String>value("cron"));
				Factory.instance().template(m_cron);
				this.connector.cron(m_cron, push.<String>value("interfaceId"));
			}
			else if( push.containsKey("input") )
			{
				IMessage m_input = new UncheckedMessage(push.<String>value("input"));
				Factory.instance().template(m_input);
				this.connector.setInput(m_input, push.<String>value("interfaceId"));
			}
			else if( push.containsKey("sample") )
			{
				sample = new UncheckedMessage(push.<String>value("sample")); // sample data -> there is nothing to push, just save the message
				Factory.instance().template(sample);
			}
			else
				throw new IllegalStateException("Unexpected push mode");
			
			// 7) tell the server the push phase is complete
			io.sendAny(Any.empty().pipe("status", "pushed"));
			
			// start loop for pulls until the server tell us to stop
			do
			{
				// 8) get the server orders
				Any pull = io.receiveAny();
				if( pull.containsKey("stop") )
				{
					// 8.a) tell the server the pull phase is complete
					io.sendAny(Any.empty().pipe("status", "pulled"));
					break;
				}
				else
				{
					IMessage m = null;
					if( sample != null )
						m = this.connector.getSampleData(sample, pull.<String>value("interfaceId"));
					else
						m = this.connector.getOutput(pull.<String>value("interfaceId"));
					
					// 8.b) give the pull result to the server
					io.sendAny(Any.empty().pipe("message", m));
				}
			}
			while(true);
		}
		catch(Exception e)
		{
			Logger.severe(Any.empty().pipe("notifyOwner", e).toJson());
			// 9) tell the server that we stop here
			try { io.sendAny(Any.empty().pipe("abort", true)); } catch(Exception ex) {}
		}
		finally
		{
			// 10) cleanup everything
			io.close();
			Logger.instance().stream.close();
		}
	}
	
	private IConnector connector = null;
	
	public void construct(String code) throws Exception
	{
		Zip source = new Zip(new Base64InputStream(code, true));
		ClassLoader c = new Loader(source);
		Context.change(c);
		
		Config.ConfigImpl conf = new Config.ConfigImpl();
		conf.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.xml"));
		String className = conf.gets("className");
		
		if( className == null || className.length() == 0 )
			throw new IllegalArgumentException("Invalid or missing 'className' in connector configuration");
		
		this.connector = (IConnector) Class.forName(className, true, Thread.currentThread().getContextClassLoader()).newInstance();
	}
	
}