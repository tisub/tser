package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.core.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.db.*;
import com.anotherservice.io.Zip;
import java.util.*;
import com.busit.routing.*;
import com.busit.security.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.security.cert.*;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Timestamp;

public class Message extends InitializerOnce
{
	private IBrokerClient broker = null;
	
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "message", "messages" });
		index.description = "Send, encrypt or decrypt messages";
		Handler.addHandler("/busit/", index);
		
		initializeEncrypt(index);
		initializeDecrypt(index);
		initializeSend(index);
		initializeCron(index);
		initializeForward(index);
		
		try
		{
			broker = (IBrokerClient) Class.forName(Config.gets("com.busit.broker.class")).newInstance();
		}
		catch(Exception e)
		{
			Logger.severe(e);
			Logger.severe("Impossible to create broker instance");
		}
		
		Self.selfize("/busit/message/send");
		Self.selfize("/busit/message/cron");
	}
	
	private void initializeEncrypt(Index index)
	{
		Action encrypt = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String sender = getParameter("sender").getValue();
				String receiver = getParameter("receiver").getValue();
				String message = getParameter("message").getValue();
				String binary = getParameter("binary").getValue();
				
				if( !IdentityChecker.getInstance().isUserIdentityOrImpersonate(Security.getInstance().getUser(), sender) )
					throw new Exception("You cannot send messages on behalf of the specified identity");
				
				com.busit.security.Identity rcv = IdentityStore.getInstance().getUnchecked();
				if( receiver != null )
					rcv = IdentityStore.getInstance().getIdentity(receiver);
				com.busit.security.Identity snd = IdentityStore.getInstance().getIdentity(sender);
				
				if( snd == null || rcv == null )
					throw new Exception("Unknown identity");
					
				try
				{
					com.busit.security.Message m = new com.busit.security.Message(snd, rcv);
					m.content(new Content(message));
					
					for( String name : Request.getAttachmentNames() )
						m.file(name, Request.getAttachment(name));
					
					if( binary != null && binary.matches("^(?i)(yes|true|1)$") )
					{
						// add the file name in the request
						Request.addAction("message.bin");
						Request.getAction();
				
						return new ByteArrayInputStream(Hex.getBytes(m.toString()));
					}
					else
					{
						Hashtable result = new Hashtable();
						result.put("message", m.toString());
						return result;
					}
				}
				catch(Exception e)
				{
					Logger.finer(e);
					throw new Exception("Cannot encrypt the message");
				}
			}
		};
		
		encrypt.addMapping(new String[] { "encrypt", "create" });
		encrypt.description = "Encrypts and signs a new message";
		encrypt.returnDescription = "The encrypted message";
		encrypt.addGrant(new String[] { "access" });
		
		Parameter sender = new Parameter();
		sender.isOptional = false;
		sender.minLength = 1;
		sender.maxLength = 100;
		sender.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		sender.allowInUrl = true;
		sender.description = "The identity name, id or principal of the sender of the message";
		sender.addAlias(new String[]{ "sender", "sender_name", "sender_id", "sender_principal", "origin", "origin_name", "origin_id", "origin_principal" });
		encrypt.addParameter(sender);
		
		Parameter receiver = new Parameter();
		receiver.isOptional = true;
		receiver.minLength = 1;
		receiver.maxLength = 100;
		receiver.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		receiver.description = "The identity name, id or principal of the receiver of the message. Only the receiver will be able to decrypt the message. If not specified, 'everyone' will be the default receiver";
		receiver.addAlias(new String[]{ "receiver", "receiver_name", "receiver_id", "receiver_principal", "destination", "destination_name", "destination_id", "destination_principal" });
		encrypt.addParameter(receiver);
		
		Parameter message = new Parameter();
		message.isOptional = false;
		message.minLength = 1;
		message.maxLength = 4*1024*1024*1024; // 4GB
		message.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		message.description = "The message to encrypt";
		message.addAlias(new String[]{ "message", "text", "content" });
		encrypt.addParameter(message);
		
		Parameter binary = new Parameter();
		binary.isOptional = true;
		binary.minLength = 1;
		binary.maxLength = 5;
		binary.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		binary.description = "Whether or not to send the result as a binary file";
		binary.addAlias(new String[]{ "binary", "raw" });
		encrypt.addParameter(binary);
		
		index.addOwnHandler(encrypt);
	}
	
	private void initializeDecrypt(Index index)
	{
		Action decrypt = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String message = getParameter("message").getValue();
				String binary = getParameter("binary").getValue();
				
				try
				{
					com.busit.security.Message m = new com.busit.security.Message(message);
					
					if( binary != null && binary.matches("^(?i)(yes|true|1)$") )
					{
						Zip zip = new Zip();
						zip.put("content.txt", m.content().toText());
						zip.put("content.html", m.content().toHtml());
						for( Map.Entry<String, InputStream> f : m.files().entrySet() )
							zip.put(f.getKey(), f.getValue());
						zip.put("author.crt", new ByteArrayInputStream(m.fromIdentity().certificate().getBytes()));

						Request.addAction("message.zip");
						Request.getAction();
				
						return zip.export();
					}
					else
					{
						Hashtable author = new Hashtable();
						author.put("principal", m.fromIdentity().getSubjectPrincipal().getName());
						author.put("name", m.fromIdentity().getSubjectName());
						author.put("guarantor", m.fromIdentity().getIssuerPrincipal().getName());
						
						Hashtable files = new Hashtable();
						for( Map.Entry<String, InputStream> f : m.files().entrySet() )
							files.put(f.getKey(), f.getValue());
						
						Hashtable result = new Hashtable();
						result.put("content", m.content());
						result.put("files", files);
						result.put("author", author);
						return result;
					}
				}
				catch(Exception e)
				{
					Logger.finer(e);
					throw new Exception("Cannot decrypt the message");
				}
			}
		};
		
		decrypt.addMapping(new String[] { "decrypt", "read", "extract", "verify" });
		decrypt.description = "Decrypts and checks the signature of a message";
		decrypt.returnDescription = "The decrypted message";
		//decrypt.addGrant(new String[] { "access" });
		
		Parameter message = new Parameter();
		message.isOptional = false;
		message.minLength = 1;
		message.maxLength = 5*1024*1024; // 5MB
		message.mustMatch = "^" + Crypto.encryptedPattern + "$";
		message.description = "The message to decrypt";
		message.addAlias(new String[]{ "message", "text", "content" });
		decrypt.addParameter(message);
		
		Parameter binary = new Parameter();
		binary.isOptional = true;
		binary.minLength = 1;
		binary.maxLength = 5;
		binary.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		binary.description = "Whether or not to send the result as a binary file";
		binary.addAlias(new String[]{ "binary", "raw" });
		decrypt.addParameter(binary);
		
		index.addOwnHandler(decrypt);
	}
	
	private void initializeSend(Index index)
	{
		Action send = new Action()
		{
			public Object execute() throws Exception
			{
				String sender = getParameter("sender").getValue();
				String receiver = getParameter("receiver").getValue();
				String message = getParameter("message").getValue();
				String connector = getParameter("connector").getValue();
				String user = getParameter("user").getValue();
				
				if( sender != null && !Security.getInstance().hasGrant(Config.gets("com.busit.rest.admin.grant")) )
				{
					if( !IdentityChecker.getInstance().isUserIdentityOrImpersonate(Security.getInstance().getUser(), sender) )
						throw new Exception("You cannot send messages on behalf of the specified identity");
				}
				if( user != null && !Security.getInstance().hasGrant(Config.gets("com.busit.rest.admin.grant"))
					&& !user.equals(Security.getInstance().getUser()) 
					&& !user.equals(""+Security.getInstance().userId()) )
					throw new Exception("You cannot send messages on behalf of the specified user");
				
				com.busit.security.Identity rcv = IdentityStore.getInstance().getUnchecked();
				if( receiver != null )
					rcv = IdentityStore.getInstance().getIdentity(receiver);
				com.busit.security.Identity snd = null;
				if( sender != null )
					snd = IdentityStore.getInstance().getIdentity(sender);
				else
					snd = IdentityChecker.getInstance().defaultIdentity(user);
				
				if( snd == null || rcv == null )
					throw new Exception("Unknown identity");
					
				try
				{
					com.busit.security.Message m = new com.busit.security.Message(snd, rcv);
					m.content(new Content(message));
					
					for( String name : Request.getAttachmentNames() )
						m.file(name, Request.getAttachment(name));
					
					Step next = INTERNAL_NEXT_STEP(null, connector, true);
					INTERNAL_FORWARD(m, new Path(), next, true, false);
					return "OK";
				}
				catch(InsufficientCreditException ice)
				{
					throw ice;
				}
				catch(Exception e)
				{
					Logger.severe(e);
					throw new Exception("Error sending message");
				}
			}
		};
		
		send.addMapping(new String[] { "send", "push" });
		send.description = "Encrypts, signs and sends a new message to the system";
		send.returnDescription = "OK";
		send.addGrant(new String[] { "access" });
		
		Parameter sender = new Parameter();
		sender.isOptional = true;
		sender.minLength = 1;
		sender.maxLength = 100;
		sender.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		sender.allowInUrl = true;
		sender.description = "The identity name, id or principal of the sender of the message";
		sender.addAlias(new String[]{ "from", "sender" });
		send.addParameter(sender);
		
		Parameter receiver = new Parameter();
		receiver.isOptional = true;
		receiver.minLength = 1;
		receiver.maxLength = 100;
		receiver.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		receiver.description = "The identity name, id or principal of the receiver of the message. Only the receiver will be able to decrypt the message. If not specified, 'everyone' will be the default receiver";
		receiver.addAlias(new String[]{ "to", "receiver" });
		send.addParameter(receiver);
		
		Parameter connector = new Parameter();
		connector.isOptional = false;
		connector.minLength = 1;
		connector.maxLength = 300;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "\\+");
		connector.description = "The connector instance name or id followed by the input name or id";
		connector.addAlias(new String[]{ "destination", "connector", "instance" });
		send.addParameter(connector);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		send.addParameter(user);
		
		Parameter message = new Parameter();
		message.isOptional = false;
		message.minLength = 1;
		message.maxLength = 4*1024*1024*1024; // 4GB
		message.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		message.description = "The message to send";
		message.addAlias(new String[]{ "message", "text", "content" });
		send.addParameter(message);
		
		index.addOwnHandler(send);
	}
	
	private void initializeCron(Index index)
	{
		Action cron = new Action()
		{
			public Object execute() throws Exception
			{
				String sender = getParameter("sender").getValue();
				String receiver = getParameter("receiver").getValue();
				String connector = getParameter("connector").getValue();
				String user = getParameter("user").getValue();
				
				if( sender != null && !Security.getInstance().hasGrant(Config.gets("com.busit.rest.admin.grant")) )
				{
					if( !IdentityChecker.getInstance().isUserIdentityOrImpersonate(Security.getInstance().getUser(), sender) )
						throw new Exception("You cannot send messages on behalf of the specified identity");
				}
				if( user != null && !Security.getInstance().hasGrant(Config.gets("com.busit.rest.admin.grant"))
					&& !user.equals(Security.getInstance().getUser()) 
					&& !user.equals(""+Security.getInstance().userId()) )
					throw new Exception("You cannot send messages on behalf of the specified user");
				
				com.busit.security.Identity rcv = IdentityStore.getInstance().getUnchecked();
				if( receiver != null )
					rcv = IdentityStore.getInstance().getIdentity(receiver);
				com.busit.security.Identity snd = null;
				if( sender != null )
					snd = IdentityStore.getInstance().getIdentity(sender);
				else
					snd = IdentityChecker.getInstance().defaultIdentity(user);
				
				if( snd == null || rcv == null )
					throw new Exception("Unknown identity");
					
				try
				{
					com.busit.security.Message m = new com.busit.security.Message(snd, rcv);
					m.content(new Content(Config.gets("com.busit.rest.cron")));
					Step next = null;
					
					// cron output
					try { next = INTERNAL_NEXT_STEP(null, connector, false); }
					// cron input
					catch(Exception e) { next = INTERNAL_NEXT_STEP(null, connector, true); }
					
					INTERNAL_FORWARD(m, new Path(), next, false, true);
					return "OK";
				}
				catch(InsufficientCreditException ice)
				{
					throw ice;
				}
				catch(Exception e)
				{
					Logger.finer(e);
					throw new Exception("Error sending message");
				}
			}
		};
		
		cron.addMapping(new String[] { "cron" });
		cron.description = "Sends a new cron signal to the system";
		cron.returnDescription = "OK";
		cron.addGrant(new String[] { "access" });
		
		Parameter sender = new Parameter();
		sender.isOptional = true;
		sender.minLength = 1;
		sender.maxLength = 100;
		sender.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		sender.allowInUrl = true;
		sender.description = "The identity name, id or principal of the sender of the message";
		sender.addAlias(new String[]{ "from", "sender" });
		cron.addParameter(sender);
		
		Parameter receiver = new Parameter();
		receiver.isOptional = true;
		receiver.minLength = 1;
		receiver.maxLength = 100;
		receiver.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		receiver.description = "The identity name, id or principal of the receiver of the message. Only the receiver will be able to decrypt the message. If not specified, 'everyone' will be the default receiver";
		receiver.addAlias(new String[]{ "to", "receiver" });
		cron.addParameter(receiver);
		
		Parameter connector = new Parameter();
		connector.isOptional = false;
		connector.minLength = 1;
		connector.maxLength = 300;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "\\+");
		connector.description = "The connector instance name or id followed by the output name or id";
		connector.addAlias(new String[]{ "destination", "connector", "instance" });
		cron.addParameter(connector);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		cron.addParameter(user);
		
		index.addOwnHandler(cron);
	}
	
	private void initializeForward(Index index)
	{
		Action forward = new Action()
		{
			public Object execute() throws Exception
			{
				String path = getParameter("path").getValue();
				String message = getParameter("message").getValue();
				String step = getParameter("step").getValue();
				
				Step next = INTERNAL_NEXT_STEP(step, null, true);
				Path previous = new Path(path);
				INTERNAL_FORWARD(new com.busit.security.Message(message), previous, next, true, false);
				return "OK";
			}
		};
		
		forward.addMapping(new String[] { "forward" });
		forward.description = "Forwards a message to the next entity";
		forward.returnDescription = "OK";
		forward.addGrant(new String[] { Config.gets("com.busit.rest.brokerGrantName") });
		
		Parameter path = new Parameter();
		path.isOptional = false;
		path.minLength = 1;
		path.maxLength = 5000;
		path.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		path.description = "The historical path of the message in JSON format";
		path.addAlias(new String[]{ "path", "history" });
		forward.addParameter(path);
		
		Parameter step = new Parameter();
		step.isOptional = false;
		step.minLength = 1;
		step.maxLength = 800;
		step.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		step.description = "The next step in JSON format";
		step.addAlias(new String[]{ "step", "next", "destination" });
		forward.addParameter(step);
		
		Parameter message = new Parameter();
		message.isOptional = false;
		message.minLength = 1;
		message.maxLength = 4*1024*1024*1024; // 4GB
		message.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		message.description = "The message to forward";
		message.addAlias(new String[]{ "message", "text", "content" });
		forward.addParameter(message);
		
		index.addOwnHandler(forward);
	}
	
	private Step INTERNAL_NEXT_STEP(String step, String destination, boolean input) throws Exception
	{
		Step next;
		if( step != null && step.length() > 0 )
			next = Step.uncheckedParse(step);
		else
			next = new Step();
		
		if( destination != null && destination.length() > 0 )
		{
			// destination format is : [USER+]INSTANCE+INTERFACE@WHATEVER

			destination = destination.replaceFirst("@.*$", "");
			String[] parts = destination.split("\\+");
			if( parts.length < 2 || parts.length > 3 )
				throw new Exception("Invalid destination : " + destination);
			
			if( parts.length == 2 )
			{
				// two parts so the first MUST be an ID
				if( !parts[0].matches("^[0-9]+$") )
					throw new Exception("Invalid destination");
				next.toInstance = parts[0];
				next.toInterface = parts[1];
			}
			else
			{
				// three parts
				if( !parts[0].matches("^[0-9]+$") ) // user name
					parts[0] = "(SELECT user_id FROM users WHERE user_name LIKE '" + Security.escape(parts[0]) + "')";

				if( !parts[1].matches("^[0-9]+$") ) // instance name
					parts[1] = "instance_name LIKE '" + Security.escape(parts[1]) + "'";
				else // instance id
					parts[1] = "instance_id = " + parts[1];
				
				Map<String, String> row = Database.getInstance().selectOne("SELECT instance_id FROM instances WHERE instance_user = " + parts[0] + " AND " + parts[1]);
				if( row == null || row.get("instance_id") == null )
					throw new Exception("Invalid destination");

				next.toInstance = row.get("instance_id");
				next.toInterface = parts[2];
			}
		}

		for( Map<String, String> i : InstanceInterface.INTERNAL_SELECT(next.toInstance, (input?"input":"output"), null, next.toInterface) )
		{
			if( next.toInterface.equals(i.get("interface_name")) )
				return next;
		}

		throw new Exception("Invalid destination (no interface found)");
	}
	
	private void INTERNAL_FORWARD(com.busit.security.Message m, Path path, Step next, boolean log, boolean cron) throws InsufficientCreditException, Exception
	{
		String message = m.toString();
		
		// Select instance user
		Map<String, String> row = Database.getInstance().selectOne("SELECT instance_user FROM instances WHERE instance_id = " + next.toInstance);
		if( row == null || row.get("instance_user") == null )
			throw new Exception("Invalid instance");
		String instance_user = row.get("instance_user");
		
		// Select plan
		row = Database.getInstance().selectOne("SELECT plan_user, plan_window, plan_factor, plan_root, plan_free, plan_sla FROM credit_plan WHERE plan_user = " + instance_user);
		String sla = row.get("plan_sla");
		
		// Select connector
		row = Database.getInstance().selectOne("SELECT connector_id, connector_use_price, connector_use_tax, connector_user, connector_language, connector_local FROM instances " +
			"LEFT JOIN connectors ON(connector_id = instance_connector) " + 
			"WHERE instance_id = " + next.toInstance);
		if( row == null || row.get("connector_user") == null )
			throw new Exception("Invalid connector");
		String connector_user = row.get("connector_user");
		String connector_id = row.get("connector_id");
		String language = row.get("connector_language");
		String local = row.get("connector_local");
		
		next.costPrice = Integer.parseInt(row.get("connector_use_price"));
		next.costTax = Integer.parseInt(row.get("connector_use_tax"));
		next.sentTime = new java.util.Date().getTime();
		next.inputSize = message.length();
		next.inputHash = MD5.hash(message);
		
		// generate message ID
		if( path.size() > 0 )
			next.identifier =  path.getLast().identifier;
		else
			next.identifier = SHA1.hash(message + instance_user + connector_user + next.sentTime);
		
		if( next.fromInstance != null )
		{
			// check if the previous step was for another user -> then it is a share and we should add the share price
			row = Database.getInstance().selectOne("SELECT instance_user FROM instances WHERE instance_id = " + next.fromInstance);
			if( row == null || row.get("instance_user") == null )
				throw new Exception("Invalid instance");
			String previous_instance_user = row.get("instance_user");
		
			if( !instance_user.equals(previous_instance_user) )
			{
				next.costShare = InstanceInterfaceShared.INTERNAL_SHAREPRICE(
					next.fromInstance, next.fromInterface, previous_instance_user, // FROM
					next.toInstance, next.toInterface, instance_user // TO
				);
				try
				{
					next.shareTransaction = "" + Credit.INTERNAL_HOLD_SHARE(instance_user, previous_instance_user, next.costShare);
				}
				catch(Exception e)
				{
					Database.getInstance().insert("INSERT INTO instance_error (error_instance, error_interface, error_date, error_code, error_message) VALUES('" + next.toInstance + "', '" + next.toInterface + "', UNIX_TIMESTAMP(), '509', 'InsufficientCreditException')");
					Logger.finest(e);
					throw new InsufficientCreditException(e);
				}
			}
		}
		
		try
		{
			if( cron )
				next.transaction = Config.gets("com.busit.rest.cron");
			else
			{
				next.transaction = "" + Credit.INTERNAL_HOLD_MESSAGE(instance_user, connector_user, next.costPrice, next.costTax, connector_id);
				row = Database.getInstance().selectOne("SELECT transaction_count, transaction_qos, transaction_size FROM credit_transaction WHERE transaction_id = " + next.transaction);
				next.costCount = Integer.parseInt(row.get("transaction_count"));
				next.costSize = Integer.parseInt(row.get("transaction_size"));
				next.costQos = Integer.parseInt(row.get("transaction_qos"));
			}
		}
		catch(Exception e)
		{
			Database.getInstance().insert("INSERT INTO instance_error (error_instance, error_interface, error_date, error_code, error_message) VALUES('" + next.toInstance + "', '" + next.toInterface + "', UNIX_TIMESTAMP(), '509', 'InsufficientCreditException')");
			Logger.finest(e);
			if( next.shareTransaction != null )
				Credit.INTERNAL_REFUND(next.shareTransaction);
			throw new InsufficientCreditException(e);
		}
		
		try
		{
			Path p = new Path();
			p.add(next);

			Hashtable<String, Object> forward = new Hashtable<String, Object>();
			forward.put("message", message);
			forward.put("path", Json.decode(p.toString()));
			forward.put("language", language);
			
			if( log == true )
			{
				int cost = next.costPrice + next.costTax + next.costShare;
				Database.getInstance().update("UPDATE instances SET instance_hits = instance_hits+1 WHERE instance_id = " + next.toInstance);
			}
			
			// transmit to RabbitMQ
			this.broker.transmit(Json.encode(forward), instance_user, language, local, sla);
			this.broker.trace(instance_user, p, m.fromIdentity(), m.toIdentity(), language, cron);
		}
		catch(Exception e)
		{
			// TODO : Log error in RabbitMQ
			
			Logger.finest(e);
			if( next.shareTransaction != null )
				Credit.INTERNAL_REFUND(next.shareTransaction);
			Credit.INTERNAL_REFUND(next.transaction);
			throw new Exception("Operation failed", e);
		}
	}
	
	private class InsufficientCreditException extends Exception
	{
		public InsufficientCreditException() { super(); }
		public InsufficientCreditException(Throwable cause) { super(cause); }
		public InsufficientCreditException(String message) { super(message); }
		
		public String getMessage() { return "Insufficient credit"; }
		public String toString() { return getMessage(); }
	}
}