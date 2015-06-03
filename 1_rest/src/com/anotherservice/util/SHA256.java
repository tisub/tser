package com.anotherservice.util;

import java.lang.*;
import java.security.*;

/**
 * This class is a shorthand to compute a SHA256 hex-string.
 *
 * @author  Simon Uyttendaele
 */
public class SHA256
{
	private SHA256() {}
	
	/**
	 * Returns the SHA256 hash of the input string.
	 * @param	input	the input string
	 * @return	the SHA256 hexadecimal string (64 hex characters) <strong>in lowercase</strong>
	 * @note	the input string is interpreted as UTF-8
	 */
	public static String hash(String input) throws Exception
	{
		return hash(Hex.getBytes(input));
	}
	
	/**
	 * Returns the SHA256 hash of the input bytes.
	 * @param	input	the input bytes
	 * @return	the SHA256 hexadecimal string (64 hex characters) <strong>in lowercase</strong>
	 */
	public static String hash(byte[] input) throws Exception
	{
		byte[] h = MessageDigest.getInstance("SHA-256").digest(input);
		
		StringBuilder sha256 = new StringBuilder(64);
		for( int i = 0; i < h.length; i++ )
		{
			String hex = Integer.toHexString(0xFF & h[i]);
			if( hex.length() == 1 )
				sha256.append('0');
			sha256.append(hex);
		}
		
		return sha256.toString().toLowerCase();
	}
}