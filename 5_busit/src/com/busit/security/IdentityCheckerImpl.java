package com.busit.security;

import com.anotherservice.rest.security.*;
import com.anotherservice.db.*;
import com.anotherservice.util.*;
import java.util.*;
import com.busit.security.*;
import java.util.regex.*;
import java.security.Principal;

public class IdentityCheckerImpl implements IIdentityChecker
{
	public boolean userExists(String user) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
			
		String sql = "SELECT u.user_id FROM users u WHERE NOT u.user_org AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("user_id") == null )
			return false;
		else
			return true;
	}
	
	public boolean orgExists(String org) throws Exception
	{
		String where = "";
		if( !org.matches("^[0-9]+$") )
			where += "o.user_name = '" + Security.escape(org) + "'";
		else
			where += "o.user_id = " + org;
			
		String sql = "SELECT o.user_id FROM users o WHERE "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("user_id") == null )
			return false;
		else
			return true;
	}
	
	public boolean isUserOrgMember(String user, String org) throws Exception
	{
		return (userOrgMemberVia(user, org) != null);
	}
	
	public boolean isIdentityOrgMember(String identity, String org) throws Exception
	{
		String where = "";
		if( !identity.matches("^[0-9]+$") )
		{
			Principal origin = PrincipalUtil.parse(identity);
			if( PrincipalUtil.isRemote(origin) )
				return false; // this is not an identity we manage
			where += "i.identity_principal = '" + Security.escape(origin.getName()) + "'";
		}
		else
			where += "i.identity_id = " + identity;
			
		if( !org.matches("^[0-9]+$") )
			where += " AND o.user_name = '" + Security.escape(org) + "'";
		else
			where += " AND o.user_id = " + org;
		
		String sql = "SELECT io.org_admin FROM users o " + 
			"LEFT JOIN org_member io ON(io.org_id = o.user_id) " +
			"LEFT JOIN identities i ON(i.identity_id = io.identity_id) " +
			"WHERE i.identity_death IS NULL AND o.user_org AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("org_admin") == null )
			return false;
		else
			return true;
	}
	
	public boolean isUserOrgAdmin(String user, String org) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
			
		if( !org.matches("^[0-9]+$") )
			where += " AND o.user_name = '" + Security.escape(org) + "'";
		else
			where += " AND o.user_id = " + org;
		
		String sql = "SELECT io.org_admin FROM users u " + 
			"LEFT JOIN identities i ON(i.identity_user = u.user_id) " +
			"LEFT JOIN org_member io ON(io.identity_id = i.identity_id) " +
			"LEFT JOIN users o ON(o.user_id = io.org_id AND o.user_org) " +
			"WHERE i.identity_death IS NULL AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("org_admin") == null )
			return false;
		else
			return r.get("org_admin").matches("^(?i)(1|true|yes)$");
	}

	public boolean isUserInstance(String user, String instance) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "AND u.user_name = '" + Security.escape(user) + "'";
		else
			where += "AND u.user_id = " + user;
			
		if( !instance.matches("^[0-9]+$") )
			where += " AND i.instance_name = '" + Security.escape(instance) + "'";
		else
			where += " AND i.instance_id = " + instance;
		
		String sql = "SELECT i.instance_user FROM users u " + 
			"LEFT JOIN instances i ON(i.instance_user = u.user_id) " +
			"WHERE 1 " + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("instance_user") == null )
			return false;
		else
			return true;
	}
	
	public boolean isUserInstanceAdmin(String user, String instance) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "AND u.user_name = '" + Security.escape(user) + "'";
		else
			where += "AND u.user_id = " + user;
			
		if( !instance.matches("^[0-9]+$") )
			where += " AND i.instance_name = '" + Security.escape(instance) + "'";
		else
			where += " AND i.instance_id = " + instance;
		
		String sql = "SELECT i.instance_id FROM users u " + 
			"LEFT JOIN identities i2 ON(i2.identity_user = u.user_id) " +
			"LEFT JOIN org_member om ON(om.identity_id = i2.identity_id AND org_admin) " + 
			"LEFT JOIN instances i ON(i.instance_user = u.user_id or i.instance_user = om.org_id) " +
			"WHERE 1 " + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("instance_id") == null )
			return false;
		else
			return true;
	}

	public boolean isUserInstanceUser(String user, String instance) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "AND u.user_name = '" + Security.escape(user) + "'";
		else
			where += "AND u.user_id = " + user;
			
		if( !instance.matches("^[0-9]+$") )
			where += " AND i.instance_name = '" + Security.escape(instance) + "'";
		else
			where += " AND i.instance_id = " + instance;
		
		String sql = "SELECT i.instance_id FROM users u " + 
			"LEFT JOIN identities i2 ON(i2.identity_user = u.user_id) " +
			"LEFT JOIN org_member om ON(om.identity_id = i2.identity_id) " + 
			"LEFT JOIN instances i ON(i.instance_user = u.user_id or i.instance_user = om.org_id) " +
			"WHERE 1 " + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("instance_id") == null )
			return false;
		else
			return true;
	}
	
	public boolean isUserConnector(String user, String connector) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "AND u.user_name = '" + Security.escape(user) + "'";
		else
			where += "AND u.user_id = " + user;
			
		if( !connector.matches("^[0-9]+$") )
			where += " AND c.connector_name = '" + Security.escape(connector) + "'";
		else
			where += " AND c.connector_id = " + connector;
		
		String sql = "SELECT c.connector_user FROM users u " + 
			"LEFT JOIN connectors c ON(c.connector_user = u.user_id) " +
			"WHERE 1 " + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("connector_user") == null )
			return false;
		else
			return true;
	}
	
	public boolean isUserBill(String user, String bill) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "AND u.user_name = '" + Security.escape(user) + "'";
		else
			where += "AND u.user_id = " + user;
			
		where += " AND b.bill_id = " + bill;
		
		String sql = "SELECT b.bill_user FROM users u " + 
			"LEFT JOIN bills b ON(b.bill_user = u.user_id) " +
			"WHERE 1 " + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("bill_user") == null )
			return false;
		else
			return true;
	}
	
	public boolean isUserSpace(String user, String space) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "AND u.user_name = '" + Security.escape(user) + "'";
		else
			where += "AND u.user_id = " + user;
			
		if( !space.matches("^[0-9]+$") )
			where += " AND s.space_name = '" + Security.escape(space) + "'";
		else
			where += " AND s.space_id = " + space;
		
		String sql = "SELECT s.space_user FROM users u " + 
			"LEFT JOIN spaces s ON(s.space_user = u.user_id) " +
			"WHERE 1 " + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("space_user") == null )
			return false;
		else
			return true;
	}
	
	public boolean isOrgSpace(String space) throws Exception
	{
		if( space == null )
			return false;
		else
			return isOrgSpace(null, space);
	}
	
	public boolean isOrgSpace(String org, String space) throws Exception
	{
		if( space == null )
			return false;
			
		String where = "";
		if( org != null && !org.matches("^[0-9]+$") )
			where += "AND u.user_name = '" + Security.escape(org) + "'";
		else if( org != null )
			where += "AND u.user_id = " + org;
			
		if( !space.matches("^[0-9]+$") )
			where += " AND s.space_name = '" + Security.escape(space) + "'";
		else
			where += " AND s.space_id = " + space;
		
		String sql = "SELECT s.space_id FROM users u " + 
			"LEFT JOIN spaces s ON(s.space_user = u.user_id) " +
			"WHERE u.user_admin " + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("space_id") == null )
			return false;
		else
			return true;
	}
	
	public boolean isUserSpaceAdmin(String user, String space) throws Exception
	{
		if( space == null )
			return false;

		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "AND u.user_name = '" + Security.escape(user) + "'";
		else
			where += "AND u.user_id = " + user;
			
		if( !space.matches("^[0-9]+$") )
			where += " AND s.space_name = '" + Security.escape(space) + "'";
		else
			where += " AND s.space_id = " + space;
		
		String sql = "SELECT s.space_id FROM users u " + 
			"LEFT JOIN identities i ON(i.identity_user = u.user_id) " +
			"LEFT JOIN org_member om ON(om.identity_id = i.identity_id AND org_admin) " + 
			"LEFT JOIN spaces s ON(s.space_user = u.user_id OR s.space_user = om.org_id) " +
			"WHERE 1 " + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("space_id") == null )
			return false;
		else
			return true;
	}
	
	public boolean isUserSpaceUser(String user, String space) throws Exception
	{
		if( space == null )
			return false;

		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "AND u.user_name = '" + Security.escape(user) + "'";
		else
			where += "AND u.user_id = " + user;
			
		if( !space.matches("^[0-9]+$") )
			where += " AND s.space_name = '" + Security.escape(space) + "'";
		else
			where += " AND s.space_id = " + space;
		
		String sql = "SELECT s.space_id FROM users u " + 
			"LEFT JOIN identities i ON(i.identity_user = u.user_id) " +
			"LEFT JOIN org_member om ON(om.identity_id = i.identity_id) " + 
			"LEFT JOIN spaces s ON(s.space_user = u.user_id OR s.space_user = om.org_id) " +
			"WHERE 1 " + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("space_id") == null )
			return false;
		else
			return true;
	}
	
	public boolean isIdentityOrgAdmin(String identity, String org) throws Exception
	{
		String where = "";
		if( !identity.matches("^[0-9]+$") )
		{
			Principal origin = PrincipalUtil.parse(identity);
			if( PrincipalUtil.isRemote(origin) )
				return false; // this is not an identity we manage
			where += "i.identity_principal = '" + Security.escape(origin.getName()) + "'";
		}
		else
			where += "i.identity_id = " + identity;
			
		if( !org.matches("^[0-9]+$") )
			where += " AND o.user_name = '" + Security.escape(org) + "'";
		else
			where += " AND o.user_id = " + org;
		
		String sql = "SELECT io.org_admin FROM users o " + 
			"LEFT JOIN org_member io ON(io.org_id = o.user_id) " +
			"LEFT JOIN identities i ON(i.identity_id = io.identity_id) " +
			"WHERE i.identity_death IS NULL AND o.user_org AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("org_admin") == null )
			return false;
		else
			return r.get("org_admin").matches("^(?i)(1|true|yes)$");
	}
	
	public boolean isUserIdentityAdmin(String user, String identity) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
			
		if( !identity.matches("^[0-9]+$") )
		{
			Principal origin = PrincipalUtil.parse(identity);
			if( PrincipalUtil.isRemote(origin) )
				return false; // this is not an identity we manage
			where += " AND i.identity_principal = '" + Security.escape(origin.getName()) + "'";
		}
		else
			where += " AND i.identity_id = " + identity;
		
		String sql = "SELECT io.org_admin FROM users u " + 
			"LEFT JOIN identities i1 ON(i1.identity_user = u.user_id) " +
			"LEFT JOIN org_member io ON(io.identity_id = i1.identity_id AND io.org_admin) " +
			"LEFT JOIN identities i ON(i.identity_user = io.org_id) " +
			"WHERE i.identity_death IS NULL AND i1.identity_death IS NULL AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("org_admin") == null )
			return false;
		else
			return r.get("org_admin").matches("^(?i)(1|true|yes)$");
	}
	
	public String userOrgMemberVia(String user, String org) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
			
		if( !org.matches("^[0-9]+$") )
			where += " AND o.user_name = '" + Security.escape(org) + "'";
		else
			where += " AND o.user_id = " + org;
		
		String sql = "SELECT i.identity_id FROM users u " + 
			"LEFT JOIN identities i ON(i.identity_user = u.user_id) " +
			"LEFT JOIN org_member io ON(io.identity_id = i.identity_id) " +
			"LEFT JOIN users o ON(o.user_id = io.org_id AND o.user_org) " +
			"WHERE i.identity_death IS NULL AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("identity_id") == null )
			return null;
		else
			return r.get("identity_id");
	}
	
	public boolean isUserIdentity(String user, String identity) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
			
		if( !identity.matches("^[0-9]+$") )
		{
			Principal origin = PrincipalUtil.parse(identity);
			if( PrincipalUtil.isRemote(origin) )
				return false; // this is not an identity we manage
			where += " AND i.identity_principal = '" + Security.escape(origin.getName()) + "'";
		}
		else
			where += " AND i.identity_id = " + identity;
		
		String sql = "SELECT i.identity_id FROM users u " +
			"LEFT JOIN identities i ON(i.identity_user = u.user_id) " +
			"WHERE i.identity_death IS NULL AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("identity_id") == null )
			return false;
		else
			return true;
	}
	
	public boolean isOrgIdentity(String org, String identity) throws Exception
	{
		String where = "";
		if( !org.matches("^[0-9]+$") )
			where += "o.user_name = '" + Security.escape(org) + "'";
		else
			where += "o.user_id = " + org;
			
		if( !identity.matches("^[0-9]+$") )
		{
			Principal origin = PrincipalUtil.parse(identity);
			if( PrincipalUtil.isRemote(origin) )
				return false; // this is not an identity we manage
			where += " AND i.identity_principal = '" + Security.escape(origin.getName()) + "'";
		}
		else
			where += " AND i.identity_id = " + identity;
		
		String sql = "SELECT i.identity_id FROM users o " +
			"LEFT JOIN identities i ON(i.identity_user = o.user_id) " +
			"WHERE i.identity_death IS NULL AND o.user_org AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("identity_id") == null )
			return false;
		else
			return true;
	}
	
	public boolean isUserImpersonate(String user, String identity) throws Exception
	{
		return (userImpersonateVia(user, identity) != null);
	}
	
	public String userImpersonateVia(String user, String identity) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
			
		if( !identity.matches("^[0-9]+$") )
		{
			Principal origin = PrincipalUtil.parse(identity);
			if( PrincipalUtil.isRemote(origin) )
				return null; // this is not an identity we manage
			where += " AND i_to.identity_principal = '" + Security.escape(origin.getName()) + "'";
		}
		else
			where += " AND i_to.identity_id = " + identity;
		
		String sql = "SELECT i_from.identity_id FROM users u " + 
			"LEFT JOIN identities i_from ON(i_from.identity_user = u.user_id) " +
			"LEFT JOIN impersonate ii ON(ii.identity_from = i_from.identity_id) " +
			"LEFT JOIN identities i_to ON(i_to.identity_id = ii.identity_to) " +
			"WHERE i_from.identity_death IS NULL AND i_to.identity_death IS NULL AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("identity_id") == null )
			return null;
		else
			return r.get("identity_id");
	}
	
	public boolean isIdentityImpersonate(String identity_from, String identity_to) throws Exception
	{
		String where = "";
		Principal origin = null;
		
		if( !identity_from.matches("^[0-9]+$") )
		{
			origin = PrincipalUtil.parse(identity_from);
			if( PrincipalUtil.isRemote(origin) )
				return false; // this is not an identity we manage
			where += " AND i_from.identity_principal = '" + Security.escape(origin.getName()) + "'";
		}
		else
			where += "i_from.identity_id = " + identity_from;
		
		if( !identity_to.matches("^[0-9]+$") )
		{
			origin = PrincipalUtil.parse(identity_to);
			if( PrincipalUtil.isRemote(origin) )
				return false; // this is not an identity we manage
			where += " AND i_to.identity_principal = '" + Security.escape(origin.getName()) + "'";
		}
		else
			where += " AND i_to.identity_id = " + identity_to;
		
		String sql = "SELECT ii.identity_from FROM identities i_from " + 
			"LEFT JOIN impersonate ii ON(ii.identity_from = i_from.identity_id) " +
			"LEFT JOIN identities i_to ON(i_to.identity_id = ii.identity_to) " +
			"WHERE i_from.identity_death IS NULL AND i_to.identity_death IS NULL AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("identity_from") == null )
			return false;
		else
			return true;
	}
	
	public boolean isUserIdentityOrImpersonate(String user, String identity) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
					
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
		
		if( !identity.matches("^[0-9]+$") )
		{
			Principal origin = PrincipalUtil.parse(identity);
			if( PrincipalUtil.isRemote(origin) )
				return false; // this is not an identity we manage
			where += " AND i.identity_principal = '" + Security.escape(origin.getName()) + "'";
		}
		else
			where += " AND i.identity_id = " + identity;
		
		String sql = "SELECT i.identity_id, u.user_id FROM users u " +
			"LEFT JOIN identities ui ON(u.user_id = ui.identity_user) " +
			"LEFT JOIN impersonate ii ON(ii.identity_from = ui.identity_id) " +
			"LEFT JOIN identities i ON(i.identity_id = ii.identity_to OR i.identity_id = ui.identity_id) " +
			"WHERE i.identity_death IS NULL AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("identity_id") == null )
			return false;
		else
			return true;
	}
	
	public Collection<Map<String, String>> userIdentities(String user) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
			
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
		
		String sql = "SELECT i.identity_principal, i.identity_id FROM users u " +
			"LEFT JOIN identities ui ON(u.user_id = ui.identity_user) " +
			"LEFT JOIN impersonate ii ON(ii.identity_from = ui.identity_id) " +
			"LEFT JOIN identities i ON(i.identity_id = ii.identity_to OR i.identity_id = ui.identity_id) " +
			"WHERE i.identity_death IS NULL AND "  + where;
			
		Vector<Map<String, String>> data = Database.getInstance().select(sql);
		for( Map<String, String> d : data )
			d.put("identity_name", PrincipalUtil.shortName(d.remove("identity_principal")));
		return data;
	}
	
	public boolean isOrg(String user) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
		
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
		
		String sql = "SELECT u.user_id FROM users u WHERE u.user_org AND "  + where;
		
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("user_id") == null )
			return false;
		else
			return true;
	}
	
	public boolean isLastOrgAdmin(String identity, String org) throws Exception
	{
		if( !isIdentityOrgAdmin(identity, org) )
			return false;
		
		String where = "";
		if( !org.matches("^[0-9]+$") )
			where += " o.user_name = '" + Security.escape(org) + "'";
		else
			where += " o.user_id = " + org;
		
		String sql = "SELECT COUNT(io.identity_id) as c FROM users o " +
			"LEFT JOIN org_member io ON(o.user_id = io.org_id) " +
			"LEFT JOIN identities i ON(io.identity_id = i.identity_id) " +
			"WHERE i.identity_death IS NULL AND io.org_admin AND o.user_org AND "  + where;
			
		Map<String,String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("c") == null )
			return false;
		else
			return (Integer.parseInt(r.get("c")) == 1);
	}

	public Identity defaultIdentity(String user) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
		
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
			
		// TODO : have a real default identity
		// meanwhile, we just take the first one
		String sql = "SELECT i.identity_principal " + 
			"FROM users u " + 
			"LEFT JOIN identities i ON(i.identity_user = u.user_id) " +
			"WHERE i.identity_death IS NULL AND "  + where + " LIMIT 1";
		
		Map<String, String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("identity_principal") == null )
			return null;
		else
			return IdentityStore.getInstance().getIdentity(r.get("identity_principal"));
	}
	
	public int defaultIdentityId(String user) throws Exception
	{
		if( user == null )
			user = Security.getInstance().userId() + "";
		
		String where = "";
		if( !user.matches("^[0-9]+$") )
			where += "u.user_name = '" + Security.escape(user) + "'";
		else
			where += "u.user_id = " + user;
			
		// TODO : have a real default identity
		// meanwhile, we just take the first one
		String sql = "SELECT i.identity_id " + 
			"FROM users u " + 
			"LEFT JOIN identities i ON(i.identity_user = u.user_id) " +
			"WHERE i.identity_death IS NULL AND "  + where + " LIMIT 1";
		
		Map<String, String> r = Database.getInstance().selectOne(sql);
		if( r == null || r.get("identity_id") == null )
			throw new Exception("No valid identity found for user " + user);
		else
			return Integer.parseInt(r.get("identity_id"));
	}
}