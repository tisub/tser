package com.busit.local;

import com.anotherservice.util.*;
import com.busit.*;
import java.io.*;
import java.util.*;

public class Processor
{
	private static byte[] readStream(InputStream in) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data = new byte[16384];
		int read = 0;
		while( (read = in.read(data)) != -1 )
			baos.write(data, 0, read);
		
		return baos.toByteArray();
	}
	
	private static void pipeStream(InputStream in, OutputStream out) throws IOException
	{
		byte[] data = new byte[16384];
		int read = 0;
		while( (read = in.read(data)) != -1 )
			out.write(data, 0, read);
	}
	
	public static void main(String[] args)
	{
		try
		{
			Logger.instance().stream = System.out;
			if( args.length != 1 || !new File(args[0]).exists() )
				throw new Exception("Input config file not specified or not found");
			
			Any config = Json.decode(new String(readStream(new FileInputStream(args[0])), "UTF-8"));
			
			// ========================================
			
			JarLoader context = new JarLoader(Thread.currentThread().getContextClassLoader());
			context.addFile(config.<String>value("jar"));
			
			IConnector instance = (IConnector) Class.forName(config.<String>value("class"), true, context).newInstance();
			Logger.info("Connector instance created");
		
			// ========================================
			
			Map<String, String> c = (Map<String, String>)config.get("config").unwrap();
			Map<String, Map<String, String>> i = (Map<String, Map<String, String>>)config.get("inputs").unwrap();
			Map<String, Map<String, String>> o = (Map<String, Map<String, String>>)config.get("outputs").unwrap();
			instance.init(c, i, o);
			Logger.info("Connector instance initialized with " + c.size() + " configs, " + i.size() + " inputs and " + o.size() + " outputs");
			
			// ========================================
			
			String cron = config.<String>value("cron");
			String push = config.<String>value("push");
			if( cron != null && cron.length() > 0 )
			{
				Logger.info("Cron on interface " + cron);
				instance.cron(new Message(true), cron);
			}
			else if( push != null && push.length() > 0 )
			{
				Message m = new Message(true);
				m.setContentUTF8(config.<String>value("message"));
				for( Any a : config.get("files") )
					m.addAttachment(a.<String>value(), new FileInputStream(a.<String>value()));
					
				Logger.info("Pushing on input " + push);
				instance.setInput(m, push);
			}
			
			// ========================================
			
			String pull = config.<String>value("pull");
			if( pull != null && pull.length() > 0 )
			{
				Logger.info("Pulling output " + pull);
				IMessage m = instance.getOutput(pull);
				
				if( m.countAttachments() > 0 )
				{
					Collection<String> names = m.getAttachmentNames();
					for( String n : names )
						pipeStream(m.getAttachment(n), new FileOutputStream(n));
					Logger.info("Pull result has " + m.countAttachments() + " attachments saved in the TEMP folder.");
				}
				else
					Logger.info("Pull result does not have attachments");
				
				IType type = m.getKnownType();
				if( type == null )
					Logger.info("Pull result content is : " + m.getContentUTF8());
				else
					Logger.info("Pull result is a known type : " + type.toJson());
			}
			
			// ========================================
			
			Logger.info("Processing complete");
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
	}
}