package com.anotherservice.io;

import java.lang.*;
import java.io.*;

/**
 * This class provides easy access to plain text file content. Binary files support is not guaranteed.
 *
 * @author  Simon Uyttendaele
 */
public class FileReader
{
	private FileReader() { }
	
	/**
	 * Reads a file and return the content
	 * @param	filename	the name and path of the file
	 * @return	the content of the file
	 */
	public static String readFile(String filename) throws IOException
	{
		return readFile(new File(filename));
	}
	
	/**
	 * Reads a file and return the content
	 * @param	file	the target file
	 * @return	the content of the file
	 */
	public static String readFile(File file) throws IOException
	{
		if( file == null || !file.exists() || !file.isFile() || !file.canRead() )
			throw new IOException("Invalid file " + file);
		
		InputStream is = new FileInputStream(file);
		try { return readFile(is); }
		finally { is.close(); }
	}
	
	/**
	 * Reads a stream and return the content. Even though this method is desined for File reads, it will work seamlessly with any <code>InputStream</code>.
	 * @note	<ul><li>Support for binary data is subject to general casting rules from <code>int</code> to <code>char</code> and to 
	 * <code>StringBuffer</code> internal support for binary data.</li>
	 * <li>This method does not close the input stream</li>
	 * @param	is	the input stream
	 * @return	the content of the stream
	 */
	public static String readFile(InputStream is) throws IOException
	{
		if( is == null )
			return null;
		
		StringBuffer buffer = new StringBuffer();
		int c = is.read();
		while(c != -1)
		{
			buffer.append((char) c);
			c = is.read();
		}
		return buffer.toString();
	}
}