package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.io.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.rest.core.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;
import java.io.InputStream;

public class ConnectorConfig extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "config", "configs" });
		index.description = "Manages connector config";
		Handler.addHandler("/busit/connector/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		
		Self.selfize("/busit/connector/config/insert");
		Self.selfize("/busit/connector/config/update");
		Self.selfize("/busit/connector/config/delete");
		Self.selfize("/busit/connector/config/select");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String key = getParameter("key").getValue();
				String value = getParameter("value").getValue();
				String hidden = getParameter("hidden").getValue();
				String user = getParameter("user").getValue();
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserConnector(user, connector) )
						throw new Exception("The current user does not own the provided connector");
				}
				
				if( value == null || value.length() == 0 )
					value = "NULL";
				else
				{
					// caution : we encrypt the config value because it may be confidential
					value = "'" + CryptoSimple.serialize(value) + "'";
				}
					
				if( !connector.matches("^[0-9]+$") )
					connector = "(SELECT connector_id FROM connectors WHERE connector_name = '" + Security.escape(connector) + "')";
				hidden = (hidden != null && hidden.matches("^(1|yes|true)$") ? "true" : "false");
				
				String sql = "INSERT INTO connector_config (connector_id, config_key, config_value, config_hidden) " +
					"VALUES (" + connector + ", '" + Security.escape(key) + "', " + value + ", " + hidden + ")";
				Database.getInstance().insert(sql);
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("key", key);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Insert a config on the connector";
		insert.returnDescription = "{id, key}";
		insert.addGrant(new String[] { "access", "connector_update" });
	
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The connector id";
		id.addAlias(new String[]{ "connector", "id", "connector_id", "cid" });
		insert.addParameter(id);
		
		Parameter key = new Parameter();
		key.isOptional = false;
		key.minLength = 1;
		key.maxLength = 30;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER | PatternBuilder.LOWER);
		key.description = "The config key";
		key.addAlias(new String[]{ "key", "config_key", "name", "config_name" });
		insert.addParameter(key);
		
		Parameter defaultvalue = new Parameter();
		defaultvalue.isOptional = true;
		defaultvalue.minLength = 0;
		defaultvalue.maxLength = 2000;
		defaultvalue.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		defaultvalue.description = "The config default value";
		defaultvalue.addAlias(new String[]{ "defaultvalue", "default", "config_default", "value", "config_value" });
		insert.addParameter(defaultvalue);
		
		Parameter hidden = new Parameter();
		hidden.isOptional = true;
		hidden.minLength = 1;
		hidden.maxLength = 5;
		hidden.mustMatch = "^(1|0|true|false|yes|no)$";
		hidden.description = "Whether or not the configuration is hidden to the user (and thus readonly)";
		hidden.addAlias(new String[]{ "hidden", "hide", "private", "config_hidden" });
		insert.addParameter(hidden);

		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		insert.addParameter(user);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String key = getParameter("key").getValue();
				String value = getParameter("value").getValue();
				String hidden = getParameter("hidden").getValue();
				String user = getParameter("user").getValue();
				
				// update language
				String name = getParameter("name").getValue();
				String description = getParameter("description").getValue();
				String input = getParameter("input").getValue();
				String language = getParameter("language").getValue();
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserConnector(user, connector) )
						throw new Exception("The current user is not an administrator of the provided connector");
				}
				
				if( !connector.matches("^[0-9]+$") )
					connector = "(SELECT connector_id FROM connectors WHERE connector_name = '" + Security.escape(connector) + "')";
				key = "'" + Security.escape(key) + "'";
				
				if( value != null || hidden != null )
				{
					if( value != null )
					{
						if( value.length() == 0 )
							value = "null";
						else
						{
							// caution : we encrypt the config value because it may be confidential
							value = "'" + CryptoSimple.serialize(value) + "'";
						}
					}
					else
						value = "config_value";
						
					if( hidden == null )
						hidden = "config_hidden";
					else
						hidden = (hidden.matches("^(1|yes|true)$") ? "true" : "false");

					Database.getInstance().update("UPDATE connector_config SET config_value = " + value + ", config_hidden = " + hidden +
						" WHERE connector_id = " + connector + " AND config_key = " + key);
				}
				
				// update language
				if( name != null || description != null || input != null )
				{
					if( language == null )
						language = "EN";
					
					// get existing translation ID
					String tid = null;
					Any data = null;
					Map<String, String> result = Database.getInstance().selectOne("SELECT t.translation_id, t.translation_text " +
						"FROM config_translation ct " + 
						"LEFT JOIN translations t ON(ct.translation_id = t.translation_id) " + 
						"WHERE ct.connector_id = " + connector + " AND ct.config_key = " + key + " AND ct.translation_language = '" + language + "'");
						
					if( result != null || result.get("translation_id") != null )
					{
						tid = result.get("translation_id");
						data = Json.decode(result.get("translation_text"));
					}
					
					if( data == null || data.isEmpty() )
					{
						data = Any.empty(); 
						data.put("name", null); data.put("description", null); 
						data.put("input", null);
					}
					
					if( name != null )
						data.put("name", name);
					if( description != null )
						data.put("description", description);
					if( input != null )
						data.put("input", input);
					
					if( tid != null )
					{
						// update existing
						Database.getInstance().update("UPDATE translations SET translation_text = '" + Security.escape(Json.encode(data)) + "' WHERE translation_id = " + tid);
					}
					else
					{
						// insert new
						tid = Database.getInstance().insert("INSERT INTO translations (translation_text) VALUES ('" + Security.escape(Json.encode(data)) + "')").toString();
						
						Database.getInstance().insert("INSERT INTO config_translation (connector_id, config_key, translation_id, translation_language) " + 
							"VALUES (" + connector + ", " + key + ", " + tid + ", '" + language + "')");
					}
				}
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modify a connector config";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "connector_update" });

		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The connector id";
		id.addAlias(new String[]{ "connector", "id", "connector_id", "cid" });
		update.addParameter(id);
	
		Parameter key = new Parameter();
		key.isOptional = false;
		key.minLength = 1;
		key.maxLength = 30;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		key.description = "The config key";
		key.addAlias(new String[]{ "key", "config_key" });
		update.addParameter(key);
		
		Parameter defaultvalue = new Parameter();
		defaultvalue.isOptional = true;
		defaultvalue.minLength = 0;
		defaultvalue.maxLength = 2000;
		defaultvalue.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		defaultvalue.description = "The config default value";
		defaultvalue.addAlias(new String[]{ "defaultvalue", "default", "config_default", "value", "config_value" });
		update.addParameter(defaultvalue);
		
		Parameter hidden = new Parameter();
		hidden.isOptional = true;
		hidden.minLength = 1;
		hidden.maxLength = 5;
		hidden.mustMatch = "^(1|0|true|false|yes|no)$";
		hidden.description = "Whether or not the configuration is hidden to the user (and thus readonly)";
		hidden.addAlias(new String[]{ "hidden", "hide", "private", "config_hidden" });
		update.addParameter(hidden);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 3;
		name.maxLength = 150;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.description = "The config name";
		name.addAlias(new String[]{ "name", "config_name" });
		update.addParameter(name);
		
		Parameter description = new Parameter();
		description.isOptional = true;
		description.minLength = 1;
		description.maxLength = 2000;
		description.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		description.description = "The config description";
		description.addAlias(new String[]{ "description", "config_description" });
		update.addParameter(description);

		Parameter input = new Parameter();
		input.isOptional = true;
		input.minLength = 1;
		input.maxLength = 500;
		input.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		input.description = "The config input";
		input.addAlias(new String[]{ "input", "config_input" });
		update.addParameter(input);
		
		Parameter language = new Parameter();
		language.isOptional = true;
		language.minLength = 2;
		language.maxLength = 2;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "Translation language currently updated. (default EN)";
		language.addAlias(new String[]{ "lang", "translation_language", "language" });
		update.addParameter(language);
		
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
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String key = getParameter("key").getValue();
				String user = getParameter("user").getValue();		
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserConnector(user, connector) )
						throw new Exception("The current user does not own the provided connector");
				}
				if( !connector.matches("^[0-9]+$") )
					connector = "(SELECT connector_id FROM connectors WHERE connector_name = '" + Security.escape(connector) + "')";
				
				Database.getInstance().delete("DELETE cc, t FROM connector_config cc " + 
					"LEFT JOIN config_translation ct ON(cc.connector_id = ct.connector_id AND cc.config_key = ct.config_key) " +
					"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
					"WHERE cc.connector_id = " + connector + " AND cc.config_key = '" + Security.escape(key) + "'");
				
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a connector config";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "connector_update" });
	
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The connector id";
		id.addAlias(new String[]{ "connector", "id", "connector_id", "cid" });
		delete.addParameter(id);
		
		Parameter key = new Parameter();
		key.isOptional = false;
		key.minLength = 1;
		key.maxLength = 30;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		key.description = "The interface key";
		key.addAlias(new String[]{ "key", "config_key", "name", "config_name" });
		delete.addParameter(key);

		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		delete.addParameter(user);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String key = getParameter("key").getValue();
				String user = getParameter("user").getValue();
				
				// include translation
				String language = getParameter("language").getValue();

				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserConnector(user, connector) )
						throw new Exception("The current user does not own the provided connector");
				}
				
				if( !connector.matches("^[0-9]+$") )
					connector = "(SELECT connector_id FROM connectors WHERE connector_name = '" + Security.escape(connector) + "')";
				
				if( key != null )
					key = " AND cc.config_key = '" + Security.escape(key) + "'";
				else
					key = "";
				
				if( language == null )
				{
					Vector<Map<String, String>> t = Database.getInstance().select("SELECT cc.config_key, cc.config_value, cc.config_hidden " + 
						"FROM connector_config cc " + 
						"WHERE cc.connector_id = " + connector + key);

					for( Map<String, String> r : t )
					{
						// caution : decrypt the config value
						if( r.get("config_value") != null )
							r.put("config_value", CryptoSimple.unserialize(r.get("config_value")));
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
						language = " GROUP BY cc.config_key";
						
					Any t = Any.wrap(Database.getInstance().select("SELECT cc.config_key, cc.config_value, cc.config_hidden, t.translation_id, t.translation_text " + 
							"FROM connector_config cc " + 
							"LEFT JOIN config_translation ct ON(ct.connector_id = cc.connector_id AND ct.config_key = cc.config_key) " + 
							"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
							"WHERE cc.connector_id = " + connector + key + language));
					
					if( t != null && t.size() > 0 && !t.get(0).get("translation_id").isNull() )
					{
						// format results
						for( Any r : t )
						{
							if( !r.get("translation_id").isNull() && !r.get("translation_text").isNull() )
								r.putAll(Json.decode(r.<String>value("translation_text")));
							r.remove("translation_text");
							
							// caution : decrypt the config value
							if( !r.get("config_value").isNull() )
								r.put("config_value", CryptoSimple.unserialize(r.<String>value("config_value")));
						}
						
						return t;
					}
				}
				
				// THIS SHOULD NEVER HAPPEN... EXCEPT IF THERE IS NO CONFIG... doh!
				return new Vector<Map<String, String>>();
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Select connector interfaces.";
		select.returnDescription = "The matching interfaces [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "connector_select" });
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The connector id";
		id.addAlias(new String[]{ "connector", "id", "connector_id", "cid" });
		select.addParameter(id);

		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 1;
		key.maxLength = 30;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		key.description = "The config key";
		key.addAlias(new String[]{ "key", "config_key", "name", "config_name" });
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