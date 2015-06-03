package com.anotherservice.io;

import java.lang.*;
import java.io.*;

/**
 * This class provides easy access for <strong>recursively</strong> deleting and copying directories.
 *
 * @author  Simon Uyttendaele
 */
public class FileUtil
{
	/**
	 * Removes a file or directory
	 * @param	filename	the file or directory to delete
	 */
	public static void delete(String filename) throws IOException
	{
		delete(new File(filename));
	}
	
	/**
	 * Removes a file or directory
	 * @param	file	the file or directory to delete
	 */
	public static void delete(File file) throws IOException
	{
		if( !file.exists() )
			return;

		if( file.isDirectory() )
			for( File f : file.listFiles() )
				delete(f);
		
		file.delete();
	}
	
	/**
	 * Copies a file or directory
	 * @param	from	the source file or directory
	 * @param	to	the destination file or directory
	 * @see #copy(File, File)
	 */
	public static void copy(String from, String to) throws IOException
	{
		copy(new File(from), new File(to));
	}
	
	/**
	 * Copies a file or directory
	 * @param	from	the source file or directory
	 * @param	to	the destination file or directory
	 * @see #copy(File, File)
	 */
	public static void copy(String from, File to) throws IOException
	{
		copy(new File(from), to);
	}
	
	/**
	 * Copies a file or directory
	 * @param	from	the source file or directory
	 * @param	to	the destination file or directory
	 * @see #copy(File, File)
	 */
	public static void copy(File from, String to) throws IOException
	{
		copy(from, new File(to));
	}
	
	/**
	 * Copies a file or directory
	 * @param	from	the source file or directory
	 * @param	to	the destination file or directory
	 * @throws	IOException	if the source file does not exist
	 * @note	If the source is a directory, all files and sub-directories are recursively copied.
	 * <br />If the source is a file and the destination is a directory, the file is copied into the directory.
	 * <br />If the destination is an existing file and the source is a directory, the operating system will most likely throw an <code>IOException</code>
	 */
	public static void copy(File from, File to) throws IOException
	{
		if( !from.exists() )
			throw new IOException("Input file does not exist");

		if( !to.exists() )
		{
			if( from.isFile() )
				to.createNewFile();
			else
				to.mkdirs();
		}
		
		if( from.isFile() )
		{
			if( to.isFile() )
			{
				FileInputStream fis = new FileInputStream(from);
				FileOutputStream fos = new FileOutputStream(to);
				int b = fis.read();
				while(b > -1)
				{
					fos.write(b);
					b = fis.read();
				}
				fos.close();
				fis.close();
			}
			else
				copy(from, new File(to.getPath() + File.separator + from.getName()));
		}
		else
		{
			for( File f : from.listFiles() )
				copy(f, new File(to.getPath() + File.separator + f.getName()));
		}
	}
}