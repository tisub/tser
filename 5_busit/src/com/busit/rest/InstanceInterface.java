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

public class InstanceInterface extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "interface", "interfaces" });
		index.description = "Manages instance interfaces";
		Handler.addHandler("/busit/instance/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		
		Self.selfize("/busit/instance/interface/insert");
		Self.selfize("/busit/instance/interface/update");
		Self.selfize("/busit/instance/interface/delete");
		Self.selfize("/busit/instance/interface/select");
	}
	
	private void initializeInsert(Index index)
	{
		// interface insert is only valid for dynamic interfaces
		Action insert = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String key = getParameter("key").getValue();
				String name = getParameter("name").getValue();
				String value = getParameter("value").getValue();
				String user = getParameter("user").getValue();

				if( name != null && Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(name).find() )
					throw new Exception("The interface name may not start, end or contain consecutive special characters");
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instance) )
						throw new Exception("The current user is not an administrator of the provided instance");
				}
				
				if( name != null )
					name = "'" + Security.escape(name) + "'";
				else
					name = "null";
				
				if( value != null )
				{
					// caution : we encrypt the config value because it may be confidential
					value = "'" + CryptoSimple.serialize(value) + "'";
				}
				else
					value = "null";

				// check for parent instance and childs
				Map<String, String> result = Database.getInstance().selectOne("SELECT instance_parent FROM instances WHERE instance_id = " + instance);
				if( result.get("instance_parent").equals("0") )
				{
					// no parents, insert interface
					Database.getInstance().insert("INSERT INTO instance_interface (instance_id, connector_id, interface_key, interface_name, interface_dynamic_value)" + 
					"SELECT i.instance_id, i.instance_connector, '" + Security.escape(key) + "', " + name + ", " + value + " " + 
					"FROM instances i " +
					"LEFT JOIN connector_interface ci ON(ci.connector_id = i.instance_connector) " +
					"WHERE i.instance_id = " + instance + " AND ci.interface_key = '" + Security.escape(key) + "'");
					
					// update children if any
					Vector<Map<String, String>> data = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + instance);
					for( Map<String, String> d : data )
					{
						Database.getInstance().insert("INSERT INTO instance_interface (instance_id, connector_id, interface_key, interface_name, interface_dynamic_value)" + 
						"SELECT i.instance_id, i.instance_connector, '" + Security.escape(key) + "', " + name + ", " + value + " " + 
						"FROM instances i " +
						"LEFT JOIN connector_interface ci ON(ci.connector_id = i.instance_connector) " +
						"WHERE i.instance_id = " + d.get("instance_id") + " AND ci.interface_key = '" + Security.escape(key) + "'");
					}
				}
				else
				{
					// have a parent, update the parent
					Database.getInstance().insert("INSERT INTO instance_interface (instance_id, connector_id, interface_key, interface_name, interface_dynamic_value)" + 
					"SELECT i.instance_id, i.instance_connector, '" + Security.escape(key) + "', " + name + ", " + value + " " + 
					"FROM instances i " +
					"LEFT JOIN connector_interface ci ON(ci.connector_id = i.instance_connector) " +
					"WHERE i.instance_id = " + result.get("instance_parent") + " AND ci.interface_key = '" + Security.escape(key) + "'");	

					// update children
					Vector<Map<String, String>> data = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + result.get("instance_parent"));
					for( Map<String, String> d : data )
					{
						Database.getInstance().insert("INSERT INTO instance_interface (instance_id, connector_id, interface_key, interface_name, interface_dynamic_value)" + 
						"SELECT i.instance_id, i.instance_connector, '" + Security.escape(key) + "', " + name + ", " + value + " " + 
						"FROM instances i " +
						"LEFT JOIN connector_interface ci ON(ci.connector_id = i.instance_connector) " +
						"WHERE i.instance_id = " + d.get("instance_id") + " AND ci.interface_key = '" + Security.escape(key) + "'");
					}					
				}
				
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Insert a dynamic interface on an instance";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "instance_update" });
	
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The instance id";
		id.addAlias(new String[]{ "instance", "id", "instance_id", "iid" });
		insert.addParameter(id);

		Parameter key = new Parameter();
		key.isOptional = false;
		key.minLength = 1;
		key.maxLength = 150;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		key.description = "The original interface key";
		key.addAlias(new String[]{ "key", "interface_key" });
		insert.addParameter(key);

		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 1;
		name.maxLength = 200;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		name.description = "The interface name";
		name.addAlias(new String[]{ "interface_name", "name" });
		insert.addParameter(name);
		
		Parameter value = new Parameter();
		value.isOptional = true;
		value.minLength = 1;
		value.maxLength = 5000;
		value.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		value.description = "The interface value";
		value.addAlias(new String[]{ "value", "interface_value" });
		insert.addParameter(value);

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
		Action update = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String oldname = getParameter("interface").getValue();
				String key = getParameter("key").getValue();
				String name = getParameter("name").getValue();
				String user = getParameter("user").getValue();
				
				if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(name).find() )
					throw new Exception("The interface name may not start, end or contain consecutive special characters");
				if( key == null && oldname == null )
					throw new Exception("One of key or interface must be specified");
				
				if( user == null && !instance.matches("^[0-9]+$") )
					throw new Exception("Cannot use instance name with no user");
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instance) )
						throw new Exception("The current user is not an administrator of the provided instance");
				}
				
				if( oldname != null )
					oldname = " AND ii.interface_name = '" + Security.escape(oldname) + "'";
				else
					oldname = "";
				
				if( key != null )
					key = " AND ii.interface_key = '" + Security.escape(key) + "'";
				else
					key = "";
				
				// check for parent instance and childs
				Map<String, String> result = Database.getInstance().selectOne("SELECT instance_parent FROM instances WHERE instance_id = " + instance);
				if( result.get("instance_parent").equals("0") )
				{
					// no parents, update interface
					Database.getInstance().update("UPDATE instance_interface ii " + 
					"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " +
					"SET ii.interface_name = '" + Security.escape(name) + "' " + 
					"WHERE i.instance_id = " + instance + key + name);
					
					// update children if any
					Vector<Map<String, String>> data = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + instance);
					for( Map<String, String> d : data )
					{
						Database.getInstance().update("UPDATE instance_interface ii " + 
						"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " +
						"SET ii.interface_name = '" + Security.escape(name) + "' " + 
						"WHERE i.instance_id = " + d.get("instance_id") + key + name);
					}
				}
				else
				{
					// have a parent, update the parent
					Database.getInstance().update("UPDATE instance_interface ii " + 
						"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " +
						"SET ii.interface_name = '" + Security.escape(name) + "' " + 
						"WHERE i.instance_id = " + result.get("instance_parent") + key + name);


					// update children
					Vector<Map<String, String>> data = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + result.get("instance_parent"));
					for( Map<String, String> d : data )
					{
						Database.getInstance().update("UPDATE instance_interface ii " + 
						"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " +
						"SET ii.interface_name = '" + Security.escape(name) + "' " + 
						"WHERE i.instance_id = " + d.get("instance_id") + key + name);
					}					
				}
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modify an instance interface name";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "instance_update" });

		Parameter instance = new Parameter();
		instance.isOptional = false;
		instance.minLength = 1;
		instance.maxLength = 30;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instance.allowInUrl = true;
		instance.description = "The instance id";
		instance.addAlias(new String[]{ "id", "iid", "instance_id", "instance" });
		update.addParameter(instance);
		
		Parameter oldname = new Parameter();
		oldname.isOptional = true;
		oldname.minLength = 1;
		oldname.maxLength = 200;
		oldname.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		oldname.description = "The current interface name";
		oldname.addAlias(new String[]{ "interface", "old_name" });
		update.addParameter(oldname);
	
		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 1;
		key.maxLength = 150;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		key.description = "The original interface key";
		key.addAlias(new String[]{ "key", "interface_key" });
		update.addParameter(key);
		
		Parameter name = new Parameter();
		name.isOptional = false;
		name.minLength = 1;
		name.maxLength = 200;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		name.description = "The interface name";
		name.addAlias(new String[]{ "interface_name", "name" });
		update.addParameter(name);
		
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
		// interface delete is only valid for dynamic interfaces
		Action delete = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String key = getParameter("key").getValue();
				String user = getParameter("user").getValue();
				String name = getParameter("name").getValue();
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instance) )
						throw new Exception("The current user is not an administrator of the provided instance");
				}
				
				if( user == null && !instance.matches("^[0-9]+$") )
					throw new Exception("Cannot use instance name with no user");
				
				if( key == null && name == null )
					throw new Exception("One of key or name must be specified");
				
				if( name != null )
					name = " AND ii.interface_name = '" + Security.escape(name) + "'";
				else
					name = "";
				
				if( key != null )
					key = " AND ii.interface_key = '" + Security.escape(key) + "'";
				else
					key = "";
				
				// check for parent instance and childs
				Map<String, String> result = Database.getInstance().selectOne("SELECT instance_parent FROM instances WHERE instance_id = " + instance);
				if( result.get("instance_parent").equals("0") )
				{
					// no parents, delete interface
					Database.getInstance().delete("DELETE ii FROM instance_interface ii " + 
					"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " + 
					"LEFT JOIN connector_interface ci ON(ci.connector_id = ii.connector_id) " + 
					"WHERE ci.interface_dynamic AND i.instance_id = " + instance + key + name);
					
					// update children if any
					Vector<Map<String, String>> data = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + instance);
					for( Map<String, String> d : data )
					{
						Database.getInstance().delete("DELETE ii FROM instance_interface ii " + 
						"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " + 
						"LEFT JOIN connector_interface ci ON(ci.connector_id = ii.connector_id) " + 
						"WHERE ci.interface_dynamic AND i.instance_id = " + d.get("instance_id") + key + name);
					}
				}
				else
				{
					// have a parent, update the parent
					Database.getInstance().delete("DELETE ii FROM instance_interface ii " + 
						"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " + 
						"LEFT JOIN connector_interface ci ON(ci.connector_id = ii.connector_id) " + 
						"WHERE ci.interface_dynamic AND i.instance_id = " + result.get("instance_parent") + key + name);

					// update children
					Vector<Map<String, String>> data = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + result.get("instance_parent"));
					for( Map<String, String> d : data )
					{
						Database.getInstance().delete("DELETE ii FROM instance_interface ii " + 
						"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " + 
						"LEFT JOIN connector_interface ci ON(ci.connector_id = ii.connector_id) " + 
						"WHERE ci.interface_dynamic AND i.instance_id = " + d.get("instance_id") + key + name);
					}					
				}
				
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Delete a dynamic interface of an instance";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "instance_update" });
	
		Parameter instance = new Parameter();
		instance.isOptional = false;
		instance.minLength = 1;
		instance.maxLength = 50;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instance.allowInUrl = true;
		instance.description = "The instance id";
		instance.addAlias(new String[]{ "id", "iid", "instance_id", "instance" });
		delete.addParameter(instance);

		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 1;
		key.maxLength = 150;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		key.description = "The original interface key";
		key.addAlias(new String[]{ "key", "interface_key" });
		delete.addParameter(key);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 1;
		name.maxLength = 200;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		name.description = "The interface name";
		name.addAlias(new String[]{ "interface_name", "name", "interface" });
		delete.addParameter(name);
		
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
		Action select = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String type = getParameter("type").getValue();
				String key = getParameter("key").getValue();
				String name = getParameter("name").getValue();
				String user = getParameter("user").getValue();

				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceUser(user, instance) )
						throw new Exception("The current user is not an user of the provided instance");
				}
				
				if( user == null && !instance.matches("^[0-9]+$") )
					throw new Exception("Cannot use instance name with no user");
				else if( !instance.matches("^[0-9]+$") )
				{
					String u2 = user;
					if( u2.matches("^[0-9]+$") )
						u2 = "u.user_id = " + u2;
					else
						u2 = "u.user_name = '" + Security.escape(u2) + "'";
						
					// CAUTION : we must use instance id
					Map<String, String> row = Database.getInstance().selectOne("SELECT i.instance_id " + 
						"FROM instances i " +
						"LEFT JOIN users u ON(u.user_id = i.instance_user) " + 
						"WHERE " + u2 + " AND i.instance_name = '" + Security.escape(instance) + "'");
					if( row == null || row.get("instance_id") == null )
						throw new Exception("Unknown instance name");
					else
						instance = row.get("instance_id");
				}
				
				return INTERNAL_SELECT(instance, type, key, name);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Select instance interfaces.";
		select.returnDescription = "The matching interfaces [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "instance_select" });
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 50;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		id.allowInUrl = true;
		id.description = "The instance name or id";
		id.addAlias(new String[]{ "instance", "id", "instance_id", "iid", "instance_name" });
		select.addParameter(id);
	
		Parameter type = new Parameter();
		type.isOptional = true;
		type.minLength = 1;
		type.maxLength = 7;
		type.mustMatch = "^(1|i|in|input|inputs|0|o|out|output|outputs)$";
		type.description = "The interface type (input or output)";
		type.addAlias(new String[]{ "type", "interface_type" });
		select.addParameter(type);

		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 1;
		name.maxLength = 200;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		name.description = "The interface name";
		name.addAlias(new String[]{ "interface_name", "name" });
		select.addParameter(name);
		
		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 1;
		key.maxLength = 150;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		key.description = "The original interface key";
		key.addAlias(new String[]{ "key", "interface_key" });
		select.addParameter(key);
		
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
	
	public static Vector<Map<String, String>> INTERNAL_SELECT(String instance, String type, String key, String name) throws Exception
	{
		return INTERNAL_SELECT(instance, type, key, name, null);
	}
	
	public static Vector<Map<String, String>> INTERNAL_SELECT(String instance, String type, String key, String name, String language) throws Exception
	{
		if( instance == null )
			throw new Exception("Instance must be provided");
		
		if( !instance.matches("^[0-9]+$") )
			throw new Exception("Only instance id is allowed in internal sub-query");
		
		if( key != null )
			key = " AND ii.interface_key = '" + Security.escape(key) + "'";
		else
			key = "";
		
		if( name != null )
			name = " AND ii.interface_name LIKE '%" + name + "%'";
		else
			name = "";
			
		if( type != null && type.matches("^(1|i|in|input|inputs)$") )
			type = " AND ci.interface_type = 1";
		else if( type != null )
			type = " AND ci.interface_type = 0";
		else
			type = "";
		
		if( language == null )
		{
			Vector<Map<String, String>> t = Database.getInstance().select("SELECT DISTINCT ii.instance_id, ii.connector_id, ii.interface_key, ii.interface_name, ii.interface_dynamic_value, " + 
				"ii.interface_cron_timer, ii.interface_cron_identity, ii.interface_public, ii.interface_public_identity, " + 
				"ii.interface_shared, ii.interface_share_tax, ii.interface_share_description, ii.interface_share_identity, " + 
				"ci.interface_type, ci.interface_dynamic, ci.interface_cron, ci.interface_tag, ci.interface_hidden, ci.interface_wkt, pi.identity_principal as interface_public_identity_name, " + 
				"si.identity_principal as interface_share_identity_name, ci2.identity_principal as interface_cron_identity_name " +
				"FROM instance_interface ii " + 
				"LEFT JOIN connector_interface ci ON(ci.connector_id = ii.connector_id AND ci.interface_key = ii.interface_key) " +
				"LEFT JOIN identities pi ON(pi.identity_id = ii.interface_public_identity) " + 
				"LEFT JOIN identities ci2 ON(ci2.identity_id = ii.interface_cron_identity) " + 
				"LEFT JOIN identities si ON(si.identity_id = ii.interface_share_identity) " + 
				"WHERE ii.instance_id = " + instance + key + name + type);
				
			for( Map<String, String> r : t )
			{
				// caution : decrypt the config value
				if( r.get("interface_dynamic_value") != null )
					r.put("interface_dynamic_value", CryptoSimple.unserialize(r.get("interface_dynamic_value")));
				r.put("interface_public_identity_name", PrincipalUtil.shortName(r.get("interface_public_identity_name")));
				r.put("interface_share_identity_name", PrincipalUtil.shortName(r.get("interface_share_identity_name")));
				r.put("interface_cron_identity_name", PrincipalUtil.shortName(r.get("interface_cron_identity_name")));
			}
			
			return t;
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
				language = "";
			
			Vector<Map<String, String>> data = Database.getInstance().select("SELECT DISTINCT ii.instance_id, ii.connector_id, ii.interface_key, ii.interface_name, ii.interface_dynamic_value, " + 
				"ii.interface_cron_timer, ii.interface_cron_identity, ii.interface_public, ii.interface_public_identity, " + 
				"ii.interface_shared, ii.interface_share_tax, ii.interface_share_description, ii.interface_share_identity, " + 
				"ci.interface_type, ci.interface_dynamic, ci.interface_cron, ci.interface_tag, ci.interface_hidden, ci.interface_wkt, pi.identity_principal as interface_public_identity_name, " + 
				"si.identity_principal as interface_share_identity_name, ci2.identity_principal as interface_cron_identity_name, " +
				"it.translation_language, t.translation_id, t.translation_text " + 
				"FROM instance_interface ii " + 
				"LEFT JOIN connector_interface ci ON(ci.connector_id = ii.connector_id AND ci.interface_key = ii.interface_key) " +
				"LEFT JOIN interface_translation it ON(it.connector_id = ii.connector_id AND it.interface_key = ii.interface_key) " + 
				"LEFT JOIN translations t ON(t.translation_id = it.translation_id) " + 
				"LEFT JOIN identities pi ON(pi.identity_id = ii.interface_public_identity) " + 
				"LEFT JOIN identities ci2 ON(ci2.identity_id = ii.interface_cron_identity) " + 
				"LEFT JOIN identities si ON(si.identity_id = ii.interface_share_identity) " + 
				"WHERE ii.instance_id = " + instance + key + name + type + language);
			
			if( data == null || data.size() == 0 || data.get(0).get("translation_id") == null )
				continue;
			
			for( Map<String, String> r : data )
			{
				if( r.get("translation_id") != null && r.get("translation_text") != null )
				{
					Any a = Json.decode(r.remove("translation_text"));
					for( String k : a.keys() )
						r.put(k, a.<String>value(k));
				}
				
				// caution : decrypt the config value
				if( r.get("interface_dynamic_value") != null )
					r.put("interface_dynamic_value", CryptoSimple.unserialize(r.get("interface_dynamic_value")));
				r.put("interface_public_identity_name", PrincipalUtil.shortName(r.get("interface_public_identity_name")));
				r.put("interface_share_identity_name", PrincipalUtil.shortName(r.get("interface_share_identity_name")));
				r.put("interface_cron_identity_name", PrincipalUtil.shortName(r.get("interface_cron_identity_name")));
			}

			return data;
		}
		
		// THIS SHOULD NEVER HAPPEN...
		return new Vector<Map<String, String>>();
	}		
}
