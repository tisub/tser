package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;

public class Relation extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "relation", "relations" });
		index.description = "Manages relations";
		Handler.addHandler("/busit/", index);
		
		initializeInsert(index);
		initializeDelete(index);
		initializeSelect(index);

		Self.selfize("/busit/relation/insert");
		Self.selfize("/busit/relation/delete");
		Self.selfize("/busit/relation/select");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String space = getParameter("space").getValue();
				String instance = getParameter("instance").getValue();
				String name = getParameter("name").getValue();
				String user = getParameter("user").getValue();

				if( !IdentityChecker.getInstance().isUserSpace(user, space) )
					throw new Exception("The target user does not own the provided space");
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				Map<String, String> check = Database.getInstance().selectOne("SELECT us.instance_id " + 
					"FROM user_shared us WHERE us.instance_id = " + instance + " AND us.space_id = " + space + " AND us.interface_name = '" + Security.escape(name) + "'");
				
				if( check.get("instance_id") != null )
					throw new Exception("Already exists.");
				else
					Database.getInstance().insert("INSERT INTO user_shared (instance_id, interface_name, space_id) VALUES (" + instance + ", '" + Security.escape(name) + "', " + space + ")");
				
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new relation";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "user_select" });
		
		Parameter space = new Parameter();
		space.isOptional = false;
		space.minLength = 1;
		space.maxLength = 20;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		space.description = "The space id(s)";
		space.addAlias(new String[]{ "space", "space_id" });
		insert.addParameter(space);

		Parameter name = new Parameter();
		name.isOptional = false;
		name.minLength = 1;
		name.maxLength = 200;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		name.description = "The interface name";
		name.addAlias(new String[]{ "interface_name", "name" });
		insert.addParameter(name);
		
		Parameter instance = new Parameter();
		instance.isOptional = false;
		instance.minLength = 1;
		instance.maxLength = 20;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instance.description = "The instance id";
		instance.addAlias(new String[]{ "instance", "instance_id" });
		insert.addParameter(instance);
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		insert.addParameter(user);
		
		index.addOwnHandler(insert);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String name = getParameter("name").getValue();
				String space = getParameter("space").getValue();
				String user = getParameter("user").getValue();
				
				if( !IdentityChecker.getInstance().isUserSpace(user, space) )
					throw new Exception("The target user does not own the provided space");
					
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";	

				// delete links from this relation
				Database.getInstance().delete("DELETE l FROM links l " +
				"LEFT JOIN instances i ON(l.instance_to = i.instance_id) " +
				"WHERE l.instance_from = " +  Security.escape(instance)  + " AND l.interface_from = '" + Security.escape(name) + "' AND i.instance_user = " + user + " AND i.instance_space = " + space);
				
				Database.getInstance().delete("DELETE FROM user_shared WHERE instance_id = " + instance + " AND space_id = " + space + " AND interface_name = '" + Security.escape(name) + "'");
				
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a relation";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "user_select" });

		Parameter space = new Parameter();
		space.isOptional = false;
		space.minLength = 1;
		space.maxLength = 20;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		space.description = "The space id(s)";
		space.addAlias(new String[]{ "space", "space_id" });
		delete.addParameter(space);
		
		Parameter name = new Parameter();
		name.isOptional = false;
		name.minLength = 1;
		name.maxLength = 200;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		name.description = "The interface name";
		name.addAlias(new String[]{ "interface_name", "name" });
		delete.addParameter(name);
		
		Parameter instance = new Parameter();
		instance.isOptional = false;
		instance.minLength = 1;
		instance.maxLength = 20;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instance.description = "The instance id";
		instance.addAlias(new String[]{ "instance", "instance_id" });
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
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String space = getParameter("space").getValue();
				String instance = getParameter("instance").getValue();
				String name = getParameter("name").getValue();
				String identity = getParameter("identity").getValue();
				String grouped = getParameter("grouped").getValue();
				String count = getParameter("count").getValue();
				String userof = getParameter("userof").getValue();
				String user = getParameter("user").getValue();
				
				String where = "";
				if( user != null )
				{
					if( !user.matches("^[0-9]+$") )
						user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					
					where += " AND s.space_user = " + user;
				}
				if( userof != null )
				{
					if( !userof.matches("^[0-9]+$") )
						userof = "(SELECT user_id FROM users where user_name = '" + Security.escape(userof) + "')";
					
					where += " AND inst.instance_user = " + userof;
				}
				if( instance != null )
					where += " AND us.instance_id = " + instance;
				if( name != null )
					where += " AND us.interface_name = '" + Security.escape(name) + "'";
				if( space != null )
					where += " AND us.space_id = " + space;
				if( identity != null )
					where += " AND ii.interface_share_identity = " + identity;
				
				String group = "";
				if( grouped != null && grouped.matches("^(?i)(yes|true|1)$") )
					group += "GROUP BY ii.interface_share_identity ";

				if( count != null && count.matches("^(?i)(yes|true|1)$") )
					return Database.getInstance().selectOne("SELECT COUNT(DISTINCT us.space_id) as count FROM user_shared us " + 
						"LEFT JOIN instance_interface ii ON(ii.instance_id = us.instance_id AND ii.interface_name = us.interface_name) " + 
						"LEFT JOIN identities i ON(i.identity_id = ii.interface_share_identity) " + 
						"LEFT JOIN spaces s ON(s.space_id = us.space_id) " +
						"LEFT JOIN instances inst ON(inst.instance_id = ii.instance_id) " + 
						"WHERE 1=1 " + where);
				
				Vector<Map<String,String>> rels = Database.getInstance().select("SELECT s.space_name, s.space_description, i.identity_principal, i.identity_description, s.space_user as user_id, us.space_id, " + 
				"ii.instance_id, ii.interface_name, ii.interface_key, ii.interface_share_identity, ii.interface_share_description, ii.interface_share_tax, inst.instance_user FROM user_shared us " +
				"LEFT JOIN instance_interface ii ON(ii.instance_id = us.instance_id AND ii.interface_name = us.interface_name) " + 
				"LEFT JOIN identities i ON(i.identity_id = ii.interface_share_identity) " + 
				"LEFT JOIN instances inst ON(inst.instance_id = ii.instance_id) " + 
				"LEFT JOIN spaces s ON(us.space_id = s.space_id) " +
				"WHERE 1=1 " + where + " " + group);
				
				for( Map<String,String> r : rels )
					r.put("identity_name", PrincipalUtil.shortName(r.remove("identity_principal")));
				
				return rels;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a relation.";
		select.returnDescription = "The matching relation [{'user', 'target'},...]";
		select.addGrant(new String[] { "access", "user_select" });

		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 1;
		name.maxLength = 200;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		name.description = "The interface name";
		name.addAlias(new String[]{ "interface_name", "name" });
		select.addParameter(name);
		
		Parameter instance = new Parameter();
		instance.isOptional = true;
		instance.minLength = 1;
		instance.maxLength = 20;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instance.description = "The instance id";
		instance.addAlias(new String[]{ "instance", "instance_id" });
		select.addParameter(instance);
		
		Parameter space = new Parameter();
		space.isOptional = true;
		space.minLength = 1;
		space.maxLength = 20;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		space.description = "The space id";
		space.addAlias(new String[]{ "space", "space_id" });
		select.addParameter(space);

		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 20;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		identity.description = "The identity id";
		identity.addAlias(new String[]{ "identity", "identity_id" });
		select.addParameter(identity);
		
		Parameter grouped = new Parameter();
		grouped.isOptional = true;
		grouped.minLength = 1;
		grouped.maxLength = 10;
		grouped.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		grouped.description = "Group by identity?";
		grouped.addAlias(new String[]{ "group", "grouped" });
		select.addParameter(grouped);	

		Parameter count = new Parameter();
		count.isOptional = true;
		count.minLength = 1;
		count.maxLength = 10;
		count.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		count.description = "Count entires?";
		count.addAlias(new String[]{ "count" });
		select.addParameter(count);	

		Parameter userof = new Parameter();
		userof.isOptional = true;
		userof.minLength = 1;
		userof.maxLength = 30;
		userof.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		userof.description = "The user of stats.";
		userof.addAlias(new String[]{ "userof", "userof_stats", "userof_name" });
		select.addParameter(userof);
		
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