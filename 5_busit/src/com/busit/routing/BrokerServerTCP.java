package com.busit.routing;

import com.anotherservice.util.*;
import java.net.*;
import java.io.InputStream;

public class BrokerServerTCP implements IBrokerServer
{
	private int port = -1;
	private ServerSocket ss = null;
	
	public BrokerServerTCP()
	{
		this(Integer.parseInt(Config.gets("com.busit.broker.tcp.port")));
	}
	
	public BrokerServerTCP(int port)
	{
		this.port = port;
	}
	
	public void initialize(Any data)
	{
	}
	
	public void start() throws Exception
	{
		Logger.info("Start listenning on port " + this.port);
		ss = new ServerSocket( port );
	}
	
	public String receive() throws Exception
	{
		Socket s = null;
		
		try
		{
			s = ss.accept();
			Logger.finest("Accepted connection");

			InputStream is = s.getInputStream();
			StringBuilder sb = new StringBuilder(16000);
			int c;
			while( (c = is.read()) != -1 )
				sb.append((char)c);
			
			return sb.toString();
		}
		finally
		{
			try { s.close(); } catch (Exception e) { }
		}
		
		// WARNING : if we dont want to lose messages in case of failure with the broker :
		// we should "mark" a message saying we are handling it
		// when the message is successfully processed, we delete it for good
		// the mark should last X and if not deleted after a while, the message should
		// be available again to be re-processed
		// If using AMQP, think about 2 queues, move messages from (pending) to another (processing)
		// and have a background treatment on the processing queue that put back messages in the pending one
		// and the broker deletes messages from the processing queue when processed successfully
	}
	
	public void stop()
	{
		try
		{
			if( this.ss != null )
				this.ss.close();
		}
		catch(Exception e)
		{
		}
	}
}