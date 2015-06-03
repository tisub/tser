import com.anotherservice.util.*;
import com.anotherservice.io.*;
import com.busit.broker.*;
import java.util.concurrent.*;
import java.util.*;
import java.lang.*;
import java.util.logging.Level;

public class cron
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
			RestApi.initialize(Config.gets("com.busit.broker.rest_url"), Config.gets("com.busit.broker.rest_auth"));
			
			int min = Integer.parseInt(Config.gets("com.busit.broker.min"));
			int max = Integer.parseInt(Config.gets("com.busit.broker.max"));
			int timeout = Integer.parseInt(Config.gets("com.busit.broker.timeout"));
			int queue = Integer.parseInt(Config.gets("com.busit.broker.queue"));
			ThreadPoolExecutor pool = new ThreadPoolExecutor(min, max, timeout, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(queue), new ThreadPoolExecutor.CallerRunsPolicy());
		
			Logger.info("Cron starting");
			Calendar now = Calendar. getInstance();
			
			String year = "" + now.get(Calendar.YEAR);
			String month = "" + now.get(Calendar.MONTH) + 1;
			String day = "" + now.get(Calendar.DATE);
			String hour = "" + now.get(Calendar.HOUR_OF_DAY);
			String minute = "" + now.get(Calendar.MINUTE);
			
			// 1) clean credits
			RestApi.request("credits/clean");
			
			// 2) get all interfaces for cron
			Hashtable<String, String> params = new Hashtable<String, String>();
			params.put("linked", "1");
			params.put("_ts", new Date().getTime() + "");
			Any interfaces = RestApi.request("instance/interface/cron/select", params);
			
			// 3) process all
			int size = interfaces.size();
			int pooled = 0;
			for( int i = 0; i < size; i++ )
			{
				String t = interfaces.get(i).<String>value("interface_cron_timer");
				if( t == null || t.length() == 0 )
					continue;
				String[] timer = t.split("-");
				if( (timer[0].equals(year) || timer[0].equals("E")) && 
					(timer[1].equals(month) || timer[1].equals("E")) && 
					(timer[2].equals(day) || timer[2].equals("E")) && 
					(timer[3].equals(hour) || timer[3].equals("E")) && 
					(timer[4].equals(minute) || timer[4].equals("E")) )
				{
					pooled++;
					pool.execute(new cron.Task(interfaces.get(i)));
				}
			}
			
			pool.shutdown();
			while( !pool.awaitTermination(1000, TimeUnit.MILLISECONDS) )
				Logger.fine("Waiting for all cron to be trigered");
			
			Logger.info("Cron done " + pooled + "/" + size + " - " + (new Date().getTime() - now.getTimeInMillis()) + "ms");
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
	}
	
	static class Task extends Thread
	{
		Any i = null;
		public Task(Any i)
		{
			this.i = i;
		}
		
		public void run()
		{
			try
			{
				Hashtable<String, String> params = new Hashtable<String, String>();
				params.put("from", i.<String>value("interface_cron_identity"));
                params.put("destination", i.<String>value("user_id") + "+" + i.<String>value("instance_id") + "+" + i.<String>value("interface_name"));
                
				Logger.config("Cron FROM identity " + params.get("from") + " TO " + params.get("destination"));

                RestApi.request("message/cron", params);
			}
			catch(Exception e)
			{
				Logger.severe(e);
				Logger.severe(i.toJson());
			}
		}
	}
	
	private static void usage()
	{
		String usage = "Usage: java -jar cron.jar [OPTIONS]\n" +
			"Options:\n" +
			"\t-h,\t--help\t\tShow this help message\n" +
			"\t-c,\t--config\tConfig file\n";
		System.out.println(usage);
		System.exit(0);
	}
}