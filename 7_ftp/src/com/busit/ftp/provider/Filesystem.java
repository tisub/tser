package com.busit.ftp.provider;

import com.anotherservice.util.*;
import java.io.*;
import java.util.*;
import java.net.*;
import com.busit.ftp.*;

public class Filesystem extends IOProvider
{
	public static String drive = "C:\\";
	
	private String toLocal(String file)
	{
		if( file.indexOf(drive) != 0 )
			file = drive + file;
		return file.replaceAll("/", "\\\\");
	}
	
	public Filesystem() { }
	public void reset() { }
	public void authenticate(String user, String pass) throws Exception { }
	
	public void delete(String file) throws Exception
	{
		new File(toLocal(file)).delete();
	}
	
	public List<Info> list(String dir) throws Exception
	{
		File[] files = new File(toLocal(dir)).listFiles();
		LinkedList<Info> list = new LinkedList<Info>();
		for( int i = 0; i < files.length; i++ )
			list.add(new Info(new Long(files[i].length()).intValue(), files[i].lastModified(), files[i].isDirectory(), files[i].getName()));
		return list;
	}
	
	public void upload(String file, byte[] data) throws Exception
	{
		new FileOutputStream(toLocal(file)).write(data, 0, data.length);
	}
	
	public byte[] download(String file) throws Exception
	{
		FileInputStream is = new FileInputStream(toLocal(file));
		int n;
		byte[] buffer = new byte[16384];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		while( (n = is.read(buffer, 0, buffer.length)) != -1)
			baos.write(buffer, 0, n);
		baos.flush();
		is.close();

		return baos.toByteArray();
	}
	
	public void rename(String from, String to) throws Exception
	{
		new File(toLocal(from)).renameTo(new File(toLocal(to)));
	}
	
	public Info info(String file) throws Exception
	{
		File f = new File(toLocal(file));
		return new Info(new Long(f.length()).intValue(), f.lastModified(), f.isDirectory(), f.getName());
	}
	
	public boolean isFile(String file)
	{
		return new File(toLocal(file)).isFile();
	}
	
	public boolean isDir(String dir)
	{
		return new File(toLocal(dir)).isDirectory();
	}
}