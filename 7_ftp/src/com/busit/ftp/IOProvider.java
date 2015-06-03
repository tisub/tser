package com.busit.ftp;

import com.anotherservice.util.*;
import java.util.*;

public abstract class IOProvider
{
	public static Class<? extends IOProvider> ioClass = Impl.class;
	
	private static Hashtable<ControlConnection, IOProvider> instances = new Hashtable<ControlConnection, IOProvider>();
	public static IOProvider get(ControlConnection c) { return instances.get(c); }
	public static IOProvider init(ControlConnection c)
	{
		try
		{
		IOProvider io = ioClass.newInstance();
		instances.put(c, io);
		return io;
		}
		catch(Exception e) { return null; }
	}
	public static void remove(ControlConnection c) { instances.remove(c); }
	
	public static String absolutize(String path, String root)
	{
		if( path == null ) path = "";
		if( root == null ) root = "";
		
		// relative -> absolute
		if( path.length() == 0 || path.charAt(0) != '/' )
			return root + "/" + path;
		else
			return path;
	}
	
	public abstract void reset();
	public abstract void authenticate(String user, String pass) throws Exception;
	public abstract void delete(String file) throws Exception;
	public abstract List<Info> list(String dir) throws Exception;
	public abstract void upload(String file, byte[] data) throws Exception;
	public abstract byte[] download(String file) throws Exception;
	public abstract void rename(String from, String to) throws Exception;
	public abstract Info info(String file) throws Exception;
	public abstract boolean isFile(String file);
	public abstract boolean isDir(String dir);
	
	public static class Info
	{
		int size = -1;
		Date modified = null;
		boolean isDir = true;
		String name = "";
		public Info(int s, long m, boolean d, String n)
		{
			size = s;
			modified = new Date(m);
			isDir = d;
			
			if( n != null )
			{
				n = n.replaceFirst("/$", "");
				int i = n.lastIndexOf("/");
				if( i >= 0 )
					n = n.substring(i+1);
			}
			
			name = n;
		}
	}
	
	private static class Impl extends IOProvider
	{
		public void reset() { }
		public void authenticate(String user, String pass) throws Exception { }
		public void delete(String file) throws Exception { }
		public List<Info> list(String dir) throws Exception { return new LinkedList<Info>(); }
		public void upload(String file, byte[] data) throws Exception { }
		public byte[] download(String file) throws Exception { return new byte[0]; }
		public void rename(String from, String to) throws Exception { }
		public Info info(String file) throws Exception { return new Info(0, new Date().getTime(), false, ""); }
		public boolean isFile(String file) { return true; }
		public boolean isDir(String dir) { return true; }
	}
}