package com.anotherservice.io;

import java.net.*;
import java.io.*;
import java.util.*;
import com.anotherservice.util.*;

/**
 * This class provides easy access to make HTTP requests and read the response from the server.
 *
 * @author  Simon Uyttendaele
 */
public class UrlReader
{
	private static byte[] readStream(InputStream in) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data = new byte[16384];
		int read = 0;
		while( (read = in.read(data)) != -1 )
			baos.write(data, 0, read);
		
		return baos.toByteArray();
	}
	
	/**
	 * Performs an HTTP GET request and reads the response
	 * @param	url	the complete URL
	 * @return	the body of the server response
	 */
	public static String readUrl(String url) throws IOException, MalformedURLException
	{
		return readUrl(new URL(url));
	}
	
	/**
	 * Performs an HTTP GET request and reads the response
	 * @param	url	the complete URL
	 * @return	the body of the server response
	 */
	public static String readUrl(URL url) throws IOException
	{
		BufferedReader in = null;
		
		try
		{
			URLConnection uc = url.openConnection();
			return Hex.toString(readStream(uc.getInputStream()));
		}
		finally
		{
			try { if( in != null ) in.close(); } catch(Exception e) { }
		}
	}
	
	/**
	 * Performs an HTTP POST request and reads the response
	 * @param	url	the complete URL
	 * @param	post	the post content
	 * @return	the body of the server response
	 */
	public static String readUrl(String url, String post) throws IOException
	{
		return readUrl(new URL(url), post);
	}
	
	/**
	 * Performs an HTTP POST request and reads the response
	 * @param	url	the complete URL
	 * @param	post	the post content
	 * @return	the body of the server response
	 */
	public static String readUrl(URL url, String post) throws IOException
	{
		OutputStreamWriter out = null;
		BufferedReader in = null;
		
		try
		{
			URLConnection uc = url.openConnection();
			
			// send post
			uc.setDoOutput(true);
			out = new OutputStreamWriter(uc.getOutputStream());
			out.write(post);
			out.flush();
			
			// read response
			return Hex.toString(readStream(uc.getInputStream()));
		}
		finally
		{
			try { if( out != null ) out.close(); } catch(Exception e) { }
			try { if( in != null ) in.close(); } catch(Exception e) { }
		}
	}
	
	/**
	 * Performs an HTTP POST request and reads the response
	 * @param	url	the complete URL
	 * @param	post	the post content
	 * @return	the body of the server response
	 */
	public static String readUrl(URL url, Map post) throws IOException
	{
		String data = "";
		for( Object key : post.keySet() )
			data += "&" + URLEncoder.encode(key.toString(), "UTF-8") + "=" + URLEncoder.encode(post.get(key).toString(), "UTF-8");
		
		return readUrl(url, data);
	}
	
	/**
	 * Performs an HTTP POST request and reads the response
	 * @param	url	the complete URL
	 * @param	post	the post content
	 * @return	the body of the server response
	 */
	public static String readUrl(String url, Map post) throws IOException
	{
		return readUrl(new URL(url), post);
	}
	
	/**
	 * Performs an HTTP POST request and joins a file upload
	 * @param	url	the complete URL
	 * @param	post	the post content
	 * @param	file	the file content
	 * @param	filename	the name of the file
	 * @return	the body of the server response
	 */
	public static String readUrl(String url, Map post, byte[] file, String filename) throws IOException
	{
		OutputStream out = null;
		BufferedReader in = null;
		
		try
		{
			String param = "value";
			String boundary = Long.toHexString(System.currentTimeMillis());
			
			URLConnection uc = new URL(url).openConnection();
			uc.setDoOutput(true);
			uc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			
			out = uc.getOutputStream();
			
			// send post
			if( post != null )
			{
				for( Object key : post.keySet() )
				{
					out.write(("--" + boundary + "\r\n" + 
						"Content-Disposition: form-data; name=\"" + key.toString() + "\"\r\n" + 
						"Content-Type: text/plain; charset=UTF-8\r\n\r\n" + 
						post.get(key).toString() + "\r\n").getBytes());
					out.flush();
				}
			}
				
			// send file
			if( file != null )
			{
				out.write(("--" + boundary + "\r\n" + 
					"Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" + 
					"Content-Type: " + URLConnection.guessContentTypeFromName(filename) + "\r\n" + 
					"Content-Transfer-Encoding: binary\r\n\r\n").getBytes());
				out.write(file);
				out.write(("\r\n").getBytes());
				out.flush();
			}
			
			out.write(("--" + boundary + "--\r\n").getBytes());
			out.flush();
			
			// read response
			return Hex.toString(readStream(uc.getInputStream()));
		}
		finally
		{
			try { if( out != null ) out.close(); } catch(Exception e) { }
			try { if( in != null ) in.close(); } catch(Exception e) { }
		}
	}
}