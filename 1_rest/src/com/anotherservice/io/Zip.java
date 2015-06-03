package com.anotherservice.io;

import java.util.zip.*;
import java.util.*;
import java.io.*;

/**
 * This class provides easy access to ZIP file content with internal in-memory representation.
 * One can easily add or remove files from an archive, (un)zip archives, merge archives,...
 * @note	Since all work is performed in-memory, the size of the contained files is constrained to the memory limits
 * @author  Simon Uyttendaele
 */
public class Zip
{
	/**
	 * Unzips a file from an archive
	 * @param	zip	the archive
	 * @param	file	which file in the archive
	 * @return	the file content unzipped
	 */
	public static byte[] extract(String zip, String file) throws IOException
	{
		return extract(new ZipFile(zip), new ZipEntry(file));
	}
	
	/**
	 * Unzips a file from an archive
	 * @param	zip	the archive
	 * @param	file	which file in the archive
	 * @return	the file content unzipped
	 */
	public static byte[] extract(String zip, ZipEntry file) throws IOException
	{
		return extract(new ZipFile(zip), file);
	}
	
	/**
	 * Unzips a file from an archive
	 * @param	zip	the archive
	 * @param	file	which file in the archive
	 * @return	the file content unzipped
	 */
	public static byte[] extract(File zip, String file) throws IOException
	{
		return extract(new ZipFile(zip), new ZipEntry(file));
	}
	
	/**
	 * Unzips a file from an archive
	 * @param	zip	the archive
	 * @param	file	which file in the archive
	 * @return	the file content unzipped
	 */
	public static byte[] extract(File zip, ZipEntry file) throws IOException
	{
		return extract(new ZipFile(zip), file);
	}
	
	/**
	 * Unzips a file from an archive
	 * @param	zip	the archive
	 * @param	file	which file in the archive
	 * @return	the file content unzipped
	 */
	public static byte[] extract(ZipFile zip, String file) throws IOException
	{
		return extract(zip, new ZipEntry(file));
	}
	
	/**
	 * Unzips a file from an archive
	 * @param	zip	the archive
	 * @param	file	which file in the archive
	 * @return	the file content unzipped
	 */
	public static byte[] extract(ZipFile zip, ZipEntry file) throws IOException
	{
		InputStream is = zip.getInputStream(file);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int b = is.read();
		while( b > -1 )
		{
			baos.write(b);
			b = is.read();
		}
		return baos.toByteArray();
	}
	
	private Hashtable<String, byte[]> entries = new Hashtable<String, byte[]>();
	
	/**
	 * Default constructor
	 */
	public Zip()
	{
	}
	
	/**
	 * Constructor with a zip file to preload
	 */
	public Zip(String zipfile) throws IOException
	{
		importFrom(zipfile);
	}
	
	/**
	 * Constructor with a zip file to preload
	 */
	public Zip(File zipfile) throws IOException
	{
		importFrom(zipfile);
	}
	
	/**
	 * Constructor with a zip stream to preload
	 */
	public Zip(InputStream zipcontent) throws IOException
	{
		importFrom(zipcontent);
	}
	
	/**
	 * Gets the number of entries (files) in the zip archive represented by this instance
	 * @return	the number of entries
	 */
	public int size()
	{
		return this.entries.size();
	}
	
	/**
	 * Loads and merges all files in the provided archive
	 * @param	zipfile	the archive
	 */
	public void importFrom(String zipfile) throws IOException
	{
		importFrom(new FileInputStream(zipfile));
	}
	
	/**
	 * Loads and merges the target file from the provided archive
	 * @param	zipfile	the archive
	 * @param	entry	the file to extract
	 */
	public void importFrom(String zipfile, String entry) throws IOException
	{
		this.entries.put(entry, extract(zipfile, entry));
	}
	
	/**
	 * Loads and merges all files in the provided archive
	 * @param	zipfile	the archive
	 */
	public void importFrom(File zipfile) throws IOException
	{
		importFrom(new FileInputStream(zipfile));
	}
	
	/**
	 * Loads and merges the target file from the provided archive
	 * @param	zipfile	the archive
	 * @param	entry	the file to extract
	 */
	public void importFrom(File zipfile, String entry) throws IOException
	{
		this.entries.put(entry, extract(zipfile, entry));
	}
	
	/**
	 * Loads and merges all files in the provided zip stream
	 * @param	zipInput	the zip stream
	 */
	public void importFrom(InputStream zipInput) throws IOException
	{
		ZipInputStream zis = new ZipInputStream(zipInput);
		ZipEntry ze = zis.getNextEntry();
		
		while( ze != null )
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int b = zis.read();
			while( b > -1 )
			{
				baos.write(b);
				b = zis.read();
			}
			this.entries.put(ze.getName(), baos.toByteArray());
			ze = zis.getNextEntry();
		}
	}
	
	/**
	 * Loads and merges the target file from the provided zip stream
	 * @param	zipInput	the zip stream
	 * @param	entry	the file to extract
	 */
	public void importFrom(InputStream zipInput, String entry) throws IOException
	{
		ZipInputStream zis = new ZipInputStream(zipInput);
		ZipEntry ze = zis.getNextEntry();
		
		while( ze != null )
		{
			if( !ze.getName().equals(entry) )
			{
				ze = zis.getNextEntry();
				continue;
			}
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int b = zis.read();
			while( b > -1 )
			{
				baos.write(b);
				b = zis.read();
			}
			this.entries.put(ze.getName(), baos.toByteArray());
			ze = zis.getNextEntry();
		}
	}
	
	/**
	 * Zips and saves all files in this representation
	 * @param	zipfile	the destination archive
	 */
	public void exportTo(String zipfile) throws IOException
	{
		exportTo(new FileOutputStream(zipfile));
	}
	
	/**
	 * Zips and saves all files in this representation
	 * @param	zipfile	the destination archive
	 */
	public void exportTo(File zipfile) throws IOException
	{
		exportTo(new FileOutputStream(zipfile));
	}
	
	/**
	 * Zips and saves all files in this representation
	 * @param	zipOutput	the destination zip stream
	 */
	public void exportTo(OutputStream zipOutput) throws IOException
	{
		ZipOutputStream zo = new ZipOutputStream(zipOutput);

		for( String key : this.entries.keySet() )
		{
			ZipEntry ze = new ZipEntry(key);
			ze.setMethod(ZipEntry.DEFLATED);
			
			zo.putNextEntry(ze);
			byte[] entry = this.entries.get(key);
			zo.write(entry, 0, entry.length);
		}
		
		zo.finish();
	}
	
	/**
	 * Zips and saves all files in this representation
	 * @return	the destination zip as an input stream
	 */
	public InputStream export() throws IOException
	{
		ByteStream bs = new ByteStream();
		ZipOutputStream zo = new ZipOutputStream(bs);

		for( String key : this.entries.keySet() )
		{
			ZipEntry ze = new ZipEntry(key);
			ze.setMethod(ZipEntry.DEFLATED);
			
			zo.putNextEntry(ze);
			byte[] entry = this.entries.get(key);
			zo.write(entry, 0, entry.length);
		}
		
		zo.finish();
		
		return bs.getInputStream();
	}
	
	/**
	 * Removes the target file from this in-memory representation
	 * @param	entry	the file to remove
	 */
	public void remove(String entry)
	{
		this.entries.remove(entry);
	}
	
	/**
	 * Loads a plain file in the target entry of this in-memory representation
	 * @param	entry	the entry to load the file to
	 * @param	file	the file to load
	 */
	public void put(String entry, String file) throws IOException
	{
		put(entry, new FileInputStream(file));
	}
	
	/**
	 * Loads a plain file in this in-memory representation
	 * @param	file	the file to load
	 */
	public void put(String file) throws IOException
	{
		put(file, new FileInputStream(file));
	}
	
	/**
	 * Loads a plain file in the target entry of this in-memory representation
	 * @param	entry	the entry to load the file to
	 * @param	file	the file to load
	 */
	public void put(String entry, File file) throws IOException
	{
		put(entry, new FileInputStream(file));
	}
	
	/**
	 * Loads a plain file in this in-memory representation
	 * @param	file	the file to load
	 */
	public void put(File file) throws IOException
	{
		put(file.getName(), new FileInputStream(file));
	}
	
	/**
	 * Loads a stream in the target entry of this in-memory representation
	 * @param	entry	the entry to load the file to
	 * @param	content	the stream to load
	 */
	public void put(String entry, InputStream content) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int b = content.read();
		while( b > -1 )
		{
			baos.write(b);
			b = content.read();
		}
		
		this.entries.put(entry, baos.toByteArray());
	}
	
	/**
	 * Returns the content of an internal file entry
	 * @param	entry	the name of the internal file entry
	 * @return	the file content
	 */
	public byte[] get(String entry)
	{
		return this.entries.get(entry);
	}
	
	/**
	 * Removes all file entries from this in-memory representation
	 */
	public void clear()
	{
		this.entries.clear();
	}
}