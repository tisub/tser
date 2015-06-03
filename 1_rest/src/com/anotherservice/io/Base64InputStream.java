package com.anotherservice.io;

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.util.regex.*;
import java.util.*;
import java.io.ByteArrayOutputStream;

public class Base64InputStream extends InputStream
{
	private boolean isDecoding = true;
	private int current = 4;
	private boolean eos = false;
	private int[] buffer_d = new int[3]; // decode bytes 3 by 3
	private int[] buffer_e = new int[4]; // encode bytes 4 by 4
	private DataReader data = null;
	
	// ========================================
	// CONSTRUCTORS
	// ========================================
	public Base64InputStream(InputStream data, boolean decode)
	{
		this(data, 0, -1, decode);
	}
	
	public Base64InputStream(InputStream data, int start, int end, boolean decode)
	{
		this.isDecoding = decode;
		this.data = DataReader.wrap(data);
		if( start > 0 )
			this.data.skip(start);
		if( end > 0 )
			this.data.limit(end);
	}
	
	public Base64InputStream(Reader data, boolean decode)
	{
		this(data, 0, -1, decode);
	}
	
	public Base64InputStream(Reader data, int start, int end, boolean decode)
	{
		this.isDecoding = decode;
		this.data = DataReader.wrap(data);
		if( start > 0 )
			this.data.skip(start);
		if( end > 0 )
			this.data.limit(end);
	}
	
	public Base64InputStream(String data, boolean decode)
	{
		this(data, 0, -1, decode);
	}
	
	public Base64InputStream(String data, int start, int end, boolean decode)
	{
		this.isDecoding = decode;
		this.data = DataReader.wrap(data);
		if( start > 0 )
			this.data.skip(start);
		if( end > 0 )
			this.data.limit(end);
	}
	
	public Base64InputStream(byte[] data, boolean decode)
	{
		this(data, 0, data.length, decode);
	}
	
	public Base64InputStream(byte[] data, int start, int end, boolean decode)
	{
		this.isDecoding = decode;
		this.data = DataReader.wrap(data);
		if( start > 0 )
			this.data.skip(start);
		if( end > 0 )
			this.data.limit(end);
	}
	
	public Base64InputStream(DataReader data, boolean decode)
	{
		this.isDecoding = decode;
		this.data = data;
	}
	
	// ========================================
	// INPUTSTREAM
	// ========================================
	public int available() throws IOException
	{
        return -1;
    }
	
	public int read() throws IOException
	{
		if( eos )
			return -1;

		if( isDecoding )
		{
			if( current > 2 )
			{
				if( decode() ) return -1;
				current = 0;
			}
			return buffer_d[current++];
		}
		else
		{
			if( current > 3 )
			{
				if( encode() ) return -1;
				current = 0;
			}
			return buffer_e[current++];
		}
	}
	
	public String toString()
	{
		
		if( isDecoding )
			throw new RuntimeException("Cannot convert to string when decoding data");

		try
		{
			StringBuilder sb = new StringBuilder();
			int c;
			while( (c = read()) != -1 )
				sb.append((char)c);
			return sb.toString();
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	public byte[] toBytes()
	{
		if( !isDecoding )
			throw new RuntimeException("Cannot convert to bytes when encoding data");

		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int c;
			while( (c = read()) != -1 )
				baos.write(c);
			return baos.toByteArray();
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	// ========================================
	// BASE64
	// ========================================
	private boolean decode() throws IOException
	{
		if( (buffer_e[0] = data.read()) == -1 )
			return eos = true;
		
		if( (buffer_e[1] = data.read()) == -1 )
			throw new IOException("Invalid input data");
		
		buffer_e[2] = data.read();
		buffer_e[3] = data.read();
		
		byte b0 = map_d[buffer_e[0]];
		byte b1 = map_d[buffer_e[1]];
		byte b2 = map_d[(buffer_e[2] != -1 ? buffer_e[2] : 'A')];
		byte b3 = map_d[(buffer_e[3] != -1 ? buffer_e[3] : 'A')];
			
		buffer_d[0] = (( b0       <<2) | (b1>>>4));
		buffer_d[1] = (((b1 & 0xf)<<4) | (b2>>>2));
		buffer_d[2] = (((b2 &   3)<<6) |  b3);
		
		return false;
	}
	
	private boolean encode()
	{
		if( (buffer_d[0] = data.read()) == -1 )
			return eos = true;
		
		buffer_d[1] = data.read();
		buffer_d[2] = data.read();
		
		int i0 = buffer_d[0] & 0xff;
		int i1 = (buffer_d[1] != -1 ? buffer_d[1] & 0xff : 0);
		int i2 = (buffer_d[2] != -1 ? buffer_d[2] & 0xff : 0);
		
		buffer_e[0] = map_e[i0 >>> 2];
		buffer_e[1] = map_e[((i0 & 3) << 4) | (i1 >>> 4)];
		buffer_e[2] = (buffer_d[1] != -1 ? map_e[((i1 & 0xf) << 2) | (i2 >>> 6)] : '=');
		buffer_e[3] = (buffer_d[2] != -1 ? map_e[i2 & 0x3F] : '=');
		
		return false;
	}
	
	// ENCODE CHARACTER MAP
	private static char[] map_e = new char[64];
	static
	{
		int i=0;
		for( char c = 'A'; c <= 'Z'; c++ ) map_e[i++] = c;
		for( char c = 'a'; c <= 'z'; c++ ) map_e[i++] = c;
		for( char c = '0'; c <= '9'; c++ ) map_e[i++] = c;
		map_e[i++] = '+'; // the norm says that 'base64' is '+' but 'base64url' is '-' (for safe url or filename)
		map_e[i++] = '/'; // the norm says that 'base64' is '/' but 'base64url' is '_' (for safe url or filename)
	}

	// DECODE CHARACTER MAP
	private static byte[] map_d = new byte[128];
	static
	{
		for( int i = 0; i < map_d.length; i++ ) map_d[i] = -1;
		for( int i = 0; i < 64; i++ ) map_d[map_e[i]] = (byte)i;
	}
}