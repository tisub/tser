package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;

public class InstanceConfig extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "config", "configs" });
		index.description = "Manages instance config";
		Handler.addHandler("/busit/instance/", index);
		
		initializeUpdate(index);
		initializeSelect(index);
		
		Self.selfize("/busit/instance/config/update");
		Self.selfize("/busit/instance/config/select");
	}
	
	private void initializeUpdate(Index index)
	{
		Action update = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String key = getParameter("key").getValue();
				String value = getParameter("value").getValue();
				String user = getParameter("user").getValue();
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instance) )
						throw new Exception("The current user is not an administrator of the provided instance");
					if( !user.matches("^[0-9]+$") )
						user = "(SELECT user_id FROM users WHERE user_name = '" + Security.escape(user) + "')";
				}
				
				if( value == null || value.length() == 0 )
					value = "NULL";
				else
				{
					// caution : we encrypt the config value because it may be confidential
					value = "'" + CryptoSimple.serialize(value) + "'";
				}
				
				if( !instance.matches("^[0-9]+$") )
				{
					if( user == null )
						throw new Exception("Cannot determine instance: missing user");
					instance = "(SELECT instance_id FROM instances WHERE instance_name = '" + Security.escape(instance) + "' and instance_user = " + user + ")";
				}
				
				Database.getInstance().update("UPDATE instance_config SET config_value = " + value + " WHERE instance_id = " + instance + " AND config_key = '" + Security.escape(key) + "'");
				
				// update children if any
				Vector<Map<String, String>> children = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + instance);
				for( Map<String, String> c : children )
					Database.getInstance().update("UPDATE instance_config SET config_value = " + value + " WHERE instance_id = " + c.get("instance_id") + " AND config_key = '" + Security.escape(key) + "'");
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify" });
		update.description = "Update an instance configuration";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "instance_update" });
	
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The instance id";
		id.addAlias(new String[]{ "instance", "id", "instance_id", "cid" });
		update.addParameter(id);

		Parameter key = new Parameter();
		key.isOptional = false;
		key.minLength = 1;
		key.maxLength = 200;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER);
		key.description = "The instance configuration key";
		key.addAlias(new String[]{ "key", "config_key" });
		update.addParameter(key);
		
		Parameter value = new Parameter();
		value.isOptional = false;
		value.minLength = 0;
		value.maxLength = 5000;
		value.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		value.description = "The instance configuration value";
		value.addAlias(new String[]{ "value", "config_value" });
		update.addParameter(value);

		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		update.addParameter(user);
		
		index.addOwnHandler(update);
	}

	private void initializeSelect(Index index)
	{
		Action select = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String key = getParameter("key").getValue();
				String language = getParameter("language").getValue();
				String user = getParameter("user").getValue();

				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceUser(user, instance) )
						throw new Exception("The current user is not an administrator of the provided instance");
					if( !user.matches("^[0-9]+$") )
						user = "(SELECT user_id FROM users WHERE user_name = '" + Security.escape(user) + "')";
				}
				
				if( !instance.matches("^[0-9]+$") )
				{
					if( user == null )
						throw new Exception("Cannot determine instance: missing user");
					instance = "(SELECT instance_id FROM instances WHERE instance_name = '" + Security.escape(instance) + "' and instance_user = " + user + ")";
				}
				
				if( key != null )
					key = " AND ic.config_key = '" + Security.escape(key) + "'";
				else
					key = "";
				
				if( language == null )
				{
					Vector<Map<String, String>> t = Database.getInstance().select("SELECT cc.config_hidden, ic.config_key, ic.config_value " + 
						"FROM instance_config ic " + 
						"LEFT JOIN connector_config cc ON(cc.connector_id = ic.connector_id AND cc.config_key = ic.config_key) " + 
						"WHERE ic.instance_id = " + instance + key);
					
					for( Map<String, String> r : t )
					{
						// caution : decrypt the config value
						if( r.get("config_value") != null )
							r.put("config_value", CryptoSimple.unserialize(r.get("config_value")));
						if( r.get("config_hidden") != null )
							r.put("config_hidden", r.get("config_hidden"));
					}
					
					return t;
				}
				
				for( int attempt = 0; attempt < 3; attempt++ )
				{
					// FIRST attempt : with provided language
					if( attempt == 0 )
						language = " AND ct.translation_language = '" + language + "'";
					// SECOND attempt : with EN
					else if( attempt == 1 )
						language = " AND ct.translation_language = 'EN'";
					// THIRD attempt : any (including null)
					else
						language = " GROUP BY ic.config_key";
						
					Any t = Any.wrap(Database.getInstance().select("SELECT cc.config_hidden, ic.config_key, ic.config_value, t.translation_id, t.translation_text " + 
							"FROM instance_config ic " + 
							"LEFT JOIN connector_config cc ON(cc.connector_id = ic.connector_id AND cc.config_key = ic.config_key) " + 
							"LEFT JOIN config_translation ct ON(ct.connector_id = ic.connector_id AND ct.config_key = ic.config_key) " + 
							"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
							"WHERE ic.instance_id = " + instance + key + language));
					
					if( t != null && t.size() > 0 && !t.get(0).get("translation_id").isNull() )
					{
						// format results
						Any results = Any.empty();
						for( Any r : t )
						{
							if( !r.get("translation_id").isNull() && !r.get("translation_text").isNull() )
								r.putAll(Json.decode(r.<String>value("translation_text")));
							r.remove("translation_text");
							
							// caution : decrypt the config value
							if( !r.get("config_value").isNull() )
								r.put("config_value", CryptoSimple.unserialize(r.<String>value("config_value")));

							if( !r.get("config_hidden").isNull() )
								r.put("config_hidden", r.get("config_hidden"));

							results.put(r.<String>value("config_key"), r);
						}

						return results;
					}
				}
				
				// THIS SHOULD NEVER HAPPEN... EXCEPT IF THERE IS NO CONFIG... doh!
				return new Vector<Map<String, String>>();
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Select instance interfaces.";
		select.returnDescription = "The matching interfaces [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "instance_select" });
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The instance id";
		id.addAlias(new String[]{ "instance", "id", "instance_id", "iid" });
		select.addParameter(id);

		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 1;
		key.maxLength = 30;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		key.description = "The config key";
		key.addAlias(new String[]{ "key", "config_key" });
		select.addParameter(key);
		
		Parameter language = new Parameter();
		language.isOptional = true;
		language.minLength = 2;
		language.maxLength = 2;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "Translation language currently updated.";
		language.addAlias(new String[]{ "lang", "translation_language", "language" });
		select.addParameter(language);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		select.addParameter(user);
		
		index.addOwnHandler(select);	
	}
}
