package com.busit.security;

import com.busit.*;
import java.util.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import com.anotherservice.io.*;
import com.anotherservice.util.*;
import java.io.ByteArrayOutputStream;
import java.util.regex.*;
import java.security.Principal;
import com.anotherservice.rest.security.*;

// TODO : 
// add the possibility to encapsulate the original message in this message
// like a "forward" -> manage like a special attachment ?

public interface ISecureMessage extends IMessage
{
	/**
	 * Get the identity of the sender of this message
	 * @return	the sender identity
	 */
	public IIdentity fromIdentity();
	
	/**
	 * Get the identity of the receiver of this message
	 * @return the receiver identity
	 */
	public IIdentity toIdentity();
	
	// =========================================
	// SERIALIZE
	// =========================================
	
	/**
	 * This method calls {@link #toString(String, String)} with the destination identity set to {@link #toIdentity()}.
	 * @param	from	The identity of the author of the message used for the digital signature
	 * @return	The message content encrypted for anyone and signed by the <code>from</code> identity
	 * @throws	RuntimeException if encryption error occurs
	 * @see		#toString(String, String)
	 */
	public String toString(IIdentity from);
	
	/**
	 * This method returns the message encrypted and signed
	 * @param	from	The identity of the author of the message used for the digital signature. In any case, this parameter cannot be <code>null</code>.
	 * @param	to		The identity of the destination of the message used for encryption. If <code>null</code>, then anyone can decrypt it.
	 * @return	The message content encrypted for the <code>to</code> identity and signed by the <code>from</code> identity
	 * @throws	RuntimeException if encryption error occurs
	 */
	public String toString(IIdentity from, IIdentity to);
	
	/**
	 * This method calls {@link #toString(String, String)} with the origin identity set to {@link #fromIdentity()}
	 * and the destination identity set to {@link #toIdentity()}.
	 * @return	The message content encrypted for anyone and signed by the <code>from</code> identity
	 * @throws	RuntimeException if encryption error occurs
	 * @see		#toString(String, String)
	 */
	public String toString();
}