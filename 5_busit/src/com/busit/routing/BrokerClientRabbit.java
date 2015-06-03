package com.busit.routing;

import java.util.*;
import java.net.Socket;
import java.io.OutputStream;
import com.anotherservice.util.*;
import com.rabbitmq.client.*;
import com.busit.security.*;

public class BrokerClientRabbit implements IBrokerClient
{
	private String host = null;
	private String vhost = null;
	private String username = null;
	private String password = null;
	private int port = -1;
	
	public BrokerClientRabbit()
	{
		this(Config.gets("com.busit.broker.rabbit.host"),
			Config.gets("com.busit.broker.rabbit.vhost"), 
			Config.gets("com.busit.broker.rabbit.username"), 
			Config.gets("com.busit.broker.rabbit.password"), 
			Integer.parseInt(Config.gets("com.busit.broker.rabbit.port")));
	}
	
	public BrokerClientRabbit(String host, String vhost, String username, String password, int port)
	{
		this.host = host;
		this.vhost = vhost;
		this.username = username;
		this.password = password;
		this.port = port;
	}
	
	public void transmit(String message, String user, String language, String local, String sla) throws Exception
	{
		ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.host);
		factory.setVirtualHost(this.vhost);
		factory.setUsername(this.username);
		factory.setPassword(this.password);
		factory.setPort(this.port);
		
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

		if( local.equals("1") )
		{
			channel.exchangeDeclare("busit." + user, "topic", true);
			channel.basicPublish("busit." + user, sla + "." + language + ".42", null, message.getBytes());
		}
		else
		{
			channel.exchangeDeclare(Config.gets("com.busit.broker.rabbit.exchange"), "topic", true);			
			channel.basicPublish(Config.gets("com.busit.broker.rabbit.exchange"), sla + "." + language + ".42", null, message.getBytes());
		}

        connection.close();
	}
	
	public void trace(String user, Path path, IIdentity from, IIdentity to, String language, boolean cron) throws Exception
	{
		ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.host);
		factory.setVirtualHost(this.vhost);
		factory.setUsername(this.username);
		factory.setPassword(this.password);
		factory.setPort(this.port);
		
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(Config.gets("com.busit.broker.rabbit.exchange"), "topic", true);
		
		double vat = Double.parseDouble(Config.gets("com.busit.rest.vat.percent"));
		
		Map<String, Object> transit = new HashMap<String, Object>();
		transit.put("time", path.getLast().sentTime);
		transit.put("id", path.getLast().identifier);
		transit.put("from", path.getLast().fromInstance);
		transit.put("from_interface", path.getLast().fromInterface);
		transit.put("to", path.getLast().toInstance);
		transit.put("to_interface", path.getLast().toInterface);
		transit.put("language", language);
		transit.put("tax", (int)((double)path.getLast().costTax + (double)path.getLast().costCount + (double)path.getLast().costSize + (double)path.getLast().costQos + Math.ceil(vat / 100.0 * (double)(path.getLast().costPrice + path.getLast().costShare))));
		transit.put("user_cost", (int)((double)path.getLast().costTax + (double)path.getLast().costCount + (double)path.getLast().costSize + (double)path.getLast().costQos + Math.ceil((double)(path.getLast().costPrice + path.getLast().costShare) + vat / 100.0 * (double)(path.getLast().costPrice + path.getLast().costShare))));
		transit.put("cost_count", (double)path.getLast().costCount);
		transit.put("cost_qos", (double)path.getLast().costQos);
		transit.put("cost_size", (double)path.getLast().costSize);
		transit.put("cost_price", (double)path.getLast().costPrice);
		transit.put("cost_tax", (double)path.getLast().costTax);
		transit.put("sender", from.getSubjectPrincipal().getName());
		transit.put("receiver", to.getSubjectPrincipal().getName());
		transit.put("size", path.getLast().inputSize);
		transit.put("user", user);
		transit.put("cron", cron);
		
        channel.basicPublish(Config.gets("com.busit.broker.rabbit.exchange"), "transit." + user, null, Json.encode(transit).getBytes());

        connection.close();
	}
	
	public void sendMessage(String exchange, String routingKey)
	{
		
	}
}