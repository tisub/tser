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

public class Message implements ISecureMessage
{
	//=====================================
	// IMessage
	//=====================================
	
	private Hashtable<String, InputStream> _files = new Hashtable<String, InputStream>();;
	
	public InputStream file(String name)
	{
		if( name == null || name.length() == 0 || !_files.containsKey(name) )
			return null;
		
		InputStream is = _files.get(name);
		try { is.reset(); } catch(Exception e) {}
		return is;
	}
	
	public void file(String name, InputStream data)
	{
		if( name != null && name.length() > 0 && data != null )
			_files.put(name, data);
	}
	
	public Map<String, InputStream> files()
	{
		// reset all files streams
		for( Map.Entry<String, InputStream> f : _files.entrySet() )
		{
			try { f.getValue().reset(); } catch(Exception e) {}
		}
		
		return _files;
	}
	
	private IContent _content = null;
	
	public IContent content()
	{
		if( _content == null )
			_content = new Content((String)null);
		return _content;
	}
	
	public void content(IContent data)
	{
		_content = data;
	}
	
	public void clear()
	{
		_content = null;
		_files.clear();
	}
	
	public IMessage copy()
	{
		Message m = new Message(fromIdentity(), toIdentity());
		m.content(content());
		for( Map.Entry<String, InputStream> f : files().entrySet() )
			m.file(f.getKey(), f.getValue());
		return m;
	}
	
	public String from()
	{
		return fromIdentity().getSubjectPrincipal().getName();
	}
	
	public String to()
	{
		return toIdentity().getSubjectPrincipal().getName();
	}
	
	//=====================================
	// ISecureMessage
	//=====================================
	
	private Identity _from = null;
	private Identity _to = null;
	
	public IIdentity fromIdentity()
	{
		return _from;
	}
	
	public IIdentity toIdentity()
	{
		return _to;
	}
	
	public String toString()
	{
		return toString(fromIdentity(), toIdentity());
	}
	
	public String toString(IIdentity from)
	{
		return toString(from, toIdentity());
	}
	
	public String toString(IIdentity from, IIdentity to)
	{
		if( from == null || !(from instanceof Identity) )
			throw new IllegalArgumentException("sender identity must be a valid identity");
		if( to == null || !(to instanceof Identity) )
			throw new IllegalArgumentException("reciever identity must be a valid identity");
		
		try
		{
			return Crypto.encrypt((Identity)from, (Identity)to, this.prepareMessage());
		}
		catch(Exception e)
		{
			Logger.finer(e);
			throw new RuntimeException("The message encryption failed");
		}
	}
	
	private byte[] prepareMessage() throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		baos.write(Hex.getBytes(Crypto.contentStart));
		baos.write(Hex.getBytes(Base64.encode(content().toJson())));
		baos.write(Hex.getBytes(Crypto.contentEnd));
		
		for( Map.Entry<String, InputStream> f : files().entrySet() )
		{
			baos.write(Hex.getBytes(Crypto.attachmentStart));
			baos.write(Hex.getBytes(Crypto.filenameStart));
			baos.write(Hex.getBytes(Base64.encode(f.getKey())));
			baos.write(Hex.getBytes(Crypto.filenameEnd));
			InputStream is = f.getValue();
			try { is.reset(); } catch(Exception e) {}
			baos.write(Hex.getBytes(Base64.encode(is)));
			baos.write(Hex.getBytes(Crypto.attachmentEnd));
		}
		
		return baos.toByteArray();
	}
	
	//=====================================
	// CONSTRUCTOR
	//=====================================
	
	public Message(String encrypted)
	{
		try
		{
			// extract the receiver principal
			Principal receiver = Crypto.parsePrincipal(encrypted);
			if( PrincipalUtil.isRemote(receiver) )
				throw new Exception("Cannot verify message that targets a remote identity");
			
			if( IdentityStore.getInstance().getUnchecked().getSubjectPrincipal().equals(receiver) )
				_to = IdentityStore.getInstance().getUnchecked();
			else if( IdentityChecker.getInstance().isUserIdentityOrImpersonate(Security.getInstance().getUser(), receiver.getName()) )
				_to = IdentityStore.getInstance().getIdentity(receiver);
			else
				throw new Exception("You cannot decrypt this message because it targets another identity");

			// decrypt only
			encrypted = Crypto.decrypt_1to4(_to.getPrivateKey(), encrypted);
			
			// extract the origin's principal AND verify signature
			Principal sender = Crypto.decrypt_5to9(encrypted);
			_from = IdentityStore.getInstance().getIdentity(sender);
			
			Matcher m = Crypto.messagePattern.matcher(encrypted);
			if( !m.find() )
				throw new Exception("Missing 'MESSAGE' section");
			encrypted = Hex.toString(Base64.decode(m.group(1)));
			
			// get the message content
			m = Crypto.contentPattern.matcher(encrypted);
			if( m.find() )
				_content = new Content(Hex.toString(Base64.decode(m.group(1))));
			else
				_content = null;

			// get all attachments
			m = Crypto.attachmentPattern.matcher(encrypted);
			while( m.find() )
			{
				String name = Hex.toString(Base64.decode(m.group(1)));
				_files.put(name, new ByteArrayInputStream(Base64.decode(m.group(2))));
			}
		}
		catch(Exception e)
		{
			_from = null;
			_to = null;
			_content = null;
			_files = null;
			
			Logger.finer(e);
			throw new IllegalArgumentException("Cannot decrypt the message");
		}
	}
	
	public Message(IIdentity from, IIdentity to)
	{
		if( from == null || !(from instanceof Identity) )
			throw new IllegalArgumentException("sender identity must be a valid identity");
		if( to == null || !(to instanceof Identity) )
			throw new IllegalArgumentException("reciever identity must be a valid identity");
		
		_from = (Identity)from;
		_to = (Identity)to;
	}
}