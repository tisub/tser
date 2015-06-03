package com.busit.broker;

import com.anotherservice.io.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.security.*;
import java.util.logging.Level;
import com.busit.security.*;
import com.busit.routing.*;

public class Broker
{
	public static void main(String[] args)
	{
		try
		{
			if( args.length < 2 )
				usage();
				
			if( args[0].equals("-c") || args[0].equals("--config") )
					Config.load(args[1]);
			else
				usage();

			Logger.instance().stream = System.out;
			if( Config.gets("com.busit.broker.logLevel") != null )
				Logger.instance().level = Level.parse(Config.gets("com.busit.broker.logLevel")).intValue();
			Logger.config("Logging engine initialized to " + Config.gets("com.busit.broker.logLevel"));
			Logger.config("Default charset " + java.nio.charset.Charset.defaultCharset().name());

			RestApi.initialize(Config.gets("com.busit.broker.rest_url"), Config.gets("com.busit.broker.rest_auth"));
			
			Security.setInstance(new com.busit.broker.SecurityImpl());
			Logger.config("Security provided instanciated");
			
			IdentityStore.setInstance(new com.busit.broker.IdentityStoreImpl());
			Logger.config("Identity store instanciated");
			
			IdentityChecker.setInstance(new com.busit.broker.IdentityCheckerImpl());
			Logger.config("Identity checker instanciated");
			
			IBrokerServer broker_java = (IBrokerServer) Class.forName(Config.gets("com.busit.broker.class")).newInstance();
			IBrokerServer broker_php = (IBrokerServer) Class.forName(Config.gets("com.busit.broker.class")).newInstance();
			
			Dispatcher.initialize();
			Dispatcher d_java = new Dispatcher(broker_java, 
				Config.gets("com.busit.broker.rabbit.exchange"), 
				Config.gets("com.busit.broker.rabbit.queue_java"), 
				Config.gets("com.busit.broker.rabbit.binding_java"),
				Config.gets("com.busit.broker.command_java"));
			Dispatcher d_php = new Dispatcher(broker_php, 
				Config.gets("com.busit.broker.rabbit.exchange"), 
				Config.gets("com.busit.broker.rabbit.queue_php"), 
				Config.gets("com.busit.broker.rabbit.binding_php"),
				Config.gets("com.busit.broker.command_php"));
			
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				public void run()
				{
					Dispatcher.shutdown();
				}
			});
			
			d_java.start();
			d_php.start();
			d_java.join();
			d_php.join();
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
	}
	
	private static void usage()
	{
		String usage = "Usage: java -jar broker.jar [OPTIONS]\n" +
			"Options:\n" +
			"\t-h,\t--help\t\tShow this help message\n" +
			"\t-c,\t--config\tConfig file\n";
		System.out.println(usage);
		System.exit(0);
	}
}