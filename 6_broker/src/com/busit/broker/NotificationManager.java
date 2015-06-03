package com.busit.broker;

import com.anotherservice.util.*;
import com.rabbitmq.client.*;

public class NotificationManager
{
	private static String host = null;
	private static String vhost = null;
	private static String username = null;
	private static String password = null;
	private static int port = -1;
	private static Channel channel = null;
	static { initialize(); }
	
	private static synchronized void initialize()
	{
		host = Config.gets("com.busit.broker.rabbit.host");
		vhost = Config.gets("com.busit.broker.rabbit.vhost");
		username = Config.gets("com.busit.broker.rabbit.username");
		password = Config.gets("com.busit.broker.rabbit.password");
		port = Integer.parseInt(Config.gets("com.busit.broker.rabbit.port"));
	}
	
	private static synchronized void connect() throws Exception
	{
		ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
		factory.setVirtualHost(vhost);
		factory.setUsername(username);
		factory.setPassword(password);
		factory.setPort(port);
		
        Connection connection = factory.newConnection();
        channel = connection.createChannel();
		channel.exchangeDeclare(Config.gets("com.busit.broker.rabbit.exchangeError"), "topic", true);
	}
	
	public static void notifyOwner(String connectorId, Any a)
	{
		try
		{
			if( !a.isMap() ) a = Any.empty().pipe("message", a);
			
			if( channel == null )
				connect();
			channel.basicPublish(Config.gets("com.busit.broker.rabbit.exchangeError"), "error.developer.42", null, a.pipe("connector", connectorId).toJson().getBytes());
		}
		catch(Exception e)
		{
			try
			{
				// try to reconnect
				connect();
				channel.basicPublish(Config.gets("com.busit.broker.rabbit.exchangeError"), "error.developer.42", null, a.pipe("connector", connectorId).toJson().getBytes());
			}
			catch(Exception e2)
			{
				Logger.info("FALLBACK NOTIFY OWNER OF [" + connectorId + "] : " + a.toJson());
			}
		}
	}
	
	public static void notifyUser(String instanceId, Any a)
	{
		try
		{
			if( !a.isMap() ) a = Any.empty().pipe("message", a);
			
			if( channel == null )
				connect();
			channel.basicPublish(Config.gets("com.busit.broker.rabbit.exchangeError"), "error.user.42", null, a.pipe("instance", instanceId).toJson().getBytes());
		}
		catch(Exception e)
		{
			try
			{
				// try to reconnect
				connect();
				channel.basicPublish(Config.gets("com.busit.broker.rabbit.exchangeError"), "error.user.42", null, a.pipe("instance", instanceId).toJson().getBytes());
			}
			catch(Exception e2)
			{
				Logger.info("FALLBACK NOTIFY USER OF [" + instanceId + "] : " + a.toJson());
			}
		}
	}
}