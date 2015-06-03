package com.busit.routing;

import java.net.Socket;
import java.io.OutputStream;
import com.anotherservice.util.*;
import com.busit.security.*;

public class BrokerClientTCP implements IBrokerClient
{
	private String host = null;
	private int port = -1;
	
	public BrokerClientTCP()
	{
		this(Config.gets("com.busit.broker.tcp.host"), 
			Integer.parseInt(Config.gets("com.busit.broker.tcp.port")));
	}
	
	public BrokerClientTCP(String host, int port)
	{
		this.host = host;
		this.port = port;
	}
	
	public void transmit(String message, String user, String language, String local, String sla) throws Exception
	{
		// TODO : send to different ports according to the language
		// TODO : use dedicated brokers per user ?

		Socket s = new Socket(this.host, this.port);
		OutputStream o = s.getOutputStream();
		o.write(Hex.getBytes(message));
		s.close();
	}
	
	public void trace(String user, Path path, IIdentity from, IIdentity to, String language, boolean cron) throws Exception
	{
		// TODO : trace message path
	}
}