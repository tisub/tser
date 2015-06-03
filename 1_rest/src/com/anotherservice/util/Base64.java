package com.anotherservice.util;

import java.io.InputStream;

public class Base64
{
	// http://tools.ietf.org/html/rfc4648

	// ENCODE CHARACTER MAP
	private static char[] map1 = new char[64];
	static
	{
		int i=0;
		for( char c = 'A'; c <= 'Z'; c++ ) map1[i++] = c;
		for( char c = 'a'; c <= 'z'; c++ ) map1[i++] = c;
		for( char c = '0'; c <= '9'; c++ ) map1[i++] = c;
		map1[i++] = '+'; // the norm says that 'base64' is '+' but 'base64url' is '-' (for safe url or filename)
		map1[i++] = '/'; // the norm says that 'base64' is '/' but 'base64url' is '_' (for safe url or filename)
	}

	// DECODE CHARACTER MAP
	private static byte[] map2 = new byte[128];
	static
	{
		for( int i = 0; i < map2.length; i++ ) map2[i] = -1;
		for( int i = 0; i < 64; i++ ) map2[map1[i]] = (byte)i;
	}

	public static String encode(String s) throws Exception
	{
		return new String(encode(Hex.getBytes(s)));
	}
	
	public static String encode(String s, int split) throws Exception
	{
		return new String(encode(Hex.getBytes(s), split));
	}
	
	public static String encode(InputStream input) throws Exception
	{
		int avail = input.available();
		int dataLength = (avail * 4 + 2) / 3;
		int outputLength = ((avail + 2) / 3) * 4;
		char[] output = new char[outputLength];
		int inputIndex = 0;
		int outputIndex = 0;
		
		int i0, i1, i2;
		while(inputIndex < avail)
		{
			i0 = input.read() & 0xff; inputIndex++;
			i1 = (inputIndex < avail ? input.read() & 0xff : 0); inputIndex++;
			i2 = (inputIndex < avail ? input.read() & 0xff : 0); inputIndex++;
			output[outputIndex++] = map1[i0 >>> 2];
			output[outputIndex++] = map1[((i0 &   3) << 4) | (i1 >>> 4)];
			output[outputIndex] = (outputIndex < dataLength ? map1[((i1 & 0xf) << 2) | (i2 >>> 6)] : '=');
			outputIndex++;
			output[outputIndex] = (outputIndex < dataLength ? map1[i2 & 0x3F] : '=');
			outputIndex++;
		}
		
		return new String(output);
	}

	public static char[] encode(byte[] input)
	{
		int dataLength = (input.length * 4 + 2) / 3;
		int outputLength = ((input.length + 2) / 3) * 4;
		char[] output = new char[outputLength];
		int inputIndex = 0;
		int outputIndex = 0;
		
		int i0, i1, i2;
		while(inputIndex < input.length)
		{
			i0 = input[inputIndex++] & 0xff;
			i1 = (inputIndex < input.length ? input[inputIndex++] & 0xff : 0);
			i2 = (inputIndex < input.length ? input[inputIndex++] & 0xff : 0);
			output[outputIndex++] = map1[i0 >>> 2];
			output[outputIndex++] = map1[((i0 &   3) << 4) | (i1 >>> 4)];
			output[outputIndex] = (outputIndex < dataLength ? map1[((i1 & 0xf) << 2) | (i2 >>> 6)] : '=');
			outputIndex++;
			output[outputIndex] = (outputIndex < dataLength ? map1[i2 & 0x3F] : '=');
			outputIndex++;
		}
		
		return output;
	}
	
	public static char[] encode(byte[] input, int split)
	{
		char[] b64 = encode(input);
		char[] b64s = new char[b64.length + (b64.length / split)];
		
		int j = 0;
		for( int i = 0; i < b64.length; i++ )
		{
			b64s[j++] = b64[i];
			if( (i+1) % split == 0 )
				b64s[j++] = '\n';
		}
		
		return b64s;
	}

	public static byte[] decode(String s)
	{
		return decode(s.replaceAll("(?s)\\s", "").toCharArray());
	}

	public static byte[] decode (char[] input)
	{
		int inputLength = input.length;
		
		if( inputLength % 4 != 0 )
			throw new IllegalArgumentException ("Length of Base64 encoded input string is not a multiple of 4.");

		while( inputLength > 0 && input[inputLength-1] == '=' )
			inputLength--;

		int outputLength = (inputLength * 3) / 4;
		byte[] output = new byte[outputLength];
		int inputIndex = 0;
		int outputIndex = 0;
		
		int b0, b1, b2, b3, o0, o1, o2;
		while (inputIndex < inputLength)
		{
			b0 = map2[input[inputIndex++]];
			b1 = map2[input[inputIndex++]];
			b2 = map2[(inputIndex < inputLength ? input[inputIndex++] : 'A')];
			b3 = map2[(inputIndex < inputLength ? input[inputIndex++] : 'A')];
			
			//if( b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0 )
			//	throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
			
			o0 = ( b0       <<2) | (b1>>>4);
			o1 = ((b1 & 0xf)<<4) | (b2>>>2);
			o2 = ((b2 &   3)<<6) |  b3;
			output[outputIndex++] = (byte)o0;
			
			if( outputIndex < outputLength ) output[outputIndex++] = (byte)o1;
			if( outputIndex < outputLength ) output[outputIndex++] = (byte)o2;
		}
		
		return output;
	}
}
