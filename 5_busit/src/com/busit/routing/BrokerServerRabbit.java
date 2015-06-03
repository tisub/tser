package com.busit.routing;

import com.anotherservice.util.*;
import java.net.*;
import java.io.InputStream;
import com.rabbitmq.client.*;

public class BrokerServerRabbit implements IBrokerServer
{
	private String host = null;
	private String vhost = null;
	private String username = null;
	private String password = null;
	private int port = -1;
	private String queueName = null;
	
	private QueueingConsumer consumer;
	private Connection c;
	private Channel ch;
	
	public BrokerServerRabbit()
	{
		this(Config.gets("com.busit.broker.rabbit.host"),
			Config.gets("com.busit.broker.rabbit.vhost"), 
			Config.gets("com.busit.broker.rabbit.username"), 
			Config.gets("com.busit.broker.rabbit.password"), 
			Integer.parseInt(Config.gets("com.busit.broker.rabbit.port")));
	}
	
	public BrokerServerRabbit(String host, String vhost, String username, String password, int port)
	{
		this.host = host;
		this.vhost = vhost;
		this.username = username;
		this.password = password;
		this.port = port;
	}
	
	private String exchange;
	private String queue;
	private String binding;
	public void initialize(Any data)
	{
		exchange = data.<String>value("exchange");
		queue = data.<String>value("queue");
		binding = data.<String>value("binding");
	}
	
	public void start() throws Exception
	{
		Logger.info("Start consuming the queue " + exchange + "/" + queue);
		
		ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.host);
		factory.setVirtualHost(this.vhost);
		factory.setUsername(this.username);
		factory.setPassword(this.password);
		factory.setPort(this.port);
		
        c = factory.newConnection();
        ch = c.createChannel();

		ch.exchangeDeclare(exchange, "topic", true);
		
		ch.queueDeclare(queue, true, false, false, null);
		ch.queueBind(queue, exchange, binding);
	
        consumer = new QueueingConsumer(ch);
        ch.basicConsume(queue, false, consumer);
	}
	
	public String receive() throws Exception
	{
		try
		{
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            String routingKey = delivery.getEnvelope().getRoutingKey();
			//Logger.finest("Received new message with key '" + routingKey + "'");
			
			// temporary, we ACK here :)
			ch.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			
			return message;
		}
		finally
		{
		}
		
		// WARNING : if we dont want to lose messages in case of failure with the broker :
		// we should "mark" a message saying we are handling it
		// when the message is successfully processed, we delete it for good
		// the mark should last X and if not deleted after a while, the message should
		// be available again to be re-processed
		// If using AMQP, think about 2 queues, move messages from (pending) to another (processing)
		// and have a background treatment on the processing queue that put back messages in the pending one
		// and the broker deletes messages from the processing queue when processed successfully
		
		// FINAL DECISION : In order to make sure a message is never lost, 
		// RabbitMQ supports message acknowledgments. An ack(nowledgement) is sent back from the consumer to tell 
		// RabbitMQ that a particular message had been received, processed and that RabbitMQ is free to delete it.
		// If consumer dies without sending an ack, RabbitMQ will understand that a message wasn't processed fully 
		// and will redeliver it to another consumer. 
		// There aren't any message timeouts; RabbitMQ will redeliver the message only when the worker connection dies. 
		// It's fine even if processing a message takes a very, very long time.
		// 
		// APPLICATION HERE : We DO NOT send ACK in this function receive(). It MUST be sent after (or the message will
		// redeliver forever).
	}
	
	public void stop()
	{
		try
		{
			if( this.c != null )
				this.c.close();
		}
		catch(Exception e)
		{
		}
	}
}