package com.anotherservice.io;

import java.io.*;
import java.util.*;

public class StreamReader
{
	public static class IllegalSizeException extends IOException
	{
		public int required = 0;
		public byte[] data = null;
		
		public IllegalSizeException(byte[] data, int required)
		{
			super("Could only read " + data.length + " bytes out of " + required);
			this.required = required;
			this.data = data;
		}
	}
	
	public static byte[] readExactly(InputStream in, int length) throws IOException
	{
		byte[] data = readAtMost(in, length);
		
		if( data.length == 0 )
			throw new EOFException();
		if( data.length != length )
			throw new IllegalSizeException(data, length);
		
		return data;
	}
	
	public static byte[] readAtMost(InputStream in, int length) throws IOException
	{
		byte[] data = new byte[length];

		int total = 0;
		for( int read = 0; read != -1 && total < length; read = in.read(data, total, length - total) )
			total += read;
		
		if( total < length )
			return Arrays.copyOf(data, total);
		else
			return data;
	}
	
	public static byte[] readAll(InputStream in) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data = new byte[16384];
		int read = 0;
		while( (read = in.read(data)) != -1 )
			baos.write(data, 0, read);
		
		return baos.toByteArray();
	}
	
	/*public static byte[] readAtLeast(InputStream in, int length) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data = new byte[16384];
		int read = 0;
		while( (read = in.read(data)) != -1 )
			baos.write(data, 0, read);
		
		return baos.toByteArray();
	}*/
}