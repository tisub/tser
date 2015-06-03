package com.busit.ftp;

import com.anotherservice.util.*;
import java.net.*;
import java.io.*;
import java.util.*;

public abstract class DataConnection extends Thread
{
	private ServerSocket ss;
	private Socket s;
	private InputStream in;
	private OutputStream out;
	protected ControlConnection cc;
	private String clientIP;
	
	private String cmd;
	private String param;
	
	public static class UploadTooBigException extends Exception { }
	
	// restrict the passive port range
	private static Object lock = new Object();
	private static int port = 60000;
	public static int port()
	{
		synchronized(lock)
		{
			if( port >= 60200 ) port = 60000;
			return ++port;
		}
	}
	
	public DataConnection()
	{
	}
	
	public void bind(InetAddress bind, ControlConnection cc) throws Exception
	{
		this.ss = new ServerSocket(port(), 1, bind);
		if( Listenner.idleTimeout > 0 )
			ss.setSoTimeout(Listenner.idleTimeout);
		
		Logger.finer("Starting data connection on " + bind + ":" + ss.getLocalPort());
		this.cc = cc;
	}
	
	public String getPasvReply()
	{
		if( Listenner.frontip != null )
		{
			String[] parts = Listenner.frontip.split("\\.");
			return "(" + parts[0] + "," + 
				parts[1] + "," + 
				parts[2] + "," + 
				parts[3] + "," + 
				String.valueOf(ss.getLocalPort()/256) + "," + 
				String.valueOf(ss.getLocalPort()%256) + ")";
		}
		else
		{
			byte[] ip = ss.getInetAddress().getAddress();
			return "(" + String.valueOf(ip[0]) + "," + 
				String.valueOf(ip[1]) + "," + 
				String.valueOf(ip[2]) + "," + 
				String.valueOf(ip[3]) + "," + 
				String.valueOf(ss.getLocalPort()/256) + "," + 
				String.valueOf(ss.getLocalPort()%256) + ")";
		}
	}
	
	public String getEpsvReply()
	{
		return "|||" + ss.getLocalPort() + "|";
	}
	
	public void accept(String cmd, String param, String clientIP)
	{
		if( cc == null )
			throw new IllegalStateException();
			
		this.cmd = cmd;
		this.param = param;
		this.clientIP = clientIP;
		
		this.start();
	}
	
	public void abort(boolean sendControlConnectionAbort) throws Exception
	{
		try { if( ss != null ) ss.close(); } catch(Exception e) { }
		
		if( cc != null )
		{
			if( sendControlConnectionAbort )
				cc.dataConnectionFailure();
			cc.dataConnectionTerminated(this);
		}
	}
	
	public void run()
	{
		try
		{
			while(true)
			{
				if( this.interrupted() )
					throw new InterruptedException();
					
				s = ss.accept();
				if( !clientIP.equals(s.getInetAddress().getHostAddress()) )
				{
					s.close();
					continue; // this is not the good client :/
				}
				else
				{
					Logger.finer("Accepted data connection with " + s.getInetAddress());
					ss.close();
					break;
				}
			}
			
			cc.dataConnectionAccepted();
			processCommand();
		}
		catch(InterruptedException ie)
		{
			Logger.finer("Aborting data connection with " + (s == null ? clientIP : "" + s.getInetAddress()));
		}
		catch(UploadTooBigException utbe)
		{
			try { cc.dataConnectionUploadTooBig(); } catch(Exception e2) { }
			Logger.finer("Upload limit reached for " + s.getInetAddress());
		}
		catch(Exception e)
		{
			try { cc.dataConnectionFailure(); } catch(Exception e2) { }
			Logger.severe(e);
		}
		finally
		{
			try { ss.close(); } catch(Exception e) { }
			try { if( out != null ) out.close(); } catch(Exception e) { }
			try { if( in != null ) in.close(); } catch(Exception e) { }
			try
			{
				if( s != null )
				{
					Logger.finer("Terminated data connection with " + s.getInetAddress());
					s.close();
				}
			} catch(Exception e) { }
			cc.dataConnectionTerminated(this);
		}
	}
	
	private void processCommand() throws Exception
	{
		if( in == null )
			this.in = s.getInputStream();
		
		switch(cmd)
		{
			case "LIST": reply(LIST(param)); break;
			case "NLST": reply(NLST(param)); break;
			case "MLSD": reply(MLSD(param)); break;
			case "STOR": STOR(param, in); break;
			case "RETR": reply(RETR(param)); break;
			default:
				throw new Exception("Unsupported command " + cmd);
		}
		
		cc.dataConnectionSuccess();
	}
	
	private void reply(String s) throws Exception
	{
		reply(s.getBytes());
	}
	
	private void reply(byte[] b) throws Exception
	{
		if( out == null )
			this.out = s.getOutputStream();
		out.write(b);
		out.flush();
	}

	protected abstract String LIST(String param) throws Exception;
	protected abstract String NLST(String param) throws Exception;
	protected abstract String MLSD(String param) throws Exception;
	protected abstract void STOR(String param, InputStream in) throws Exception;
	protected abstract byte[] RETR(String param) throws Exception;
}