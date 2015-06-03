package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;

public class Instance extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "instance", "instances" });
		index.description = "Manages instances";
		Handler.addHandler("/busit/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		initializeSearch(index);
		
		Self.selfize("/busit/instance/insert");
		Self.selfize("/busit/instance/update");
		Self.selfize("/busit/instance/delete");
		Self.selfize("/busit/instance/select");
		Self.selfize("/busit/instance/search");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String name = getParameter("name").getValue();
				String connector = getParameter("connector").getValue();
				String uuid = getParameter("uuid").getValue();
				String user = getParameter("user").getValue();
				String space = getParameter("space").getValue();
				String language = getParameter("language").getValue();
				String running = getParameter("running").getValue();
				String parent = getParameter("parent").getValue();
				
				if( language == null )
					language = "EN";
				else
					language = language.substring(0,2).toUpperCase();
				
				if( name.matches("^[0-9]+$") || name.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The instance name may not be numeric and may not start or end with special characters");
				if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(name).find() )
					throw new Exception("The instance name may not start, end or contain consecutive special characters");
				
				if( space != null )
				{
					if( !IdentityChecker.getInstance().isUserSpaceAdmin(user, space) )
						throw new Exception("The current user is not an administrator of the provided space");
				}
				
				if( parent != null && !parent.equals("0") )
				{
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, parent) )
						throw new Exception("The current user is not an administrator of the provided parent");
				}
				
				String sqluser = user;
				
				if( !sqluser.matches("^[0-9]+$") )
					sqluser = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";

				if( space != null )
				{
					// force the user to be admin of the space
					sqluser = "(SELECT DISTINCT s.space_user FROM users u " + 
						"LEFT JOIN identities i ON(i.identity_user = u.user_id) " + 
						"LEFT JOIN org_member om ON(om.identity_id = i.identity_id AND om.org_admin) " + 
						"LEFT JOIN users o ON(o.user_id = om.org_id) " + 
						"LEFT JOIN spaces s ON(s.space_user = u.user_id OR s.space_user = o.user_id) " + 
						"WHERE u.user_id = " + sqluser + " AND s.space_id = " + space + ")";
				}
				
				Map<String, String> check = Database.getInstance().selectOne("SELECT instance_id FROM instances WHERE instance_name = '" + Security.escape(name) + "' AND instance_user = " + sqluser);
				if( check != null && check.get("instance_id") != null )
					throw new Exception("An instance already exists with this name.");
				
				check = Database.getInstance().selectOne("SELECT connector_id FROM connectors " + 
					"WHERE connector_id = " + connector + " AND connector_user_status > 1 AND connector_intern_status > 1");
				if( check == null )
					throw new Exception("Impossible to create instance of pending connector.");
				
				if( running == null )
					running = "0";
				
				String sqlparent = "";
				if( parent != null )
					sqlparent = parent;
				else
					sqlparent = "0";

				if( uuid != null )
					uuid = "'" + Security.escape(uuid) + "'";
				else
					uuid = "NULL";
				
				Quota.getInstance().increment(user, Config.gets("com.busit.rest.quota.instance"));
					
				// ==========================
				// CONNECTOR PRICE
				// ==========================
				Credit.INTERNAL_BUY_CONNECTOR(user, connector);
				
				Long uid = Database.getInstance().insert("INSERT INTO instances (instance_name, instance_uuid, instance_connector, instance_user, instance_date, instance_running, instance_parent)" + 
					"VALUES ('" + Security.escape(name) + "'," + uuid + ", '" + Security.escape(connector) + "', " + sqluser + 
					", UNIX_TIMESTAMP(), " + running + ", " + sqlparent + ")");
				
				// tag instance in space
				if( space != null )
					Database.getInstance().insert("INSERT IGNORE INTO instance_space (space_id, instance_id) VALUES (" + space + ", " + uid + ")");
				
				// insert all interfaces 
				if( parent != null )
				{
					Database.getInstance().insert("INSERT INTO instance_interface (instance_id, connector_id, interface_key, interface_name, interface_dynamic_value, interface_cron_timer, interface_cron_identity) " + 
					"SELECT " + uid + ", " + connector + ", interface_key, interface_name, interface_dynamic_value, interface_cron_timer, interface_cron_identity " + 
					"FROM instance_interface " + 
					"WHERE instance_id = " + parent);							
				}
				else
				{
					// insert all non-dynamic interfaces
					// 1) select all config with the translation
					// 2) insert them with instance_name = translated name
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
						
						Any rows = Any.wrap(Database.getInstance().select("SELECT c.connector_id, ci.interface_key, ci.interface_cron_timer, t.translation_text, t.translation_id " + 
							"FROM connectors c " + 
							"LEFT JOIN connector_interface ci ON(ci.connector_id = c.connector_id) " + 
							"LEFT JOIN interface_translation it ON(it.connector_id = ci.connector_id AND it.interface_key = ci.interface_key) " + 
							"LEFT JOIN translations t ON(t.translation_id = it.translation_id) " + 
							"WHERE c.connector_id = " + connector + " AND NOT interface_dynamic " + language
						));
			
						if( rows != null && rows.size() > 0 && !rows.get(0).get("translation_id").isNull() )
						{
							String values = "";
							for( Any r : rows )
							{
								Any text = Json.decode(r.<String>value("translation_text"));
								if( text.isEmpty() )
									text.put("name", r.<String>value("interface_key"));
									values += ",(" + uid + ", " + r.<String>value("connector_id") + ", '" + Security.escape(r.<String>value("interface_key")) + "', '" + Security.escape(text.<String>value("name")) + "', '" + Security.escape(r.<String>value("interface_cron_timer")) + "')";
							}
							
							Database.getInstance().insert("INSERT INTO instance_interface (instance_id, connector_id, interface_key, interface_name, interface_cron_timer) " + 
							"VALUES " + values.substring(1));
							break;
						}
					}
				}
								
				// insert all configs
				if( parent != null )
				{
					Database.getInstance().insert("INSERT INTO instance_config (instance_id, connector_id, config_key, config_value) " + 
						"SELECT " + uid + ", " + connector + ", config_key, config_value " + 
						"FROM instance_config " + 
						"WHERE instance_id = " + parent);
				}
				else
				{
					Database.getInstance().insert("INSERT INTO instance_config (instance_id, connector_id, config_key, config_value) " + 
						"SELECT " + uid + ", " + connector + ", config_key, config_value " + 
						"FROM connector_config " + 
						"WHERE connector_id = " + connector);
				}
				
				// update instance count in ALL connectors
				Database.getInstance().update("UPDATE connectors SET connector_instances = (SELECT COUNT(instance_id) FROM instances WHERE instance_connector = connector_id)");
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", name);
				result.put("id", uid);
				
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new instance";
		insert.returnDescription = "The newly created instance {'name', 'id'}";
		insert.addGrant(new String[] { "access", "instance_insert" });
		
		Parameter name = new Parameter();
		name.isOptional = false;
		name.minLength = 2;
		name.maxLength = 150;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.allowInUrl = true;
		name.description = "The instance name";
		name.addAlias(new String[]{ "name", "instance_name" });
		insert.addParameter(name);
		
		
		Parameter connector = new Parameter();
		connector.isOptional = false;
		connector.minLength = 1;
		connector.maxLength = 30;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		connector.allowInUrl = true;
		connector.description = "The instance connector id";
		connector.addAlias(new String[]{ "connector", "connector_description", "instance_connector" });
		insert.addParameter(connector);

		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		insert.addParameter(user);
		
		Parameter space = new Parameter();
		space.isOptional = true;
		space.minLength = 1;
		space.maxLength = 50;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		space.allowInUrl = true;
		space.description = "The instance space";
		space.addAlias(new String[]{ "space", "space_description", "space_id" });
		insert.addParameter(space);
		
		Parameter uuid = new Parameter();
		uuid.isOptional = true;
		uuid.minLength = 1;
		uuid.maxLength = 90;
		uuid.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		uuid.description = "The instance uuid";
		uuid.addAlias(new String[]{ "uuid", "instance_uuid", "device_id" });
		insert.addParameter(uuid);
		
		Parameter lang = new Parameter();
		lang.isOptional = true;
		lang.minLength = 2; // FR
		lang.maxLength = 5; // FR_BE
		lang.mustMatch = "^[a-zA-Z]{2}(_[a-zA-Z]{2})?$";
		lang.description = "The language of the instance (for default values to be translated)";
		lang.addAlias(new String[]{ "lang", "language", "locale" });
		insert.addParameter(lang);
		
		Parameter running = new Parameter();
		running.isOptional = true;
		running.minLength = 1;
		running.maxLength = 1;
		running.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		running.description = "Is this instance is a running instance?";
		running.addAlias(new String[]{ "running", "instance_running" });
		insert.addParameter(running);
		
		Parameter parent = new Parameter();
		parent.isOptional = true;
		parent.minLength = 1;
		parent.maxLength = 50;
		parent.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		parent.description = "Is this instance have a parent (for config & interface propagation)";
		parent.addAlias(new String[]{ "parent", "parend_id", "parent_instance" });
		insert.addParameter(parent);

		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String name = getParameter("name").getValue();
				String inter = getParameter("inter").getValue();
				Collection<String> space = getParameter("space").getValues();
				String spacemethod = getParameter("spacemethod").getValue();
				String active = getParameter("active").getValue();
				String configured = getParameter("configured").getValue();
				String position = getParameter("position").getValue();
				String user = getParameter("user").getValue();
				String uuid = getParameter("uuid").getValue();
				
				if( name != null )
				{
					if( name.matches("^[0-9]+$") || name.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
						throw new Exception("The instance name may not be numeric and may not start or end with special characters");
					if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(name).find() )
						throw new Exception("The instance name may not start, end or contain consecutive special characters");
				}
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instance) )
					{
						if( !instance.matches("^[0-9]+$") )
							throw new Exception("Cannot use instance name with shared instances");

						String u2 = user;
						if( !u2.matches("^[0-9]+$") )
							u2 = "(SELECT user_id FROM users where user_name = '" + Security.escape(u2) + "')";
										
						Map<String, String> check = Database.getInstance().selectOne("SELECT us.space_id " + 
							"FROM user_shared us " +
							"LEFT JOIN instance_interface ii ON(us.instance_id = ii.instance_id AND us.interface_name = ii.interface_name) " +
							"LEFT JOIN spaces s ON(s.space_id = us.space_id) " + 
							"WHERE s.space_user = " + u2 + " AND ii.interface_shared AND us.instance_id = " + instance + " AND us.interface_name = '" + Security.escape(inter) + "'");
						
						if( check.get("space_id") == null )
							throw new Exception("The current user is not an administrator of the provided instance");
						else
						{
							if( position != null )
								Database.getInstance().update("UPDATE user_shared SET instance_position = '" + Security.escape(position) + "' " + 
									"WHERE instance_id = " + instance + " AND interface_name = '" + Security.escape(inter) + "' AND space_id = " + check.get("space_id"));
							
							return "OK";
						}
					}
				}
				else if( !instance.matches("^[0-9]+$") )
					throw new Exception("Cannot use instance name with no user");
				
				if( spacemethod == null )
					spacemethod = "add";
					
				if( space.size() > 0 )
				{
					for( String s : space )
					{
						if( spacemethod.equals("add") )
							Database.getInstance().insert("INSERT IGNORE INTO instance_space (space_id, instance_id) VALUES (" + s + ", " + Security.escape(instance) + ")");
						else if( spacemethod.equals("delete") )
							Database.getInstance().insert("DELETE FROM instance_space WHERE space_id = " + s + " AND instance_id = " + Security.escape(instance));
					}
				}
				
				String set = "";
				if( name != null )
					set += "i.instance_name = '" + Security.escape(name) + "', ";
				if( active != null )
					set += "i.instance_active = '" + Security.escape(active) + "', ";
				if( configured != null )
					set += "i.instance_configured = '" + Security.escape(configured) + "', ";
				if( position != null )
					set += "i.instance_position = '" + Security.escape(position) + "', ";
				if( uuid != null )
					set += "i.instance_uuid = '" + Security.escape(uuid) + "', ";
					
				if( instance.matches("^[0-9]+$") )
				{
					instance = "i.instance_id = " + instance;
					user = "1=1";
				}
				else
				{
					instance = "i.instance_name = '" + Security.escape(instance) + "'";
					
					if( user == null )
						user = "1=1";
					else if( user.matches("^[0-9]+$") )
						user = "u.user_id = " + user;
					else
						user = "u.user_name = '" + Security.escape(user) + "'";
				}

				Database.getInstance().update("UPDATE instances i " + 
					"LEFT JOIN users u ON(u.user_id = i.instance_user) " + 
					"SET " + set + " i.instance_id = i.instance_id " + 
					"WHERE " + user + " AND " + instance);
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modify an instance";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "instance_update" });
	
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 50;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		id.allowInUrl = true;
		id.description = "The current instance name or id";
		id.addAlias(new String[]{ "instance", "id", "instance_id", "iid", "old_instance_name" });
		update.addParameter(id);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 2;
		name.maxLength = 150;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.description = "The new instance name";
		name.addAlias(new String[]{ "name", "instance_name" });
		update.addParameter(name);
		
		Parameter inter = new Parameter();
		inter.isOptional = true;
		inter.minLength = 1;
		inter.maxLength = 200;
		inter.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		inter.description = "The interface name";
		inter.addAlias(new String[]{ "interface_name", "interface", "inter" });
		update.addParameter(inter);
	
		Parameter space = new Parameter();
		space.isOptional = true;
		space.minLength = 1;
		space.maxLength = 20;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		space.isMultipleValues = true;
		space.description = "The space id(s)";
		space.addAlias(new String[]{ "space", "space_id", "sid", "spaces", "spaces_id", "sids" });
		update.addParameter(space);
		
		Parameter spacemethod = new Parameter();
		spacemethod.isOptional = true;
		spacemethod.minLength = 1;
		spacemethod.maxLength = 7;
		spacemethod.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		spacemethod.description = "Space method (add or delete)";
		spacemethod.addAlias(new String[]{ "spacemethod", "space_method", "method" });
		update.addParameter(spacemethod);
		
		Parameter active = new Parameter();
		active.isOptional = true;
		active.minLength = 1;
		active.maxLength = 1;
		active.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		active.description = "Is this instance active?";
		active.addAlias(new String[]{ "active", "instance_active" });
		update.addParameter(active);

		Parameter configured = new Parameter();
		configured.isOptional = true;
		configured.minLength = 1;
		configured.maxLength = 1;
		configured.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		configured.description = "Is this instance configured?";
		configured.addAlias(new String[]{ "configured", "instance_configured" });
		update.addParameter(configured);

		Parameter position = new Parameter();
		position.isOptional = true;
		position.minLength = 1;
		position.maxLength = 200;
		position.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		position.description = "Instance coordinates in overview";
		position.addAlias(new String[]{ "position", "instance_position" });
		update.addParameter(position);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		update.addParameter(user);
		
		Parameter uuid = new Parameter();
		uuid.isOptional = true;
		uuid.minLength = 1;
		uuid.maxLength = 90;
		uuid.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		uuid.description = "The instance uuid";
		uuid.addAlias(new String[]{ "uuid", "instance_uuid", "device_id" });
		update.addParameter(uuid);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> instance = getParameter("name").getValues();
				String user = getParameter("user").getValue();
				
				String where = "1>1";
				for( String i : instance )
				{
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, i) )
						throw new Exception("The current user is not an administrator of the provided instance");
					
					if( !i.matches("^[0-9]+$") )
						where += " OR instance_name = '" + Security.escape(i) + "'";
					else
						where += " OR instance_id = " + i;
				}
				
				if( user.matches("^[0-9]+$") )
					user = "u.user_id = " + user;
				else
					user = "u.user_name = '" + Security.escape(user) + "'";
				
				long count = Database.getInstance().delete("DELETE i FROM instances i " + 
					"LEFT JOIN users u ON(u.user_id = i.instance_user) " + 
					"WHERE " + user + " AND (" + where + ")");
				Quota.getInstance().substract(getParameter("user").getValue(), Config.gets("com.busit.rest.quota.instance"), (int)count, true);
				
				// update instance count in connector
				Database.getInstance().update("UPDATE connectors SET connector_instances = (SELECT COUNT(instance_id) FROM instances WHERE instance_connector = connector_id)");

				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes an instance";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "instance_delete" });
		
		Parameter instance = new Parameter();
		instance.isOptional = false;
		instance.minLength = 1;
		instance.maxLength = 30;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		instance.allowInUrl = true;
		instance.isMultipleValues = true;
		instance.description = "The instance name(s) or id(s)";
		instance.addAlias(new String[]{ "name", "instance_name", "id", "instance_id", "iid", "names", "instance_names", "instance_names", "ids", "instance_ids", "instance_ids", "iids" });
		delete.addParameter(instance);

		Parameter user = new Parameter();
		user.isOptional = false;
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
				Collection<String> instance = getParameter("name").getValues();
				Collection<String> space = getParameter("space").getValues();
				String instanceconnector = getParameter("connector").getValue();
				String inter = getParameter("inter").getValue();
				String active = getParameter("active").getValue();
				String extended = getParameter("extended").getValue();
				String language = getParameter("language").getValue();
				String user = getParameter("user").getValue();
				String count = getParameter("count").getValue();
				String running = getParameter("running").getValue();
				
				return INTERNAL_SELECT(instance, space, instanceconnector, inter, active, extended, language, user, count, running);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "view" });
		select.description = "Retrieves information about an instance. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching instance [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "instance_select" });
		
		Parameter instance = new Parameter();
		instance.isOptional = true;
		instance.minLength = 1;
		instance.maxLength = 50;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		instance.allowInUrl = true;
		instance.isMultipleValues = true;
		instance.description = "The instance name or id (will match *[instance]* if not a number or an exact instance id match if numeric)";
		instance.addAlias(new String[]{ "name", "instance_name", "id", "iid", "instance_id", "instance", "names", "instance_names", "instances", "ids", "iids", "instance_ids" });
		select.addParameter(instance);
		
		Parameter tag = new Parameter();
		tag.isOptional = true;
		tag.minLength = 1;
		tag.maxLength = 30;
		tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		tag.isMultipleValues = true;
		tag.description = "The tag name(s) or id(s)";
		tag.addAlias(new String[]{ "tag", "tag_name", "tid", "tag_id", "tag_names", "tags", "tids", "tag_ids" });
		select.addParameter(tag);
		
		Parameter space = new Parameter();
		space.isOptional = true;
		space.minLength = 1;
		space.maxLength = 50;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		space.isMultipleValues = true;
		space.description = "The instance space(s) name or id(s)";
		space.addAlias(new String[]{ "space", "space_name", "space_id", "spaces", "space_names", "space_ids" });
		select.addParameter(space);
		
		Parameter connector = new Parameter();
		connector.isOptional = true;
		connector.minLength = 1;
		connector.maxLength = 30;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		connector.description = "The instance connector";
		connector.addAlias(new String[]{ "connector", "connector_id" });
		select.addParameter(connector);
		
		Parameter inter = new Parameter();
		inter.isOptional = true;
		inter.minLength = 1;
		inter.maxLength = 200;
		inter.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		inter.description = "The interface name";
		inter.addAlias(new String[]{ "interface_name", "inter", "interface" });
		select.addParameter(inter);
		
		Parameter active = new Parameter();
		active.isOptional = true;
		active.minLength = 1;
		active.maxLength = 10;
		active.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		active.description = "Only show active instance?";
		active.addAlias(new String[]{ "active", "instance_active" });
		select.addParameter(active);

		Parameter running = new Parameter();
		running.isOptional = true;
		running.minLength = 1;
		running.maxLength = 10;
		running.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		running.description = "Only show running instance?";
		running.addAlias(new String[]{ "running", "instance_running" });
		select.addParameter(running);
		
		Parameter extended = new Parameter();
		extended.isOptional = true;
		extended.minLength = 1;
		extended.maxLength = 10;
		extended.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		extended.description = "Show extended information?";
		extended.addAlias(new String[]{ "extended", "more" });
		select.addParameter(extended);

		Parameter language = new Parameter();
		language.isOptional = true;
		language.minLength = 2;
		language.maxLength = 2;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "Translation language.";
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
		
		Parameter count = new Parameter();
		count.isOptional = true;
		count.minLength = 1;
		count.maxLength = 10;
		count.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		count.description = "Count instances?";
		count.addAlias(new String[]{ "count" });
		select.addParameter(count);	
		
		index.addOwnHandler(select);
	}
	
	static Object INTERNAL_SELECT(Collection<String> instance, Collection<String> space, String instanceconnector, String inter,
		String active, String extended, String language, String user, String count, String running) throws Exception
	{
		String where = "";
		String where2 = "";
		if( user != null )
		{
			if( !user.matches("^[0-9]+$") )
				user = "AND u.user_name = '" + Security.escape(user) + "'";
			else
				where += " AND u.user_id = " + user;
		}
		if( instanceconnector != null)
				where += " AND i.instance_connector = " + instanceconnector;
				
		if( running != null && running.matches("^(?i)(yes|true|1)$") )
			where += " AND i.instance_running";
		else if( running != null )
			where += " AND NOT i.instance_running";
				
		if( inter != null )
			where2 += " AND us.interface_name = '" + Security.escape(inter) + "'";
		
		if( active != null && active.matches("^(?i)(yes|true|1)$") )
			where += " AND i.instance_active";
		else if( active != null )
			where += " AND NOT i.instance_active";
		
		if( language == null )
			language = "EN";

		if( instance.size() > 0 )
		{
			where += " AND (1>1";
			for( String i : instance )
			{
				if( i.matches("^[0-9]+$") )
					where += " OR i.instance_id = " + Security.escape(i);
				else
					where += " OR i.instance_name LIKE '%" + Security.escape(i) + "%'";
			}
			where += ")";
		}
		
		if( space.size() > 0 )
		{
			where += " AND (1>1";
			for( String s : space )
			{
				if( s.matches("^[0-9]+$") )
					where += " OR s.space_id = " + Security.escape(s);
				else
					where += " OR s.space_name = '" + Security.escape(s) + "'";
			}
			where += ")";
		}	
				
		// count only
		if( count != null && count.matches("^(?i)(yes|true|1)$") )
		{
			return Database.getInstance().select("SELECT COUNT(DISTINCT i.instance_id) as count " + 
				"FROM instances i " + 
				"LEFT JOIN instance_space isp ON(i.instance_id = isp.instance_id) " +
				"LEFT JOIN spaces s ON(isp.space_id = s.space_id) " +
				"LEFT JOIN users o ON(s.space_user = o.user_id) " + 
				"LEFT JOIN org_member om ON(om.org_id = o.user_id) " +
				"LEFT JOIN identities i2 ON(i2.identity_id = om.identity_id) " +
				"LEFT JOIN users u ON(i2.identity_user = i.instance_user OR u.user_id = i.instance_user)" +
				"WHERE 1=1 " + where);
		}
		
		Vector<Map<String, Object>> instances = new Vector<Map<String, Object>>();
				
		// ================================
		// INSTANCES
		// ================================
		Vector<Map<String, String>> tmp = Database.getInstance().select("SELECT DISTINCT i.instance_id, i.instance_connector, i.instance_user, " + 
			"i.instance_date, i.instance_name, i.instance_active, i.instance_configured, i.instance_hits, i.instance_position, i.instance_running, i.instance_parent, 0 as instance_shared " + 
			"FROM instances i " + 
			"LEFT JOIN instance_space isp ON(i.instance_id = isp.instance_id) " +
			"LEFT JOIN spaces s ON(isp.space_id = s.space_id) " +
			"LEFT JOIN users o ON(s.space_user = o.user_id) " + 
			"LEFT JOIN org_member om ON(om.org_id = o.user_id) " +
			"LEFT JOIN identities i2 ON(i2.identity_id = om.identity_id) " +
			"LEFT JOIN users u ON(i2.identity_user = i.instance_user OR u.user_id = i.instance_user) " +
			"WHERE 1=1 " + where + " " +
			"ORDER BY i.instance_id DESC");
			
		for( Map<String, String> t : tmp )
		{
			Map<String, Object> t2 = new HashMap<String, Object>();
			t2.putAll(t);
	
			if( extended != null && extended.matches("^(?i)(yes|true|1)$") )
			{
				Vector<Map<String, String>> spaces = Database.getInstance().select("SELECT space_id FROM instance_space WHERE instance_id = " + t.get("instance_id"));
				t2.put("spaces", spaces);

				Vector<Map<String, String>> children = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + t.get("instance_id"));
				t2.put("children", children);
				
				Vector<Map<String, String>> delegates = Database.getInstance().select("SELECT delegate_id, delegate_value, delegate_lease FROM delegates WHERE delegate_instance = " + t.get("instance_id"));
				t2.put("delegates", delegates);
			}
			
			instances.add(t2);
		}
		
		// ================================
		// SHARES
		// ================================
		tmp = Database.getInstance().select("SELECT DISTINCT ii.instance_id, ii.interface_name, ii.interface_share_identity, ii.connector_id as instance_connector, id.identity_principal, id.identity_description, us.space_id as instance_space, us.instance_position, " +
			"ii.interface_share_tax, ii.interface_share_description, ii.interface_key, ci.interface_type, 1 as instance_shared " + 
			"FROM user_shared us " +
			"LEFT JOIN instance_interface ii ON(us.instance_id = ii.instance_id AND us.interface_name = ii.interface_name) " + 
			"LEFT JOIN connector_interface ci ON(ci.connector_id = ii.connector_id AND ci.interface_key = ii.interface_key) " + 
			"LEFT JOIN spaces s ON(s.space_id = us.space_id) " + 
			"LEFT JOIN users u ON(u.user_id = u.user_id) " + 
			"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " + 
			"LEFT JOIN identities id ON(id.identity_id = ii.interface_share_identity) " + 
			"WHERE 1=1 " + where + where2 + " " +
			"ORDER BY ii.instance_id DESC");
			
		for( Map<String, String> t : tmp )
		{
			Map<String, Object> t2 = new HashMap<String, Object>();
			t2.putAll(t);
			t2.put("identity_name", PrincipalUtil.shortName((String)t2.remove("identity_principal")));
			instances.add(t2);
		}
		
		// ================================
		// EXTENDED
		// ================================
		if( extended != null && extended.matches("^(?i)(yes|true|1)$") )
		{
			for( Map<String, Object> i : instances )
			{				
				// ================================
				// CONFIG
				// ================================
				Vector<Map<String, String>> configs = Database.getInstance().select("SELECT ic.config_key, ic.config_value, cc.config_hidden " + 
					"FROM instance_config ic " + 
					"LEFT JOIN connector_config cc ON(cc.connector_id = ic.connector_id AND cc.config_key = ic.config_key) " + 
					"WHERE ic.instance_id = " + i.get("instance_id"));
				
				for( Map<String, String> c : configs )
				{
					// caution : decrypt the config value
					if( c.get("config_value") != null )
						c.put("config_value", CryptoSimple.unserialize(c.get("config_value")));
				}
				
				i.put("configs", configs);
				
				// ================================
				// CONNECTOR
				// ================================
				String l = "";
				Map<String, Object> connector = new HashMap<String, Object>();
				for( int attempt = 0; attempt < 3; attempt++ )
				{
					// FIRST attempt : with provided language
					if( attempt == 0 )
						l = " AND ct.translation_language = '" + language + "'";
					// SECOND attempt : with EN
					else if( attempt == 1 )
						l = " AND ct.translation_language = 'EN'";
					// THIRD attempt : any (including null)
					else
						l = "";
						
					Map<String, String> tmp2 = Database.getInstance().selectOne("SELECT ct.translation_language, t.translation_id, t.translation_text, " +
						"c.connector_name, c.connector_direction, c.connector_use_price, c.connector_buy_tax, c.connector_buy_price, c.connector_use_tax " + 
						"FROM connectors c " +
						"LEFT JOIN connector_translation ct ON(c.connector_id = ct.connector_id) " +
						"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
						"WHERE c.connector_id = " + i.get("instance_connector") + l);
					
					double vat = Double.parseDouble(Config.gets("com.busit.rest.vat.percent"));
					
					if( tmp2.get("connector_use_price") != null && tmp2.get("connector_use_tax") != null && tmp2.get("connector_buy_price") != null && tmp2.get("connector_buy_tax") != null )
					{
						tmp2.put("connector_use_price_ttc", "" + (Double.parseDouble(tmp2.get("connector_use_price")) + Math.ceil(vat / 100.0 * Double.parseDouble(tmp2.get("connector_use_price"))) + Double.parseDouble(tmp2.get("connector_use_tax"))));
						tmp2.put("connector_buy_price_ttc", "" + (Double.parseDouble(tmp2.get("connector_buy_price")) + Math.ceil(vat / 100.0 * Double.parseDouble(tmp2.get("connector_buy_price"))) + Double.parseDouble(tmp2.get("connector_buy_tax"))));
					}
					
					if( tmp2 == null || tmp2.get("translation_id") == null )
						continue;
					
					connector.putAll(tmp2);
					
					if( connector.get("translation_text") != null )
						connector.putAll(Json.decode(connector.remove("translation_text").toString()).map());
					break;
				}
				i.put("connector", connector);
				
				// ================================
				// CONNECTOR INTERFACES
				// ================================
				Map<String, String> connectorint = ConnectorInterface.INTERNAL_SELECT(i.get("instance_connector").toString(), null, null, null, language);
				i.put("connector_interfaces", connectorint);
				
				// ================================
				// INTERFACES
				// ================================
				Vector<Map<String, String>> interfaces = InstanceInterface.INTERNAL_SELECT(i.get("instance_id").toString(), null, null, null, language);
				i.put("interfaces", interfaces);
				
				String where3 = " l.instance_from = " + i.get("instance_id");
				
				if( space.size() > 0 )
				{
					where3 += " AND (1>1";
					for( String s : space )
					{
						if( s.matches("^[0-9]+$") )
							where3 += " OR s.space_id = " + Security.escape(s);
						else
							where3 += " OR s.space_name = '" + Security.escape(s) + "'";
					}
					where3 += ")";
				}
				
				if( i.get("instance_shared").equals("1") )
				{
					// this is a shared instance, so everything belongs to the original user
					// and should not be included here
					// EXCEPT links that target an instance of this current list (= same user)
					Vector<Map<String, String>> links = Database.getInstance().select("SELECT instance_from, interface_from, instance_to, interface_to, link_active " + 
						"FROM links " + 
						"WHERE instance_from = " + i.get("instance_id"));
					for( int link = links.size() -1; link >= 0; link-- )
					{
						boolean found = false;
						for( int j = 0; j < instances.size(); j++ )
						{
							if( instances.get(j).get("instance_id").equals(links.get(link).get("instance_to")) )
							{
								found = true;
								break;
							}
						}
						
						if( !found )
							links.remove(link);
					}
					i.put("links", links);
				
					continue;
				}
				
				// ================================
				// LINKS
				// ================================
				Vector<Map<String, String>> links = Database.getInstance().select("SELECT DISTINCT l.link_id, ls.link_order, l.instance_from, l.interface_from, l.instance_to, l.interface_to, l.link_active " + 
					"FROM links l " +
					"LEFT JOIN link_space ls ON(ls.link_id = l.link_id) " + 
					"LEFT JOIN spaces s ON(s.space_id = ls.space_id) " +
					"WHERE " + where3 + " " +
					"ORDER BY ls.link_order ASC");
				
				i.put("links", links);
				
				String where4 = " l.instance_to = " + i.get("instance_id");
				
				// ================================
				// LINKS TO
				// ================================
				Vector<Map<String, String>> links2 = Database.getInstance().select("SELECT DISTINCT l.link_id, ls.link_order, l.instance_from, l.interface_from, l.instance_to, l.interface_to, l.link_active " + 
					"FROM links l " +
					"LEFT JOIN link_space ls ON(ls.link_id = l.link_id) " + 
					"LEFT JOIN spaces s ON(s.space_id = ls.space_id) " +
					"WHERE " + where4 + " " +
					"ORDER BY ls.link_order ASC");
				
				i.put("links_to", links2);
			}
		}
		
		return instances;
	}
	
	private void initializeSearch(Index index)
	{
		Action search = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				String keyword = getParameter("keyword").getValue();
				String uuid = getParameter("uuid").getValue();
				String connectorid = getParameter("connector").getValue();
				String language = getParameter("language").getValue().substring(0,2).toUpperCase();
				String extended = getParameter("extended").getValue();
				String running = getParameter("running").getValue();
				String language2 = language;
				
				String where = "";
				
				if( user != null )
				{
					if( !user.matches("^[0-9]+$") )
						user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					
					where += " AND instance_user = " + user;
				}
				
				if( running != null && running.matches("^(?i)(yes|true|1)$") )
					where += " AND i.instance_running";
				else if( running != null )
					where += " AND NOT i.instance_running";
				
				if( uuid != null )
					where += " AND i.instance_uuid = '" + Security.escape(uuid) + "'";
				if( connectorid != null )
					where += " AND i.instance_connector = '" + Security.escape(connectorid) + "'";
					
				// split the keyword for AND searches
				if( keyword != null )
				{
					Matcher m = Pattern.compile("([^\\s,;\\+]+)").matcher(keyword);
					while(m.find())
					{
						where += " AND (t.translation_text LIKE '%" + Security.escape(m.group(1)) + 
							"%' OR i.instance_name LIKE '%" + Security.escape(m.group(1)) + 
							"%')";
					}
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
						language = "";
					
					Vector<Map<String, Object>> instances = new Vector<Map<String, Object>>();
					
					String sql = "SELECT DISTINCT i.instance_id, i.instance_connector, i.instance_user, " + 
					"i.instance_date, i.instance_name, i.instance_active, i.instance_configured, i.instance_hits, " + 
					"i.instance_position, i.instance_running, i.instance_parent, 0 as instance_shared " + 
					"FROM instances i " +
					"LEFT JOIN connectors c ON(c.connector_id = i.instance_connector) " +
					"LEFT JOIN connector_translation ct ON(ct.connector_id = c.connector_id" + language + ") " +
					"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
					"WHERE 1=1 " + where + (attempt < 2 ? " AND t.translation_id IS NOT NULL" : "") + " ORDER BY instance_name DESC LIMIT 10";

					Vector<Map<String, String>> tmp = Database.getInstance().select(sql);
					
					for( Map<String, String> t : tmp )
					{
						Map<String, Object> t2 = new HashMap<String, Object>();
						t2.putAll(t);
						
						Vector<Map<String, String>> spaces = Database.getInstance().select("SELECT space_id FROM instance_space WHERE instance_id = " + t.get("instance_id"));
						t2.put("spaces", spaces);
						
						Vector<Map<String, String>> children = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + t.get("instance_id"));
						t2.put("children", children);
						
						Vector<Map<String, String>> delegates = Database.getInstance().select("SELECT delegate_id, delegate_value, delegate_lease FROM delegates WHERE delegate_instance = " + t.get("instance_id"));
						t2.put("delegates", delegates);
						
						instances.add(t2);
					}
					
					if( instances != null && instances.size() > 0 )
					{
						// ================================
						// EXTENDED
						// ================================
						if( extended != null && extended.matches("^(?i)(yes|true|1)$") )
						{
							for( Map<String, Object> i : instances )
							{
								if( i.get("instance_shared").equals("1") )
								{
									// this is a shared instance, so everything belongs to the original user
									// and should not be included here
									// EXCEPT links that target an instance of this current list (= same user)
									Vector<Map<String, String>> links = Database.getInstance().select("SELECT instance_from, interface_from, instance_to, interface_to, link_active " + 
										"FROM links " + 
										"WHERE instance_from = " + i.get("instance_id"));
									for( int l = links.size() -1; l >= 0; l-- )
									{
										boolean found = false;
										for( int j = 0; j < instances.size(); j++ )
										{
											if( instances.get(j).get("instance_id").equals(links.get(l).get("instance_to")) )
											{
												found = true;
												break;
											}
										}
										
										if( !found )
											links.remove(l);
									}
									i.put("links", links);
								
									continue;
								}
				
								// ================================
								// CONFIG
								// ================================
								Vector<Map<String, String>> configs = Database.getInstance().select("SELECT ic.config_key, ic.config_value, cc.config_hidden " + 
									"FROM instance_config ic " + 
									"LEFT JOIN connector_config cc ON(cc.connector_id = ic.connector_id AND cc.config_key = ic.config_key) " + 
									"WHERE ic.instance_id = " + i.get("instance_id"));
								
								for( Map<String, String> c : configs )
								{
									// caution : decrypt the config value
									if( c.get("config_value") != null )
										c.put("config_value", CryptoSimple.unserialize(c.get("config_value")));
								}
								
								i.put("configs", configs);
				
								// ================================
								// CONNECTOR
								// ================================
								Map<String, Object> connector = new HashMap<String, Object>();
								Map<String, String> tmp2 = Database.getInstance().selectOne("SELECT ct.translation_language, t.translation_id, t.translation_text, " +
									"c.connector_name, c.connector_direction, c.connector_use_price, c.connector_buy_tax, c.connector_buy_price, c.connector_use_tax " + 
									"FROM connectors c " +
									"LEFT JOIN connector_translation ct ON(c.connector_id = ct.connector_id) " +
									"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
									"WHERE c.connector_id = " + i.get("instance_connector") + language);
								
								double vat = Double.parseDouble(Config.gets("com.busit.rest.vat.percent"));
								
								if( tmp2.get("connector_use_price") != null && tmp2.get("connector_use_tax") != null && tmp2.get("connector_buy_price") != null && tmp2.get("connector_buy_tax") != null )
								{
									tmp2.put("connector_use_price_ttc", "" + (Double.parseDouble(tmp2.get("connector_use_price")) + Math.ceil(vat / 100.0 * Double.parseDouble(tmp2.get("connector_use_price"))) + Double.parseDouble(tmp2.get("connector_use_tax"))));
									tmp2.put("connector_buy_price_ttc", "" + (Double.parseDouble(tmp2.get("connector_buy_price")) + Math.ceil(vat / 100.0 * Double.parseDouble(tmp2.get("connector_buy_price"))) + Double.parseDouble(tmp2.get("connector_buy_tax"))));
								}
								
								if( tmp2 == null || tmp2.get("translation_id") == null )
									continue;
								
								connector.putAll(tmp2);
									
								if( connector.get("translation_text") != null )
									connector.putAll(Json.decode(connector.remove("translation_text").toString()).map());
									
								i.put("connector", connector);
								
								// ================================
								// CONNECTOR INTERFACES
								// ================================
								Map<String, String> connectorint = ConnectorInterface.INTERNAL_SELECT(i.get("instance_connector").toString(), null, null, null, language2);
								i.put("connector_interfaces", connectorint);
								
								// ================================
								// INTERFACES
								// ================================
								Vector<Map<String, String>> interfaces = InstanceInterface.INTERNAL_SELECT(i.get("instance_id").toString(), null, null, null, language2);
								i.put("interfaces", interfaces);
								
								String where3 = " l.instance_from = " + i.get("instance_id");
								
								// ================================
								// LINKS
								// ================================
								Vector<Map<String, String>> links = Database.getInstance().select("SELECT DISTINCT l.link_id, ls.link_order, l.instance_from, l.interface_from, l.instance_to, l.interface_to, l.link_active " + 
									"FROM links l " +
									"LEFT JOIN link_space ls ON(ls.link_id = l.link_id) " + 
									"LEFT JOIN spaces s ON(s.space_id = ls.space_id) " +
									"WHERE " + where3 + " " +
									"ORDER BY ls.link_order ASC");
								
								i.put("links", links);
							}
						}
						
						return instances;
					}
				}
				
				// THIS SHOULD NEVER HAPPEN...
				return new Vector<Map<String, String>>();
			}
		};
		
		search.addMapping(new String[] { "search", "find" });
		search.description = "Search for connectors or instances based on a keyword.";
		search.returnDescription = "The matching instances [{'name', 'id'},...]";
		search.addGrant(new String[] { "access", "space_select" });
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		search.addParameter(user);
		
		Parameter keyword = new Parameter();
		keyword.isOptional = true;
		keyword.minLength = 3;
		keyword.maxLength = 30;
		keyword.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		keyword.description = "The keyword to search for";
		keyword.addAlias(new String[]{ "keyword", "key", "needle", "text" });
		search.addParameter(keyword);
		
		Parameter lang = new Parameter();
		lang.isOptional = false;
		lang.minLength = 2; // FR
		lang.maxLength = 5; // FR_BE
		lang.mustMatch = "^[a-zA-Z]{2}(_[a-zA-Z]{2})?$";
		lang.description = "The language to search for";
		lang.addAlias(new String[]{ "lang", "language", "locale" });
		search.addParameter(lang);
		
		Parameter extended = new Parameter();
		extended.isOptional = true;
		extended.minLength = 1;
		extended.maxLength = 10;
		extended.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		extended.description = "Show extended information?";
		extended.addAlias(new String[]{ "extended", "more" });
		search.addParameter(extended);
		
		Parameter running = new Parameter();
		running.isOptional = true;
		running.minLength = 1;
		running.maxLength = 10;
		running.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		running.description = "Only show running instance?";
		running.addAlias(new String[]{ "running", "instance_running" });
		search.addParameter(running);
		
		Parameter connectorid = new Parameter();
		connectorid.isOptional = true;
		connectorid.minLength = 1;
		connectorid.maxLength = 30;
		connectorid.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		connectorid.description = "The instance connector";
		connectorid.addAlias(new String[]{ "connector", "connector_id" });
		search.addParameter(connectorid);
		
		Parameter uuid = new Parameter();
		uuid.isOptional = true;
		uuid.minLength = 1;
		uuid.maxLength = 90;
		uuid.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		uuid.description = "The instance uuid";
		uuid.addAlias(new String[]{ "uuid", "instance_uuid", "device_id" });
		search.addParameter(uuid);
		
		index.addOwnHandler(search);
	}
}
