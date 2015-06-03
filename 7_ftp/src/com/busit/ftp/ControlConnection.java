package com.busit.ftp;

import com.anotherservice.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;

public abstract class ControlConnection extends Thread
{
	private Socket s;
	private BufferedReader in;
	private OutputStream out;
	private boolean allowed = false;
	private boolean stopHandlingUserCommands = false;
	
	private class QuitException extends Exception { }
	
	public ControlConnection()
	{
	}
	
	public void refuse(Socket s)
	{
		if( s == null )
		{
			Logger.finer("Connection closed prematurely");
			Listenner.controlConnectionTerminated(this);
			return;
		}
		
		this.s = s;
		this.allowed = false;
		this.start();
	}
	
	public void accept(Socket s)
	{
		if( s == null )
		{
			Logger.finer("Connection closed prematurely");
			Listenner.controlConnectionTerminated(this);
			return;
		}
		
		this.s = s;
		this.allowed = true;
		this.start();
	}
	
	private void terminate()
	{
		String remote = (s == null ? "UNKNOWN" : "" + s.getInetAddress());
		
		try { if( nextDataConnection != null ) nextDataConnection.abort(false); } catch(Exception e) { }
		try { if( out != null ) out.close(); } catch(Exception e) { }
		try { if( in != null ) in.close(); } catch(Exception e) { }
		try { if( s != null ) s.close(); } catch(Exception e) { }
		
		Logger.finer("Terminated connection with " + remote);
		Listenner.controlConnectionTerminated(this);
	}

	protected void reset() throws Exception { }
	
	protected void cleanup() { }
	
	protected abstract void login(String user, String pass) throws Exception;
	
	public synchronized void reply(int code, String message) throws Exception
	{
		Logger.fine(code + " " + message);
		out.write((code + " " + message + "\r\n").getBytes());
	}
	
	public synchronized void reply(String message) throws Exception
	{
		Logger.fine(message);
		out.write((message + "\r\n").getBytes());
	}
	
	protected String remote()
	{
		return s.getInetAddress().getHostAddress();
	}
	
	//==========================================
	//
	// DATA CONNECTIONS
	//
	//==========================================
	
	protected DataConnection nextDataConnection;
	protected LinkedList<DataConnection> connections = new LinkedList<DataConnection>();
	private synchronized boolean hasPendingDataConnections()
	{
		return (connections.size() > 0);
	}
	
	public synchronized void dataConnectionTerminated(DataConnection dc)
	{
		connections.remove(dc);
	}
	
	public void dataConnectionAccepted() throws Exception
	{
		reply(150, "Accepted data connection.");
	}
	
	public void dataConnectionSuccess() throws Exception
	{
		reply(226, "Closing data connection.");
	}
	
	public void dataConnectionFailure() throws Exception
	{
		reply(426, "Aborted data connection.");
	}
	
	public void dataConnectionUploadTooBig() throws Exception
	{
		reply(452, "File size too big.");
	}
	
	//==========================================
	//
	// READ & PROCESS COMMANDS
	//
	//==========================================
	
	public void run()
	{
		try
		{
			this.out = s.getOutputStream();
			this.reply(220, "Welcome.");
			
			if( !allowed )
			{
				this.reply(421, "Too many connections as this time. Please try again later.");
				Logger.finer("Refused connection with " + s.getInetAddress());
				return;
			}
			else
				Logger.finer("Accepted connection with " + s.getInetAddress());
			
			this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			
			while(true)
			{
				try
				{
					if( stopHandlingUserCommands && hasPendingDataConnections() )
						Thread.sleep(100);
					else if( stopHandlingUserCommands )
						break;
					else
						getCommandFromUser();
				}
				catch(SocketTimeoutException ste)
				{
					if( hasPendingDataConnections() )
						continue;
					else
					{
						this.reply(221, "Timeout.");
						break; // timeout and no more data connections => quit
					}
				}
				catch(QuitException qe)
				{
					break; // normal quit
				}
			}
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
		finally
		{
			terminate();
			cleanup();
		}
	}
	
	private void getCommandFromUser() throws Exception
	{
		String line = null;
		String cmd = null;
		String param = null;
		
		line = in.readLine();
		if( line == null )
			throw new QuitException();
		else if( line.length() < 3 || stopHandlingUserCommands )
			return;
		
		cmd = (line.indexOf(' ') >= 0 ? line.substring(0, line.indexOf(' ')) : line).toUpperCase();
		param = (line.indexOf(' ') >= 0 ? line.substring(line.indexOf(' ')+1) : null);
		
		if( cmd.length() == 0 )
			return;
		
		// do not log the password
		if( cmd.equals("PASS") )
			Logger.fine("PASS ******");
		else
			Logger.fine(line);
		
		if( !authenticated )
		{
			if( user == null && !cmd.equals("USER") && !cmd.equals("QUIT") )
			{
				reply(530, "User required.");
				return;
			}
			if( user != null && pass == null && !cmd.equals("PASS") && !cmd.equals("USER") && !cmd.equals("QUIT") && !cmd.equals("REIN") )
			{
				reply(530, "Pass required.");
				return;
			}
		}
		
		processCommand(cmd, param);
	}
	
	private void processCommand(String cmd, String param) throws Exception
	{
		switch(cmd)
		{
			// ======================== AUTHENTICATION
			
			case "USER": USER(param); break;
			case "PASS": PASS(param); break;
			
			// ======================== CONNECTION
			
			case "FEAT": FEAT(); break;
			case "CDUP": CDUP(); break;
			case "PASV": PASV(); break;
			case "EPSV": EPSV(); break;
			case "QUIT": QUIT(); break;
			case "TYPE": TYPE(param); break;
			case "STRU": STRU(param); break;
			case "MODE": MODE(param); break;
			case "LANG": LANG(param); break;
			case "HELP": HELP(); break;
			case "NOOP": NOOP(); break;
			case "SYST": SYST(); break;
			case "OPTS": OPTS(param); break;
			case "REIN": REIN(); break;
			
			// ======================== FILES
			
			case "PWD": PWD(); break;
			case "CWD": CWD(param); break;
			case "SIZE": SIZE(param); break;
			case "RETR": RETR(param); break;
			case "RNFR": RNFR(param); break;
			case "RNTO": RNTO(param); break;
			case "DELE": DELE(param); break;
			case "RMD": RMD(param); break;
			case "MKD": MKD(param); break;
			case "MLST": MLST(param); break;
			case "ABOR": ABOR(); break;
			
			// ======================== PASSIVE DATA TRANSFER
			
			case "LIST": LIST(); break;
			case "NLST": NLST(); break;
			case "MLSD": MLSD(param); break;
			case "STOR": STOR(param); break;
			
			// ======================== NOT IMPLEMENTED
			
			default:
				// PORT, EPRT, ACCT, SMNT, APPE, ALLO, SITE, MDTM, AUTH, ADAT, PROT, PBSZ, CCC, MIC, CONF, ENC, STOU
				// REST, STAT
				reply(502, "Not implemented.");
				break;
		}
	}
	
	// ===================================
	// OPERATIONS LINKED TO AUTHENTICATION
	// ===================================
	
	protected String user = null;
	protected String pass = null;
	protected boolean authenticated = false;
	
	protected void USER(String param) throws Exception
	{
		user = param;
		pass = null;
		authenticated = false;
		reply(331, "Pass required.");
	}
	
	protected void PASS(String param) throws Exception
	{
		try
		{
			if( user == null )
			{
				reply(503, "Bad sequence of commands.");
				return;
			}
			
			pass = param;
			
			login(user, pass);
			
			authenticated = true;
			reply(200, "OK.");
		}
		catch(Exception e)
		{
			Logger.info(e);
			reply(530, "Authentication failed.");
			authenticated = false;
			pass = null;
		}
	}
	
	// ===================================
	// OPERATIONS LINKED TO CONNECTION
	// ===================================
	
	protected void FEAT() throws Exception
	{
		reply("211-EXT:");
		reply(" UTF8");
		reply(" LANG EN*");
		reply(" PASV");
		reply(" EPSV");
		reply(" SIZE");
		reply(" MLST type*;size*;modify*;perm*;");
		reply(211, "END.");
	}
	
	protected void PASV() throws Exception
	{	
		if( nextDataConnection != null )
		{
			try { nextDataConnection.abort(false); } catch(Exception e) { }
			nextDataConnection = null;
		}
		
		nextDataConnection = Listenner.dataClass.newInstance();
		nextDataConnection.bind(s.getLocalAddress(), this);
		reply(227, "Entering Passive Mode " + nextDataConnection.getPasvReply());
	}
	
	protected void EPSV() throws Exception
	{
		// param = ALL : special meaning
		// param = 1 (ipv4) | 2 (ipv6)
		// the DataConnection uses the same origin address so we dont care about this
		
		if( nextDataConnection != null )
		{
			try { nextDataConnection.abort(false); } catch(Exception e) { }
			nextDataConnection = null;
		}
		
		nextDataConnection = Listenner.dataClass.newInstance();
		nextDataConnection.bind(s.getLocalAddress(), this);
		reply(229, "Entering Passive Mode (" + nextDataConnection.getEpsvReply() + ")");
	}
	
	protected void QUIT() throws Exception
	{
		reply(221, "Quitting.");
			
		if( !hasPendingDataConnections() )
			throw new QuitException();
		else
			stopHandlingUserCommands = true;
	}

	protected String type = "A";
	protected void TYPE(String param) throws Exception
	{
		if( param.equalsIgnoreCase("I") )
		{
			type = "I";
			reply(200, "OK.");
		}
		else if( param.equalsIgnoreCase("A") || param.equalsIgnoreCase("A N") )
		{
			type = "A";
			reply(200, "OK.");
		}
		else
			reply(504, "Type not supported.");
	}
	
	protected void STRU(String param) throws Exception
	{
		if( param.equalsIgnoreCase("F") )
			reply(200, "OK.");
		else
			reply(504, "Structure not supported.");
	}
	
	protected void MODE(String param) throws Exception
	{
		if( param.equalsIgnoreCase("S") )
			reply(200, "OK.");
		else
			reply(504, "Mode not supported.");
	}
	
	protected void LANG(String param) throws Exception
	{
		if( param == null || param.length() == 0 ||
			(param.length() >= 2 && param.substring(0,2).equalsIgnoreCase("EN")) )
			reply(200, "OK. Language is EN.");
		else
			reply(504, "Language not known.");
	}
	
	protected void HELP() throws Exception
	{
		reply(214, "See Wikipedia FTP.");
	}
		
	protected void NOOP() throws Exception
	{
		reply(200, "OK... You'r the NOOP !");
	}
	
	protected void SYST() throws Exception
	{
		reply(215, "NONE.");
	}
	
	protected void OPTS(String param) throws Exception
	{
		if( param.equalsIgnoreCase("UTF8 ON") )
			reply(200, "OK.");
		else
			reply(504, "Option unknown.");
	}
	
	protected void REIN() throws Exception
	{
		user = null;
		pass = null;
		authenticated = false;
		dir = "";
		type = "A";
		
		if( nextDataConnection != null )
		{
			nextDataConnection.abort(true);
			nextDataConnection = null;
		}
		
		reset();
		reply(200, "OK.");
	}
	
	protected void ABOR() throws Exception
	{
		synchronized(this)
		{
			for( Thread t : connections )
				t.interrupt();
		}
		reply(226, "OK.");
	}
	
	// ===================================
	// OPERATIONS LINKED TO FILES
	// ===================================
	
	protected String dir = "";
	public String getDir() { return dir; }
	protected void CDUP() throws Exception
	{
		if( dir.lastIndexOf('/') == -1 )
			dir = "";
		else
			dir = dir.substring(0, dir.lastIndexOf('/'));
		reply(200, "OK.");
	}
	
	protected void PWD() throws Exception
	{
		reply(257, "\"/" + dir + "\" is current location.");
	}
	
	protected abstract void CWD(String param) throws Exception;
	protected abstract void SIZE(String param) throws Exception;
	protected abstract void RNFR(String param) throws Exception;
	protected abstract void RNTO(String param) throws Exception;
	protected abstract void DELE(String param) throws Exception;
	protected abstract void RMD(String param) throws Exception;
	protected abstract void MKD(String param) throws Exception;
	protected abstract void MLST(String param) throws Exception;
	
	// ===================================
	// OPERATIONS LINKED TO DATA CONNECTION
	// ===================================
	
	protected abstract void RETR(String param) throws Exception;
	protected abstract void MLSD(String param) throws Exception;
	protected abstract void STOR(String param) throws Exception;
	
	protected void LIST() throws Exception
	{
		connections.add(nextDataConnection);
		nextDataConnection.accept("LIST", null, s.getInetAddress().getHostAddress());
		nextDataConnection = null;
	}
	
	protected void NLST() throws Exception
	{
		connections.add(nextDataConnection);
		nextDataConnection.accept("NLST", null, s.getInetAddress().getHostAddress());
		nextDataConnection = null;
	}
}