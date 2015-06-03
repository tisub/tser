package com.anotherservice.io;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ByteStream extends ByteArrayOutputStream
{
	public ByteStream()
	{
		this(16384); // 16KB by default
	}
	
	public ByteStream(int size)
	{
		super(size);
	}
	
	public ByteStream(byte[] buf)
	{
		super(buf.length);
		this.write(buf, 0, buf.length);
	}
	
	public InputStream getInputStream()
	{
		return new ByteArrayInputStream(this.buf, 0, this.count);
	}
}