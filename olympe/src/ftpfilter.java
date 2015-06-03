import com.olympe.*;
import com.anotherservice.util.*;
import java.util.logging.Level;

public class ftpfilter
{
	public static void main(String[] args)
	{
		try
		{
			String configFile = null;
			String inputMode = null;

			for( int i = 0; i < args.length; i += 2 )
			{
				if( args[i].equals("-h") || args[i].equals("--help") )
				{
					String usage = "Usage: java -jar ftpfilter.jar [OPTIONS]\n" +
						"Options:\n" +
						"\t-h,\t--help\t\tShow this help message\n" +
						"\t-c,\t--config\tConfig file\n" +
						"\t-i,\t--input-mode\tInput mode from the config file\n" +
						"\t-l,\t--log-level\tThe log level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL)";
					System.out.println(usage);
					System.exit(0);
				}

				if( i+1 >= args.length )
					break;

				if( args[i].equals("-c") || args[i].equals("--config") )
					configFile = args[i+1];
				else if( args[i].equals("-i") || args[i].equals("--input-mode") )
					inputMode = args[i+1];
				else if( args[i].equals("-l") || args[i].equals("--log-level") )
					Logger.instance().level = Level.parse(args[i+1]).intValue();;
			}
			
			if( configFile == null || inputMode == null )
			{
				String usage = "Usage: java -jar ftpfilter.jar [OPTIONS]\n" +
					"Options:\n" +
					"\t-h,\t--help\t\tShow this help message\n" +
					"\t-c,\t--config\t\tConfig file\n" +
					"\t-i,\t--input-mode\tInput mode from the config file\n" +
					"\t-l,\t--log-level\tThe log level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL)";
				System.out.println(usage);
				System.exit(0);
			}
			
			Logger.Log l = Logger.instance();
			l.stream = System.out;
			l.config("Loading config file : " + configFile);
			Config.load(configFile);
			
			if( Config.gets("com.olympe.ftpfilter.input." + inputMode) == null )
			{
				System.out.println("Error : The input configuration directive '" + inputMode + "' does not exist.");
				System.exit(-1);
			}
			
			Logger.finer("Starting filter");
			new OlympeFilter(inputMode);
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
	}
}