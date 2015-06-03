package com.busit.ela;

import com.anotherservice.util.*;
import com.anotherservice.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.lang.*;
import java.io.*;

public class Listenner
{
	public static int port = 10001;
	public static String ip = "0.0.0.0";
	
	public static int maxPendingAccept = 10;
	public static int maxConnectionsHandle = 10;
	public static int maxConnectionsListen = 100;
	
	static ThreadPoolExecutor pool = null;
	private static ThreadPoolExecutor reader = null;
	
	public Listenner()
	{
	}
	
	public static void start()
	{
		try
		{
			initialize();
			
			ServerSocket ss = new ServerSocket(port, maxPendingAccept, InetAddress.getByName(ip));
			Logger.info("ELA listenner started.");
			
			// if we want to check some variable while listenning
			// then we should set a timeout which will throw an exception
			// and allow us to check the variable on next loop
			//ss.setSoTimeout(100);
			
			Socket s = null;
			while(true)
			{
				try
				{
					s = ss.accept();
					pool.execute(new ReaderTask(s));
				}
				catch(SocketTimeoutException  ste) { /* accept timeout */ }
				catch(RejectedExecutionException ree)
				{
					if( s != null )
					{
						try { s.close(); } catch(Exception e) { }
					}
				}
			}
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
	}
	
	private static void initialize()
	{
		pool = new ThreadPoolExecutor(
			maxConnectionsHandle / 2, 
			maxConnectionsHandle, 
			30000, TimeUnit.MILLISECONDS, 
			new ArrayBlockingQueue<Runnable>(maxPendingAccept), 
			new ThreadPoolExecutor.CallerRunsPolicy());
		reader = new ThreadPoolExecutor(
			1, 
			maxConnectionsListen, 
			10000, TimeUnit.MILLISECONDS, 
			new ArrayBlockingQueue<Runnable>(1), 
			new ThreadPoolExecutor.AbortPolicy());
	}
	
	static class ReaderTask extends Thread
	{
		private Socket s = null;
		private InputStream is = null;
		public ReaderTask(Socket s) { this.s = s; }
		public void run()
		{
			try
			{
				is = s.getInputStream();
				StringBuffer buffer = new StringBuffer(14);
				int i = 0;
				char ch = ' ';
				for( int c = is.read(); c != -1; c = is.read() )
				{
					ch = (char)(c & 0xFF);
					buffer.append(ch);
					if( i > 0 && ((ch == ']' && buffer.charAt(0) == '[') || (ch == '[' && buffer.charAt(0) == ']')) )
					{
						//process
						i = 0;
						try { Listenner.pool.execute(new SendTask(buffer.toString(), ((InetSocketAddress)s.getRemoteSocketAddress()).getAddress().toString())); } 
						catch(Exception e) { Logger.warning(e); }
						buffer = new StringBuffer(14);
					}
					i++;
				}
			}
			catch(Exception e)
			{
				Logger.severe(e);
			}
			finally
			{
				if( is != null )
				{
					try { is.close(); } catch(Exception e) { }
				}
				if( s != null )
				{
					try { s.close(); } catch(Exception e) { }
				}
			}
		}
	}
	
	static class SendTask extends Thread
	{
		private String data;
		private String ip;
		public SendTask(String data, String ip)
		{
			if(data == null || data.length() < 10) throw new IllegalArgumentException("Incomplete frame");
			if(ip == null || ip.length() < 7) throw new IllegalArgumentException("Incomplete ip");
			
			this.data = data;
			this.ip = ip.substring(1, ip.length()); // remove leading "/" wtf?
			Logger.info("Received tag frame " + this.data + " from " + this.ip);
		}
		public void run()
		{
			try
			{
				// extract ID from data
				String tag = data.substring(3, data.length()-3);
				
				// if 24bit or 32bit ID the first 4 bits are alarm info
				//if( tag.length() > 4 )
				//	tag = tag.substring(1, tag.length());
				
				String connector = "";
				switch(tag.charAt(0))
				{
					case '0': connector = "341"; break; // Identifier
					case '8': connector = "333"; tag = tag.substring(0, tag.length()-3); break; // temperature
					case '9': connector = "334"; tag = tag.substring(0, tag.length()-3); break; // humidity
					case 'A': connector = "339"; tag = tag.substring(0, tag.length()-3); break; // analogic
					case 'B': connector = "335"; tag = tag.substring(0, tag.length()-3); break; // motion
					case 'C': connector = "338"; tag = tag.substring(0, tag.length()-3); break; // magnet
					case 'D': connector = "340"; tag = tag.substring(0, tag.length()-3); break; // numeric
					default:
						throw new Exception("Unknown tag type " + tag.charAt(0));
				}
				
				String uuid = ip + ":" + tag;
				Hashtable<String, String> params = new Hashtable<String, String>();
				params.put("connector", connector);
				params.put("uuid", uuid);
				params.put("lang", "en");
				Any instances = RestApi.request("instance/search", params);
				
				for( Any i : instances )
				{
					params.clear();
					params.put("user", i.<String>value("instance_user"));
					params.put("instance", i.<String>value("instance_id") + "+push");
					params.put("message", data);
					RestApi.request("message/send", params);
				}
			}
			catch(Exception e)
			{
				Logger.severe(e);
			}
		}
	}
}