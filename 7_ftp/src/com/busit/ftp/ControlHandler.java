package com.busit.ftp;

import com.anotherservice.util.*;

public class ControlHandler extends ControlConnection
{
	private IOProvider io = null;
	
	public ControlHandler()
	{
		io = IOProvider.init(this);
	}
	
	protected void reset() throws Exception
	{
		io.reset();
	}
	
	protected void cleanup()
	{
		io = null;
		IOProvider.remove(this);
	}
	
	protected void login(String user, String pass) throws Exception
	{
		io.authenticate(user, pass);
	}
	
	protected void CWD(String param) throws Exception
	{
		if( param == null || param.length() == 0 )
		{
			reply(501, "Missing parameter.");
			return;
		}
		
		if( param.equals("/") )
		{
			dir = "";
			reply(200, "OK.");
			return;
		}
		
		if( param.equals(".") || param.equals("/.") )
		{
			reply(200, "OK.");
			return;
		}
		
		if( param.equals("..") || param.equals("/..") )
		{
			if( dir.lastIndexOf('/') == -1 )
				dir = "";
			else
				dir = dir.substring(0, dir.lastIndexOf('/'));
			reply(200, "OK.");
			return;
		}
		
		param = IOProvider.absolutize(param, dir);

		if( io.isDir(param) )
		{
			dir = param;
			reply(200, "OK.");
		}
		else
			reply(550, "Path not found or not a directory.");
	}
	
	protected void SIZE(String param) throws Exception
	{
		IOProvider.Info i = io.info(IOProvider.absolutize(param, dir));
		if( i == null )
			reply(550, "File not found.");
		else
			reply(213, i.size + "");
	}
	
	private String renameFrom = null;
	protected void RNFR(String param) throws Exception
	{
		if( !io.isFile(IOProvider.absolutize(param, dir)) )
			reply(550, "File not found.");
		else
		{
			renameFrom = param;
			reply(350, "Requested file action pending further information.");
		}
	}
	
	protected void RNTO(String param) throws Exception
	{
		if( renameFrom == null )
		{
			reply(503, "Bad sequence of commands.");
			return;
		}
		
		param = IOProvider.absolutize(param, dir);
		if( io.isFile(param) || io.isDir(param) )
			reply(553, "File name not allowed.");
		else
		{
			try
			{
				io.rename(IOProvider.absolutize(renameFrom, dir), param);
				reply(250, "Requested file action okay, completed.");
			}
			catch(Exception e)
			{
				reply(550, e.getMessage());
			}
		}
		
		renameFrom = null;
	}
	
	protected void DELE(String param) throws Exception
	{
		try
		{
			io.delete(IOProvider.absolutize(param, dir));
		}
		catch(Exception e)
		{
			reply(550, e.getMessage());
		}
	}
	
	protected void RMD(String param) throws Exception { reply(550, "Operation not allowed here."); }
	protected void MKD(String param) throws Exception { reply(550, "Operation not allowed here."); }
	
	protected void MLST(String param) throws Exception
	{
		if( param == null || param.length() == 0 || param.equals(".") || param.equals(dir) )
		{
			reply("250-BEGIN.\r\n");
			reply(" type=cdir;size=1;modify=20000101000001;perm=celw " + dir + "\r\n");
			reply(250, "END.");
		}
		else if( param.equals("/") )
		{
			reply("250-BEGIN.\r\n");
			reply(" type=" + (dir.length()==0?"c":"p") + "dir;size=1;modify=20000101000001;perm=celw /\r\n");
			reply(250, "END.");
		}
		else
		{
			param = IOProvider.absolutize(param, dir);
			
			try
			{
				IOProvider.Info i = io.info(param);
				String response = " type=";
				if( i.isDir )
					response += (i.name.equals(".")?"c":(i.name.equals("..")?"p":"")) + "dir";
				else
					response += "file";
				response += ";size=" + i.size + ";modify=20000101000001;perm=celw " + i.name + "\r\n";
			
				reply("250-BEGIN.\r\n");
				reply(response);
				reply(250, "END.");
			}
			catch(Exception e)
			{
				reply(550, "File not found.");
			}
		}
	}
	
	protected void RETR(String param) throws Exception
	{
		if( !io.isFile(IOProvider.absolutize(param, dir)) )
			reply(550, "File not found.");
		else
		{
			connections.add(nextDataConnection);
			nextDataConnection.accept("RETR", param, remote());
			nextDataConnection = null;
		}
	}
	
	protected void MLSD(String param) throws Exception
	{
		if( param == null || param.length() == 0 || param.equals(".") || param.equals(dir) || param.equals("/") || io.isDir(IOProvider.absolutize(param, dir)) )
		{
			connections.add(nextDataConnection);
			nextDataConnection.accept("MLSD", param, remote());
			nextDataConnection = null;
		}
		else
			reply(501, "Not a directory.");
	}
	
	protected void STOR(String param) throws Exception
	{
		param = IOProvider.absolutize(param, dir);
		if( param.indexOf('/') == -1 )
			reply(550, "Access denied.");
		else if( io.isDir(param) )
		{
			reply(550, "Name targets an existing directory.");
		}
		else
		{
			connections.add(nextDataConnection);
			nextDataConnection.accept("STOR", param, remote());
			nextDataConnection = null;
		}
	}
}