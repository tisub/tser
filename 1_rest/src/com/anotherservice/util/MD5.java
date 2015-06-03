package com.anotherservice.util;

import java.lang.*;
import java.security.*;

/**
 * This class is a shorthand to compute a MD5 hex-string.
 *
 * @author  Simon Uyttendaele
 */
public class MD5
{
	private MD5() {}
	
	/**
	 * Returns the MD5 hash of the input string.
	 * @param	input	the input string
	 * @return	the MD5 hexadecimal string (32 hex characters) <strong>in lowercase</strong>
	 * @note	the input string is interpreted as UTF-8
	 */
	public static String hash(String input) throws Exception
	{
		return hash(Hex.getBytes(input));
	}
	
	/**
	 * Returns the MD5 hash of the input bytes.
	 * @param	input	the input bytes
	 * @return	the MD5 hexadecimal string (32 hex characters) <strong>in lowercase</strong>
	 */
	public static String hash(byte[] input) throws Exception
	{
		byte[] h = MessageDigest.getInstance("MD5").digest(input);
		
		StringBuilder md5 = new StringBuilder(32);
		for( int i = 0; i < h.length; i++ )
		{
			String hex = Integer.toHexString(0xFF & h[i]);
			if( hex.length() == 1 )
				md5.append('0');
			md5.append(hex);
		}
		
		return md5.toString().toLowerCase();
	}
}