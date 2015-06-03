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

public class InstanceInterfacePublic extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "public", "publics" });
		index.description = "Manages instance public interfaces";
		Handler.addHandler("/busit/instance/interface/", index);
		
		initializeUpdate(index);
		initializeSelect(index);
		
		Self.selfize("/busit/instance/interface/public/update");
		Self.selfize("/busit/instance/interface/public/select");
	}
	
	private void initializeUpdate(Index index)
	{
		Action update = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String key = getParameter("key").getValue();
				String name = getParameter("name").getValue();
				String pub = getParameter("public").getValue();
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
					if( !IdentityChecker.getInstance().isUserIdentity(user, identity) &&  !IdentityChecker.getInstance().isUserIdentityAdmin(user, identity) )
						throw new Exception("The current user is not and administrator of the provided identity");
				}
				else
					identity = "null";
				
				if( pub.matches("^(?i)(yes|true|1)$") )
					pub = "true";
				else
					pub = "false";
				
				if( key != null )
					key = " AND ii.interface_key = '" + Security.escape(key) + "'";
				else
					key = "";
				
				if( name != null )
					name = " AND ii.interface_name = '" + Security.escape(name) + "'";
				else
					name = "";
					
				// todo update childs
				Database.getInstance().update("UPDATE instance_interface ii SET ii.interface_public = " + pub + ", ii.interface_public_identity = " + identity + " " +
					"WHERE ii.instance_id = " + instance + key + name);
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modify an instance public interface";
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
		
		Parameter pub = new Parameter();
		pub.isOptional = false;
		pub.minLength = 1;
		pub.maxLength = 5;
		pub.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		pub.description = "Whether the interface is public or not";
		pub.addAlias(new String[]{ "public", "interface_public" });
		update.addParameter(pub);

		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 20;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		identity.description = "The public source identity id (if not specified, it will be reset)";
		identity.addAlias(new String[]{ "identity", "public_identity" });
		update.addParameter(identity);
		
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
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String key = getParameter("key").getValue();
				String name = getParameter("name").getValue();
				String pub = getParameter("public").getValue();
				String identity = getParameter("identity").getValue();
				String user = getParameter("user").getValue();
				
				if( user != null )
				{
					if( !user.matches("^[0-9]+$") )
						user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				}
				
				if( instance != null )
				{			
					if( !instance.matches("^[0-9]+$") && user != null )
						instance = "(SELECT instance_id FROM instances where instance_name = '" + Security.escape(instance) + "' AND instance_user = " + user + ")";
					
					instance = " AND ii.instance_id = " + instance;
				}
				else
					instance = "";
					
				if( user != null )
					user = " AND u.user_id = " + user;
				else
					user = "";
				
				if( identity != null )
				{			
					if( !identity.matches("^[0-9]+$") )
						identity = "(SELECT identity_id FROM identities where identity_principal = '" + Security.escape(PrincipalUtil.parse(identity).getName()) + "')";
					
					identity = " AND ii.interface_public_identity = " + identity;
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
				
				if( pub == null || pub.matches("^(?i)(yes|true|1)$") )
					pub = " AND ii.interface_public = 1";
				else
					pub = " AND ii.interface_public = 0";
					
				return Database.getInstance().select("SELECT ii.instance_id, ii.interface_key, ii.interface_name, ii.interface_public, ii.interface_public_identity, u.user_id " + 
					"FROM instance_interface ii " + 
					"LEFT JOIN instances i ON(ii.instance_id = i.instance_id) " + 
					"LEFT JOIN users u ON(u.user_id = i.instance_user) " +
					"WHERE 1=1 " + user + pub + key + name + instance + identity);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Select instance public interface.";
		select.returnDescription = "The matching interfaces [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "instance_select" });
		
		Parameter instance = new Parameter();
		instance.isOptional = true;
		instance.minLength = 1;
		instance.maxLength = 30;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		instance.allowInUrl = true;
		instance.description = "The instance name or id (will match *[instance]* if not a number or an exact instance id match if numeric)";
		instance.addAlias(new String[]{ "instance_name", "id", "iid", "instance_id", "instance" });
		select.addParameter(instance);
		
		Parameter pub = new Parameter();
		pub.isOptional = true;
		pub.minLength = 1;
		pub.maxLength = 5;
		pub.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		pub.description = "Whether the interface is public or not (default true)";
		pub.addAlias(new String[]{ "public", "interface_public" });
		select.addParameter(pub);

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
		identity.addAlias(new String[]{ "identity", "identity_name", "identity_id" });
		select.addParameter(identity);

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
