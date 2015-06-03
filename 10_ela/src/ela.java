import com.anotherservice.util.*;
import com.anotherservice.io.*;
import com.busit.ela.*;
import java.lang.*;
import java.util.*;
import java.io.*;

public class ela
{
	private static void usage()
	{
		String usage = "\nBusit ELA Proxy\n" + 
			"Usage: java [-Xmx2g] -jar file.jar [OPTIONS]\n\n" +
			"Options: (* = mandatory)\n\n" +
			"*\t-a,\t--auth\t\tBusit API auth\n" +
			"\t-h,\t--help\t\tShow this help message\n" +
			"\t-ip, \t--bind-ip\tIP Address to bind to (default: 0.0.0.0)\n" + 
			"\t-l,\t--log-file\tLog file name (default: stdout)\n" +
			"\t-mh,\t--max-handle\tMax handled connections (default: 10)\n" +
			"\t-ml,\t--max-listen\tMax listenned connections (default 100)\n" +
			"\t-mq,\t--max-queue\tMax pending connections (default: 10)\n" +
			"\t-p,\t--port\tPort number (default: 10001)\n" +
			"*\t-u,\t--url\tThe busit API url\n" + 
			"\t-v,\t--verbose\t(1|0) Log more than warnings (default: 0)\n";
		System.out.println(usage);
	}
	
	public static void main(String[] args)
	{
		try
		{
			Logger.instance().level = 900; // Level.WARNING
			Logger.instance().stream = System.out;
			Logger.instance().async(true);
			
			String url = null;
			String auth = null;
			
			if( args.length % 2 != 0 )
			{
				usage();
				return;
			}
			
			for( int i = 0; i < args.length; i += 2 )
			{
				if( args[i].equals("-h") || args[i].equals("--help") )
				{
					usage();
					return;
				}
				else if( args[i].equals("-ip") || args[i].equals("--bind-ip") )
					Listenner.ip = args[i+1];
				else if( args[i].equals("-l") || args[i].equals("--log-file") )
					Logger.instance().stream = new PrintStream(args[i+1]);
				else if( args[i].equals("-mh") || args[i].equals("--max-handle") )
					Listenner.maxConnectionsHandle = Integer.parseInt(args[i+1]);
				else if( args[i].equals("-ml") || args[i].equals("--max-listen") )
					Listenner.maxConnectionsListen = Integer.parseInt(args[i+1]);
				else if( args[i].equals("-mq") || args[i].equals("--max-queue") )
					Listenner.maxPendingAccept = Integer.parseInt(args[i+1]);
				else if( args[i].equals("-p") || args[i].equals("--port") )
					Listenner.port = Integer.parseInt(args[i+1]);
				else if( args[i].equals("-u") || args[i].equals("--url") )
					url = args[i+1];
				else if( args[i].equals("-a") || args[i].equals("--auth") )
					auth = args[i+1];
				else if( args[i].equals("-v") || args[i].equals("--verbose") )
					Logger.instance().level = (args[i+1].equals("1") ? 200 : 1000); // Level.FINEST : Level.WARNING;
			}
			
			if( url == null || auth == null )
			{
				usage();
				return;
			}
			
			RestApi.initialize(url, auth, false);
			Listenner.start();
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
		finally
		{
			Logger.async(false);
			try { Thread.sleep(Logger.instance().asyncInterval); } catch (Exception e) { }
		}
	}
}