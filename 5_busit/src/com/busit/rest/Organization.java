package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;
import java.security.Principal;

public class Organization extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "organization" });
		index.description = "Manages organizations";
		Handler.addHandler("/busit/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		
		Self.selfize("/busit/organization/insert");
		Self.selfize("/busit/organization/update");
		Self.selfize("/busit/organization/delete");
		Self.selfize("/busit/organization/select");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String organization = getParameter("name").getValue();
				String identity = getParameter("identity").getValue();
				String user = getParameter("user").getValue();
				
				if( organization.matches("^[0-9]+$") || organization.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The organization name may not be numeric and may not start or end with special characters");
				if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(organization).find() )
					throw new Exception("The organization name may not start, end or contain consecutive special characters");
				
				if( !IdentityChecker.getInstance().isUserIdentity(user, identity) )
					throw new Exception("The provided identity does not belong to the current user.");
				
				if( !identity.matches("^[0-9]+$") )
					identity = "(SELECT identity_id FROM identities WHERE identity_principal = '" + Security.escape(PrincipalUtil.parse(identity).getName()) + "')";
				
				Map<String, String> check = Database.getInstance().selectOne("SELECT COUNT(identity_id) as c FROM identities WHERE identity_id = (SELECT identity_id FROM identities WHERE identity_principal = '" + Security.escape(PrincipalUtil.parse(organization).getName()) + "')");
				if( check == null || check.get("c") == null || Integer.parseInt(check.get("c")) > 0 )
					throw new Exception("The provided organization name is not available");
				
				Long uid = Database.getInstance().insert("INSERT INTO users (user_name, user_password, user_date, user_org) VALUES ('" + Security.escape(organization) + "', '', UNIX_TIMESTAMP(), TRUE)");
				Database.getInstance().insert("INSERT INTO org_member (org_id, org_admin, identity_id) VALUES (" + uid + ", true, " + identity + ")");
				
				// generate the initial identity
				long identity_uid = Identity.INTERNAL_IDENTITY_INSERT(organization, organization, uid.toString());
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", organization);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new organization";
		insert.returnDescription = "The newly created organization {'name', 'id'}";
		insert.addGrant(new String[] { "access", "org_insert" });
		
		Parameter organization = new Parameter();
		organization.isOptional = false;
		organization.minLength = 3;
		organization.maxLength = 100;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		organization.allowInUrl = true;
		organization.description = "The organization name";
		organization.addAlias(new String[]{ "name", "organization_name", "org_name" });
		insert.addParameter(organization);
		
		Parameter identity = new Parameter();
		identity.isOptional = false;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		identity.description = "The organization administrator's identity. The identity must be owned by the current user.";
		identity.addAlias(new String[]{ "identity", "admin", "administrator", "owner" });
		insert.addParameter(identity);

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
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String organization = getParameter("name").getValue();
				String description = getParameter("description").getValue();
				String info = getParameter("info").getValue();
				String mail = getParameter("mail").getValue();
				String id = getParameter("id").getValue();
				String user = getParameter("user").getValue();
				String searchable = getParameter("public").getValue();
				
				if( searchable != null && searchable.matches("^(?i)(yes|true|1)$") )
					searchable = "1";
				else if( searchable != null )
					searchable = "0";
					
				if( organization != null )
				{	
					if( organization.matches("^[0-9]+$") || organization.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
						throw new Exception("The organization name may not be numeric and may not start or end with special characters");
					
					if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(organization).find() )
						throw new Exception("The organization name may not start, end or contain consecutive special characters");
				}
				
				if( !IdentityChecker.getInstance().isUserOrgAdmin(user, id) )
					throw new Exception("The current user is not an administrator of the provided organization");
				
				String update = "";
				if( organization != null )
					update += ", user_name = '" + Security.escape(organization) + "'";
				if( description != null )
					update += ", user_firstname = '" + Security.escape(description) + "'";
				if( info != null )
					update += ", user_address = '" + Security.escape(info) + "'";
				if( mail != null )
					update += ", user_mail = '" + Security.escape(mail) + "'";
				if( searchable != null )
					update += ", user_public = '" + Security.escape(searchable) + "'";
					
				Database.getInstance().update("UPDATE users SET user_id = user_id " + update + " WHERE user_org AND user_id = " + Security.escape(id));
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the organization name";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "org_update" });
		
		Parameter organization = new Parameter();
		organization.isOptional = true;
		organization.minLength = 3;
		organization.maxLength = 30;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		organization.description = "The organization name";
		organization.addAlias(new String[]{ "name", "organization_name", "org_name" });
		update.addParameter(organization);

		Parameter description = new Parameter();
		description.isOptional = true;
		description.minLength = 3;
		description.maxLength = 200;
		description.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		description.description = "The organization description";
		description.addAlias(new String[]{ "description", "organization_description", "desc" });
		update.addParameter(description);

		Parameter mail = new Parameter();
		mail.isOptional = true;
		mail.minLength = 0;
		mail.maxLength = 150;
		mail.mustMatch = "^[_\\w\\.\\+-]+@[a-zA-Z0-9\\.-]{1,100}\\.[a-zA-Z0-9]{2,6}$";
		mail.description = "The org's email";
		mail.addAlias(new String[]{ "mail", "email", "address", "org_email", "org_mail", "org_address" });
		update.addParameter(mail);

		Parameter info = new Parameter();
		info.isOptional = true;
		info.minLength = 3;
		info.maxLength = 2000;
		info.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		info.description = "The organization informations";
		info.addAlias(new String[]{ "info", "organization_info", "information" });
		update.addParameter(info);
		
		Parameter searchable = new Parameter();
		searchable.isOptional = true;
		searchable.minLength = 1;
		searchable.maxLength = 10;
		searchable.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		searchable.description = "Is this org can be found?";
		searchable.addAlias(new String[]{ "public", "searchable" });
		update.addParameter(searchable);
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.allowInUrl = true;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The organization id";
		id.addAlias(new String[]{ "id", "oid", "organization_id", "org_id" });
		update.addParameter(id);

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
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> organization = getParameter("name").getValues();
				String user = getParameter("user").getValue();
				
				String where = "1>1";
				for( String o : organization )
				{
					if( !IdentityChecker.getInstance().isUserOrgAdmin(user, o) )
						throw new Exception("The current user is not an administrator of the provided organization");
					
					if( !o.matches("^[0-9]+$") )
						where += " OR u.user_name = '" + Security.escape(o) + "'";
					else
						where += " OR u.user_id = " + o;
				}
				
				// CAUTION : when we delete an org, we also delete all its identities
				Database.getInstance().delete("DELETE i FROM users u LEFT JOIN identities i ON(u.user_id = i.identity_user) WHERE u.user_org AND (" + where + ")");
				
				Database.getInstance().delete("DELETE u FROM users u WHERE u.user_org AND (" + where + ")");
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a organization";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "org_delete" });
		
		Parameter organization = new Parameter();
		organization.isOptional = false;
		organization.minLength = 1;
		organization.maxLength = 30;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		organization.allowInUrl = true;
		organization.isMultipleValues = true;
		organization.description = "The organization name(s) or id(s)";
		organization.addAlias(new String[]{ "name", "organization_name", "org_name", "id", "organization_id", "org_id", "oid", "names", "organization_names", "org_names", "ids", "organization_ids", "org_ids", "oids" });
		delete.addParameter(organization);
	
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
		// TODO : have public/private organizations
		// public : everyone can see it
		// private : only if you are member -> then think about a way to join a private org...
		
		// TODO : extended select for org admin : include org identities and identity of members (+status)
		
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String organization = getParameter("name").getValue();
				String user = getParameter("user").getValue();
				String identity = getParameter("identity").getValue();
				String keyword = getParameter("keyword").getValue();
				
				String where = "";
				
				// ===================
				// BASED ON KEYWORD
				// ===================
				if( keyword != null )
				{
					where = "AND (o.user_name LIKE '%" + Security.escape(keyword) + "%' OR o.user_mail LIKE '%" + Security.escape(keyword) + "%' OR o.user_firstname LIKE '%" + Security.escape(keyword) + "%' OR o.user_lastname LIKE '%" + Security.escape(keyword) + "%')";
					
					return Database.getInstance().select("SELECT o.user_id as org_id, o.user_name as org_name, o.user_date as org_date, o.user_firstname as org_description, o.user_address as org_info, o.user_mail as org_mail, o.user_public as org_public " +
					"FROM users o " +
					"WHERE o.user_org AND o.user_public = 1 " + where);
				}
				
				if( organization != null )
				{
					where += " AND (1>1";
					if( organization.matches("^[0-9]+$") )
						where += " OR o.user_id = " + Security.escape(organization);
					else
						where += " OR o.user_name LIKE '%" + Security.escape(organization) + "%'";
					where += ")";
				}
				
				if( user != null  )
				{
					if( user.matches("^[0-9]+$") )
						where += " AND u.user_id = " + user;
					else
						where += " AND u.user_name = '" + Security.escape(user) + "'";
				}
				
				if( identity != null && identity.length() > 0 )
				{
					if( !identity.matches("^[0-9]+$") )
					{
						Principal origin = PrincipalUtil.parse(identity);
						if( PrincipalUtil.isRemote(origin) )
							throw new Exception("Unsuported identity");
						where += " AND i.identity_principal = '" + Security.escape(origin.getName()) + "'";
					}
					else
						where += " AND i.identity_id = " + identity;
				}
				
				Vector<Map<String,String>> org = Database.getInstance().select("SELECT o.user_id as org_id, o.user_name as org_name, o.user_date as org_date, o.user_firstname as org_description, o.user_address as org_info, o.user_mail as org_mail, o.user_public as org_public, om.org_admin, i.identity_id, i.identity_principal " +
					"FROM users o " + 
					"LEFT JOIN org_member om ON(om.org_id = o.user_id) " +
					"LEFT JOIN identities i ON(i.identity_id = om.identity_id) " + 
					"LEFT JOIN users u ON(u.user_id = i.identity_user) " +
					"WHERE o.user_org " + where);
				
				for( Map<String,String> o : org )
					o.put("identity_name", PrincipalUtil.shortName(o.remove("identity_principal")));
				
				return org;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a organization. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching organization [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "org_select" });
	
		Parameter keyword = new Parameter();
		keyword.isOptional = true;
		keyword.minLength = 2;
		keyword.maxLength = 50;
		keyword.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		keyword.isMultipleValues = true;
		keyword.description = "A keyword to search (excludes all other parameters!)";
		keyword.addAlias(new String[]{ "keyword", "key" });
		select.addParameter(keyword);
		
		Parameter organization = new Parameter();
		organization.isOptional = true;
		organization.minLength = 1;
		organization.maxLength = 30;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		organization.allowInUrl = true;
		organization.description = "The organization name or id (will match *[organization]* if not a number or an exact organization id match if numeric)";
		organization.addAlias(new String[]{ "name", "organization_name", "org_name", "id", "oid", "organization_id", "org_id", "organization", "names", "organization_names", "org_names", "organizations", "ids", "oids", "organization_ids", "org_ids" });
		select.addParameter(organization);
	
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		select.addParameter(user);

		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity.description = "The identity name, id or complete principal";
		identity.addAlias(new String[]{ "identity", "name", "identity_name", "identity_id" });
		select.addParameter(identity);
		
		index.addOwnHandler(select);
	}
}