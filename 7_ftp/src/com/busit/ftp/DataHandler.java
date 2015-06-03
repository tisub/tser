package com.busit.ftp;

import com.anotherservice.util.*;
import java.io.*;

public class DataHandler extends DataConnection
{
	public DataHandler()
	{
	}
	
	protected String LIST(String param) throws Exception
	{
		StringBuffer sb = new StringBuffer();
		for( IOProvider.Info dir : IOProvider.get(cc).list(IOProvider.absolutize(param, cc.getDir())) )
			sb.append((dir.isDir ? "d" : "-") + "------rwx " + dir.size + " none none             1 Jan 1   2000 " + dir.name + "\r\n");
		return sb.toString();
	}
	
	protected String NLST(String param) throws Exception
	{
		StringBuffer sb = new StringBuffer();
		for( IOProvider.Info dir : IOProvider.get(cc).list(IOProvider.absolutize(param, cc.getDir())) )
			sb.append(dir.name + "\r\n");
		return sb.toString();
	}
	
	protected String MLSD(String param) throws Exception
	{
		StringBuffer sb = new StringBuffer();
		for( IOProvider.Info dir : IOProvider.get(cc).list(IOProvider.absolutize(param, cc.getDir())) )
		{
			sb.append(" type=");
			if( dir.isDir )
				sb.append((dir.name.equals(".")?"c":(dir.name.equals("..")?"p":"")) + "dir");
			else
				sb.append("file");
			sb.append(";size=" + dir.size + ";modify=20000101000001;perm=celw " + dir.name + "\r\n");
		}
		return sb.toString();
	}
	
	protected void STOR(String param, InputStream in) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for( int b = in.read(); b >= 0; b = in.read() )
		{
			baos.write(b & 0xFF);
			if( baos.size() > Listenner.maxUploadSize )
				throw new DataConnection.UploadTooBigException();
		}
		
		IOProvider.get(cc).upload(IOProvider.absolutize(param, cc.getDir()), baos.toByteArray());
		try { baos.reset(); } catch(Exception e) { } 
	}
	
	protected byte[] RETR(String param) throws Exception
	{
		return IOProvider.get(cc).download(IOProvider.absolutize(param, cc.getDir()));
	}
}