package com.anotherservice.io;

import java.lang.*;
import java.io.*;

/**
 * This class provides easy access for writing text file content. Binary content support is not guaranteed.
 *
 * @author  Simon Uyttendaele
 */
public class FileWriter
{
	/**
	 * Writes some text to a file
	 * @param	filename	the name and path of the file
	 * @param	content	the text content to write
	 */
	public static void writeFile(String filename, String content) throws IOException
	{
		writeFile(new FileOutputStream(filename), content);
	}
	
	/**
	 * Writes some text to a file
	 * @param	file	the target file
	 * @param	content	the text content to write
	 */
	public static void writeFile(File file, String content) throws IOException
	{
		if( file == null || !file.isFile() || !file.canWrite() )
			throw new IOException("Invalid file " + file);

		OutputStream os = new FileOutputStream(file);
		try { writeFile(os, content); }
		finally { os.close(); }
	}
	
	/**
	 * Writes some content to an output stream. Even though this method is desined for File writes, it will work seamlessly with any <code>OutputStream</code>.
	 * @note	<ul><li>Support for binary data is subject to general casting rules from <code>int</code> to <code>char</code>.</li>
	 * <li>This method does not close the output stream</li></ul>
	 * @param	os	the output stream
	 * @param	content	the text content to write
	 */
	public static void writeFile(OutputStream os, String content) throws IOException
	{
		os.write(content.getBytes());
		os.flush();
	}
	
	/**
	 * Writes the content of an input stream to a file
	 * @param	filename	the name and path of the file
	 * @param	content	the stream content to write
	 * @note	This method does not close the input stream
	 */
	public static void writeFile(String filename, InputStream content) throws IOException
	{
		writeFile(new FileOutputStream(filename), content);
	}
	
	/**
	 * Writes the content of an input stream to a file
	 * @param	file	the target file
	 * @param	content	the stream content to write
	 * @note	This method does not close the input stream
	 */
	public static void writeFile(File file, InputStream content) throws IOException
	{
		if( file == null )
			throw new IOException("Invalid file " + file);

		OutputStream os = new FileOutputStream(file);
		try { writeFile(os, content); }
		finally { os.close(); }
	}
	
	/**
	 * Writes the content of an input stream to an output stream. Even though this method is desined for File writes, it will work seamlessly with any <code>OutputStream</code>.
	 * @note	<ul><li>Support for binary data is subject to general casting rules from <code>int</code> to <code>char</code>.</li>
	 * <li>This method does not close the output stream</li>
	 * <li>This method does not close the input stream</li></ul>
	 * @param	os	the output stream
	 * @param	content	the stream content to write
	 */
	public static void writeFile(OutputStream os, InputStream content) throws IOException
	{
		int c = content.read();
		while(c != -1)
		{
			os.write(c);
			c = content.read();
		}
		os.flush();
	}
}