package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.security.*;
import com.busit.security.*;
import java.util.*;

public class Link extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "link", "links" });
		index.description = "Manages links";
		Handler.addHandler("/busit/", index);

		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);

		Self.selfize("/busit/link/insert");
		Self.selfize("/busit/link/update");
		Self.selfize("/busit/link/delete");
		Self.selfize("/busit/link/select");
	}

	private void initializeInsert(Index index)
	{
		Action insert = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String instanceFrom = getParameter("instance_from").getValue();
				String interfaceFrom = getParameter("interface_from").getValue();
				String instanceTo = getParameter("instance_to").getValue();
				String interfaceTo = getParameter("interface_to").getValue();
				String user = getParameter("user").getValue();
				String order = getParameter("order").getValue();
				String space = getParameter("space").getValue();
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instanceTo))
						throw new Exception("The current user is not an administrator of the provided instances");
						
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instanceFrom) )
					{
						if( !user.matches("^[0-9]+$") )
							user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";

						Map<String, String> check = Database.getInstance().selectOne("SELECT us.space_id " + 
							"FROM user_shared us " +
							"LEFT JOIN instance_interface ii ON(us.instance_id = ii.instance_id AND us.interface_name = ii.interface_name) " +
							"LEFT JOIN spaces s ON(s.space_id = us.space_id) " +
							"WHERE s.space_user = " + user + " AND ii.instance_id = " + instanceFrom);
						
						if( check.get("space_id") == null )
							throw new Exception("The current user is not an administrator of the provided instances");
					}
				}
				
				// LINK INSERT
				
				// Check if the link already exists
				Map<String, String> check = Database.getInstance().selectOne("SELECT link_id FROM links WHERE 1=1 " + 
				"AND instance_from = '" + Security.escape(instanceFrom) + "' " +
				"AND interface_from = '" + Security.escape(interfaceFrom) + "' " +
				"AND instance_to = '" + Security.escape(instanceTo) + "' " +
				"AND interface_to = '" + Security.escape(interfaceTo) + "'");
				
				// Insert link if not exists
				Long uid = new Long(0);
				if( check.get("link_id") == null )
				{
					uid = Database.getInstance().insert("INSERT INTO links (instance_from, interface_from, instance_to, interface_to) VALUES " + 
					"('" + Security.escape(instanceFrom) + "', '" + 
					Security.escape(interfaceFrom) + "', '" + 
					Security.escape(instanceTo) + "', '" + 
					Security.escape(interfaceTo) + "')");
				}
				else
					uid = Long.parseLong(check.get("link_id"));
				
				// LINK TAG
				if( space != null )
				{
					if( order != null )
						Database.getInstance().insert("INSERT INTO link_space (space_id, link_id, link_order) VALUES (" + space + ", " + uid + ", " + order + ") ON DUPLICATE KEY UPDATE link_order = " + order);
					else
						Database.getInstance().insert("INSERT INTO link_space (space_id, link_id, link_order) VALUES (" + space + ", " + uid + ", 0) ON DUPLICATE KEY UPDATE link_order = link_order");
				}
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("instance_from", instanceFrom);
				result.put("instance_to", instanceTo);

				return result;
			}
		};

		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new link";
		insert.returnDescription = "The newly created link {'instance_from', 'instance_to'}";
		insert.addGrant(new String[] { "access", "link_insert" });

		Parameter instanceFrom = new Parameter();
		instanceFrom.isOptional = false;
		instanceFrom.minLength = 1;
		instanceFrom.maxLength = 30;
		instanceFrom.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instanceFrom.description = "The source instance of the link (from)";
		instanceFrom.addAlias(new String[]{ "instance_from" });
		insert.addParameter(instanceFrom);

		Parameter interfaceFrom = new Parameter();
		interfaceFrom.isOptional = false;
		interfaceFrom.minLength = 1;
		interfaceFrom.maxLength = 200;
		interfaceFrom.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		interfaceFrom.description = "The interface of the source instance";
		interfaceFrom.addAlias(new String[]{ "interface_from" });
		insert.addParameter(interfaceFrom);

		Parameter instanceTo = new Parameter();
		instanceTo.isOptional = false;
		instanceTo.minLength = 1;
		instanceTo.maxLength = 30;
		instanceTo.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instanceTo.description = "The destination instance of the link (to).";
		instanceTo.addAlias(new String[]{ "instance_to" });
		insert.addParameter(instanceTo);

		Parameter interfaceTo = new Parameter();
		interfaceTo.isOptional = false;
		interfaceTo.minLength = 1;
		interfaceTo.maxLength = 200;
		interfaceTo.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		interfaceTo.description = "The interface of the destination instance";
		interfaceTo.addAlias(new String[]{ "interface_to" });
		insert.addParameter(interfaceTo);

		Parameter space = new Parameter();
		space.isOptional = true;
		space.minLength = 1;
		space.maxLength = 50;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		space.allowInUrl = true;
		space.description = "The link space";
		space.addAlias(new String[]{ "space", "space_description", "space_id" });
		insert.addParameter(space);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		insert.addParameter(user);
		
		Parameter order = new Parameter();
		order.isOptional = true;
		order.minLength = 1;
		order.maxLength = 30;
		order.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		order.description = "The order number";
		order.addAlias(new String[]{ "order", "ordered", "order_number" });
		insert.addParameter(order);

		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String link = getParameter("link").getValue();
				String instanceFrom = getParameter("instance_from").getValue();
				String interfaceFrom = getParameter("interface_from").getValue();
				String instanceTo = getParameter("instance_to").getValue();
				String interfaceTo = getParameter("interface_to").getValue();
				String active = getParameter("active").getValue();
				String user = getParameter("user").getValue();
				String order = getParameter("order").getValue();
				Collection<String> space = getParameter("space").getValues();
				String spacemethod = getParameter("spacemethod").getValue();
				
				if( user != null )
				{
					Map<String, String> data = Database.getInstance().selectOne("SELECT link_id, instance_from, instance_to FROM links WHERE link_id = " + link);
					instanceTo = data.get("instance_to");
					instanceFrom = data.get("instance_from");
					
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instanceTo))
						throw new Exception("The current user is not an administrator of the provided instances");
					
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instanceFrom) )
					{
						if( !user.matches("^[0-9]+$") )
							user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
						
						Map<String, String> check = Database.getInstance().selectOne("SELECT us.space_id " + 
							"FROM user_shared us " +
							"LEFT JOIN instance_interface ii ON(us.instance_id = ii.instance_id AND us.interface_name = ii.interface_name) " +
							"LEFT JOIN spaces s ON(s.space_id = us.space_id) " +
							"WHERE s.space_user = " + user + " AND ii.instance_id = " + instanceFrom);
						
						if( check.get("space_id") == null )
							throw new Exception("The current user is not an administrator of the provided instances");
					}
				}

				String set = "";
				
				if( instanceFrom != null )
					set += "instance_from = '" + Security.escape(instanceFrom) + "', ";
				if( interfaceFrom != null )
					set += "interface_from = '" + Security.escape(interfaceFrom) + "', ";
				if( instanceTo != null )
					set += "instance_to = '" + Security.escape(instanceTo) + "', ";
				if( interfaceTo != null )
					set += "interface_to = '" + Security.escape(interfaceTo) + "', ";
				if( active != null )
					set += "link_active = " + (active.matches("^(?i)(yes|true|1)$") ? "TRUE" : "FALSE");
				
				// Update the link
				Database.getInstance().update("UPDATE links SET link_id = link_id, " + set + " WHERE link_id = " + link);

				// Tag in space
				if( spacemethod == null )
					spacemethod = "add";

				if( space.size() > 0 )
				{
					for( String s : space )
					{
						if( spacemethod.equals("add") )
						{
							if( order != null )
								Database.getInstance().insert("INSERT INTO link_space (space_id, link_id, link_order) VALUES (" + s + ", " + Security.escape(link) + ", " + order + ") ON DUPLICATE KEY UPDATE link_order = " + order);
							else
								Database.getInstance().insert("INSERT INTO link_space (space_id, link_id, link_order) VALUES (" + s + ", " + Security.escape(link) + ", " + order + ") ON DUPLICATE KEY UPDATE link_order = link_order");
						}
						else if( spacemethod.equals("delete") )
							Database.getInstance().insert("DELETE FROM link_space WHERE space_id = " + s + " AND link_id = " + Security.escape(link));
						else
							throw new Exception("Unsupported space method");
					}
				}
				
				return "OK";
			}
		};

		update.addMapping(new String[] { "update", "change", "modify", "activate", "deactivate" });
		update.description = "Update a link";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "link_update" });

		Parameter link = new Parameter();
		link.isOptional = false;
		link.minLength = 1;
		link.maxLength = 30;
		link.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		link.description = "The link id.";
		link.addAlias(new String[]{ "link", "id", "link_id", "lid" });
		update.addParameter(link);
		
		Parameter instanceFrom = new Parameter();
		instanceFrom.isOptional = true;
		instanceFrom.minLength = 1;
		instanceFrom.maxLength = 30;
		instanceFrom.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instanceFrom.description = "The source instance of the link (from)";
		instanceFrom.addAlias(new String[]{ "instance_from" });
		update.addParameter(instanceFrom);

		Parameter interfaceFrom = new Parameter();
		interfaceFrom.isOptional = true;
		interfaceFrom.minLength = 1;
		interfaceFrom.maxLength = 200;
		interfaceFrom.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		interfaceFrom.description = "The interface of the source instance";
		interfaceFrom.addAlias(new String[]{ "interface_from" });
		update.addParameter(interfaceFrom);

		Parameter instanceTo = new Parameter();
		instanceTo.isOptional = true;
		instanceTo.minLength = 1;
		instanceTo.maxLength = 30;
		instanceTo.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instanceTo.description = "The destination instance of the link (to).";
		instanceTo.addAlias(new String[]{ "instance_to" });
		update.addParameter(instanceTo);

		Parameter interfaceTo = new Parameter();
		interfaceTo.isOptional = true;
		interfaceTo.minLength = 1;
		interfaceTo.maxLength = 200;
		interfaceTo.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		interfaceTo.description = "The interface of the destination instance";
		interfaceTo.addAlias(new String[]{ "interface_to" });
		update.addParameter(interfaceTo);

		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		update.addParameter(user);
		
		Parameter active = new Parameter();
		active.isOptional = true;
		active.minLength = 1;
		active.maxLength = 5;
		active.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		active.description = "Whether or not the link is active.";
		active.addAlias(new String[]{ "active", "on" });
		update.addParameter(active);
		
		Parameter order = new Parameter();
		order.isOptional = true;
		order.minLength = 1;
		order.maxLength = 30;
		order.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		order.description = "The order number";
		order.addAlias(new String[]{ "order", "ordered", "order_number" });
		update.addParameter(order);
		
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
		
		index.addOwnHandler(update);
	}

	private void initializeDelete(Index index)
	{
		Action delete = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String link = getParameter("link").getValue();
				String instanceFrom = getParameter("instance_from").getValue();
				String interfaceFrom = getParameter("interface_from").getValue();
				String instanceTo = getParameter("instance_to").getValue();
				String interfaceTo = getParameter("interface_to").getValue();
				String user = getParameter("user").getValue();

				if( user != null )
				{
					if( link != null )
					{
						Map<String, String> data = Database.getInstance().selectOne("SELECT link_id, instance_from, instance_to FROM links WHERE link_id = " + link);
						instanceTo = data.get("instance_to");
						instanceFrom = data.get("instance_from");
					}
					
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instanceTo))
						throw new Exception("The current user is not an administrator of the provided instances");
						
					if( !IdentityChecker.getInstance().isUserInstanceAdmin(user, instanceFrom) )
					{
						if( !user.matches("^[0-9]+$") )
							user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
										
						Map<String, String> check = Database.getInstance().selectOne("SELECT us.space_id " + 
							"FROM user_shared us " +
							"LEFT JOIN instance_interface ii ON(us.instance_id = ii.instance_id AND us.interface_name = ii.interface_name) " +
							"LEFT JOIN spaces s ON(s.space_id = us.space_id) " +
							"WHERE s.space_user = " + user + " AND ii.instance_id = " + instanceFrom);
						
						if( check.get("space_id") == null )
							throw new Exception("The current user is not an administrator of the provided instances");
					}
				}
				
				String where = "";
				if( link != null )
					where = "link_id = " + link;
				else if( instanceFrom != null && interfaceFrom != null && instanceTo != null && interfaceTo != null )
					where = "instance_from = '" + Security.escape(instanceFrom) + "' AND interface_from = '" + Security.escape(interfaceFrom) + "' AND instance_to = '" + Security.escape(instanceTo) + "' AND interface_to = '" + Security.escape(interfaceTo) + "'";
				else
					throw new Exception("You have to give at least link id or link informations");
					
				Database.getInstance().insert("DELETE FROM links WHERE " + where);

				return "OK";
			}
		};

		delete.addMapping(new String[] { "delete", "del", "destroy", "remove" });
		delete.description = "Delete a link";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "link_delete" });

		Parameter link = new Parameter();
		link.isOptional = true;
		link.minLength = 1;
		link.maxLength = 30;
		link.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		link.description = "The link id.";
		link.addAlias(new String[]{ "link", "id", "link_id", "lid" });
		delete.addParameter(link);
		
		Parameter instanceFrom = new Parameter();
		instanceFrom.isOptional = true;
		instanceFrom.minLength = 1;
		instanceFrom.maxLength = 30;
		instanceFrom.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instanceFrom.description = "The source instance of the link (from)";
		instanceFrom.addAlias(new String[]{ "instance_from" });
		delete.addParameter(instanceFrom);

		Parameter interfaceFrom = new Parameter();
		interfaceFrom.isOptional = true;
		interfaceFrom.minLength = 1;
		interfaceFrom.maxLength = 200;
		interfaceFrom.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		interfaceFrom.description = "The interface of the source instance";
		interfaceFrom.addAlias(new String[]{ "interface_from" });
		delete.addParameter(interfaceFrom);

		Parameter instanceTo = new Parameter();
		instanceTo.isOptional = true;
		instanceTo.minLength = 1;
		instanceTo.maxLength = 30;
		instanceTo.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instanceTo.description = "The destination instance of the link (to).";
		instanceTo.addAlias(new String[]{ "instance_to" });
		delete.addParameter(instanceTo);

		Parameter interfaceTo = new Parameter();
		interfaceTo.isOptional = true;
		interfaceTo.minLength = 1;
		interfaceTo.maxLength = 200;
		interfaceTo.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		interfaceTo.description = "The interface of the destination instance";
		interfaceTo.addAlias(new String[]{ "interface_to" });
		delete.addParameter(interfaceTo);

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
				String instanceFrom = getParameter("instance_from").getValue();
				String interfaceFrom = getParameter("interface_from").getValue();
				String instanceTo = getParameter("instance_to").getValue();
				String interfaceTo = getParameter("interface_to").getValue();
				String active = getParameter("active").getValue();
				String operator = getParameter("operator").getValue();
				String user = getParameter("user").getValue();
				String count = getParameter("count").getValue();
				String extended = getParameter("extended").getValue();
				String fromShared = "";
				String toShared = "";
				
				if( user != null && instanceFrom != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceUser(user, instanceFrom) )
					{
						String user2 = user;
						
						if( !user2.matches("^[0-9]+$") )
							user2 = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
						
						Map<String, String> check = Database.getInstance().selectOne("SELECT us.space_id " + 
							"FROM user_shared us " +
							"LEFT JOIN instance_interface ii ON(us.instance_id = ii.instance_id AND us.interface_name = ii.interface_name) " +
							"LEFT JOIN spaces s ON(s.space_id = us.space_id) " +
							"WHERE s.space_user = " + user + " AND us.instance_id = " + instanceFrom);
						
						if( check.get("space_id") == null )
							throw new Exception("The current user is not an member of the 'from' instance");
						else
							fromShared = ", 1 as instance_shared";
					}
				}
				if( user != null && instanceTo != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceUser(user, instanceTo) )
					{
						String user2 = user;
						
						if( !user2.matches("^[0-9]+$") )
							user2 = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
						
						Map<String, String> check = Database.getInstance().selectOne("SELECT us.space_id " + 
							"FROM user_shared us " +
							"LEFT JOIN instance_interface ii ON(us.instance_id = ii.instance_id AND us.interface_name = ii.interface_name) " +
							"LEFT JOIN spaces s ON(s.space_id = us.space_id) " +
							"WHERE s.space_user = " + user + " AND us.instance_id = " + instanceTo);
						
						if( check.get("space_id") == null )
							throw new Exception("The current user is not an member of the 'to' instance");
						else
							toShared = ", 1 as instance_shared";
					}						
				}
				
				if( user != null && !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
							
				String where = "";
				String joins = "";
				String op = "AND";
				if( operator != null )
					 op = operator;
				
				if( instanceFrom != null)
					where += " " + op + " l.instance_from = " + Security.escape(instanceFrom);
				if( instanceTo != null )
					where += " " + op + " l.instance_to = " + Security.escape(instanceTo);
				if( interfaceFrom != null)
					where += " " + op + " l.interface_from = '" + Security.escape(interfaceFrom) + "'";
				if( interfaceTo != null )
					where += " " + op + " l.interface_to = '" + Security.escape(interfaceTo) + "'";
				if( active != null )
					where += " " + op + " l.link_active = " + (active.matches("^(?i)(yes|true|1)$") ? "TRUE" : "FALSE");
				if( user != null )
				{
					where += " " + op + " u.user_id = " + user;
					joins = "LEFT JOIN users o ON(o.user_id = i.instance_user) " + 
						"LEFT JOIN org_member om ON(om.org_id = o.user_id) " + 
						"LEFT JOIN identities i2 ON(i2.identity_id = om.identity_id) " +
						"LEFT JOIN users u ON(i2.identity_user = u.user_id OR u.user_id = i.instance_user) ";
				}
					
				if( count != null && count.matches("^(?i)(yes|true|1)$") )
					return Database.getInstance().select("SELECT COUNT(*) as count FROM links WHERE 1=1 " + where);
					
				Vector<HashMap<String, Object>> result = new Vector<HashMap<String, Object>>();
				Vector<Map<String, String>> links = Database.getInstance().select("SELECT l.link_id, l.instance_from, l.interface_from, l.instance_to, l.interface_to " + 
					"FROM links l " + 
					"LEFT JOIN instances i ON(l.instance_to = i.instance_id) " + joins + 
					"WHERE 1=1 " + where + " " +
					"ORDER BY l.link_id ASC");
			
				if( extended != null && extended.matches("^(?i)(yes|true|1)$") )
				{
					for( Map<String, String> l : links )
					{
						HashMap<String, Object> row = new HashMap<String, Object>();
						
						Vector<Map<String, String>> spaces = Database.getInstance().select("SELECT space_id FROM link_space WHERE link_id = " + l.get("link_id"));
						row.put("spaces", spaces);
						
						Map<String, String> source = Database.getInstance().selectOne("SELECT instance_id, instance_connector " + fromShared + " FROM instances WHERE instance_id = " + l.get("instance_from"));
						Map<String, String> target = Database.getInstance().selectOne("SELECT instance_id, instance_connector " + toShared + " FROM instances WHERE instance_id = " + l.get("instance_to"));
						
						row.put("source", source);
						row.put("target", target);
					
						for( String key : l.keySet() )
							row.put(key, l.get(key));

						result.add(row);
					}
					
					return result;
				}
				else				
					return links;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about links.";
		select.returnDescription = "The matching links [{'instance_from', 'instance_to'},...]";
		select.addGrant(new String[] { "access", "link_select" });
		
		Parameter instanceFrom = new Parameter();
		instanceFrom.isOptional = true;
		instanceFrom.minLength = 1;
		instanceFrom.maxLength = 30;
		instanceFrom.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instanceFrom.description = "The source instance of the link (from)";
		instanceFrom.addAlias(new String[]{ "instance_from" });
		select.addParameter(instanceFrom);

		Parameter interfaceFrom = new Parameter();
		interfaceFrom.isOptional = true;
		interfaceFrom.minLength = 1;
		interfaceFrom.maxLength = 200;
		interfaceFrom.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		interfaceFrom.description = "The interface of the source instance";
		interfaceFrom.addAlias(new String[]{ "interface_from" });
		select.addParameter(interfaceFrom);

		Parameter instanceTo = new Parameter();
		instanceTo.isOptional = true;
		instanceTo.minLength = 1;
		instanceTo.maxLength = 30;
		instanceTo.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instanceTo.description = "The destination instance of the link (to).";
		instanceTo.addAlias(new String[]{ "instance_to" });
		select.addParameter(instanceTo);

		Parameter interfaceTo = new Parameter();
		interfaceTo.isOptional = true;
		interfaceTo.minLength = 1;
		interfaceTo.maxLength = 200;
		interfaceTo.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		interfaceTo.description = "The interface of the destination instance";
		interfaceTo.addAlias(new String[]{ "interface_to" });
		select.addParameter(interfaceTo);
		
		Parameter operator = new Parameter();
		operator.isOptional = true;
		operator.minLength = 1;
		operator.maxLength = 3;
		operator.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.UPPER);
		operator.description = "The operator for selection (OR / AND)";
		operator.addAlias(new String[]{ "operator" });
		select.addParameter(operator);
		
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
		count.description = "Count messages?";
		count.addAlias(new String[]{ "count" });
		select.addParameter(count);	

		Parameter extended = new Parameter();
		extended.isOptional = true;
		extended.minLength = 1;
		extended.maxLength = 10;
		extended.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		extended.description = "Show extended information?";
		extended.addAlias(new String[]{ "extended", "more" });
		select.addParameter(extended);
		
		Parameter active = new Parameter();
		active.isOptional = true;
		active.minLength = 1;
		active.maxLength = 5;
		active.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		active.description = "Whether or not the link is active. Default: dont care";
		active.addAlias(new String[]{ "active", "on" });
		select.addParameter(active);
		
		index.addOwnHandler(select);
	}
}
