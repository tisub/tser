package com.anotherservice.rest.model;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;

public class Confirm extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "confirm", "confirms" });
		index.description = "Manages confirmation codes. Those can be sent by mail to validate an action or else. Those codes are not linked to any other action or user. " +
			"Thus as long as you have a matching id-code pair, this is valid.";
		Handler.addHandler(Initializer.path + "/", index);
		
		initializeInsert(index);
		initializeDelete(index);
		initializeSelect(index);
		initializeCleanup(index);
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String mail = getParameter("mail").getValue();
				String user = getParameter("user").getValue();
				
				if( user == null )
					user = "0";
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";

				Map<String, String> count = Initializer.db.selectOne("SELECT COUNT(*) as c FROM confirm WHERE confirm_email = '" + Security.escape(mail) + "'");
				if( count != null && count.get("c") != null && !count.get("c").equals("0") )
					throw new Exception("This email address is already waiting for confirmation");
				
				// generate a 42 character random string
				String chars = "azertyuiopqsdfghjklmwxcvbn5678904321POIUYTREZAMLKJHGFDSQNBVCXW";
				Random rand = new Random();
				String code = "";
				for( int i = 0; i < 42; i++ )
					code += chars.charAt(rand.nextInt(chars.length()));
				
				Long uid = Initializer.db.insert("INSERT INTO confirm (confirm_code, confirm_date, confirm_email, confirm_user) VALUES ('" + Security.escape(code) + "', UNIX_TIMESTAMP(), '" + Security.escape(mail) + "', " + user + ")");
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				
				// ================================
				// delete old 
				// ================================
				String ttl = "604800"; // 3600*24*7
				Vector<Map<String, String>> confirms = Initializer.db.select("SELECT confirm_email FROM confirm WHERE confirm_date < (UNIX_TIMESTAMP() - " + ttl + ")");
				
				for( Map<String, String> c : confirms )
				{
					Initializer.db.delete("DELETE FROM users WHERE user_mail = '" + c.get("confirm_email") + "'");
					Initializer.db.delete("DELETE FROM confirm WHERE confirm_email = '" + c.get("confirm_email") + "'");
				}
				
				result.put("code", code);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add", "generate", "random" });
		insert.description = "Creates a new confirmation code";
		insert.returnDescription = "The newly created confirmation pair {'id', 'code'}";
		insert.addGrant(new String[] { "access", "confirm_insert" });
		
		Parameter mail = new Parameter();
		mail.isOptional = false;
		mail.minLength = 0;
		mail.maxLength = 150;
		mail.mustMatch = "^[_\\w\\.\\+-]+@[a-zA-Z0-9\\.-]{1,100}\\.[a-zA-Z0-9]{2,6}$";
		mail.description = "The user's email";
		mail.addAlias(new String[]{ "mail", "email", "address", "user_email", "user_mail", "user_address" });
		insert.addParameter(mail);

		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "name", "user_name", "username", "login", "user", "id", "user_id", "uid" });
		insert.addParameter(user);
		
		index.addOwnHandler(insert);
	}

	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String confirm = getParameter("id").getValue();
				
				Initializer.db.delete("DELETE FROM confirm WHERE confirm_id = " + confirm);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a confirmation code";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "confirm_delete" });
		
		Parameter confirm = new Parameter();
		confirm.isOptional = false;
		confirm.minLength = 1;
		confirm.maxLength = 20;
		confirm.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		confirm.allowInUrl = true;
		confirm.description = "The confirmation id";
		confirm.addAlias(new String[]{ "id", "confirm_id", "cid" });
		delete.addParameter(confirm);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String code = getParameter("code").getValue();
				String confirm = getParameter("id").getValue();
				String mail = getParameter("mail").getValue();
				
				if( confirm != null && code != null && mail != null )
					return Initializer.db.select("SELECT confirm_id, confirm_email, confirm_date, confirm_user, confirm_code FROM confirm WHERE confirm_id = " + confirm + " AND confirm_code = '" + Security.escape(code) + "' AND confirm_email = '" + Security.escape(mail) + "'");
				
				if( confirm != null )
					return Initializer.db.select("SELECT confirm_id, confirm_email, confirm_date, confirm_user, confirm_code FROM confirm WHERE confirm_id = " + confirm);
				else if( code != null )
					return Initializer.db.select("SELECT confirm_id, confirm_email, confirm_date, confirm_user, confirm_code FROM confirm WHERE confirm_code = '" + Security.escape(code) + "'");
				else if( mail != null )
					return Initializer.db.select("SELECT confirm_id, confirm_email, confirm_date, confirm_user, confirm_code FROM confirm WHERE confirm_email = '" + Security.escape(mail) + "'");
				else
					throw new Exception("You must at least provide one email or one confirm id or one code.");
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view", "check" });
		select.description = "Checks whether the provided confirmation code and id matches.";
		select.returnDescription = "The confirmation date that matches (or null if it doesnt match) {'date'}";
		select.addGrant(new String[] { "access", "confirm_select" });

		Parameter code = new Parameter();
		code.isOptional = true;
		code.minLength = 42;
		code.maxLength = 42;
		code.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT);
		code.description = "The confirmation code";
		code.addAlias(new String[]{ "code", "confirm_code", "challenge", "cypher" });
		select.addParameter(code);
		
		Parameter confirm = new Parameter();
		confirm.isOptional = true;
		confirm.minLength = 1;
		confirm.maxLength = 20;
		confirm.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		confirm.allowInUrl = true;
		confirm.description = "The confirmation id";
		confirm.addAlias(new String[]{ "id", "cid", "confirm_id" });
		select.addParameter(confirm);
		
		Parameter mail = new Parameter();
		mail.isOptional = true;
		mail.minLength = 0;
		mail.maxLength = 150;
		mail.mustMatch = "^[_\\w\\.\\+-]+@[a-zA-Z0-9\\.-]{1,100}\\.[a-zA-Z0-9]{2,6}$";
		mail.description = "The user's email";
		mail.addAlias(new String[]{ "mail", "email", "address", "user_email", "user_mail", "user_address" });
		select.addParameter(mail);
		
		index.addOwnHandler(select);
	}
	
	private void initializeCleanup(Index index)
	{
		Action cleanup = new Action()
		{
			public Object execute() throws Exception
			{
				String ttl = getParameter("ttl").getValue();
				
				Initializer.db.delete("DELETE FROM confirm WHERE confirm_date < (UNIX_TIMESTAMP() - " + ttl + ")");
				return "OK";
			}
		};
		
		cleanup.addMapping(new String[] { "cleanup" });
		cleanup.description = "Removes all confirmation code that were emitted more than the provided number of seconds ago.";
		cleanup.returnDescription = "OK";
		cleanup.addGrant(new String[] { "access", "confirm_delete" });
		
		Parameter ttl = new Parameter();
		ttl.isOptional = false;
		ttl.minLength = 1;
		ttl.maxLength = 11;
		ttl.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		ttl.description = "The confirmation expiration value";
		ttl.addAlias(new String[]{ "expire", "ttl", "seconds" });
		cleanup.addParameter(ttl);
		
		index.addOwnHandler(cleanup);
	}
}