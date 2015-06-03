package com.busit.ftp;

import com.anotherservice.util.*;
import java.net.*;
import java.util.*;

public class Listenner
{
	public static int port = 21;
	public static String ip = "0.0.0.0";
	public static String frontip = null;
	
	public static int maxPendingAccept = 10;
	public static int maxConnectionsHandle = 10;
	public static int maxConnectionsListen = 20;
	public static int maxUploadSize = 5242880;
	public static int idleTimeout = 20000;
	
	public static Class<? extends ControlConnection> controlClass = ControlHandler.class;
	public static Class<? extends DataConnection> dataClass = DataHandler.class;
	
	public Listenner()
	{
	}
	
	public static void start()
	{
		try
		{
			ServerSocket ss = new ServerSocket(port, maxPendingAccept, InetAddress.getByName(ip));
			Logger.info("FTP listenner started.");
			
			// if we want to check some variable while listenning
			// then we should set a timeout which will throw an exception
			// and allow us to check the variable on next loop
			//ss.setSoTimeout(100);
			
			while(true)
			{
				try
				{
					// if we reached the listen limit, dont accept anymore
					if( connections.size() >= maxConnectionsListen )
						Thread.sleep(100);
					else
						handle(ss.accept());
				}
				catch(SocketTimeoutException  ste) { /* accept timeout */ }
			}
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
	}
	
	private static LinkedList<ControlConnection> connections = new LinkedList<ControlConnection>();
	private synchronized static void handle(Socket s)
	{
		try
		{
			if( idleTimeout > 0 )
				s.setSoTimeout(idleTimeout);
			ControlConnection cc = controlClass.newInstance();
			connections.add(cc);
			
			if( connections.size() >= maxConnectionsHandle )
				cc.refuse(s);
			else
				cc.accept(s);
		}
		catch(Exception e)
		{
			try { s.close(); } catch(Exception e2) { }
		}
	}
	
	public synchronized static void controlConnectionTerminated(ControlConnection cc)
	{
		connections.remove(cc);
	}
}