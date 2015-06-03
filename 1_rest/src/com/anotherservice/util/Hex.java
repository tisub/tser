package com.anotherservice.util;

import java.io.*;

public class Hex
{
	public static String utf8ize(String text)
	{
		// if string is not UTF-8 but contains UTF-8 characters, then convert it to UTF-8 instead
		try
		{
			for (int i = 0; i < text.length(); i++)
			{
				int codepoint = text.codePointAt(i);
				if( codepoint >= 0xc2 && codepoint <= 0xff )
				{
					if( i+1 < text.length() )
					{
						codepoint = text.codePointAt(i+1);
						if( codepoint >= 0x80 && codepoint <= 0xbf )
						{
							text = new String(text.getBytes("ISO-8859-1"), "UTF-8");
							break;
						}
					}
				}
			}
		}
		catch(java.io.UnsupportedEncodingException uee)
		{
			// conversion failed... too bad
		}
		
		return text;
	}
	
	public static byte[] getBytes(String text)
	{
		try
		{
			return utf8ize(text).getBytes("UTF-8");
		}
		catch(UnsupportedEncodingException uee)
		{
			char[] chars = text.toCharArray();
			byte[] bytes = new byte[chars.length];
			for( int i = 0; i < chars.length; i++ )
				bytes[i] = (byte)(chars[i] & 0xFF);
		
			return bytes;
		}
	}
	
	public static byte[] getBytes(InputStream is)
	{
		try
		{
			int n;
			byte[] buffer = new byte[16384];
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			while( (n = is.read(buffer, 0, buffer.length)) != -1)
				baos.write(buffer, 0, n);
			baos.flush();

			return baos.toByteArray();
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	public static String toString(byte[] bytes)
	{
		try
		{
			return utf8ize(new String(bytes, "UTF-8"));
		}
		catch(UnsupportedEncodingException uee)
		{
			StringBuffer s = new StringBuffer(bytes.length);
			for ( int i = 0; i < bytes.length; i++ )
				s.append((char)(bytes[i] & 0xFF));
			return s.toString();
		}
	}
	
	public static String toString(char[] chars)
	{
		StringBuffer s = new StringBuffer(chars.length);
		for ( int i = 0; i < chars.length; i++ )
			s.append((char)chars[i]);
		return s.toString();
	}
	
	public static String toString(InputStream is)
	{
		return toString(getBytes(is));
	}
	
	private static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	public static String toHexString(String text)
	{
		return toHexString(getBytes(text));
	}
	
	public static String toHexString(byte[] bytes)
	{
		StringBuffer s = new StringBuffer(bytes.length * 5);
		int hex;
		for ( int j = 0; j < bytes.length; j++ )
		{
			s.append("0x");
			hex = bytes[j] & 0xFF;
			s.append(hexArray[hex >>> 4]);
			s.append(hexArray[hex & 0x0F]);
			s.append(' ');
		}
		return s.substring(0, s.length()-1);
	}
	
	public static String toHexString(char[] chars)
	{
		StringBuffer s = new StringBuffer(chars.length * 5);
		int hex;
		for ( int j = 0; j < chars.length; j++ )
		{
			s.append("0x");
			hex = ((byte)chars[j]) & 0xFF;
			s.append(hexArray[hex >>> 4]);
			s.append(hexArray[hex & 0x0F]);
			s.append(' ');
		}
		return s.substring(0, s.length()-1);
	}
	
	public static char toChar(String hex)
	{
		return (char) (Integer.parseInt(hex.replaceFirst("^0x", "").replaceFirst("^[^0-9a-fA-F]*", ""), 16) & 0xFF);
	}
}