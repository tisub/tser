package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;

public class Space extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "space", "spaces" });
		index.description = "Manages spaces";
		Handler.addHandler("/busit/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeCleanup(index);
		initializeSelect(index);
		initializeSearch(index);

		Self.selfize("/busit/space/insert");
		Self.selfize("/busit/space/update");
		Self.selfize("/busit/space/delete");
		Self.selfize("/busit/space/cleanup");
		Self.selfize("/busit/space/select");
		Self.selfize("/busit/space/search");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String name = getParameter("name").getValue();
				String showcase = getParameter("showcase").getValue();
				String user = getParameter("user").getValue();
				String org = getParameter("org").getValue();
				
				if( user == null && org == null )
					throw new Exception("One of user or org must be specified");
				
				// org prevails over user
				if( org != null )
				{
					if( !IdentityChecker.getInstance().isUserOrgAdmin(Security.getInstance().getUser(), org) )
						throw new Exception("The target user is not an admin of the provided organization");
					user = org;
				}
				else if( IdentityChecker.getInstance().isOrg(user) )
				{
					if( !IdentityChecker.getInstance().isUserOrgAdmin(Security.getInstance().getUser(), user) )
						throw new Exception("The target user is not an admin of the provided organization");
				}
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				if( showcase == null )
					showcase = "0";
				
				Quota.getInstance().increment(user, Config.gets("com.busit.rest.quota.space"));
				
				Long uid = Database.getInstance().insert("INSERT INTO spaces (space_name, space_user, space_date, space_showcase) VALUES " + 
					"('" + Security.escape(name) + "', " + user + ", UNIX_TIMESTAMP(), '" + Security.escape(showcase) + "')");
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", name);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new space";
		insert.returnDescription = "The newly created space {'name', 'id'}";
		insert.addGrant(new String[] { "access", "space_insert" });
		
		Parameter name = new Parameter();
		name.isOptional = false;
		name.minLength = 3;
		name.maxLength = 250;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.allowInUrl = true;
		name.description = "The space name";
		name.addAlias(new String[]{ "name", "space_name", "space" });
		insert.addParameter(name);

		Parameter showcase = new Parameter();
		showcase.isOptional = true;
		showcase.minLength = 1;
		showcase.maxLength = 1;
		showcase.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		showcase.description = "Is this space a showcase (1|0)?";
		showcase.addAlias(new String[]{ "showcase", "space_showcase" });
		insert.addParameter(showcase);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		insert.addParameter(user);
		
		Parameter organization = new Parameter();
		organization.isOptional = true;
		organization.minLength = 1;
		organization.maxLength = 30;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		organization.description = "The organization name or id (prevails over user)";
		organization.addAlias(new String[]{ "org", "organization_name", "org_name", "organization_id", "org_id", "oid" });
		insert.addParameter(organization);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String space = getParameter("space").getValue();
				String name = getParameter("name").getValue();
				String description = getParameter("description").getValue();
				String showcase = getParameter("showcase").getValue();
				String active = getParameter("active").getValue();
				String category = getParameter("category").getValue();
				String metadata = getParameter("metadata").getValue();
				String user = getParameter("user").getValue();

				if( !IdentityChecker.getInstance().isUserSpaceAdmin(user, space) )
					throw new Exception("The current user is not an administrator of the provided space");
				
				String set = "";
				if( name != null )
					set += "space_name = '" + Security.escape(name) + "', ";
				if( description != null )
					set += "space_description = '" + Security.escape(description) + "', ";
				if( showcase != null )
					set += "space_showcase = '" + Security.escape(showcase) + "', ";
				if( active != null )
					set += "space_active = '" + Security.escape(active) + "', ";
				if( category != null )
					set += "space_category = '" + Security.escape(category) + "', ";
				if( metadata != null )
					set += "space_metadata = '" + Security.escape(metadata) + "', ";
					
				Database.getInstance().update("UPDATE spaces SET " + set + " space_id = space_id WHERE space_id = " + space);

				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modify a space";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "space_update" });
	
		Parameter space = new Parameter();
		space.isOptional = false;
		space.minLength = 1;
		space.maxLength = 30;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		space.allowInUrl = true;
		space.description = "The space id";
		space.addAlias(new String[]{ "space", "id", "space_id", "sid" });
		update.addParameter(space);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 3;
		name.maxLength = 250;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.description = "The space name";
		name.addAlias(new String[]{ "name" });
		update.addParameter(name);
	
		Parameter description = new Parameter();
		description.isOptional = true;
		description.minLength = 1;
		description.maxLength = 500;
		description.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		description.description = "The space description";
		description.addAlias(new String[]{ "description", "space_description" });
		update.addParameter(description);

		Parameter showcase = new Parameter();
		showcase.isOptional = true;
		showcase.minLength = 1;
		showcase.maxLength = 1;
		showcase.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		showcase.description = "Is this space a showcase (1|0)?";
		showcase.addAlias(new String[]{ "showcase", "space_showcase" });
		update.addParameter(showcase);

		Parameter active = new Parameter();
		active.isOptional = true;
		active.minLength = 1;
		active.maxLength = 1;
		active.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		active.description = "Is this space active (1|0)?";
		active.addAlias(new String[]{ "active", "space_active" });
		update.addParameter(active);
		
		Parameter category = new Parameter();
		category.isOptional = true;
		category.minLength = 1;
		category.maxLength = 2;
		category.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		category.description = "Space category";
		category.addAlias(new String[]{ "category", "space_category" });
		update.addParameter(category);
		
		Parameter metadata = new Parameter();
		metadata.isOptional = true;
		metadata.minLength = 1;
		metadata.maxLength = 200;
		metadata.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		metadata.description = "Space metadata";
		metadata.addAlias(new String[]{ "metadata", "space_metadata" });
		update.addParameter(metadata);
		
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
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> space = getParameter("space").getValues();
				String user = getParameter("user").getValue();
				
				String where = "1=1";
				if( space.size() > 0 )
				{
					where += " AND (1>1";
					for( String s : space )
					{
						if( !IdentityChecker.getInstance().isUserSpaceAdmin(user, s) )
							throw new Exception("The current user is not an administrator of the provided space");
						
						where += " OR space_id = " + s;
					}
					where += ")";
				}
				
				String where2 = "1=1";
				if( space.size() > 0 )
				{
					where2 += " AND (1>1";
					for( String s : space )
					{
						where2 += " OR s.space_id = " + s;
					}
					where2 += ")";
				}
				
				String where3 = "1=1";
				if( space.size() > 0 )
				{
					for( String s : space )
					{
						where3 += " AND space_id != " + s;
					}
				}
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users WHERE user_name = '" + Security.escape(user) + "')";
				
				// ================================
				// LINKS
				// ================================
				Vector<Map<String, String>> links = Database.getInstance().select("SELECT l.link_id " + 
					"FROM links l " +
					"LEFT JOIN link_space ls ON(ls.link_id = l.link_id) " + 
					"LEFT JOIN spaces s ON(s.space_id = ls.space_id) " +
					"WHERE " + where2 + " " +
					"ORDER BY ls.link_order ASC");
				
				for( Map<String, String> l : links )
				{
					// Check if the link is only tagged in this space
					Vector<Map<String, String>> check = Database.getInstance().select("SELECT link_id FROM link_space WHERE " + where3 + " AND link_id = " + l.get("link_id"));
					
					// // The link is used in another space, just untag
					if( check.size() > 0 )
					{
						Database.getInstance().delete("DELETE FROM link_space WHERE " + where + " AND link_id = " + l.get("link_id"));
					}
					// No other space, delete the link
					else
					{
						Database.getInstance().delete("DELETE FROM links WHERE link_id = " + l.get("link_id"));
					}
				}

				// ================================
				// INSTANCES WITH PARENTS
				// ================================
				Vector<Map<String, String>> instances = Database.getInstance().select("SELECT DISTINCT i.instance_id " + 
					"FROM instances i " +
					"LEFT JOIN instance_space isp ON(isp.instance_id = i.instance_id) " + 
					"LEFT JOIN spaces s ON(s.space_id = isp.space_id) " +
					"WHERE " + where2 + " AND i.instance_parent != 0 " +
					"ORDER BY i.instance_id ASC");
				
				for( Map<String, String> i : instances )
					Database.getInstance().delete("DELETE FROM instances WHERE instance_id = " + i.get("instance_id"));
				
				long count = Database.getInstance().delete("DELETE FROM spaces WHERE  " + where + " AND space_user = " + user);
				Quota.getInstance().substract(getParameter("user").getValue(), Config.gets("com.busit.rest.quota.space"), (int)count, true);
				
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a space";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "space_delete" });
		
		Parameter space = new Parameter();
		space.isOptional = false;
		space.minLength = 1;
		space.maxLength = 50;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		space.allowInUrl = true;
		space.isMultipleValues = true;
		space.description = "The space id(s)";
		space.addAlias(new String[]{ "space", "spaces", "id", "space_id", "ids", "space_ids", "space_ids", "ids" });
		delete.addParameter(space);
		
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
	
	private void initializeCleanup(Index index)
	{
		Action cleanup = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> space = getParameter("space").getValues();
				String user = getParameter("user").getValue();
				
				String where = "1=1";
				if( space.size() > 0 )
				{
					where += " AND (1>1";
					for( String s : space )
					{
						if( !IdentityChecker.getInstance().isUserSpaceAdmin(user, s) )
							throw new Exception("The current user is not an administrator of the provided space");
						
						where += " OR space_id = " + s;
					}
					where += ")";
				}
				
				String where2 = "1=1";
				if( space.size() > 0 )
				{
					where2 += " AND (1>1";
					for( String s : space )
					{
						where2 += " OR s.space_id = " + s;
					}
					where2 += ")";
				}
				
				String where3 = "1=1";
				if( space.size() > 0 )
				{
					for( String s : space )
					{
						where3 += " AND space_id != " + s;
					}
				}
				
				// ================================
				// LINKS
				// ================================
				Vector<Map<String, String>> links = Database.getInstance().select("SELECT l.link_id, ls.link_order, l.instance_from, l.interface_from, l.instance_to, l.interface_to, l.link_active " + 
					"FROM links l " +
					"LEFT JOIN link_space ls ON(ls.link_id = l.link_id) " + 
					"LEFT JOIN spaces s ON(s.space_id = ls.space_id) " +
					"WHERE " + where2 + " " +
					"ORDER BY ls.link_order ASC");
				
				for( Map<String, String> l : links )
				{
					// Check if the link is only tagged in this space
					Vector<Map<String, String>> check = Database.getInstance().select("SELECT link_id FROM link_space WHERE " + where3 + " AND link_id = " + l.get("link_id"));
					
					// // The link is used in another space, just untag
					if( check.size() > 0 )
					{
						Database.getInstance().delete("DELETE FROM link_space WHERE " + where + " AND link_id = " + l.get("link_id"));
					}
					// No other space, delete the link
					else
					{
						Database.getInstance().delete("DELETE FROM links WHERE link_id = " + l.get("link_id"));
					}
				}

				// ================================
				// INSTANCES WITH PARENTS
				// ================================
				Vector<Map<String, String>> instances = Database.getInstance().select("SELECT DISTINCT i.instance_id " + 
					"FROM instances i " +
					"LEFT JOIN instance_space isp ON(isp.instance_id = i.instance_id) " + 
					"LEFT JOIN spaces s ON(s.space_id = isp.space_id) " +
					"WHERE " + where2 + " AND i.instance_parent != 0 " +
					"ORDER BY i.instance_id ASC");
				
				for( Map<String, String> i : instances )
					Database.getInstance().delete("DELETE FROM instances WHERE instance_id = " + i.get("instance_id"));
				
				// ================================
				// UNTAG INSTANCES
				// ================================	
				Database.getInstance().delete("DELETE FROM instance_space WHERE " + where);
				
				return "OK";
			}
		};
		
		cleanup.addMapping(new String[] { "cleanup", "clean" });
		cleanup.description = "Clean a space";
		cleanup.returnDescription = "OK";
		cleanup.addGrant(new String[] { "access", "space_delete" });
		
		Parameter space = new Parameter();
		space.isOptional = false;
		space.minLength = 1;
		space.maxLength = 50;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		space.allowInUrl = true;
		space.isMultipleValues = true;
		space.description = "The space id(s)";
		space.addAlias(new String[]{ "space", "spaces", "id", "space_id", "ids", "space_ids", "space_ids", "ids" });
		cleanup.addParameter(space);
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		cleanup.addParameter(user);
		
		index.addOwnHandler(cleanup);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> space = getParameter("name").getValues();
				String showcase = getParameter("showcase").getValue();
				String category = getParameter("category").getValue();
				String limit = getParameter("limit").getValue();
				String user = getParameter("user").getValue();
				String org = getParameter("org").getValue();
				String extended = getParameter("extended").getValue();
				String language = getParameter("language").getValue();
				String count = getParameter("count").getValue();
				
				// CAUTION : showcase = public for all !
				
				if( limit == null )
					limit = "200";
				
				String where = "";
				
				// org prevails over user
				if( org != null )
				{
					if( !IdentityChecker.getInstance().isUserOrgMember(Security.getInstance().getUser(), org) )
						throw new Exception("The target user is not a member of the provided organization");
					user = org;
				}
				if( showcase != null && showcase.matches("^(?i)(yes|true|1)$") )
				{
					where += " AND space_showcase = 1";
				}
				if( category != null )
				{
					where += " AND space_category = " + category;
				}
				if( space.size() > 0 )
				{
					where += " AND (1>1";
					for( String m : space )
					{
						if( user != null )
						{
							if( !IdentityChecker.getInstance().isUserSpaceUser(user, m) )
								throw new Exception("The current user is not a member of the provided space");
						}
						
						if( m.matches("^[0-9]+$") )
							where += " OR space_id = " + Security.escape(m);
						else
							where += " OR space_name LIKE '%" + Security.escape(m) + "%'";
					}
					where += ")";
				}
				else if( user != null )
				{
					if( !user.matches("^[0-9]+$") )
						user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					
					where += " AND space_user = " + user;
				}
				
				// count only
				if( count != null && count.matches("^(?i)(yes|true|1)$") )
				{
					return Database.getInstance().select("SELECT COUNT(DISTINCT space_id) as count FROM spaces WHERE 1=1 " + where);
				}
		
				Vector<Map<String, String>> result = Database.getInstance().select("SELECT space_id, space_name, space_date, space_user, space_active, space_description, space_showcase, space_category, space_metadata " + 
					"FROM spaces WHERE 1=1 " + where + " ORDER BY space_name ASC LIMIT 0," + limit );

				if( extended != null && extended.matches("^(?i)(yes|true|1)$") )
				{
					Vector<Map<String, Object>> ext = new Vector<Map<String, Object>>();
					for( Map<String, String> row : result )
					{
						Map<String, Object> e = new HashMap<String, Object>();
						e.putAll(row);
						Collection<String> spaces = new ArrayList<String>();
						spaces.add(row.get("space_id"));
						e.put("instances", Instance.INTERNAL_SELECT(new LinkedList<String>(), spaces, null, null, null, "true", language, null, null, null));
						ext.add(e);
					}
					return ext;
				}
				else
					return result;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "view" });
		select.description = "Retrieves information about an space. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching space [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "space_select" });
		
		Parameter space = new Parameter();
		space.isOptional = true;
		space.minLength = 1;
		space.maxLength = 50;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		space.allowInUrl = true;
		space.isMultipleValues = true;
		space.description = "The space name or id (will match *[space]* if not a number or an exact space id match if numeric)";
		space.addAlias(new String[]{ "name", "space_name", "id", "mid", "space_id", "space", "names", "space_names", "spaces", "ids", "mids", "space_ids" });
		select.addParameter(space);

		Parameter showcase = new Parameter();
		showcase.isOptional = true;
		showcase.minLength = 1;
		showcase.maxLength = 10;
		showcase.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		showcase.description = "Select only showcase?";
		showcase.addAlias(new String[]{ "showcase", "space_showcase" });
		select.addParameter(showcase);

		Parameter category = new Parameter();
		category.isOptional = true;
		category.minLength = 1;
		category.maxLength = 2;
		category.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		category.description = "Space category";
		category.addAlias(new String[]{ "category", "space_category" });
		select.addParameter(category);

		Parameter limit = new Parameter();
		limit.isOptional = true;
		limit.minLength = 1;
		limit.maxLength = 11;
		limit.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		limit.description = "Space limit";
		limit.addAlias(new String[]{ "limit" });
		select.addParameter(limit);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		select.addParameter(user);
		
		Parameter organization = new Parameter();
		organization.isOptional = true;
		organization.minLength = 1;
		organization.maxLength = 30;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		organization.description = "The organization name or id (prevails over user)";
		organization.addAlias(new String[]{ "org", "organization_name", "org_name", "organization_id", "org_id", "oid" });
		select.addParameter(organization);
		
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
		
		Parameter count = new Parameter();
		count.isOptional = true;
		count.minLength = 1;
		count.maxLength = 10;
		count.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		count.description = "Count spaces?";
		count.addAlias(new String[]{ "count" });
		select.addParameter(count);	
		
		index.addOwnHandler(select);
	}
	
	private void initializeSearch(Index index)
	{
		Action search = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				String keyword = getParameter("keyword").getValue();
				String extended = getParameter("extended").getValue();
				String language = getParameter("language").getValue().substring(0,2).toUpperCase();
				String language2 = language;
				
				String where = "";
				
				if( user != null )
				{
					if( !user.matches("^[0-9]+$") )
						user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					
					where += " AND space_user = " + user;
				}
				
				// split the keyword for AND searches
				Matcher m = Pattern.compile("([^\\s,;\\+]+)").matcher(keyword);
				while(m.find())
				{
					where += " AND (t.translation_text LIKE '%" + m.group(1) + 
						"%' OR i.instance_name LIKE '%" + m.group(1) + 
						"%' OR s.space_name LIKE '%" + m.group(1) + 
						"%' OR s.space_description LIKE '%" + m.group(1) + 
						"%')";
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
					
					String sql = "SELECT DISTINCT s.space_id, s.space_name, s.space_date, s.space_user, s.space_active, s.space_description, s.space_showcase, s.space_category, s.space_metadata " + 
					"FROM spaces s " + 
					"LEFT JOIN instance_space isp ON(isp.space_id = s.space_id) " + 
					"LEFT JOIN instances i ON(i.instance_id = isp.instance_id) " +
					"LEFT JOIN connectors c ON(c.connector_id = i.instance_connector) " +
					"LEFT JOIN connector_translation ct ON(ct.connector_id = c.connector_id" + language + ") " +
					"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
					"WHERE 1=1 " + where + (attempt < 2 ? " AND t.translation_id IS NOT NULL" : "") + " ORDER BY space_id DESC LIMIT 10";

					Vector<Map<String, String>> data = Database.getInstance().select(sql);
					
					if( data != null && data.size() > 0 )
					{
						if( extended != null && extended.matches("^(?i)(yes|true|1)$") )
						{
							Vector<Map<String, Object>> ext = new Vector<Map<String, Object>>();
							for( Map<String, String> row : data )
							{
								Map<String, Object> e = new HashMap<String, Object>();
								e.putAll(row);
								Collection<String> spaces = new ArrayList<String>();
								spaces.add(row.get("space_id"));
								e.put("instances", Instance.INTERNAL_SELECT(new LinkedList<String>(), spaces, null, null, null, "true", language2, null, null, null));
								ext.add(e);
							}
							return ext;
						}
						else
							return data;
					}
				}
				
				// THIS SHOULD NEVER HAPPEN...
				return new Vector<Map<String, String>>();
			}
		};
		
		search.addMapping(new String[] { "search", "find" });
		search.description = "Search for spaces, connectors or instances based on a keyword.";
		search.returnDescription = "The matching space [{'name', 'id'},...]";
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
		keyword.isOptional = false;
		keyword.minLength = 3;
		keyword.maxLength = 30;
		keyword.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		keyword.description = "The keyword to search for";
		keyword.addAlias(new String[]{ "keyword", "key", "needle", "text" });
		search.addParameter(keyword);
		
		Parameter extended = new Parameter();
		extended.isOptional = true;
		extended.minLength = 1;
		extended.maxLength = 10;
		extended.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		extended.description = "Show extended information?";
		extended.addAlias(new String[]{ "extended", "more" });
		search.addParameter(extended);
		
		Parameter lang = new Parameter();
		lang.isOptional = false;
		lang.minLength = 2; // FR
		lang.maxLength = 5; // FR_BE
		lang.mustMatch = "^[a-zA-Z]{2}(_[a-zA-Z]{2})?$";
		lang.description = "The language to search for";
		lang.addAlias(new String[]{ "lang", "language", "locale" });
		search.addParameter(lang);
				
		index.addOwnHandler(search);
	}
}
