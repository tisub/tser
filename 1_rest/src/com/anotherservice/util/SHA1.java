package com.anotherservice.util;

import java.lang.*;
import java.security.*;

/**
 * This class is a shorthand to compute a SHA1 hex-string.
 *
 * @author  Simon Uyttendaele
 */
public class SHA1
{
	private SHA1() {}
	
	/**
	 * Returns the SHA1 hash of the input string.
	 * @param	input	the input string
	 * @return	the SHA1 hexadecimal string (40 hex characters) <strong>in lowercase</strong>
	 * @note	the input string is interpreted as UTF-8
	 */
	public static String hash(String input) throws Exception
	{
		return hash(Hex.getBytes(input));
	}
	
	/**
	 * Returns the SHA1 hash of the input bytes.
	 * @param	input	the input bytes
	 * @return	the SHA1 hexadecimal string (40 hex characters) <strong>in lowercase</strong>
	 */
	public static String hash(byte[] input) throws Exception
	{
		byte[] h = MessageDigest.getInstance("SHA-1").digest(input);
		
		StringBuilder sha1 = new StringBuilder(40);
		for( int i = 0; i < h.length; i++ )
		{
			String hex = Integer.toHexString(0xFF & h[i]);
			if( hex.length() == 1 )
				sha1.append('0');
			sha1.append(hex);
		}
		
		return sha1.toString().toLowerCase();
	}
}