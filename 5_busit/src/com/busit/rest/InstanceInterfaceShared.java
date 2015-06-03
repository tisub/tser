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

public class InstanceInterfaceShared extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "shared" });
		index.description = "Manages instance shared interfaces";
		Handler.addHandler("/busit/instance/interface/", index);
		
		initializeUpdate(index);
		initializeSelect(index);
		
		Self.selfize("/busit/instance/interface/shared/update");
		Self.selfize("/busit/instance/interface/shared/select");
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
				String identity = getParameter("identity").getValue();
				String shared = getParameter("shared").getValue();
				String description = getParameter("description").getValue();
				String tax = getParameter("tax").getValue();
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
					identity = ", ii.interface_share_identity = " + identity;
				}
				else
					identity = ", ii.interface_share_identity = null";
				
				if( shared != null && shared.matches("^(?i)(yes|true|1)$") )
					shared = ", ii.interface_shared = true";
				else if( shared != null )
				{
					shared = ", ii.interface_shared = false";
					// todo :  delete links from pseudo-instances, delete user_shared entries
				}
				else
					shared = "";
				
					
				if( key != null )
					key = " AND ii.interface_key = '" + Security.escape(key) + "'";
				else
					key = "";
				
				if( name != null )
					name = " AND ii.interface_name = '" + Security.escape(name) + "'";
				else
					name = "";
					
				if( description != null )
					description = ", interface_share_description = '" + Security.escape(description) + "'";
				else
					description = "";
					
				if( tax != null )
					tax = ", interface_share_tax = " + tax;
				else
					tax = "";
					
				Database.getInstance().update("UPDATE instance_interface ii SET ii.instance_id = ii.instance_id" + identity + shared + description + tax + 
					" WHERE ii.instance_id = " + instance + key + name);

				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modify an instance shared interface";
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
		
		Parameter identity = new Parameter();
		identity.isOptional = false;
		identity.minLength = 1;
		identity.maxLength = 30;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		identity.description = "The shared source identity id";
		identity.addAlias(new String[]{ "identity", "shared_identity" });
		update.addParameter(identity);

		Parameter description = new Parameter();
		description.isOptional = true;
		description.minLength = 3;
		description.maxLength = 200;
		description.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		description.description = "The identity description";
		description.addAlias(new String[]{ "description", "identity_description" });
		update.addParameter(description);

		Parameter tax = new Parameter();
		tax.isOptional = true;
		tax.minLength = 1;
		tax.maxLength = 20;
		tax.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		tax.description = "The interface tax";
		tax.addAlias(new String[]{ "tax", "price" });
		update.addParameter(tax);
		
		Parameter shared = new Parameter();
		shared.isOptional = true;
		shared.minLength = 1;
		shared.maxLength = 5;
		shared.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		shared.description = "Whether the interface is shared or not";
		shared.addAlias(new String[]{ "shared", "interface_shared" });
		update.addParameter(shared);
		
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
				String identity = getParameter("identity").getValue();
				String shared = getParameter("shared").getValue();
				String user = getParameter("user").getValue();

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
					
					instance = " AND ii.instance_id = " + instance;
				}
				else
					instance = "";
				
				if( identity != null )
				{			
					if( !identity.matches("^[0-9]+$") )
						identity = "(SELECT identity_id FROM identities where identity_principal = '" + Security.escape(PrincipalUtil.parse(identity).getName()) + "')";
					
					identity = " AND ii.interface_share_identity = " + identity;
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
					
				if( shared == null || shared.matches("^(?i)(yes|true|1)$") )
					shared = " AND ii.interface_shared = 1";
				else
					shared = " AND ii.interface_shared = 0";
				
				return Database.getInstance().select("SELECT ii.instance_id, ii.interface_key, ii.interface_name, ii.interface_shared, ii.interface_share_identity, ii.interface_share_description, ii.interface_share_tax, u.user_id " + 
					"FROM instance_interface ii " + 
					"LEFT JOIN instances i ON(ii.instance_id = i.instance_id) " + 
					"LEFT JOIN users u ON(u.user_id = i.instance_user) " +
					"WHERE 1=1 " + user + shared + key + instance + identity + name);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Select instance shared interface.";
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

		Parameter count = new Parameter();
		count.isOptional = true;
		count.minLength = 1;
		count.maxLength = 10;
		count.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		count.description = "Count entires?";
		count.addAlias(new String[]{ "count" });
		select.addParameter(count);	
		
		Parameter shared = new Parameter();
		shared.isOptional = true;
		shared.minLength = 1;
		shared.maxLength = 5;
		shared.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		shared.description = "Whether the interface is shared or not";
		shared.addAlias(new String[]{ "shared", "interface_shared" });
		select.addParameter(shared);
		
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

	static int INTERNAL_SHAREPRICE(String instanceFrom, String interfaceFrom, String userFrom, String instanceTo, String interfaceTo, String userTo) throws Exception
	{
		// ========================
		// FROM
		// userFrom is sharing instanceFrom with userTo. Hence, userTo has a user_shared in his own space
		// ========================
		if( userTo.matches("^[0-9]+$") )
			userTo = " AND u.user_id = " + userTo;
		else
			userTo = " AND u.user_name = '" + Security.escape(userTo) + "'";
		
		if( instanceFrom.matches("^[0-9]+$") )
			instanceFrom = " AND i.instance_id = " + instanceFrom;
		else
			instanceFrom = " AND i.instance_name = '" + Security.escape(instanceFrom) + "'";
			
		interfaceFrom = " AND us.interface_name = '" + Security.escape(interfaceFrom) + "'";

		Map<String, String> row_from = Database.getInstance().selectOne("SELECT ii.interface_share_tax, u.user_id, i.instance_id " + 
			"FROM user_shared us " +
			"LEFT JOIN spaces s ON(us.space_id = s.space_id) " +
			"LEFT JOIN users u ON(u.user_id = s.space_user) " + 
			"LEFT JOIN instance_interface ii ON(ii.instance_id = us.instance_id AND ii.interface_name = us.interface_name) " +
			"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " +
			"WHERE 1=1 " + userTo + instanceFrom + interfaceFrom + " LIMIT 1");

		// ========================
		// TO
		// userTo is sharing instanceTo with userFrom. Hence, userFrom has a user_shared in his own space
		// ========================
		if( userFrom.matches("^[0-9]+$") )
			userFrom = " AND u.user_id = " + userFrom;
		else
			userFrom = " AND u.user_name = '" + Security.escape(userFrom) + "'";
		
		if( instanceTo.matches("^[0-9]+$") )
			instanceTo = " AND i.instance_id = " + instanceTo;
		else
			instanceTo = " AND i.instance_name = '" + Security.escape(instanceTo) + "'";
			
		interfaceTo = " AND us.interface_name = '" + Security.escape(interfaceTo) + "'";

		Map<String, String> row_to = Database.getInstance().selectOne("SELECT ii.interface_share_tax, u.user_id, i.instance_id " + 
			"FROM user_shared us " +
			"LEFT JOIN spaces s ON(us.space_id = s.space_id) " +
			"LEFT JOIN users u ON(u.user_id = s.space_user) " + 
			"LEFT JOIN instance_interface ii ON(ii.instance_id = us.instance_id AND ii.interface_name = us.interface_name) " +
			"LEFT JOIN instances i ON(i.instance_id = ii.instance_id) " +
			"WHERE 1=1 " + userFrom + instanceTo + interfaceTo + " LIMIT 1");
		
		boolean is_from = (row_from != null && row_from.get("interface_share_tax") != null);
		boolean is_to = (row_to != null && row_to.get("interface_share_tax") != null);
		if( !is_from && !is_to )
			throw new Exception("The target instance is not shared with the provided user");
		if( !is_from )
			return Integer.parseInt(row_to.get("interface_share_tax"));
		if( !is_to )
			return Integer.parseInt(row_from.get("interface_share_tax"));
			
		// past this point, the instance is shared from both parties...
		// TODO
		return 0;
	}
}
