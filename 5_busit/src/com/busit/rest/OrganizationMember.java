package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.db.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;
import java.security.Principal;

public class OrganizationMember extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "organizationmember" });
		index.description = "Manages organizations members";
		Handler.addHandler("/busit/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		
		Self.selfize("/busit/organizationmember/insert");
		Self.selfize("/busit/organizationmember/update");
		Self.selfize("/busit/organizationmember/delete");
		Self.selfize("/busit/organizationmember/select");
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
				
				if( !IdentityChecker.getInstance().isUserOrgAdmin(user, organization) )
				{
					if( !IdentityChecker.getInstance().isUserIdentity(user, identity) )
						throw new Exception("The provided identity does not belong to the current user.");
				}
				
				if( !organization.matches("^[0-9]+$") )
					organization = "(SELECT user_id FROM users WHERE user_name = '" + Security.escape(organization) + "')";
				if( !identity.matches("^[0-9]+$") )
					identity = "(SELECT identity_id FROM identities WHERE identity_principal = '" + Security.escape(PrincipalUtil.parse(identity).getName()) + "')";
			
				Database.getInstance().insert("INSERT INTO org_member (org_id, org_admin, identity_id) VALUES (" + organization + ", false, " + identity + ")");
				
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Join organization";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "org_update" });
		
		Parameter organization = new Parameter();
		organization.isOptional = false;
		organization.minLength = 3;
		organization.maxLength = 30;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		organization.description = "The organization name";
		organization.addAlias(new String[]{ "name", "organization_name", "org_name", "org", "organization" });
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
				String status = getParameter("status").getValue();
				String admin = getParameter("admin").getValue();
				String identity = getParameter("identity").getValue();
				String user = getParameter("user").getValue();
						
				if( !IdentityChecker.getInstance().isUserOrgAdmin(user, organization) )
					throw new Exception("The current user is not an administrator of the provided organization");
			
				if( !organization.matches("^[0-9]+$") )
					organization = "(SELECT user_id FROM users WHERE user_name = '" + Security.escape(organization) + "' AND user_org)";
				
				String where = "";
				if( !identity.matches("^[0-9]+$") )
					where = "(SELECT identity_id FROM identities WHERE identity_principal = '" + Security.escape(PrincipalUtil.parse(identity).getName()) + "')";
				else
					where = identity;
			
				String set = "";
				if( status != null )
					set += "member_status = '" + Security.escape(status) + "', ";
				if( admin != null )
				{
					if( admin.matches("^(?i)(no|false|0)$") && IdentityChecker.getInstance().isLastOrgAdmin(identity, organization) )
						throw new Exception("You cannot remove the last admin of the organization");
					set += "org_admin = " + (admin.matches("^(?i)(no|false|0)$") ? "FALSE" : "TRUE") + ", ";
				}
					
				Database.getInstance().update("UPDATE org_member SET " + set + " org_id = org_id WHERE org_id = " + organization + " AND identity_id = " + where);
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the org member attributes";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "org_update" });
		
		Parameter organization = new Parameter();
		organization.isOptional = false;
		organization.minLength = 1;
		organization.maxLength = 200;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		organization.description = "The organization name or id";
		organization.addAlias(new String[]{ "id", "oid", "organization_id", "organization", "name", "organization_name", "org_name", "org" });
		update.addParameter(organization);
		
		Parameter admin = new Parameter();
		admin.isOptional = true;
		admin.minLength = 1;
		admin.maxLength = 5;
		admin.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		admin.description = "Is this user an admin of the org?";
		admin.addAlias(new String[]{ "admin", "org_admin" });
		update.addParameter(admin);
		
		Parameter status = new Parameter();
		status.isOptional = true;
		status.minLength = 1;
		status.maxLength = 1;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		status.description = "Is this member active?";
		status.addAlias(new String[]{ "status", "active", "member_active" });
		update.addParameter(status);

		Parameter identity = new Parameter();
		identity.isOptional = false;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		identity.description = "The organization administrator's identity. The identity must be owned by the current user.";
		identity.addAlias(new String[]{ "identity", "admin", "administrator", "owner" });
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
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String organization = getParameter("name").getValue();
				String identity = getParameter("identity").getValue();
				String user = getParameter("user").getValue();
				
				if( !IdentityChecker.getInstance().isUserOrgAdmin(user, organization) )
				{
					if( !IdentityChecker.getInstance().isUserIdentity(user, identity) )
						throw new Exception("The provided identity does not belong to the current user.");
				}
				
				if( IdentityChecker.getInstance().isLastOrgAdmin(identity, organization) )
					throw new Exception("You cannot remove the last admin of the organization");
				
				if( !organization.matches("^[0-9]+$") )
					organization = "(SELECT user_id FROM users WHERE user_name = '" + Security.escape(organization) + "')";
				if( !identity.matches("^[0-9]+$") )
					identity = "(SELECT identity_id FROM identities WHERE identity_principal = '" + Security.escape(PrincipalUtil.parse(identity).getName()) + "')";
					
				Database.getInstance().delete("DELETE FROM org_member WHERE org_id = " + organization + " AND identity_id = " + identity);
				
				// CAUTION : do not forget to remove any impersonation
				Database.getInstance().insert("DELETE i " + 
					"FROM impersonate i " + 
					"LEFT JOIN identities i_to ON(i_to.identity_id = i.identity_to) " + 
					"LEFT JOIN users o ON(o.user_id = i_to.identity_user) " +
					"WHERE o.user_id = " + organization + " AND i.identity_from = " + identity);
				
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a member from an org";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "org_update" });
		
		Parameter organization = new Parameter();
		organization.isOptional = false;
		organization.minLength = 1;
		organization.maxLength = 200;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		organization.description = "The organization name or id";
		organization.addAlias(new String[]{ "id", "oid", "organization_id", "organization", "name", "organization_name", "org_name", "org" });
		delete.addParameter(organization);

		Parameter identity = new Parameter();
		identity.isOptional = false;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		identity.description = "The organization administrator's identity. The identity must be owned by the current user.";
		identity.addAlias(new String[]{ "identity", "admin", "administrator", "owner" });
		delete.addParameter(identity);
		
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
				String organization = getParameter("organization").getValue();
				String identity = getParameter("identity").getValue();
				String user = getParameter("user").getValue();
				String count = getParameter("count").getValue();
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserOrgMember(user, organization) )
						throw new Exception("The provided user does not belong to the current organization.");
				}
				
				if( !organization.matches("^[0-9]+$") )
					organization = "(SELECT user_id FROM users WHERE user_name = '" + Security.escape(organization) + "')";
					
				if( count != null && count.matches("^(?i)(yes|true|1)$") )
				{
					return Database.getInstance().select("SELECT COUNT(i.identity_id) as count " + 
					"FROM users o " + 
					"LEFT JOIN org_member om ON(o.user_id = om.org_id) " + 
					"LEFT JOIN identities i ON(i.identity_id = om.identity_id) " + 
					"LEFT JOIN users u ON(u.user_id = i.identity_user) " + 
					"WHERE o.user_id = " + organization);
				}
				
				Vector<Map<String,String>> data = Database.getInstance().select("SELECT DISTINCT i.identity_id, i.identity_principal, i.identity_description, u.user_date, om.org_admin, om.member_status, " + 
					"GROUP_CONCAT(IFNULL(oi.identity_principal, '') SEPARATOR '|') as user_identity_names, GROUP_CONCAT(IFNULL(oi.identity_id, '') SEPARATOR '|') as user_identity_ids " + 
					"FROM users o " + 
					"LEFT JOIN org_member om ON(o.user_id = om.org_id) " + 
					"LEFT JOIN identities i ON(i.identity_id = om.identity_id) " + 
					"LEFT JOIN impersonate ii ON(ii.identity_from = i.identity_id) " + 
					"LEFT JOIN identities oi ON(oi.identity_id = ii.identity_to AND oi.identity_user = o.user_id) " +
					"LEFT JOIN users u ON(u.user_id = i.identity_user) " + 
					"WHERE o.user_id = " + organization + " GROUP BY i.identity_id");
					
				Vector<Map<String, Object>> result = new Vector<Map<String, Object>>();
				for( Map<String, String> d : data )
				{
					Map<String, Object> r = new HashMap<String, Object>();
					r.putAll(d);
					r.put("identity_name", PrincipalUtil.shortName(d.get("identity_principal")));
					
					// split impersonate
					Vector<Map<String, String>> impersonates = new Vector<Map<String, String>>();
					List<String> _ids = new LinkedList<String>();
					String[] ids = d.get("user_identity_ids").split("\\|");
					String[] names = d.get("user_identity_names").split("\\|");

					for( int i = 0; i < ids.length; i++ )
					{
						if( ids[i] == null || ids[i].length() == 0 ) continue;
						if( _ids.contains(ids[i]) ) continue;
						_ids.add(ids[i]);
						Map<String, String> impersonate = new HashMap<String, String>();
						impersonate.put("identity_id", ids[i]);
						impersonate.put("identity_principal", names[i]);
						impersonate.put("identity_name", PrincipalUtil.shortName(names[i]));
						impersonates.add(impersonate);
					}
					r.put("impersonate", impersonates);
					r.remove("user_identity_ids");
					r.remove("user_identity_names");
					result.add(r);
				}
				return result;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a organization. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching organization [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "org_select" });
		
		Parameter organization = new Parameter();
		organization.isOptional = false;
		organization.minLength = 1;
		organization.maxLength = 200;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		organization.description = "The organization name or id";
		organization.addAlias(new String[]{ "id", "oid", "organization_id", "organization", "name", "organization_name", "org_name", "org" });
		select.addParameter(organization);
	
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
		count.description = "Count members?";
		count.addAlias(new String[]{ "count" });
		select.addParameter(count);
		
		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity.description = "The identity name, id or complete principal";
		identity.addAlias(new String[]{ "identity", "name", "identity_name", "identity_id", "id" });
		select.addParameter(identity);
		
		index.addOwnHandler(select);
	}
}