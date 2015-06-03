package com.busit.broker;

import com.anotherservice.util.*;
import java.util.concurrent.*;
import java.io.*;
import com.busit.routing.*;
import java.util.concurrent.*;

public class Dispatcher extends Thread
{
	private static ThreadPoolExecutor pool;
	private IBrokerServer broker = null;
	private String exchange;
	private String queue;
	private String binding;
	private String command;
	
	public static void initialize()
	{
		int min = Integer.parseInt(Config.gets("com.busit.broker.min"));
		int max = Integer.parseInt(Config.gets("com.busit.broker.max"));
		int timeout = Integer.parseInt(Config.gets("com.busit.broker.timeout"));
		int queue = Integer.parseInt(Config.gets("com.busit.broker.queue"));
		
		pool = new ThreadPoolExecutor(min, max, timeout, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(queue), new ThreadPoolExecutor.CallerRunsPolicy());
		
		Logger.config("Initialized Dispatcher pool parameters : \n\tmin: " + min + "\n\tmax: " + max + "\n\ttimeout: " + timeout + "\n\tqueue: " + queue);
	}
	
	public Dispatcher(IBrokerServer broker, String exchange, String queue, String binding, String command)
	{
		this.broker = broker;
		this.exchange = exchange;
		this.queue = queue;
		this.binding = binding;
		this.command = command;
	}
	
	public void run()
	{
		try
		{
			broker.initialize(Any.empty().pipe("exchange", exchange).pipe("queue", queue).pipe("binding", binding));
			broker.start();
			
			while( !shuttingDown )
			{
				try
				{
					pool.execute(new RemoteProcessorServer(broker.receive(), command));
				}
				catch(com.rabbitmq.client.ShutdownSignalException sse)
				{
					// broker lost connection, so reconnect now
					broker.start();
				}
				catch(Exception e)
				{
					Logger.severe(e);
				}
			}
			
			broker.stop();
			
			// completing all pending jobs
			Logger.info("Shutting down...");
			while( !pool.isTerminated() )
				Thread.sleep(10);
			Logger.info("Shut down complete");
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
	}
	
	private static boolean shuttingDown = false;
	public static void shutdown()
	{
		Logger.info("Shut down requested...");
		shuttingDown = true;
		pool.shutdown();
	}
}