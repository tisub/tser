package com.anotherservice.io;

import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;
import com.anotherservice.util.*;

public class StdIOIPC
{
	public static class IllegalControlSequence extends IOException
	{
		public String sequence = null;
		public IllegalControlSequence(String sequence)
		{
			super("Illegal control sequence [" + sequence + "]");
			this.sequence = sequence;
		}
	}
	
	
	public static int MAX_LENGTH = 1073741824; // 1GB
	public static String CONTROL_SEQUENCE = "#BUSIT-STDIOIPC#";
	
	public InputStream in;
	public OutputStream out;
	
	public StdIOIPC(Process process)
	{
		out = process.getOutputStream();
		in = process.getInputStream();
	}
	
	public StdIOIPC(InputStream in, OutputStream out)
	{
		this.in = in;
		this.out = out;
	}
	
	public void sendAny(Any data) throws IOException { send(Hex.getBytes(data.toJson())); }
	public void send(byte[] data) throws IOException
	{
		if( this.out == null ) throw new IOException("Output stream not ready");
		if( data.length > MAX_LENGTH ) throw new IOException("Input data too big");
		
		// only one write at a time
		synchronized(out)
		{
			// first send control sequence
			if( CONTROL_SEQUENCE != null && CONTROL_SEQUENCE.length() > 0 )
				out.write(CONTROL_SEQUENCE.getBytes());
			
			// then send the length as 32bit int
			int length = data.length;
			out.write(length >> 24 & 0xff);
			out.write(length >> 16 & 0xff);
			out.write(length >> 8 & 0xff);
			out.write(length >> 0 & 0xff);
			out.flush();
			
			// then send the data
			out.write(data, 0, length);
			out.flush();
		}
	}
	
	
	public Any receiveAny() throws Exception { String data = Hex.toString(receive()); return data.length() > 0 ? Json.decode(data) : Any.empty(); }
	public byte[] receive() throws IOException
	{
		if( this.in == null ) throw new EOFException("Input stream not ready");
		
		// only one read at a time
		synchronized(in)
		{
			if( CONTROL_SEQUENCE != null && CONTROL_SEQUENCE.length() > 0 )
			{
				String seq = new String(StreamReader.readExactly(in, CONTROL_SEQUENCE.length()));
				if( !CONTROL_SEQUENCE.equals(seq) )
					throw new IllegalControlSequence(seq);
			}
			
			byte[] raw_length = StreamReader.readExactly(in, 4);
			int length = 0 
				| ((((int)raw_length[0]) & 0xff) << 24) 
				| ((((int)raw_length[1]) & 0xff) << 16)
				| ((((int)raw_length[2]) & 0xff) << 8)
				| ((((int)raw_length[3]) & 0xff) << 0);
			
			if( length < 0 || length > MAX_LENGTH )
				throw new IOException("Input data too big");
			
			return StreamReader.readExactly(in, length);
		}
	}
	
	public void close(boolean wait)
	{
		if( wait )
		{
			if( this.in != null )
			{
				synchronized(in)
				{
					try { in.close(); } catch(IOException e) { }
					in = null;
				}
			}
			
			if( this.out != null )
			{
				synchronized(out)
				{
					try { out.close(); } catch(IOException e) { }	
					out = null;
				}
			}
		}
		else
		{
			if( this.out != null )
			{
				try { out.close(); } catch(IOException e) { }
				out = null;
			}
			
			if( this.in != null )
			{
				try { in.close(); } catch(IOException e) { }
				in = null;
			}
		}
	}
	
	public void close()
	{
		close(false);
	}
	
	// ===================================
	//
	// GROUP SEND/RECEIVE MULTIPLE MESSAGES AT ONCE
	//
	// ===================================
	
	public void sendAll(List<byte[]> buffer) throws IOException
	{
		if( buffer == null )
			return;
		for( int i = 0; i < buffer.size(); i++ )
			send(buffer.get(i));
	}
	
	public List<byte[]> receiveAll()
	{
		LinkedList<byte[]> buffer = new LinkedList<byte[]>();
		
		try
		{
			while( true )
				buffer.add(receive());
		}
		catch(IOException e) { }
		
		return buffer;
	}
}