package com.busit.security;

import sun.security.x509.*;
import sun.security.rsa.*;
import sun.security.util.*;

import java.security.cert.*;
import java.security.interfaces.*;
import java.security.*;
import java.math.BigInteger;
import java.util.Date;
import java.io.*;
import com.anotherservice.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.regex.*;
import java.util.*;
import java.text.*;

public class CryptoSimple
{
	// ===========================
	// STANDARD AES
	// ===========================
	/**
	 * Standard AES encryption
	 * @param key	The symmetric encryption key
	 * @param text	The text to encrypt
	 * @return	The Base64'd AES encrypted text
	 */
	public static String encrypt(String key, String text) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Hex.getBytes(key), "AES"));
		return new String(Base64.encode(cipher.doFinal(Hex.getBytes(text)), Crypto.base64Split));
	}
	
	/**
	 * Standard AES decryption
	 * @param key	The symmetric encryption key
	 * @param text	The Base64'd AES encrypted text
	 * @return	The original text
	 */
	public static String decrypt(String key, String text) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Hex.getBytes(key), "AES"));
		return new String(cipher.doFinal(Base64.decode(text)));
	}
	
	// ===========================
	// ALMOST STANDARD RSA
	// ===========================
	/**
	 * Almost standard RSA encryption
	 * @param from	The origin's <code>PrivateKey</code> or {@link com.busit.security.Principal}
	 * @param to	The destination's <code>PublicKey</code>, {@link com.busit.security.Principal}, or {@link com.busit.security.Certificate}
	 * @param text	The text to encrypt
	 * @return	The Base64'd RSA encrypted text
	 */
	public static String encrypt(String from, String to, String text) throws Exception
	{
		PrivateKey key_from = null;
		if( PrincipalUtil.isPrincipal(from) )
			key_from = IdentityStore.getInstance().getIdentity(from).getPrivateKey();
		else
			key_from = Crypto.parsePrivateKey(from);
		
		PublicKey key_to = null;
		if( PrincipalUtil.isPrincipal(to) )
			key_to = IdentityStore.getInstance().getIdentity(to).getPublicKey();
		else if( Certificate.isCertificate(to) )
			key_to = Crypto.parseCertificate(to).x509().getPublicKey();
		else
			key_to = Crypto.parsePublicKey(to);
		
		return encrypt(key_from, key_to, text);
	}
	
	/**
	 * Almost standard RSA encryption
	 * @param from	The origin's {@link com.busit.security.Identity}
	 * @param to	The destination's {@link com.busit.security.Identity}
	 * @param text	The text to encrypt
	 * @return	The Base64'd RSA encrypted text
	 */
	public static String encrypt(Identity from, Identity to, String text) throws Exception
	{
		return encrypt(from.getPrivateKey(), to.getPublicKey(), text);
	}
	
	/**
	 * Almost standard RSA encryption
	 * @param from	The origin's <code>PrivateKey</code>
	 * @param to	The destination's <code>PublicKey</code>
	 * @param text	The text to encrypt
	 * @return	The Base64'd RSA encrypted text
	 */
	public static String encrypt(PrivateKey from, PublicKey to, String text) throws Exception
	{
		String signature = Crypto.sign(from, text);
		String signedText = Crypto.toString(Hex.getBytes(text)) + signature;
		SecretKey s = Crypto.generateSecretKey();
		String encryptedSignedText = Crypto.encryptSymmetric(s, signedText);
		String encryptedKey = Crypto.encryptAsymmetric(to, Crypto.toString(s));
		return encryptedKey + encryptedSignedText;
	}
	
	/**
	 * Almost standard RSA decryption
	 * @param from	The origin's <code>PublicKey</code>, {@link com.busit.security.Principal}, or {@link com.busit.security.Certificate}
	 * @param to	The destination's <code>PrivateKey</code> or {@link com.busit.security.Principal}
	 * @param text	The Base64'd RSA encrypted text
	 * @return	The original text
	 */
	public static String decrypt(String from, String to,  String text) throws Exception
	{
		PublicKey key_from = null;
		if( PrincipalUtil.isPrincipal(from) )
			key_from = IdentityStore.getInstance().getIdentity(from).getPublicKey();
		else if( Certificate.isCertificate(from) )
			key_from = Crypto.parseCertificate(from).x509().getPublicKey();
		else
			key_from = Crypto.parsePublicKey(from);
		
		PrivateKey key_to = null;
		if( PrincipalUtil.isPrincipal(to) )
			key_to = IdentityStore.getInstance().getIdentity(to).getPrivateKey();
		else
			key_to = Crypto.parsePrivateKey(to);
		
		return decrypt(key_from, key_to, text);
	}
	
	/**
	 * Almost standard RSA decryption
	 * @param from	The origin's {@link com.busit.security.Identity}
	 * @param to	The destination's {@link com.busit.security.Identity}
	 * @param text	The Base64'd RSA encrypted text
	 * @return	The original text
	 */
	public static String decrypt(Identity from, Identity to, String text) throws Exception
	{
		return decrypt(from.getPublicKey(), to.getPrivateKey(), text);
	}
	
	/**
	 * Almost standard RSA decryption
	 * @param from	The origin's <code>PublicKey</code>
	 * @param to	The destination's <code>PrivateKey</code>
	 * @param text	The Base64'd RSA encrypted text
	 * @return	The original text
	 */
	public static String decrypt(PublicKey from, PrivateKey to,  String text) throws Exception
	{
		String rsaSecretKey = Crypto.extractRsaEncryptedPart(text);
		String aesSignedMessage = Crypto.extractAesEncryptedPart(text);
		SecretKey symmetricKey = Crypto.parseSecretKey(Crypto.decryptAsymmetric(to, rsaSecretKey));
		String signedMessage = Crypto.decryptSymmetric(symmetricKey, aesSignedMessage);
		byte[] message = Crypto.parseMessage(Crypto.extractMessagePart(signedMessage));
		String signature = Crypto.extractSignaturePart(signedMessage);
		
		if( !Crypto.verify(from, message, signature) )
			throw new Exception("Signature mismatch. The original message may have been altered.");
		
		return Hex.toString(message);
	}
	
	
	public static String serialize(String text) throws Exception
	{
		return encrypt(IdentityStore.getInstance().getUnchecked(), IdentityStore.getInstance().getAuthority(), text);
	}
	
	public static String unserialize(String text) throws Exception
	{
		return decrypt(IdentityStore.getInstance().getUnchecked(), IdentityStore.getInstance().getAuthority(), text);
	}
	
	// ===========================
	// BUSIT
	// ===========================
	/**
	 * Bus-IT everyone encryption
	 * @param text	The text to encrypt
	 * @return	The Base64'd encrypted text from and to everyone ({@link com.busit.security.IIdentityStore.getUnchecked()})
	 */
	public static String encrypt(String message) throws Exception
	{
		Identity everyone = IdentityStore.getInstance().getUnchecked();
		return Crypto.encrypt(everyone.getPublicKey(), 
			everyone.getSubjectPrincipal(), 
			everyone.getPrivateKey(), 
			everyone.getSubjectPrincipal(), 
			Hex.getBytes(message));
	}
	
	/**
	 * Bus-IT decryption
	 * @param text	The Base64'd encrypted text to decrypt
	 * @return	The original text (whichever from and to)
	 */
	public static String decrypt(String message) throws Exception
	{
		Principal to = Crypto.parsePrincipal(message);
		return Hex.toString(Crypto.decrypt(IdentityStore.getInstance().getIdentity(to).getPrivateKey(), message));
	}
}