package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.io.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.rest.core.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;
import java.util.regex.*;
import java.lang.*;
import java.security.KeyPair;
import com.busit.security.*;
import java.security.cert.*;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.security.Principal;

// TODO : BUSIT should be an ORG ->
// "authority" should be an identity of the org "busit"
// "everyone" should be an identity of the org "busit"
// -> find a way to do this nicely in the DB (if possible)

// TODO : possibility for an ORG to sign a user's identity ->
// 1) the user makes a request to the ORG
// 2) the org accepts -> identity created
// -----> this is the impersonate. So the identity is th eorg's and the user can user it

// TODO : possibility for an ORG (and busit) to revoke an identity ->
// 1) if the identity's issuer is the ORG
// 2) mark the identity as dead
// ----> stop impersonation and/or delete the identity

// TODO : possibility for anyone to sign an identity (like an ORG) 
// but with his own private key. As such, governments do not have to store
// an identity IN busit but can still sign its citizens's identities

// TODO : let identity certificates expire (and thus check everywhere)
// but keep a history in order to decode previous messages
// -> another DB table -> NO because even if the cert expires, it is the same public/private key pair. So we can reuse the same (expired) cert.

public class Identity extends InitializerOnce
{
	public Identity() throws Exception
	{
		super();
		IdentityStore.setInstance(new IdentityStoreImpl());
		IdentityChecker.setInstance(new IdentityCheckerImpl());
	}
	
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "identity", "identities" });
		index.description = "Manages busit identities";
		Handler.addHandler("/busit/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		initializeDownloadPublic(index);
		initializeDownloadPrivate(index);
		initializeDownloadCRL(index);
		
		Self.selfize("/busit/identity/insert");
		Self.selfize("/busit/identity/delete");
		Self.selfize("/busit/identity/update");
		Self.selfize("/busit/identity/select");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String identity = getParameter("identity").getValue();
				String user = getParameter("user").getValue();
				String org = getParameter("org").getValue();
				
				if( identity.matches("^[0-9]+$") || identity.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The identity name may not be numeric and may not start or end with special characters");
				if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(identity).find() )
					throw new Exception("The identity name may not start, end or contain consecutive special characters");
				
				if( PrincipalUtil.isPrincipal(identity) )
					throw new Exception("The identity name may not look like a Distinguished Name (DN)");
				if( !IdentityChecker.getInstance().userExists(user) )
					throw new Exception("The target user does not exist");
				
				// if the org is specified : check if the user is admin of that org
				if( org != null && !IdentityChecker.getInstance().isUserOrgAdmin(user, org) )
					throw new Exception("The target user is not administrator of the provided organization");
				
				long uid = INTERNAL_IDENTITY_INSERT(identity, org, (org != null ? org : user));
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", identity);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new secured identity";
		insert.returnDescription = "The newly created identity {'name', 'id'}";
		insert.addGrant(new String[] { "access", "identity_insert" });
		
		Parameter identity = new Parameter();
		identity.isOptional = false;
		identity.minLength = 3;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		identity.allowInUrl = true;
		identity.description = "The identity name";
		identity.addAlias(new String[]{ "identity", "name", "identity_name"});
		insert.addParameter(identity);
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		insert.addParameter(user);
		
		Parameter org = new Parameter();
		org.isOptional = true;
		org.minLength = 1;
		org.maxLength = 30;
		org.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		org.description = "The organization name or id";
		org.addAlias(new String[]{ "org", "organization", "org_name", "org_id", "oid" });
		insert.addParameter(org);
		
		index.addOwnHandler(insert);
	}

	static long INTERNAL_IDENTITY_INSERT(String identity, String org, String user) throws Exception
	{
		Quota.getInstance().increment(user, Config.gets("com.busit.rest.quota.identity"));
		
		Long uid = 0L;

		com.busit.security.Identity signer = null;
		//if( org == null || org.length() == 0 )
			signer = IdentityStore.getInstance().getUnchecked();
		//else
		//	signer = IdentityStore.getInstance().getIdentity(org);
		
		if( org != null && org.matches("^[0-9]+$") )
		{
			Map<String, String> o = Database.getInstance().selectOne("SELECT user_name FROM users WHERE user_org AND user_id = " + org);
			if( o == null || o.get("user_name") == null )
				throw new Exception("Invalid organization");
			org = o.get("user_name");
		}
		
		if( !user.matches("^[0-9]+$") )
			user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";

		for( int attempt = 0; attempt <= 10; attempt++ )
		{
			if( attempt == 10 )
				throw new Exception("Failed to generate a unique identity");

			// ==========================================
			// Create the user certificate
			// ==========================================
			KeyPair pair = Crypto.generateKeys();
			com.busit.security.Identity i = new com.busit.security.Identity(
				pair.getPrivate(), 
				Crypto.generateCertificate(
					Crypto.generatePrincipal(identity, org),
					pair.getPublic(),
					signer)
				);
				
			if( IdentityStore.getInstance().getAuthority().equals(i) || 
				IdentityStore.getInstance().getAuthentic().equals(i) || 
				IdentityStore.getInstance().getUnchecked().equals(i) )
				continue;
			
			// important : we sign and crypt (origin + destination) using the authority
			// because if we encrypt the user's private key with itself, no one can decrypt it afterwards
			// and if we sign using the user's public key, the IdentityStore cannot load the identity (loop)
			String private_key = Crypto.encrypt(
				IdentityStore.getInstance().getAuthority(),
				IdentityStore.getInstance().getAuthority(),
				pair.getPrivate().getEncoded());
			
			// ==========================================
			// Insert the identity
			// ==========================================
			long expire = Crypto.generateExpireTimestamp(i.getCertificate());
			
			try
			{
				String certificate = Crypto.toString(i.getCertificate());// + "\n" + Crypto.toString(signer.getCertificate());
				uid = Database.getInstance().insert("INSERT INTO identities (identity_birth, identity_key_public, identity_key_private, " + 
					"identity_key_expire, identity_principal, identity_user) VALUES (" + 
					"UNIX_TIMESTAMP(), '" + Security.escape(certificate) + "', '" + Security.escape(private_key) + "', " + expire + 
					", '" + Security.escape(i.getSubjectPrincipal().getName()) + "', " + user + ")");
			}
			catch(Exception e)
			{
// TODO :
// catch ERROR 1062 (23000): Duplicate entry 'xxx...' for key 'identity_principal'
//						if( ? )
//							continue;
//						else
					throw e;
			}
			
			break;
		}

		return uid;
	}
	
	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String identity = getParameter("identity").getValue();
				String description = getParameter("description").getValue();
				String info = getParameter("info").getValue();
				String visible = getParameter("visible").getValue();
				String user = getParameter("user").getValue();
				
				if( !IdentityChecker.getInstance().isUserIdentity(user, identity) && !IdentityChecker.getInstance().isUserIdentityAdmin(user, identity) )
					throw new Exception("The target user is not administrator of the organization owning the provided identity");
				
				String where = "";
				if( identity.matches("^[0-9]+$") )
					where = "AND identity_id = " + identity;
				else
					where = "AND identity_principal = '" + Security.escape(PrincipalUtil.parse(identity).getName()) + "'";
				
				String set = "";
				if( description != null )
					set += "identity_description = '" + Security.escape(description) + "', ";
				if( info != null )
					set += "identity_info = '" + Security.escape(info) + "', ";
					
				if( visible != null && visible.matches("^(?i)(yes|true|1)$") )
					set += "identity_visible = 1, ";
				else if ( visible != null )
					set += "identity_visible = 0, ";
					
				Database.getInstance().update("UPDATE identities SET " + set + " identity_id = identity_id WHERE 1=1 " + where);
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Update an identity";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "identity_update" });

		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity.allowInUrl = true;
		identity.description = "The identity name, principal or id.";
		identity.addAlias(new String[]{ "identity", "identity_name", "identity_id", "principal", "identity_principal"});
		update.addParameter(identity);

		Parameter description = new Parameter();
		description.isOptional = true;
		description.minLength = 3;
		description.maxLength = 200;
		description.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		description.description = "The identity description";
		description.addAlias(new String[]{ "description", "identity_description" });
		update.addParameter(description);

		Parameter info = new Parameter();
		info.isOptional = true;
		info.minLength = 3;
		info.maxLength = 500;
		info.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		info.description = "The identity informations (JSON)";
		info.addAlias(new String[]{ "info", "information", "identity_info" });
		update.addParameter(info);
		
		Parameter visible = new Parameter();
		visible.isOptional = true;
		visible.minLength = 1;
		visible.maxLength = 5;
		visible.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		visible.description = "Whether or not to search this identity";
		visible.addAlias(new String[]{ "visible", "public" });
		update.addParameter(visible);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id. Mutually exclusive with 'org' and 'identity'.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		update.addParameter(user);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		/*
		IMPORTANT NOTE : an identity cannot be removed because we must be able to trace
		what has been sent with this identity and disable anyone (else) to reuse the same
		name and thus probably hijack the previous identity.
		Hence, we kill it and mark it as -dead-... Serial-identity killer ! Mouahahhaaaa
		*/
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String identity = getParameter("identity").getValue();
				String user = getParameter("user").getValue();
				String ghost = getParameter("ghost").getValue();
				
				// TODO : if an ORG -> we should invalidate all signed identities...
				
				if( !IdentityChecker.getInstance().isUserIdentity(user, identity) && !IdentityChecker.getInstance().isUserIdentityAdmin(user, identity) )
					throw new Exception("The target user is not administrator of the organization owning the provided identity");
				
				String where = "";
				if( identity.matches("^[0-9]+$") )
					where = "identity_id = " + identity;
				else
					where = "identity_principal = '" + Security.escape(PrincipalUtil.parse(identity).getName()) + "'";
				
				long count = 0;
				if( ghost != null && ghost.matches("^(?i)(no|false|0)$") )
					count = Database.getInstance().delete("DELETE FROM identities WHERE " + where);
				else
					count = Database.getInstance().delete("UPDATE identities SET identity_death = UNIX_TIMESTAMP() WHERE identity_death IS NULL AND " + where);
				Quota.getInstance().substract(user, Config.gets("com.busit.rest.quota.identity"), (int)count, true);
				
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy", "deactivate" });
		delete.description = "Deactivates an identity permanently.";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "identity_delete" });
		
		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity.allowInUrl = true;
		identity.description = "The identity name or id";
		identity.addAlias(new String[]{ "identity", "name", "identity_name", "identity_id", "id" });
		delete.addParameter(identity);
		
		Parameter ghost = new Parameter();
		ghost.isOptional = true;
		ghost.minLength = 1;
		ghost.maxLength = 5;
		ghost.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		ghost.description = "Should the identity be marked as ghost instead of true deletion (default: true)";
		ghost.addAlias(new String[]{ "ghost" });
		delete.addParameter(ghost);
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
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
				String identity = getParameter("identity").getValue();
				String user = getParameter("user").getValue();
				String org = getParameter("org").getValue();
				String keyword = getParameter("keyword").getValue();
				
				// identity, user, org are mutually exclusive
				if( identity != null && org != null )
					throw new Exception("The org and identity parameters are mutually exclusive");
				if( identity == null && org == null && user == null && keyword == null )
					throw new Exception("One of keyword, user, org or identity parameters must be specified");
				
				// ===================
				// BASED ON KEYWORD
				// ===================
				if( keyword != null )
				{
					String where = "";
					where = "identity_visible = 1 AND identity_death IS NULL " + 
						"AND (identity_principal LIKE '%" + Security.escape(keyword) + "%' OR " + 
						"identity_description LIKE '%" + Security.escape(keyword) + "%' OR " + 
						"identity_info LIKE '%" + Security.escape(keyword) + "%')";
					
					String sql = "SELECT identity_id, identity_key_expire, identity_birth, identity_description, identity_principal, identity_info, identity_visible " + 
						"FROM identities WHERE " + where;
					Vector<Map<String, String>> data = Database.getInstance().select(sql);
					for( Map<String, String> d : data )
						d.put("identity_name", PrincipalUtil.shortName(d.get("identity_principal")));
					return data;
				}
				
				// ===================
				// BASED ON IDENTITY
				// ===================
				if( identity != null )
				{
					String where = "";
					if( !identity.matches("^[0-9]+$") )
					{
						Principal origin = PrincipalUtil.parse(identity);
						if( PrincipalUtil.isRemote(origin) )
							throw new Exception("Unknown identity"); // this is not an identity we manage
						where += " AND i.identity_principal = '" + Security.escape(origin.getName()) + "'";
					}
					else
						where += " AND i.identity_id = " + identity;
					
					if( user != null )
					{
						if( !user.matches("^[0-9]+$") )
							where += " AND u.user_name = '" + Security.escape(user) + "'";
						else
							where += " AND u.user_id = " + user;
					}
					
					String sql = "SELECT DISTINCT i.identity_id, i.identity_key_expire, i.identity_birth, i.identity_description, i.identity_principal, i.identity_info, " + 
						"i.identity_visible, GROUP_CONCAT(IFNULL(o.user_id,'') SEPARATOR '|') as org_ids, GROUP_CONCAT(IFNULL(o.user_name,'') SEPARATOR '|') as org_names, " +
						"GROUP_CONCAT(IFNULL(om.org_admin,FALSE) SEPARATOR '|') as org_admins, IF(ii.identity_to IS NULL, FALSE, TRUE) as identity_impersonate " + 
						"FROM users u " +
						"LEFT JOIN identities ui ON(u.user_id = ui.identity_user) " +
						"LEFT JOIN impersonate ii ON(ii.identity_from = ui.identity_id) " +
						"LEFT JOIN identities i ON(i.identity_id = ii.identity_to OR i.identity_id = ui.identity_id) " +
						"LEFT JOIN org_member om ON(om.identity_id = i.identity_id) " +
						"LEFT JOIN users o ON(o.user_id = om.org_id AND o.user_org) " +
						"WHERE i.identity_death IS NULL AND i.identity_id IS NOT NULL "  + where + 
						" GROUP BY i.identity_id";
						
					Vector<Map<String, String>> data = Database.getInstance().select(sql);
					Vector<Map<String, Object>> result = new Vector<Map<String, Object>>();
					for( Map<String, String> d : data )
					{
						Map<String, Object> r = new HashMap<String, Object>();
						r.putAll(d);
						r.put("identity_name", PrincipalUtil.shortName(d.get("identity_principal")));
						
						// split orgs
						Vector<Map<String, String>> orgs = new Vector<Map<String, String>>();
						List<String> _ids = new LinkedList<String>();
						String[] ids = d.get("org_ids").split("\\|");
						String[] names = d.get("org_names").split("\\|");
						String[] admins = d.get("org_admins").split("\\|");
						for( int i = 0; i < ids.length; i++ )
						{
							if( ids[i] == null || ids[i].length() == 0 ) continue;
							if( _ids.contains(ids[i]) ) continue;
							_ids.add(ids[i]);
							Map<String, String> o = new HashMap<String, String>();
							o.put("org_id", ids[i]);
							o.put("org_name", names[i]);
							o.put("org_admin", admins[i]);
							orgs.add(o);
						}
						r.put("membership", orgs);
						r.remove("org_ids");
						r.remove("org_names");
						r.remove("org_admins");
						result.add(r);
					}
					return result;
				}
				
				// ===================
				// BASED ON ORG
				// ===================
				if( org != null )
				{
					// only the org admin can list org identities
					if( user != null && !IdentityChecker.getInstance().isUserOrgAdmin(user, org) )
						throw new Exception("You are not allowed to list members of the provided organization");
					
					String where = "";
					if( !org.matches("^[0-9]+$") )
						where += "o.user_name = '" + Security.escape(org) + "'";
					else
						where += "o.user_id = " + org;
					
					String sql = "SELECT DISTINCT i.identity_id, i.identity_key_expire, i.identity_birth, i.identity_description, i.identity_principal, i.identity_info, " + 
						"i.identity_visible, GROUP_CONCAT(IFNULL(ui.identity_principal, '') SEPARATOR '|') as user_identity_names, " + 
						"GROUP_CONCAT(IFNULL(ui.identity_id, '') SEPARATOR '|') as user_identity_ids " + 
						"FROM identities i " + 
						"LEFT JOIN users o ON(i.identity_user = o.user_id AND o.user_org) " +
						"LEFT JOIN impersonate ii ON(ii.identity_to = i.identity_id) " + 
						"LEFT JOIN identities ui ON(ui.identity_id = ii.identity_from) " +
						"WHERE i.identity_death IS NULL AND "  + where + 
						" GROUP BY i.identity_id";
					
					Vector<Map<String, String>> data = Database.getInstance().select(sql);
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
				
				// ===================
				// BASED ON USER
				// ===================
				if( user != null )
				{
					String where = "";
					if( !user.matches("^[0-9]+$") )
						where += "u.user_name = '" + Security.escape(user) + "'";
					else
						where += "u.user_id = " + user;
					
					String sql = "SELECT DISTINCT i.identity_id, i.identity_key_expire, i.identity_birth, i.identity_description, i.identity_principal, i.identity_info, " + 
						"i.identity_visible, GROUP_CONCAT(IFNULL(o.user_id,'') SEPARATOR '|') as org_ids, GROUP_CONCAT(IFNULL(o.user_name,'') SEPARATOR '|') as org_names, " +
						"GROUP_CONCAT(IFNULL(om.org_admin,FALSE) SEPARATOR '|') as org_admins, IF(ii.identity_to IS NULL, FALSE, TRUE) as identity_impersonate " + 
						"FROM users u " +
						"LEFT JOIN identities ui ON(u.user_id = ui.identity_user) " +
						"LEFT JOIN impersonate ii ON(ii.identity_from = ui.identity_id) " +
						"LEFT JOIN identities i ON(i.identity_id = ii.identity_to OR i.identity_id = ui.identity_id) " +
						"LEFT JOIN org_member om ON(om.identity_id = i.identity_id) " +
						"LEFT JOIN users o ON(o.user_id = om.org_id AND o.user_org) " +
						"WHERE i.identity_death IS NULL AND i.identity_id IS NOT NULL AND " + where + 
						" GROUP BY i.identity_id";
						
					Vector<Map<String, String>> data = Database.getInstance().select(sql);
					Vector<Map<String, Object>> result = new Vector<Map<String, Object>>();
					for( Map<String, String> d : data )
					{
						Map<String, Object> r = new HashMap<String, Object>();
						r.putAll(d);
						r.put("identity_name", PrincipalUtil.shortName(d.get("identity_principal")));
						
						// split orgs
						Vector<Map<String, String>> orgs = new Vector<Map<String, String>>();
						List<String> _ids = new LinkedList<String>();
						String[] ids = d.get("org_ids").split("\\|");
						String[] names = d.get("org_names").split("\\|");
						String[] admins = d.get("org_admins").split("\\|");
						for( int i = 0; i < ids.length; i++ )
						{
							if( ids[i] == null || ids[i].length() == 0 ) continue;
							if( _ids.contains(ids[i]) ) continue;
							_ids.add(ids[i]);
							Map<String, String> o = new HashMap<String, String>();
							o.put("org_id", ids[i]);
							o.put("org_name", names[i]);
							o.put("org_admin", admins[i]);
							orgs.add(o);
						}
						r.put("membership", orgs);
						r.remove("org_ids");
						r.remove("org_names");
						r.remove("org_admins");
						result.add(r);
					}
					return result;
				}
				
				throw new Exception("Unreachable statement detected");
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves identities related to a user, an org or an identity itself.";
		select.returnDescription = "The matching identities [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "identity_select" });

		Parameter keyword = new Parameter();
		keyword.isOptional = true;
		keyword.minLength = 2;
		keyword.maxLength = 50;
		keyword.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		keyword.isMultipleValues = true;
		keyword.description = "A keyword to search (excludes all other parameters!)";
		keyword.addAlias(new String[]{ "keyword", "key" });
		select.addParameter(keyword);
		
		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity.allowInUrl = true;
		identity.description = "The identity name, principal or id. Mutually exclusive with 'org' and 'user'.";
		identity.addAlias(new String[]{ "identity", "identity_name", "identity_id", "principal", "identity_principal"});
		select.addParameter(identity);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id. Mutually exclusive with 'org' and 'identity'.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		select.addParameter(user);
		
		Parameter org = new Parameter();
		org.isOptional = true;
		org.minLength = 1;
		org.maxLength = 30;
		org.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		org.description = "The organization name or id. Mutually exclusive with 'identity' and 'user'.";
		org.addAlias(new String[]{ "org", "organization", "org_name", "org_id", "oid" });
		select.addParameter(org);

		index.addOwnHandler(select);
	}
	
	private void initializeDownloadPublic(Index index)
	{
		Action download = new Action()
		{
			public Object execute() throws Exception
			{
				String identity = getParameter("identity").getValue();
				String binary = getParameter("binary").getValue();
				
				com.busit.security.Identity i = IdentityStore.getInstance().getIdentity(identity);

				if( i == null )
					throw new Exception("Unknown identity");
				
				if( binary != null && binary.matches("^(?i)(no|false|0)$") )
				{
					Hashtable result = new Hashtable();
					result.put("certificate", i.getCertificate());
					return result;
				}
				else
				{
					// add the file name in the request
					Responder.contentType.set("application/pkix-cert");
					Request.addAction(Security.escape(i.getSubjectName()) + ".crt");
					Request.getAction();
					return new ByteArrayInputStream(i.getCertificate().x509().getEncoded());
				}
			}
		};
		
		download.addMapping(new String[] { "certificate", "public", "download_public", "public_key" });
		download.description = "Download the public key certificate of the provided identity";
		download.returnDescription = "The certificate";
		
		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity.description = "The identity name, id or complete principal";
		identity.addAlias(new String[]{ "identity", "name", "identity_name", "identity_id", "id" });
		download.addParameter(identity);
		
		Parameter binary = new Parameter();
		binary.isOptional = true;
		binary.minLength = 1;
		binary.maxLength = 5;
		binary.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		binary.description = "Whether or not to send the result as a binary file (default true)";
		binary.addAlias(new String[]{ "binary", "raw" });
		download.addParameter(binary);
		
		index.addOwnHandler(download);
	}
	
	private void initializeDownloadPrivate(Index index)
	{
		Action download = new Action()
		{
			public Object execute() throws Exception
			{
				String identity = getParameter("identity").getValue();
				String binary = getParameter("binary").getValue();
				com.busit.security.Identity i = null;
				
				// check if the user is has the proper privileges (or is the broker -> can get all)
				// the broker can request EVERYONE or AUTHORITY that will be handled by the IdentityStore
				if( Security.getInstance().hasGrant(Config.gets("com.busit.rest.brokerGrantName")) || 
					IdentityChecker.getInstance().isUserIdentityOrImpersonate(Security.getInstance().getUser(), identity) )
				{
					i = IdentityStore.getInstance().getIdentity(identity);
					if( i == null )
						throw new Exception("Unknown identity " + identity + " requested as " + Security.getInstance().getUser());
				}
				else
					throw new Exception("Not authorized");
				
				String privateKey = Crypto.toString(i.getPrivateKey());
				if( binary != null && binary.matches("^(?i)(yes|true|1)$") )
				{
					// add the file name in the request
					Request.addAction(Security.escape(i.getSubjectName()) + ".rsa");
					Request.getAction();
					return new ByteArrayInputStream(Hex.getBytes(privateKey));
				}
				else
				{
					Hashtable result = new Hashtable();
					result.put("key", privateKey);
					return result;
				}
			}
		};
		
		download.addMapping(new String[] { "private", "download_private", "private_key" });
		download.description = "Download the private key of the provided identity. The target identity must be available to the current authenticated user.";
		download.returnDescription = "The private key";
		download.addGrant(new String[] { "access" });
		
		Parameter identity = new Parameter();
		identity.isOptional = false;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity.description = "The identity name, id or complete principal";
		identity.addAlias(new String[]{ "identity", "name", "identity_name", "identity_id", "id" });
		download.addParameter(identity);
		
		Parameter binary = new Parameter();
		binary.isOptional = true;
		binary.minLength = 1;
		binary.maxLength = 5;
		binary.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		binary.description = "Whether or not to send the result as a binary file";
		binary.addAlias(new String[]{ "binary", "raw" });
		download.addParameter(binary);
		
		index.addOwnHandler(download);
	}
	
	private void initializeDownloadCRL(Index index)
	{
		Action download = new Action()
		{
			public Object execute() throws Exception
			{
				String binary = getParameter("binary").getValue();
				
/*
Denis,

 I believe that RFC 3280 is quite clear on this issue.  If the certificate does not include a cRLDistributionPoints extension with cRLIssuer present, 
 then the CRL used to determine the certificate's status must have the same issuer name as the certificate:

6.3.3  CRL Processing

   For each distribution point (DP) in the certificate CRL distribution
   points extension, for each corresponding CRL in the local CRL cache,
   while ((reasons_mask is not all-reasons) and (cert_status is
   UNREVOKED)) perform the following:

      (a) ...

      (b)  Verify the issuer and scope of the complete CRL as follows

         (1)  If the DP includes cRLIssuer, then verify that the issuer
         field in the complete CRL matches cRLIssuer in the DP and that
         the complete CRL contains an issuing distribution point
         extension with the indrectCRL boolean asserted.  Otherwise,
         verify that the CRL issuer matches the certificate issuer.

   ...

   If the revocation status has not been determined, repeat the process
   above with any available CRLs not specified in a distribution point
   but issued by the certificate issuer.  For the processing of such a
   CRL, assume a DP with both the reasons and the cRLIssuer fields
   omitted and a distribution point name of the certificate issuer.
   That is, the sequence of names in fullName is generated from the
   certificate issuer field as well as the certificate issuerAltName
   extension.


 So, if the cRLDistributionPoints extension is absent, revocation status must be determined as specified in the final paragraph of 6.3.3 which states that the CRL issuer and certificate issuer must match (this is stated in the first sentence and backed up in the second sentence which states that the CRL should be processed as if the certificate included a CDP with the cRLIssuer field absent).  Step (b)(1) states that if the cRLIssuer field is absent, the certificate and CRL issuer names must match.

 Dave

 Denis Pinkas wrote:

David, 

 It has been a long time since we exchanged e-mails on delta CRLs. :-) 


Juergen, 

 What you are describing below is an indirect CRL, a CRL that covers certificates issued different entity.  (I know you stated that same CA issues both the certificates and the CRL, but since the issuer names are different, they are considered different entities.  Furthermore, there is no way for a relying party to know whether the certificate issuer and CRL issuer are the same entity). 

 Usually, a CRL can not be used to determine the status of a certificate unless the certificate and CRL both have the same issuer name.  The value of the AKI extension does not matter, as it is only a hint that can aid in building paths and in selecting the right CRL. 

 In order to allow a relying party to use a CRL to determine the status of a certificate where the issuer names differ, you need to do the following: 

 1) Each certificate issued by "CA1" must include a cRLDistributionPoints extension with a cRLIssuer field that indicates that "CRL1" is the CRL issuer. 

 2) The CRLs issued by "CRL1" must include an issuingDistributionPoint extension with the indirectCRL flag set to TRUE. 

 3)  If any of CA1's certificates have been revoked, the certificateIssuer CRL entry extension must be used within the CRLs issued by "CRL1" to indicate that list of revoked serial numbers are of certificates issued by "CA1" (see RFC 3280 for details on the use of these three extensions). 


 While what is above is true, I wonder that is the *only* case "to allow a relying party to use a CRL to determine the status of a certificate where the issuer names differ". 

 In particular, if CDP are *not* used, then it is still possible to have a CRL issuer. 

 RFC 3280 states: 

    CRL issuer: an optional system to which a CA delegates the 
                publication of certificate revocation lists; 

    CRL issuers issue CRLs.  In general, the CRL issuer is the CA. 
    CAs publish CRLs to provide status information about the certificates 
    they issued.  However, a CA may delegate this responsibility to 
    another trusted authority.  Whenever the CRL issuer is not the CA 
    that issued the certificates, the CRL is referred to as an indirect 
    CRL. 

 The wording may be confusing. A CRL issuer name is a name that is different from the name of the CA. However, since the CA may delegate the publication of the CRLs to a CRL issuer, is it still the CA or a CRL issuer ? I would think the later.A CA may delegate CRL issuing, but must use the cRLIssuer field of the cRLDistributionPoints extension to indicate that it has done so.

   Whenever the CRL issuer is not the CA that issued the certificates, 
    the CRL is referred to as an indirect CRL. 

 I wonder whether the wording indirect CRL is fine, since it has a different meaning in section 5.2.5  Issuing Distribution Point. 

    The CRL issuer MUST assert the indirectCRL boolean, if the scope of 
    the CRL includes certificates issued by authorities other than the 
    CRL issuer. 

 The indirectCRL boolean flag is something that may be present or absent in what is currently called a indirect CRL. A little bit confusing. A change or a clarification would be apropriate. 
If the indirectCRL flag is absent, then the CRL is not an indirect CRL.  If the indirectCRL flag is not set to TRUE, then the CRL may only covers certificates issued by the CRL issuer.  Such a CRL is not an indirect CRL.



While the rules for issuing and processing indirect CRLs are specified in RFC 3280, RFC 3280 compliant clients are not required to be able to process indirect CRLs. 


 Section 6.3 CRL Validation is almost silent on how to determine that the CRL is the right one, in particular when the CRL issuer name is different from the CA name and when no CDP is being used. The only guidance is: 

 " (f)  Obtain and validate the certification path for the complete CRL
 issuer." 
 I would assume that this means find out a certificate issued by the CA which has issued the certificate to be tested and which includes the cRLSign bit 6 in the keyUsage extension. Then, use that certificate to validate CRLs that seem to be originating from that CRL issuer name. 

 It would be useful to include such additional information in a revised version of RFC 3280. 

    Conforming applications are NOT 
    REQUIRED to support processing of delta CRLs, indirect CRLs, or CRLs 
    with a scope other than all certificates issued by one CA. 

 Conforming applications are certainly not required to be able to process the indirectCRL boolean, when CDP are being used. What does mean however mean "not required to processing of indirect CRLs" in case the CA nominates a single CRL issuer ? 

 Denis 



Dave 

 Juergen Brauckmann wrote: 


Hi. 

 I have a question regarding the naming scheme for CRLs. Consider the 
 following: 

 EE certificates are issued by a CA, Issuer-DN "CA1". 
 The CA uses a CA certificate issued by a root ca: Issuer-DN "Root1", 
 Subject-DN "CA1", serialnumber "1". 
 The CA issues CRLs, and signs them with a CRL certificate wich was also 
 issued by a root CA, but with another distinguished name than the main 
 CA certificate: Issuer-DN "Root1", Subject-DN "CRL1", serialnumber "2" 

 Is an RFC 3280 compliant client supposed to be able to process the CRLs 
 if the CRL Issuer-DN is set to "CA1" and the CRL contains an 
 AuthorityKeyIdentifier pointing to the CRL certificate with Subject-DN 
 "CRL1"? 

 Is this scheme, where the issuer name from CRL does not match Subject DN 
 from CRL signing certificate, covered by step f) from chapter 6.3.3 from 
 RFC 3280? 

 "f) Obtain and validate the certification path for the complete CRL 
   issuer.  If a key usage extension is present in the CRL issuer's 
   certificate, verify that the cRLSign bit is set." 


 Regards, 
   Juergen 
*/
				// get all dead identities
				String sql = "SELECT i.identity_key_public, i.identity_death " + 
						"FROM identities i " +
						"WHERE i.identity_death IS NOT NULL AND i.identity_key_expire > UNIX_TIMESTAMP()";
						
				Vector<Map<String, String>> identities = Database.getInstance().select(sql);
				Map<com.busit.security.Certificate, Long> list = new Hashtable<com.busit.security.Certificate, Long>(identities.size());
				for( Map<String, String> identity : identities )
					list.put(Crypto.parseCertificate(identity.get("identity_key_public")), Long.parseLong(identity.get("identity_death")) * 1000);
				
				X509CRL crl = Crypto.generateCRL(list);
				
				if( binary != null && binary.matches("^(?i)(no|false|0)$") )
				{
					Hashtable result = new Hashtable();
					result.put("crl", Crypto.toString(crl));
					return result;
				}
				else
				{
					// add the file name in the request
					Responder.contentType.set("application/pkix-crl");
					Request.addAction("global.crl");
					Request.getAction();
					return new ByteArrayInputStream(crl.getEncoded());
				}
			}
		};
		
		download.addMapping(new String[] { "crl", "revoked" });
		download.description = "Download the public key certificate of the provided identity";
		download.returnDescription = "The certificate";
		
		Parameter binary = new Parameter();
		binary.isOptional = true;
		binary.minLength = 1;
		binary.maxLength = 5;
		binary.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		binary.description = "Whether or not to send the result as a binary file (default true)";
		binary.addAlias(new String[]{ "binary", "raw" });
		download.addParameter(binary);
		
		index.addOwnHandler(download);
	}
	
	private void initializeSign(Index index)
	{
		// todo : keep a table of signed identities
		// --> revoke = resign using everyone
		
		// todo : how to get the target identity's consent ?
		// using a special token for that identity ?
	}
}