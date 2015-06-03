package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;

public class Template extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "template", "templates" });
		index.description = "Manages templates";
		Handler.addHandler("/busit/", index);
		
		initializeSelect(index);
		initializeSearch(index);
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
				String language = getParameter("language").getValue();
				
				// CAUTION : showcase = public for all !
				
				if( limit == null )
					limit = "50";
				
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
				
				Vector<Map<String, String>> result = Database.getInstance().select("SELECT count(*) as count, space_id, space_user, space_name, space_date, space_active, space_description, space_showcase, space_category, space_metadata " + 
					"FROM spaces WHERE space_metadata != '' " + where + " GROUP BY space_metadata ORDER BY count DESC LIMIT 0," + limit );

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
		};
		
		select.addMapping(new String[] { "select", "list", "view" });
		select.description = "Retrieves information about a template. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching template [{'name', 'id'},...]";
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
		
		Parameter language = new Parameter();
		language.isOptional = true;
		language.minLength = 2;
		language.maxLength = 2;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "Translation language.";
		language.addAlias(new String[]{ "lang", "translation_language", "language" });
		select.addParameter(language);
		
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
					
					String sql = "SELECT DISTINCT s.space_id, count(DISTINCT s.space_id) as count, s.space_name, s.space_date, s.space_active, s.space_description, s.space_user, s.space_showcase, s.space_category, s.space_metadata " + 
					"FROM spaces s " + 
					"LEFT JOIN instance_space isp ON(isp.space_id = s.space_id) " + 
					"LEFT JOIN instances i ON(i.instance_id = isp.instance_id) " +
					"LEFT JOIN connectors c ON(c.connector_id = i.instance_connector) " +
					"LEFT JOIN connector_translation ct ON(ct.connector_id = c.connector_id" + language + ") " +
					"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
					"WHERE 1=1 " + where + (attempt < 2 ? " AND t.translation_id IS NOT NULL" : "") + " GROUP BY s.space_metadata ORDER BY count DESC LIMIT 10";

					Vector<Map<String, String>> data = Database.getInstance().select(sql);
					
					if( data != null && data.size() > 0 )
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
				}
				
				// THIS SHOULD NEVER HAPPEN...
				return new Vector<Map<String, String>>();
			}
		};
		
		search.addMapping(new String[] { "search", "find" });
		search.description = "Search for templates, connectors or instances based on a keyword.";
		search.returnDescription = "The matching template [{'name', 'id'},...]";
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
