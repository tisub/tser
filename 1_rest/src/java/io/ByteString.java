package java.io;

import java.io.*;
import java.util.*;
import java.lang.*;

public class ByteString extends ByteArrayInputStream implements Iterable<Byte>
{
	public ByteString(ByteArrayInputStream source)
	{
		super(source.buf, source.mark, source.count);
	}
	
	public ByteString(ByteArrayInputStream source, int offset, int length)
	{
		super(source.buf, source.mark + offset, Math.min(source.count - offset, length));
	}
	
	public ByteString(ByteString source)
	{
		this(source.buf, source.mark, source.count);
	}
	
	public ByteString(ByteString source, int offset, int length)
	{
		this(source.buf, source.mark + offset, Math.min(source.count - offset, length));
	}
	
	public ByteString(byte[] source)
	{
		this(source, 0, source.length);
	}
	
	public ByteString(byte[] source, int offset, int length)
	{
		super(source, offset, length);
	}
	
	public ByteString(String source) throws UnsupportedEncodingException
	{
		super(source.getBytes("UTF-8"));
	}
	
	public ByteString(String source, int offset, int length) throws UnsupportedEncodingException
	{
		super(source.substring(offset, offset + length).getBytes("UTF-8"));
	}
	
	public String toString()
	{
		try { return new String(this.buf, this.mark, this.count, "UTF-8"); } catch(Exception e) { throw new RuntimeException("Unsupported encoding"); }
	}
	
	public byte[] toBytes()
	{
		return Arrays.copyOfRange(this.buf, this.mark, this.mark + this.count);
	}
	
	public InputStream toStream()
	{
		return new ByteArrayInputStream(this.buf, this.mark, this.count);
	}
	
	public ByteString sub(int offset, int length)
	{
		return new ByteString(this.buf, this.mark + offset, Math.min(length, this.count - offset));
	}
	
	public int indexOf(String search)
	{
		try { return indexOf(search.getBytes("UTF-8"), 0); } catch(Exception e) { throw new RuntimeException("Unsupported encoding"); }
	}
	
	public int indexOf(String search, int offset)
	{
		try { return indexOf(search.getBytes("UTF-8"), offset); } catch(Exception e) { throw new RuntimeException("Unsupported encoding"); }
	}
	
	public int indexOf(ByteArrayInputStream search)
	{
		return indexOf(Arrays.copyOfRange(search.buf, search.mark, search.mark + search.count), 0);
	}
	
	public int indexOf(ByteArrayInputStream search, int offset)
	{
		return indexOf(Arrays.copyOfRange(search.buf, search.mark + offset, search.mark + search.count), 0);
	}
	
	public int indexOf(byte[] search)
	{
		return indexOf(search, 0);
	}
	
	public int indexOf(byte[] search, int offset)
	{
		int end = this.count + this.mark;
		int start = this.mark + offset;
		byte first = search[0];
		for( int i = start; i < end; i++ )
		{
			if( end - i > search.length )
				return -1;

			if( this.buf[i] == first )
			{
				int j = 1;
				for( ; j < search.length; j++ )
				{
					if( this.buf[i] != search[j] )
						break;
				}
				
				if( j == search.length )
					return (i - offset);
			}
		}
		
		return -1;
	}
	
	public byte byteAt(int index)
	{
		if( index >= this.count )
			throw new IndexOutOfBoundsException();
			
		return this.buf[this.mark + index];
	}
	
	public Iterator<Byte> iterator()
	{
		return new Iterator<Byte>()
		{
			int i = 0;
			public boolean hasNext()
			{
				return i < count;
			}
			
			public Byte next()
			{
				if( i >= count )
					throw new NoSuchElementException();
				return byteAt(i++);
			}
			
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public List<ByteString> split(String delimiter)
	{
		try { return split(delimiter.getBytes("UTF-8")); } catch(Exception e) { throw new RuntimeException("Unsupported encoding"); }
	}
	
	public List<ByteString> split(ByteArrayInputStream delimiter)
	{
		return split(Arrays.copyOfRange(delimiter.buf, delimiter.mark, delimiter.mark + delimiter.count));
	}
	
	public List<ByteString> split(byte[] delimiter)
	{
		LinkedList<ByteString> parts = new LinkedList<ByteString>();
		
		int offset = 0;
		int index = -1;
		while( (index = indexOf(delimiter)) > -1 )
		{
			parts.add(new ByteString(this, offset, index - offset));
			offset += index + delimiter.length;
		}
		
		return parts;
	}
}