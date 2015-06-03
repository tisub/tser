package com.anotherservice.io;

import java.io.Reader;
import java.io.InputStream;

public abstract class DataReader
{
	public abstract int read();
	public abstract void skip(int amount);
	public abstract void limit(int limit);
	
	public static DataReader wrap(String data)
	{
		return new DataReader_String(data);
	}
	
	public static DataReader wrap(byte[] data)
	{
		return new DataReader_ByteArray(data);
	}
	
	public static DataReader wrap(InputStream data)
	{
		return new DataReader_InputStream(data);
	}
	
	public static DataReader wrap(Reader data)
	{
		return new DataReader_Reader(data);
	}
}

class DataReader_String extends DataReader
{
	private int counter = 0;
	private int limit = Integer.MAX_VALUE;
	private String data = null;
	
	public DataReader_String(String data)
	{
		this.data = data;
	}
	
	public void skip(int amount)
	{
		counter += amount;
	}
	
	public void limit(int limit)
	{
		this.limit = limit;
	}
	
	public int read()
	{
		if( data == null || counter >= data.length() || counter >= limit )
			return -1;
		return data.charAt(counter++) & 0xff;
	}
}

class DataReader_ByteArray extends DataReader
{
	private int counter = 0;
	private int limit = Integer.MAX_VALUE;
	private byte[] data = null;
	
	public DataReader_ByteArray(byte[] data)
	{
		this.data = data;
	}
	
	public void skip(int amount)
	{
		counter += amount;
	}
	
	public void limit(int limit)
	{
		this.limit = limit;
	}
	
	public int read()
	{
		if( data == null || counter >= data.length || counter >= limit )
			return -1;
		return data[counter++] & 0xff;
	}
}

class DataReader_InputStream extends DataReader
{
	private int counter = 0;
	private int limit = Integer.MAX_VALUE;
	private InputStream data = null;
	
	public DataReader_InputStream(InputStream data)
	{
		this.data = data;
	}
	
	public void skip(int amount)
	{
		counter += amount;
		try { data.skip(amount); }
		catch(Exception e) { }
	}
	
	public void limit(int limit)
	{
		this.limit = limit;
	}
	
	public int read()
	{
		if( data == null || counter >= limit )
			return -1;
		counter++;
		
		try{ return data.read(); }
		catch(Exception e) { return -1; }
	}
}

class DataReader_Reader extends DataReader
{
	private int counter = 0;
	private int limit = Integer.MAX_VALUE;
	private Reader data = null;
	
	public DataReader_Reader(Reader data)
	{
		this.data = data;
	}
	
	public void skip(int amount)
	{
		counter += amount;
		try { data.skip(amount); }
		catch(Exception e) { }
	}
	
	public void limit(int limit)
	{
		this.limit = limit;
	}
	
	public int read()
	{
		if( data == null || counter >= limit )
			return -1;
		counter++;
		
		try{ return data.read(); }
		catch(Exception e) { return -1; }
	}
}