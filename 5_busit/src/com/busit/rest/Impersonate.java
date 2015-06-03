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

public class Impersonate extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "impersonate", "impersonation" });
		index.description = "Manages identity impersonation";
		Handler.addHandler("/busit/identity/", index);
		
		initializeInsert(index);
		initializeDelete(index);
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				// The org admin can say which member can use one of the org's identities
				String org = getParameter("org").getValue();
				String identity_org = getParameter("identity_org").getValue();
				String identity_user = getParameter("identity_user").getValue();
				
				if( !IdentityChecker.getInstance().isUserOrgAdmin(null, org) && !Security.getInstance().hasGrant(Config.gets("com.busit.rest.admin.grant")) )
					throw new Exception("The current user is not an administrator of the provided organization");
				if( !IdentityChecker.getInstance().isOrgIdentity(org, identity_org) )
					throw new Exception("The provided identity does not belong to the provided organization");
				if( !IdentityChecker.getInstance().isIdentityOrgMember(identity_user, org) )
					throw new Exception("The provided user identity is not member of the provided organization");

				if( !identity_org.matches("^[0-9]+$") )
					identity_org = "(SELECT identity_id FROM identities WHERE identity_principal = '" + Security.escape(PrincipalUtil.parse(identity_org).getName()) + "')";
				if( !identity_user.matches("^[0-9]+$") )
					identity_user = "(SELECT identity_id FROM identities WHERE identity_principal = '" + Security.escape(PrincipalUtil.parse(identity_user).getName()) + "')";

				Database.getInstance().insert("INSERT INTO impersonate (identity_from, identity_to) VALUES (" + identity_user + ", " + identity_org + ")");
				
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "associate", "allow" });
		insert.description = "Allows a user identity to use an organization identity";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "org_update" });

		Parameter organization = new Parameter();
		organization.isOptional = false;
		organization.minLength = 1;
		organization.maxLength = 30;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		organization.description = "The organization name or id";
		organization.addAlias(new String[]{ "org", "organization", "organization_name", "org_name", "organization_id", "org_id", "oid" });
		insert.addParameter(organization);
		
		Parameter identity_org = new Parameter();
		identity_org.isOptional = false;
		identity_org.minLength = 1;
		identity_org.maxLength = 100;
		identity_org.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity_org.description = "The organization's identity name or id";
		identity_org.addAlias(new String[]{ "identity_org", "identity_org_name", "identity_org_id", "identity_to" });
		insert.addParameter(identity_org);
		
		Parameter identity_user = new Parameter();
		identity_user.isOptional = false;
		identity_user.minLength = 1;
		identity_user.maxLength = 100;
		identity_user.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity_user.description = "The user's identity name or id";
		identity_user.addAlias(new String[]{ "identity_user", "identity_user_name", "identity_user_id", "identity_from" });
		insert.addParameter(identity_user);
		
		index.addOwnHandler(insert);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String org = getParameter("org").getValue();
				String identity_org = getParameter("identity_org").getValue();
				String identity_user = getParameter("identity_user").getValue();
				
				if( !IdentityChecker.getInstance().isUserOrgAdmin(null, org) && !Security.getInstance().hasGrant(Config.gets("com.busit.rest.admin.grant")) )
					throw new Exception("The current user is not an administrator of the provided organization");
				if( !IdentityChecker.getInstance().isOrgIdentity(org, identity_org) || !IdentityChecker.getInstance().isIdentityImpersonate(identity_user, identity_org) )
					throw new Exception("The provided identities do not fulfill requirements for depersonation");

				if( !identity_org.matches("^[0-9]+$") )
					identity_org = "(SELECT identity_id FROM identities WHERE identity_principal = '" + Security.escape(PrincipalUtil.parse(identity_org).getName()) + "')";
				if( !identity_user.matches("^[0-9]+$") )
					identity_user = "(SELECT identity_id FROM identities WHERE identity_principal = '" + Security.escape(PrincipalUtil.parse(identity_user).getName()) + "')";

				Database.getInstance().insert("DELETE FROM impersonate WHERE identity_from = " + identity_user + " AND identity_to = " + identity_org);
				
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "disociate", "disallow" });
		delete.description = "Denies a user identity to use an organization identity";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "org_update" });

		Parameter organization = new Parameter();
		organization.isOptional = false;
		organization.minLength = 1;
		organization.maxLength = 30;
		organization.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		organization.description = "The organization name or id";
		organization.addAlias(new String[]{ "org", "organization", "organization_name", "org_name", "organization_id", "org_id", "oid" });
		delete.addParameter(organization);
		
		Parameter identity_org = new Parameter();
		identity_org.isOptional = false;
		identity_org.minLength = 1;
		identity_org.maxLength = 100;
		identity_org.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity_org.description = "The organization's identity name or id";
		identity_org.addAlias(new String[]{ "identity_org", "identity_org_name", "identity_org_id", "identity_to" });
		delete.addParameter(identity_org);
		
		Parameter identity_user = new Parameter();
		identity_user.isOptional = false;
		identity_user.minLength = 1;
		identity_user.maxLength = 100;
		identity_user.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity_user.description = "The user's identity name or id";
		identity_user.addAlias(new String[]{ "identity_user", "identity_user_name", "identity_user_id", "identity_from" });
		delete.addParameter(identity_user);
		
		index.addOwnHandler(delete);
	}
}
