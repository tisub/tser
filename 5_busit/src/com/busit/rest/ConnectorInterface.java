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

public class ConnectorInterface extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "interface", "interfaces" });
		index.description = "Manages connector interfaces";
		Handler.addHandler("/busit/connector/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		
		Self.selfize("/busit/connector/interface/insert");
		Self.selfize("/busit/connector/interface/update");
		Self.selfize("/busit/connector/interface/delete");
		Self.selfize("/busit/connector/interface/select");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String type = getParameter("type").getValue();
				String tag = getParameter("tag").getValue();
				String key = getParameter("key").getValue();
				String dynamic = getParameter("dynamic").getValue();
				String cron = getParameter("cron").getValue();
				String cascade = getParameter("cascade").getValue();
				String user = getParameter("user").getValue();

				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserConnector(user, connector) )
						throw new Exception("The current user does not own the provided connector");
				}
				
				if( !connector.matches("^[0-9]+$") )
					connector = "(SELECT connector_id FROM connectors WHERE connector_name = '" + Security.escape(connector) + "')";
				dynamic = (dynamic != null && dynamic.matches("^(1|yes|true)$") ? "1" : "0");
				cron = (cron != null && cron.matches("^(1|yes|true)$") ? "1" : "0");
				type = (type != null && type.matches("^(1|in|input)$") ? "1" : "0");
				
				String cronTimer = null;
				if( cron.equals("1") )
					cronTimer = "'E-E-E-E-E'";
				else
					cronTimer = "NULL";
				
				if( tag == null )
					tag = "producer";
				
				String sql = "INSERT INTO connector_interface (connector_id, interface_key, interface_type, interface_tag, interface_dynamic, interface_cron, interface_cron_timer, interface_wkt) " +
					"VALUES (" + connector + ", '" + Security.escape(key) + "', " + type + ", '" + tag + "', " + dynamic + ", " + cron + ", " + cronTimer + ", NULL)";
				Database.getInstance().insert(sql);
				
				if( cascade != null && cascade.matches("^(1|yes|true)$") && Security.getInstance().hasGrant(Config.gets("com.busit.rest.admin.grant")) )
				{
					sql = "INSERT IGNORE INTO instance_interface (instance_id, connector_id, interface_key, interface_name, interface_cron_timer) " +
						"SELECT instance_id, instance_connector, '" + Security.escape(key) + "', '" + Security.escape(key) + "', " + cronTimer + " " +
						"FROM instances WHERE instance_connector = " + connector;
					Database.getInstance().insert(sql);
				}
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("key", key);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Insert an interface on the connector";
		insert.returnDescription = "OK";
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

		Parameter type = new Parameter();
		type.isOptional = false;
		type.minLength = 1;
		type.maxLength = 6;
		type.mustMatch = "^(?i)(1|in|input|0|out|output)$";
		type.description = "The interface type (input or output)";
		type.addAlias(new String[]{ "type", "interface_type" });
		insert.addParameter(type);
		
		Parameter tag = new Parameter();
		tag.isOptional = true;
		tag.minLength = 1;
		tag.maxLength = 30;
		tag.mustMatch = "^(?i)(producer|consumer|transformer)$";
		tag.description = "Interface tag?";
		tag.addAlias(new String[]{ "tag", "interface_tag" });
		insert.addParameter(tag);
		
		Parameter key = new Parameter();
		key.isOptional = false;
		key.minLength = 1;
		key.maxLength = 30;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		key.description = "The interface key";
		key.addAlias(new String[]{ "key", "interface_key" });
		insert.addParameter(key);

		Parameter dynamic = new Parameter();
		dynamic.isOptional = true;
		dynamic.minLength = 1;
		dynamic.maxLength = 5;
		dynamic.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		dynamic.description = "Whether or not is this interface dynamic?";
		dynamic.addAlias(new String[]{ "dynamic", "dynamic" });
		insert.addParameter(dynamic);
		
		Parameter cron = new Parameter();
		cron.isOptional = true;
		cron.minLength = 1;
		cron.maxLength = 5;
		cron.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		cron.description = "Whether or not is this interface cronable?";
		cron.addAlias(new String[]{ "cron", "interface_cron" });
		insert.addParameter(cron);
		
		Parameter cascade = new Parameter();
		cascade.isOptional = true;
		cascade.minLength = 1;
		cascade.maxLength = 5;
		cascade.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		cascade.description = "Whether or not to propagate this interface to already created instances. (default false)";
		cascade.addAlias(new String[]{ "cascade", "propagate" });
		insert.addParameter(cascade);

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
				String tag = getParameter("tag").getValue();
				String cron = getParameter("cron").getValue();
				String hidden = getParameter("hidden").getValue();
				String dynamic = getParameter("dynamic").getValue();
				String type = getParameter("type").getValue();
				String user = getParameter("user").getValue();
				Collection<String> wkt = getParameter("wkt").getValues();
				
				// update language
				String name = getParameter("name").getValue();
				String description = getParameter("description").getValue();
				String help = getParameter("help").getValue();
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
				
				if( type != null || cron != null || dynamic != null || wkt != null )
				{
					if( dynamic != null )
						dynamic = (dynamic.matches("^(1|yes|true)$") ? "true" : "false");
					else
						dynamic = "interface_dynamic";
					
					String cronTimer = null;
					Boolean changeCron = false;
					if( cron != null )
					{
						changeCron = true;
						cron = (cron.matches("^(1|yes|true)$") ? "true" : "false");
						if( cron.equals("true") )
							cronTimer = "'E-E-E-E-E'";
						else
							cronTimer = "NULL";
					}
					else
					{
						cron = "interface_cron";
						cronTimer = "interface_cron_timer";
					}

					if( hidden != null )
						hidden = (hidden.matches("^(1|yes|true)$") ? "true" : "false");
					else
						hidden = "interface_hidden";
						
					if( type != null )
						type = (type.matches("^(1|in|input)$") ? "1" : "0");
					else
						type = "interface_type";
					
					if( tag == null )
						tag = "interface_tag";
					else
						tag = "'" + tag + "'";
					
					String wkts = "interface_wkt";
					if( wkt != null )
						wkts = "'" + Json.encode(wkt) + "'";
				
					Database.getInstance().update("UPDATE connector_interface SET interface_type = " + type + ", interface_dynamic = " + dynamic + 
						", interface_cron = " + cron + 
						", interface_cron_timer = " + cronTimer +
						", interface_hidden = " + hidden + 
						", interface_wkt = " + wkts + 
						", interface_tag = " + tag +
						" WHERE connector_id = " + connector + " AND interface_key = " + key);
					
					if( changeCron == true )
					{
						// change instances interface
						Database.getInstance().update("UPDATE instance_interface SET interface_cron_timer = " + cronTimer +
							" WHERE connector_id = " + connector + " AND interface_key = " + key);
					}
				}
				
				// update language
				if( name != null || description != null || help != null || input != null )
				{
					if( language == null )
						language = "EN";
					
					// get existing translation ID
					String tid = null;
					Any data = null;
					Map<String, String> result = Database.getInstance().selectOne("SELECT t.translation_id, t.translation_text " +
						"FROM interface_translation it " + 
						"LEFT JOIN translations t ON(it.translation_id = t.translation_id) " + 
						"WHERE it.connector_id = " + connector + " AND it.interface_key = " + key + " AND it.translation_language = '" + language + "'");
						
					if( result != null || result.get("translation_id") != null )
					{
						tid = result.get("translation_id");
						data = Json.decode(result.get("translation_text"));
					}
					
					if( data == null || data.isEmpty() )
					{
						data = Any.empty(); 
						data.put("name", null); data.put("description", null); 
						data.put("help", null); data.put("input", null);
					}
					
					if( name != null )
						data.put("name", name);
					if( description != null )
						data.put("description", description);
					if( input != null )
						data.put("input", input);
					if( help != null )
						data.put("help", help);
					
					if( tid != null )
					{
						// update existing
						Database.getInstance().update("UPDATE translations SET translation_text = '" + Security.escape(Json.encode(data)) + "' WHERE translation_id = " + tid);
					}
					else
					{
						// insert new
						tid = Database.getInstance().insert("INSERT INTO translations (translation_text) VALUES ('" + Security.escape(Json.encode(data)) + "')").toString();
						
						Database.getInstance().insert("INSERT INTO interface_translation (connector_id, interface_key, translation_id, translation_language) " + 
							"VALUES (" + connector + ", " + key + ", " + tid + ", '" + language + "')");
					}
				}
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modify a connector interface";
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
	
		Parameter type = new Parameter();
		type.isOptional = true;
		type.minLength = 1;
		type.maxLength = 6;
		type.mustMatch = "^(?i)(1|in|input|0|out|output)$";
		type.description = "The interface type (input or output)";
		type.addAlias(new String[]{ "type", "interface_type" });
		update.addParameter(type);

		Parameter key = new Parameter();
		key.isOptional = false;
		key.minLength = 1;
		key.maxLength = 30;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		key.description = "The interface key";
		key.addAlias(new String[]{ "key", "interface_key" });
		update.addParameter(key);
		
		Parameter dynamic = new Parameter();
		dynamic.isOptional = true;
		dynamic.minLength = 1;
		dynamic.maxLength = 5;
		dynamic.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		dynamic.description = "Whether or not is this interface dynamic?";
		dynamic.addAlias(new String[]{ "dynamic", "dynamic" });
		update.addParameter(dynamic);
		
		Parameter cron = new Parameter();
		cron.isOptional = true;
		cron.minLength = 1;
		cron.maxLength = 5;
		cron.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		cron.description = "Whether or not is this interface cronable?";
		cron.addAlias(new String[]{ "cron", "interface_cron" });
		update.addParameter(cron);

		Parameter hidden = new Parameter();
		hidden.isOptional = true;
		hidden.minLength = 1;
		hidden.maxLength = 5;
		hidden.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		hidden.description = "Whether or not is this interface hidden?";
		hidden.addAlias(new String[]{ "hidden", "interface_hidden" });
		update.addParameter(hidden);
		
		Parameter tag = new Parameter();
		tag.isOptional = true;
		tag.minLength = 1;
		tag.maxLength = 30;
		tag.mustMatch = "^(?i)(producer|consumer|transformer)$";
		tag.description = "Interface tag?";
		tag.addAlias(new String[]{ "tag", "interface_tag" });
		update.addParameter(tag);
		
		Parameter wkt = new Parameter();
		wkt.isOptional = true;
		wkt.minLength = 1;
		wkt.maxLength = 8;
		wkt.isMultipleValues = true;
		wkt.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		wkt.description = "The list of compatible WKTs";
		wkt.addAlias(new String[]{ "wkt", "accepted_wkt", "input_wkt", "output_wkt", "declared_wkt", "interface_wkt" });
		update.addParameter(wkt);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 3;
		name.maxLength = 100;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		name.description = "The interface name";
		name.addAlias(new String[]{ "name", "interface_name" });
		update.addParameter(name);
		
		Parameter description = new Parameter();
		description.isOptional = true;
		description.minLength = 1;
		description.maxLength = 300;
		description.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		description.description = "The interface description";
		description.addAlias(new String[]{ "description", "interface_description" });
		update.addParameter(description);

		Parameter help = new Parameter();
		help.isOptional = true;
		help.minLength = 1;
		help.maxLength = 300;
		help.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		help.description = "The interface help";
		help.addAlias(new String[]{ "help", "interface_help" });
		update.addParameter(help);

		Parameter input = new Parameter();
		input.isOptional = true;
		input.minLength = 1;
		input.maxLength = 500;
		input.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		input.description = "The interface input";
		input.addAlias(new String[]{ "input", "interface_input" });
		update.addParameter(input);
		
		Parameter language = new Parameter();
		language.isOptional = false;
		language.minLength = 2;
		language.maxLength = 2;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "Translation language currently updated.";
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
				
				Database.getInstance().delete("DELETE ci, t FROM connector_interface ci " + 
					"LEFT JOIN interface_translation it ON(ci.connector_id = it.connector_id AND ci.interface_key = it.interface_key) " +
					"LEFT JOIN translations t ON(t.translation_id = it.translation_id) " + 
					"WHERE ci.connector_id = " + connector + " AND ci.interface_key = '" + Security.escape(key) + "'");
				
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a connector interface";
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
		key.addAlias(new String[]{ "key", "interface_key" });
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
				String type = getParameter("type").getValue();
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
					key = " AND ci.interface_key = '" + Security.escape(key) + "'";
				else
					key = "";
					
				if( type != null )
					type = " AND ci.interface_type = " + (type.matches("^(1|in|input)$") ? "1" : "0");
				else
					type = "";
				
				if( language == null )
				{
					return Database.getInstance().select("SELECT ci.interface_key, ci.interface_type, ci.interface_tag, ci.interface_dynamic, ci.interface_cron, ci.interface_cron_timer, ci.interface_hidden, ci.interface_wkt " + 
						"FROM connector_interface ci " + 
						"WHERE ci.connector_id = " + connector + key + type);
				}
				
				for( int attempt = 0; attempt < 3; attempt++ )
				{
					// FIRST attempt : with provided language
					if( attempt == 0 )
						language = " AND it.translation_language = '" + language + "'";
					// SECOND attempt : with EN
					else if( attempt == 1 )
						language = " AND it.translation_language = 'EN'";
					// THIRD attempt : any (including null)
					else
						language = " GROUP BY ci.interface_key";
				
					Any t = Any.wrap(Database.getInstance().select("SELECT ci.interface_key, ci.interface_type, ci.interface_dynamic, ci.interface_cron, ci.interface_cron_timer, ci.interface_hidden, ci.interface_wkt, " + 
							"t.translation_id, t.translation_text " + 
							"FROM connector_interface ci " + 
							"LEFT JOIN interface_translation it ON(it.connector_id = ci.connector_id AND it.interface_key = ci.interface_key) " + 
							"LEFT JOIN translations t ON(t.translation_id = it.translation_id) " + 
							"WHERE ci.connector_id = " + connector + key + type + language));
					if( t != null && t.size() > 0 && !t.get(0).get("translation_id").isNull() )
					{
						// format results
						Any interfaces = Any.empty();
						for( Any r : t )
						{
							Any text = Json.decode(r.<String>value("translation_text"));
							
							if( text.isEmpty() )
							{
								text.put("name", null); text.put("description", null); 
								text.put("help", null); text.put("input", null);
							}
							
							r.remove("translation_text");
							r.remove("translation_id");
							text.putAll(r);
							interfaces.put(text.<String>value("interface_key"), text);
						}

						return interfaces;
					}
				}
				
				// THIS SHOULD NEVER HAPPEN... EXCEPT IF THERE IS NO INTERFACE... doh!
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
	
		Parameter type = new Parameter();
		type.isOptional = true;
		type.minLength = 1;
		type.maxLength = 6;
		type.mustMatch = "^(?i)(1|in|input|0|out|output)$";
		type.description = "The interface type (input or output)";
		type.addAlias(new String[]{ "type", "interface_type" });
		select.addParameter(type);

		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 1;
		key.maxLength = 30;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		key.description = "The interface key";
		key.addAlias(new String[]{ "key", "interface_key" });
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
	
	public static Map<String, String> INTERNAL_SELECT(String connector, String type, String key, String name, String language) throws Exception
	{
		if( !connector.matches("^[0-9]+$") )
			connector = "(SELECT connector_id FROM connectors WHERE connector_name = '" + Security.escape(connector) + "')";
				
		if( key != null )
			key = " AND ci.interface_key = '" + Security.escape(key) + "'";
		else
			key = "";
					
		if( type != null )
			type = " AND ci.interface_type = " + (type.matches("^(1|in|input)$") ? "1" : "0");
		else
			type = "";
				
		if( language == null )
		{
			Vector<Map<String, String>> data =  Database.getInstance().select("SELECT ci.interface_key, ci.interface_type, ci.interface_tag, ci.interface_dynamic, ci.interface_cron, ci.interface_cron_timer, ci.interface_hidden, ci.interface_wkt " + 
				"FROM connector_interface ci " + 
				"WHERE ci.connector_id = " + connector + key + type);
				
			return data.get(0);
		}
		
		for( int attempt = 0; attempt < 3; attempt++ )
		{
			// FIRST attempt : with provided language
			if( attempt == 0 )
				language = " AND it.translation_language = '" + language + "'";
			// SECOND attempt : with EN
			else if( attempt == 1 )
				language = " AND it.translation_language = 'EN'";
			// THIRD attempt : any (including null)
			else
				language = " GROUP BY ci.interface_key";
		
			Any t = Any.wrap(Database.getInstance().select("SELECT ci.interface_key, ci.interface_type, ci.interface_dynamic, ci.interface_tag, ci.interface_cron, ci.interface_hidden, ci.interface_cron_timer, ci.interface_wkt, " + 
					"t.translation_id, t.translation_text " + 
					"FROM connector_interface ci " + 
					"LEFT JOIN interface_translation it ON(it.connector_id = ci.connector_id AND it.interface_key = ci.interface_key) " + 
					"LEFT JOIN translations t ON(t.translation_id = it.translation_id) " + 
					"WHERE ci.connector_id = " + connector + key + type + language));
			if( t != null && t.size() > 0 && !t.get(0).get("translation_id").isNull() )
			{
				// format results
				Any interfaces = Any.empty();
				for( Any r : t )
				{
					Any text = Json.decode(r.<String>value("translation_text"));
					
					if( text.isEmpty() )
					{
						text.put("name", null); text.put("description", null);
						text.put("help", null); text.put("input", null);
					}
					
					r.remove("translation_text");
					r.remove("translation_id");
					text.putAll(r);
					interfaces.put(text.<String>value("interface_key"), text);
				}
				
				return (Map<String, String>)interfaces.unwrap();
			}
		}
		
		// THIS SHOULD NEVER HAPPEN... EXCEPT IF THERE IS NO INTERFACE... doh!
		return new HashMap<String, String>();
	}
}



