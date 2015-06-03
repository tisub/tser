package com.olympe;

import java.util.regex.*;
import java.util.*;
import java.lang.*;
import java.io.File;
import java.io.PrintStream;
import com.anotherservice.util.*;
import com.anotherservice.io.*;
import com.anotherservice.db.*;
import java.net.*;

public class OlympeFilter extends Tail
{
	
	private long maxFileSize = 1048576L;
	private Pattern pattern = null;
	private String userRoot = "/dns/in/olympe/";
	private int outputMode = 0;
	
	public OlympeFilter(String inputMode) throws Exception
	{
		super(Config.gets("com.olympe.ftpfilter.input." + inputMode + ".logfile"));
		Logger.config("Input log file : " + Config.gets("com.olympe.ftpfilter.input." + inputMode + ".logfile"));
		this.pattern = Pattern.compile(Config.gets("com.olympe.ftpfilter.input." + inputMode + ".format"));
		Logger.config("Input matching pattern : " + this.pattern);
		this.maxFileSize = Long.parseLong(Config.gets("com.olympe.ftpfilter.input.skip_files_larger_than"));
		Logger.config("Maximum file size : " + this.maxFileSize);
		this.userRoot = Config.gets("com.olympe.ftpfilter.input.user_root_dir");
		Logger.config("User root dir : " + this.userRoot);
		this.failIfNotExist = "true".equals(Config.gets("com.olympe.ftpfilter.input.stop_on_log_rotate"));
		Logger.config("Fail if file rotates : " + this.failIfNotExist);
		this.fromStart = "true".equals(Config.gets("com.olympe.ftpfilter.input.start_from_beginning"));
		Logger.config("Read log from start : " + this.fromStart);
		
		Logger.Log l = Logger.instance("in.olympe");
		l.logDate = false;
		l.logClass = false;
		l.logMethod = false;
		l.logLevel = false;
		l.level = 800;
		l.stream = new PrintStream(new File(Config.gets("com.olympe.ftpfilter.output.file")));
		l.async(true);
		
		Mysql.init(Config.gets("com.olympe.ftpfilter.output.mysql.host"),
			Config.gets("com.olympe.ftpfilter.output.mysql.port"),
			Config.gets("com.olympe.ftpfilter.output.mysql.db"),
			Config.gets("com.olympe.ftpfilter.output.mysql.user"),
			Config.gets("com.olympe.ftpfilter.output.mysql.pass"));
		
		Logger.finer("Starting tail");
		this.startTail();
		Logger.finer("Joining thread");
		this.join();
	}
	
	protected void data(String line)
	{
		try
		{
			Logger.finer("Processing data");
			Logger.finest(line);
			
			Matcher m = this.pattern.matcher(line);
			if( m.find() )
			{
				String file = userRoot + m.group("file");
				String user = m.group("user");
				String ip = m.group("ip");
				
				Logger.finer("Data filtering for user " + user + " and file " + file);
				
				File f = new File(file);
				if( !f.canRead() || f.length() > maxFileSize )
				{
					Logger.finer("Ignoring file (too big or not readeable)");
					return;
				}
				
				boolean matchedAny = false;
				String filter = checkFilter(file, "com.olympe.ftpfilter.filters.banned_filename_filter");
				if( filter != null )
				{
					detection(file, user, filter, ip, true, true);
					return;
				}
				
				filter = checkFilter(file, "com.olympe.ftpfilter.filters.suspect_filename_filter");
				if( filter != null )
				{
					detection(file, user, filter, ip, true, false);
					matchedAny = true;
				}
				
				String data = getFileContent(file);
				if( data == null || data.length() == 0 )
					return;
				
				filter = checkFilter(data, "com.olympe.ftpfilter.filters.banned_content_filter");
				if( filter != null )
				{
					detection(file, user, filter, ip, false, true);
					return;
				}
				
				filter = checkFilter(data, "com.olympe.ftpfilter.filters.suspect_content_filter");
				if( filter != null )
				{
					detection(file, user, filter, ip, false, false);
					matchedAny = true;
				}
				
				if( !matchedAny )
				{
					Logger.instance("in.olympe").warning(Any.empty()
						.pipe("user", user)
						.pipe("ip", ip)
						.pipe("file", file)
						.pipe("filter", null)
						.pipe("date", new Date().getTime())
						.toJson());
				}
			}
		}
		catch(Exception e)
		{
			error(e);
		}
	}
	
	protected void error(Exception e)
	{
		Logger.warning(e);
	}
	
	private String getFileContent(String file)
	{
		String content = "";
		try { content = FileReader.readFile(file); }
		catch(Exception e) {}
		
		// decode base64 encoded content and skip BOM
		String UTF_HEADERS = "\\xEF\\xBB\\xBF|\\xFE\\xFF|\\xFF\\xFE|\\x00\\x00\\xFE\\xFF|\\xFF\\xFE\\x00\\x00|\\x0E\\xFE\\xFF|\\x2B\\x2F\\x76|\\xDD\\x73\\x66\\x73|\\xFB\\xEE\\x28";
		Matcher m = Pattern.compile("(?:^(?:" + UTF_HEADERS + ")?|'|\")([a-zA-Z0-9\\+\\n\\r=\\/\\x00]{4,})(?:$|'|\")").matcher(content);
		while( m.find() )
		{
			try { content += new String(Base64.decode(m.group(1))); } 
			catch(Exception e) { Logger.finest(e); }
		}
		
		return content;
	}
	
	private String checkFilter(String content, String config)
	{
		Object o = Config.get(config);
		if( o == null )
			return null;
		if( o instanceof Map )
		{
			ArrayList<Object> t = new ArrayList<Object>();
			t.add(o);
			o = t;
		}
		if( o instanceof Collection )
		{
			for( Object filter : (Collection)o )
			{
				if( !(filter instanceof Map) )
					continue;
				
				String regex = "" + ((Map<String, Object>)filter).get("match");
				regex = regex.replaceAll("\\r|\\n|\\t", "");
				
				if( Pattern.compile("(" + regex + ")", Pattern.CASE_INSENSITIVE).matcher(content).find() )
					return "" + ((Map<String, Object>)filter).get("name");
			}
		}
		return null;
	}
	
	private void detection(String file, String user, String filter, String ip, boolean isFileName, boolean isBanned)
	{
		try
		{
			Logger.instance("in.olympe").severe(Any.empty()
				.pipe("user", user)
				.pipe("ip", ip)
				.pipe("file", file)
				.pipe("filter", filter)
				.pipe("date", new Date().getTime())
				.toJson());
			Mysql.getInstance().insert(Config.gets("com.olympe.ftpfilter.output.mysql.sql").
				replaceAll("{USER}", user.replaceAll("'", "\\'")).
				replaceAll("{FILTER}", filter.replaceAll("'", "\\'")).
				replaceAll("{FILE}", file.replaceAll("'", "\\'")).
				replaceAll("{IP}", ip.replaceAll("'", "\\'")).
				replaceAll("{DATE}", new Date().getTime() + ""));
		}
		catch(Exception e)
		{
			Logger.warning(e);
			Logger.info("Filter [" + filter + "] match for user [" + user + "] with file [" + file + "] from ip [" + ip + "]");
		}
	}
}