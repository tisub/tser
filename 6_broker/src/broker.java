
public class broker
{
	com.busit.broker.RemoteProcessorClient c;
	com.busit.broker.RemoteProcessorServer s;
	
	public static void main(String[] args)
	{
		if( args.length == 1 && args[0].equals("--remote-client") )
			new com.busit.broker.RemoteProcessorClient();
		else
			com.busit.broker.Broker.main(args);
	}
}