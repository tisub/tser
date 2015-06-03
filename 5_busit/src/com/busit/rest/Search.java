package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;

public class Search extends InitializerOnce
{
	public void initialize()
	{
		initializeSearch((Index) Handler.getHandler("/busit/"));
		Self.selfize("/busit/search");
	}
	
	private void initializeSearch(Index index)
	{
		Action search = new Action()
		{
			public Object execute() throws Exception
			{
				String table = getParameter("table").getValue();
				String keyword = getParameter("keyword").getValue();
				String user = getParameter("user").getValue();
				Collection<String> joins = getParameter("join").getValues();
				
				if( user != null && !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				String field = null;
				String from = null;
				String where = null;
				
				if( table.startsWith("connector") )
				{
					field = "c.connector_id";
					from = "connectors c " +
						"LEFT JOIN connector_translation ct ON(ct.connector_id = c.connector_id) " + 
						"LEFT JOIN connector_config cc ON(cc.connector_id = c.connector_id AND NOT cc.config_hidden) " + 
						"LEFT JOIN config_translation cct ON(cct.connector_id = cc.connector_id AND cct.config_key = cc.config_key) " +
						"LEFT JOIN connector_interface ci ON(ci.connector_id = c.connector_id) " + 
						"LEFT JOIN interface_translation cit ON(cit.connector_id = ci.connector_id AND cit.interface_key = ci.interface_key) " +
						"LEFT JOIN translations t ON(ct.translation_id = t.translation_id OR cct.translation_id = t.translation_id OR cit.translation_id = t.translation_id)";

					where = "t.translation_text LIKE '%" + Security.escape(keyword, true) + "%'";
					if( keyword.matches("^[0-9]+$") )
						where += " OR c.connector_id = " + keyword;
				}
				else if( table.startsWith("instance") )
				{
					if( user == null )
						throw new Exception("User parameter is required when searching for instances");
					
					field = "i.instance_id";
					from = "instances i " + 
						"LEFT JOIN instance_interface ii ON(ii.instance_id = i.instance_id) " +
						"LEFT JOIN connectors c ON(c.connector_id = i.instance_connector) " +
						"LEFT JOIN connector_translation ct ON(ct.connector_id = c.connector_id) " + 
						"LEFT JOIN connector_config cc ON(cc.connector_id = c.connector_id AND NOT cc.config_hidden) " + 
						"LEFT JOIN config_translation cct ON(cct.connector_id = cc.connector_id AND cct.config_key = cc.config_key) " +
						"LEFT JOIN connector_interface ci ON(ci.connector_id = c.connector_id) " + 
						"LEFT JOIN interface_translation cit ON(cit.connector_id = ci.connector_id AND cit.interface_key = ci.interface_key) " +
						"LEFT JOIN translations t ON(ct.translation_id = t.translation_id OR cct.translation_id = t.translation_id OR cit.translation_id = t.translation_id)";

					where = "i.instance_user = " + user + " AND (t.translation_text LIKE '%" + Security.escape(keyword, true) + "%'";
					if( keyword.matches("^[0-9]+$") )
						where += " OR i.instance_id = " + keyword;
					where += " OR i.instance_name LIKE '%" + Security.escape(keyword, true) + "%' " + 
						"OR ii.interface_name LIKE '%" + Security.escape(keyword, true) + "%')";
				}
				else if( table.startsWith("space") || table.startsWith("flow") )
				{
					if( user == null )
						throw new Exception("User parameter is required when searching for spaces");
					
					field = "s.space_id";
					from = "spaces s ";
					where = "s.space_user = " + user + " AND (s.space_name LIKE '%" + Security.escape(keyword, true) + "%'";
					if( keyword.matches("^[0-9]+$") )
						where += " OR s.space_id = " + keyword;
					where += ")";
				}
				else if( table.startsWith("identit") )
				{
					if( user == null )
						throw new Exception("User parameter is required when searching for identities");
					
					throw new RuntimeException("searching for identities is not implemented yet");
				}
				else if( table.startsWith("org") )
				{
					throw new RuntimeException("searching for organizations is not implemented yet");
				}
				else if( table.startsWith("token") )
				{
					if( user == null )
						throw new Exception("User parameter is required when searching for tokens");
					
					throw new RuntimeException("searching for tokens is not implemented yet");
				}
				else if( table.startsWith("bill") )
				{
					if( user == null )
						throw new Exception("User parameter is required when searching for bills");
					throw new RuntimeException("searching for bills is not implemented yet");
				}
				else
					throw new Exception("searching in " + table + " is not supported");
				
				if( joins == null || joins.size() == 0 )
					return Database.getInstance().select("SELECT DISTINCT " + field + " FROM " + from + " WHERE " + where);
				
				// now that we have the IDs, we can link the info
				throw new Exception("joins are not supported at this time");
			}
		};
		
		search.addMapping(new String[] { "search" });
		search.description = "Search for some data";
		search.returnDescription = "Depends on the desired output. By default, just the ID of the matching records";
		search.addGrant(new String[] { "access" });
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "ui_user" });
		search.addParameter(user);
		
		Parameter keyword = new Parameter();
		keyword.isOptional = false;
		keyword.minLength = 1;
		keyword.maxLength = 50;
		keyword.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		keyword.description = "The keyword to search for (if numeric, a match on the ID will be performed as well)";
		keyword.addAlias(new String[]{ "keyword", "needle" });
		search.addParameter(keyword);
		
		Parameter item = new Parameter();
		item.isOptional = false;
		item.minLength = 1;
		item.maxLength = 50;
		item.mustMatch = "^[a-zA-Z0-9_]+$";
		item.description = "The type of information to search for";
		item.addAlias(new String[]{ "item", "haystack", "table" });
		search.addParameter(item);
		
		Parameter join = new Parameter();
		join.isOptional = true;
		join.minLength = 1;
		join.maxLength = 50;
		join.isMultipleValues = true;
		join.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		join.description = "The additionnal info to join to the results";
		join.addAlias(new String[]{ "join", "link", "joins", "links" });
		search.addParameter(join);
		
		index.addOwnHandler(search);
	}

}