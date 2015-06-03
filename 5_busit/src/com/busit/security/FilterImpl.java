package com.busit.security;

import com.anotherservice.rest.security.*;
import com.anotherservice.rest.core.*;
import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;
import java.io.PrintStream;
import java.io.FileOutputStream;
import com.rabbitmq.client.*;

import com.mongodb.*;

public class FilterImpl implements IFilter
{
	private static List<String> NO_LOG_PARAMS = new LinkedList<String>();
	static 
	{
		NO_LOG_PARAMS.add("(?i)^(/self)?/busit/messages?/.+$");
		NO_LOG_PARAMS.add("(?i)^(/self)?/busit/instances?/interfaces?/(publics?/|shared/)?(update|modify|change).*$");
		NO_LOG_PARAMS.add("(?i)^(/self)?/busit/instances?/configs?/(update|modify|change).*$");
		NO_LOG_PARAMS.add("(?i)^(/self)?/system/token/(select|list|search|find|view).*$");
	}
	
	private static ThreadLocal<Long> _time = new ThreadLocal<Long>();
	
	public void preFilter() throws Exception
	{
		// TODO : check for banned IP or users ?
		// Request.getRemoteIp();
		
		_time.set(new Long(System.nanoTime()));
	}
	
	private boolean initialized = false;
	private String level = null;
	private String ignore = null;
	private String mode = null;
	public Object postFilter(Object result) throws Exception
	{
		// we need a backup because we clear it so that it is not logged (see finally)
		String format = Request.getParam(Config.gets("com.anotherservice.rest.core.formatParameter"));
		
		try
		{
			if( !initialized )
			{
				level = Config.gets("com.busit.rest.filterParams.level");
				ignore = Config.gets("com.busit.rest.filterParams.ignore");
				mode = Config.gets("com.busit.rest.filterParams.destination");
				initialized = true;
			}
			
			if( level == null || level.length() == 0 || level.equals("none") || (level.equals("error") && !(result instanceof Exception)) )
				return result;
				
			String user = Security.getInstance().getUser();
			if( user != null && ignore != null && ignore.length() > 0 && ignore.matches("(^|,|\\s|;)" + Pattern.quote(user) + "(;|\\s|,|$)") )
				return result;
			
			String path = Request.getCurrentPath();
			
			Any log = Any.empty();
			log.put("user", Security.getInstance().userId());
			log.put("username", Security.getInstance().getUser());
			log.put("date", new Date().getTime());
			log.put("path", path);
			log.put("ip", Request.getRemoteIp());
			log.put("error", (result instanceof Exception ? ((Exception)result).getMessage() : null));
			log.put("attachments", Request.getAttachmentNames());
			log.put("time", (System.nanoTime() - _time.get()) / 1000000); // ns -> ms
			
			String forget = Config.gets("com.anotherservice.rest.model.authParameter") + "," 
				+ Config.gets("com.anotherservice.rest.core.formatParameter") 
				+ ",nohttperrorheader,pass,password,user_password,user_pass";
			Request.clearParam(forget);
			
			Map<String, String> params = Request.getParams();
			for( String s : NO_LOG_PARAMS )
				if( path.matches(s) )
					params.clear();
			log.put("parameters", params);
			
			if( mode != null && mode.equals("file") )
				logFile(log);
			else if( mode != null && mode.equals("rabbit") )
				logRabbit(log);
		}
		catch(Exception e)
		{
			Logger.warning(e);
		}
		finally
		{
			if( !Request.hasParam(Config.gets("com.anotherservice.rest.core.formatParameter")) )
				Request.addParam(Config.gets("com.anotherservice.rest.core.formatParameter"), format);
			_time.remove();
		}
		
		return result;
	}
	
	private static Logger.Log logger = null;
	private void logFile(Any a) throws Exception
	{
		// CAUTION : we are committed to NOT log :
		// - user password (login)
		// - sent messages
		
		if( logger == null )
		{
			logger = Logger.instance("com.busit.filter");
			logger.level = 800;
			logger.logDate = false;
			logger.logClass = false;
			logger.logMethod = false;
			logger.logLevel = false;
			logger.stream = new PrintStream(new FileOutputStream(Config.gets("com.busit.rest.filterParams.file"), true));
			logger.async(true);
		}
		
		logger.info(Json.encode(a));
	}
	
	private static Channel channel = null;
	private static void reconnectRabbit() throws Exception
	{
		ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(Config.gets("com.busit.broker.rabbit.host"));
		factory.setVirtualHost(Config.gets("com.busit.broker.rabbit.vhost"));
		factory.setUsername(Config.gets("com.busit.broker.rabbit.username"));
		factory.setPassword(Config.gets("com.busit.broker.rabbit.password"));
		factory.setPort(Integer.parseInt(Config.gets("com.busit.broker.rabbit.port")));
		
        Connection connection = factory.newConnection();
        channel = connection.createChannel();
		channel.exchangeDeclare(Config.gets("com.busit.broker.rabbit.exchangeLog"), "topic", true);
	}
	private void logRabbit(Any a) throws Exception
	{
		try
		{
			if( channel == null )
				reconnectRabbit();
			channel.basicPublish(Config.gets("com.busit.broker.rabbit.exchangeLog"), "log.api.42", null, a.toJson().getBytes());
		}
		catch(Exception e)
		{
			try
			{
				// try to reconnect
				reconnectRabbit();
				channel.basicPublish(Config.gets("com.busit.broker.rabbit.exchangeLog"), "log.api.42", null, a.toJson().getBytes());
			}
			catch(Exception e2)
			{
				// fallback on file log
				logFile(a);
			}
		}
	}
}