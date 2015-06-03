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

public class InstanceInterfaceCron extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "cron", "crons" });
		index.description = "Manages instance interfaces crons";
		Handler.addHandler("/busit/instance/interface/", index);
		
		initializeUpdate(index);
		initializeSelect(index);
		
		Self.selfize("/busit/instance/interface/cron/update");
		Self.selfize("/busit/instance/interface/cron/select");
	}
	
	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String key = getParameter("key").getValue();
				String name = getParameter("name").getValue();
				String cron = getParameter("cron").getValue();
				String identity = getParameter("identity").getValue();
				String user = getParameter("user").getValue();

				if( key == null && name == null )
					throw new Exception("One of key or name must be specified");
					
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instance) )
						throw new Exception("The current user is not an administrator of the provided instance");
				}

				if( identity != null )
				{
					if( !IdentityChecker.getInstance().isUserIdentity(user, identity) && !IdentityChecker.getInstance().isUserIdentityAdmin(user, identity) )
						throw new Exception("The current user is not and administrator of the provided identity");
				}
				else
					identity = "null";
				
				if( key != null )
					key = " AND ii.interface_key = '" + Security.escape(key) + "'";
				else
					key = "";
				
				if( name != null )
					name = " AND ii.interface_name = '" + Security.escape(name) + "'";
				else
					name = "";
					
				if( cron.equals("OFF") )
					cron = "NULL";
				
				// check for parent instance and childs
				Map<String, String> result = Database.getInstance().selectOne("SELECT instance_parent FROM instances WHERE instance_id = " + instance);
				if( result.get("instance_parent").equals("0") )
				{
					// no parents, update
					Database.getInstance().update(
					"UPDATE instance_interface ii " + 
					"LEFT JOIN connector_interface ci ON(ii.connector_id = ci.connector_id) " + 
					"SET ii.interface_cron_timer = '" + Security.escape(cron) + "', ii.interface_cron_identity = " + identity + " " +
					"WHERE ci.interface_cron AND ii.instance_id = " + instance + key + name);
					
					// update children if any
					Vector<Map<String, String>> data = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + instance);
					for( Map<String, String> d : data )
					{
						Database.getInstance().update(
						"UPDATE instance_interface ii " + 
						"LEFT JOIN connector_interface ci ON(ii.connector_id = ci.connector_id) " + 
						"SET ii.interface_cron_timer = '" + Security.escape(cron) + "', ii.interface_cron_identity = " + identity + " " +
						"WHERE ci.interface_cron AND ii.instance_id = " + d.get("instance_id") + key + name);
					}
				}
				else
				{
					// have a parent, update the parent
					Database.getInstance().update(
						"UPDATE instance_interface ii " + 
						"LEFT JOIN connector_interface ci ON(ii.connector_id = ci.connector_id) " + 
						"SET ii.interface_cron_timer = '" + Security.escape(cron) + "', ii.interface_cron_identity = " + identity + " " +
						"WHERE ci.interface_cron AND ii.instance_id = " + result.get("instance_parent") + key + name);

					// update children
					Vector<Map<String, String>> data = Database.getInstance().select("SELECT instance_id FROM instances WHERE instance_parent = " + result.get("instance_parent"));
					for( Map<String, String> d : data )
					{
						Database.getInstance().update(
						"UPDATE instance_interface ii " + 
						"LEFT JOIN connector_interface ci ON(ii.connector_id = ci.connector_id) " + 
						"SET ii.interface_cron_timer = '" + Security.escape(cron) + "', ii.interface_cron_identity = " + identity + " " +
						"WHERE ci.interface_cron AND ii.instance_id = " + d.get("instance_id") + key + name);
					}
				}
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modify an instance interface cron";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "instance_update" });

		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The instance id";
		id.addAlias(new String[]{ "instance", "id", "instance_id", "iid" });
		update.addParameter(id);

		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 1;
		key.maxLength = 50;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		key.description = "The interface key";
		key.addAlias(new String[]{ "key", "interface_key", "interface" });
		update.addParameter(key);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 1;
		name.maxLength = 200;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		name.description = "The interface name";
		name.addAlias(new String[]{ "interface_name", "name" });
		update.addParameter(name);
		
		Parameter cron = new Parameter();
		cron.isOptional = false;
		cron.minLength = 1;
		cron.maxLength = 50;
		cron.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		cron.description = "The interface cron";
		cron.addAlias(new String[]{ "cron", "interface_cron" });
		update.addParameter(cron);

		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 30;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		identity.description = "The cron source identity id (if not specified, it will be reset)";
		identity.addAlias(new String[]{ "identity", "cron_identity" });
		update.addParameter(identity);
		
		Parameter user = new Parameter();
		user.isOptional = false;
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
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String key = getParameter("key").getValue();
				String name = getParameter("name").getValue();
				String cron = getParameter("cron").getValue();
				String identity = getParameter("identity").getValue();
				String user = getParameter("user").getValue();
				String linked = getParameter("linked").getValue();
				String cronned = getParameter("cronned").getValue();

				if( user != null )
				{			
					if( !user.matches("^[0-9]+$") )
						user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					
					user = " AND user_id = " + user;
				}
				else
					user = "";
				
				if( instance != null )
				{			
					if( !instance.matches("^[0-9]+$") )
						instance = "(SELECT instance_id FROM instances where instance_name = '" + Security.escape(instance) + "')";
					
					instance = " AND instance_id = " + instance;
				}
				else
					instance = "";
				
				if( identity != null )
				{			
					if( !identity.matches("^[0-9]+$") )
						identity = "(SELECT identity_id FROM identities where identity_principal = '" + Security.escape(PrincipalUtil.parse(identity).getName()) + "')";
					
					identity = " AND ii.interface_cron_identity = " + identity;
				}
				else
					identity = "";
				
				if( key != null )
					key = " AND ii.interface_key = '" + Security.escape(key) + "'";
				else
					key = "";
				
				if( name != null )
					name = " AND ii.interface_name = '" + Security.escape(name) + "'";
				else
					name = "";
					
				if( cron != null )
					cron = " AND ii.interface_cron_timer = '" + Security.escape(cron) + "'";
				else
					cron = "";
				
				String linkedJoin = "";
				if( linked != null && linked.matches("^(1|yes|true)$") )
				{
					// must have a link OR must be an input interface
					linkedJoin = "LEFT JOIN links l ON((l.instance_from = i.instance_id AND l.interface_from = ii.interface_name) OR (l.instance_to = i.instance_id AND l.interface_to = ii.interface_name)) " + 
								 "LEFT JOIN connector_interface ci ON(ci.connector_id = i.instance_connector AND ci.interface_key = ii.interface_key)";
					linked = " AND (l.instance_from IS NOT NULL OR ci.interface_type = 1)";
				}
				else
					linked = "";
				
				if( cronned != null && cronned.matches("^$") )
					cron += " AND ii.interface_cron_timer IS NOT NULL";

				// CAUTION HERE (TODO ?)
                // we are selecting the default identity directly in the select to avoid doing it in the loop
                Any data = Any.wrap(Database.getInstance().select("SELECT DISTINCT ii.instance_id, ii.interface_name, ii.interface_key, ii.interface_cron_timer, ii.interface_cron_identity, u.user_id, x.identity_id " + 
                    "FROM instance_interface ii " + 
                    "LEFT JOIN instances i ON(ii.instance_id = i.instance_id) " + 
                    "LEFT JOIN users u ON(u.user_id = i.instance_user) " + 
                    "LEFT JOIN identities x ON(x.identity_id = (SELECT y.identity_id FROM identities y WHERE y.identity_death IS NULL AND y.identity_user = u.user_id LIMIT 1)) " + 
					linkedJoin + 
                    "WHERE x.identity_id IS NOT NULL " + user + cron + key + instance + identity + name + linked));

                for( Any a : data )
                {
                    if( a.isEmpty("interface_cron_identity") )
                        //a.set("interface_cron_identity", "" + IdentityChecker.getInstance().defaultIdentityId(getParameter("user").getValue()));
                        a.set("interface_cron_identity", a.<String>value("identity_id"));
                    a.remove("identity_id");
                }
				
				return data;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Select instance interface cronable.";
		select.returnDescription = "The matching interfaces [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "instance_select" });
		
		Parameter instance = new Parameter();
		instance.isOptional = true;
		instance.minLength = 1;
		instance.maxLength = 30;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		instance.allowInUrl = true;
		instance.description = "The instance name or id (will match *[instance]* if not a number or an exact instance id match if numeric)";
		instance.addAlias(new String[]{ "name", "instance_name", "id", "iid", "instance_id", "instance" });
		select.addParameter(instance);

		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 1;
		key.maxLength = 50;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		key.description = "The interface key";
		key.addAlias(new String[]{ "key", "interface_key", "interface" });
		select.addParameter(key);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 1;
		name.maxLength = 200;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		name.description = "The interface name";
		name.addAlias(new String[]{ "interface_name", "name" });
		select.addParameter(name);
		
		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity.description = "The identity name or id.";
		identity.addAlias(new String[]{ "identity", "identity_name", "name", "identity_id" });
		select.addParameter(identity);

		Parameter cron = new Parameter();
		cron.isOptional = true;
		cron.minLength = 1;
		cron.maxLength = 50;
		cron.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		cron.description = "The interface cron";
		cron.addAlias(new String[]{ "cron", "interface_cron" });
		select.addParameter(cron);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		select.addParameter(user);
		
		Parameter linked = new Parameter();
		linked.isOptional = true;
		linked.minLength = 1;
		linked.maxLength = 5;
		linked.mustMatch = "^(1|0|yes|no|true|false)$";
		linked.description = "Whether or not the interface must have active links to it";
		linked.addAlias(new String[]{ "linked" });
		select.addParameter(linked);
		
		Parameter cronned = new Parameter();
		cronned.isOptional = true;
		cronned.minLength = 1;
		cronned.maxLength = 5;
		cronned.mustMatch = "^(1|0|yes|no|true|false)$";
		cronned.description = "Whether or not the interface must have 'some' cron";
		cronned.addAlias(new String[]{ "cronned" });
		select.addParameter(cronned);
		
		index.addOwnHandler(select);			
	}
}
