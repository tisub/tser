package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.rest.core.*;
import com.anotherservice.db.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.lang.*;
import java.security.Principal;

public class CA extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "CA" });
		index.description = "Manages busit CA";
		Handler.addHandler("/busit/", index);
		
		initializeUpdate(index);
		initializeRegen(index);
	}
	
	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String type = getParameter("type").getValue();
				
				if( !Request.hasAttachment() )
					throw new Exception("Missing attachment");
					
				Collection<String> names = Request.getAttachmentNames();
				if( names.size() != 2 )
					throw new Exception("There should be only 2 attachments : the .crt file and the .priv file");
				
				String certificate = "";
				String privateKey = "";
				for( String name : names )
				{
					if( name.endsWith(".crt") )
						certificate = Hex.toString(Request.getAttachment(name));
					else if( name.endsWith(".priv") )
						privateKey = Hex.toString(Request.getAttachment(name));
					else
						throw new Exception("The attachment must be a .crt or .priv file");
				}
				com.busit.security.Identity CA = new com.busit.security.Identity(privateKey, certificate);
				
				Logger.warning("Updating root CA for " + type);
				switch(type.toLowerCase())
				{
					case "unchecked": updateUnchecked(CA); break;
					case "authentic": updateAuthentic(CA); break;
					case "authroity": updateAuthority(CA); break;
				}
				
				return "OK";
			}
			
			private void updateUnchecked(com.busit.security.Identity new_CA) throws Exception
			{
				List<Map<String, String>> rows = Database.getInstance().select("SELECT i.identity_id, i.identity_key_public, i.identity_key_private " + 
					"FROM identities i " +
					"LEFT JOIN users u ON(u.user_id = i.identity_user) " +
					"WHERE NOT u.user_org"); // TODO : where everyone ?
				
				com.busit.security.Identity i;
				for( Map<String, String> row : rows )
				{
					Certificate c = new Certificate(row.get("identity_key_public"));
					
					// generate a new certificate with the new CA
					String cert = Crypto.toString(Crypto.generateCertificate(
						c.x509().getSubjectX500Principal(), 
						c.x509().getPublicKey(),
						new_CA.getSubjectPrincipal(),
						new_CA.getPrivateKey(),
						new_CA.getPublicKey(),
						c.x509().getNotBefore(),
						c.x509().getNotAfter()));
					
					Database.getInstance().update("UPDATE identities SET identity_key_public = '" + cert + "' WHERE identity_id = " + row.get("identity_id"));
				}
				// value = "'" + CryptoSimple.serialize(value) + "'";
				// r.put("config_value", CryptoSimple.unserialize(r.get("config_value")));
				
			}
			
			private void updateAuthentic(com.busit.security.Identity new_CA) throws Exception
			{
			}
			
			private void updateAuthority(com.busit.security.Identity new_CA) throws Exception
			{
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Update the root CA. You must provide 2 attachments : the PEM .crt certificate file and the PEM .priv private key file. Don't forget to update the server certificate and restart the server after this";
		update.returnDescription = "OK";
		update.addGrant(new String[] { Config.gets("com.busit.rest.admin.grant") });
		
		Parameter type = new Parameter();
		type.isOptional = false;
		type.mustMatch = "^(?i)(authority|authentic|unchecked)$";
		type.description = "Which type of CA to modify (authority|authentic|unchecked).";
		type.addAlias(new String[]{ "which", "who", "type" });
		update.addParameter(type);
		
		index.addOwnHandler(update);
	}
	
	private void initializeRegen(Index index)
	{
		Action regen = new Action()
		{
			public Object execute() throws Exception
			{
				String identity = getParameter("identity").getValue();
				
				String where = "";
				if( identity != null )
				{
					if( !identity.matches("^[0-9]+$") )
					{
						Principal origin = PrincipalUtil.parse(identity);
						where += " AND i.identity_principal = '" + Security.escape(origin.getName()) + "'";
					}
					else
						where += " AND i.identity_id = " + identity;
				}
				else
					where = "TRUE";
				
				
				List<Map<String, String>> rows = Database.getInstance().select("SELECT i.identity_id, i.identity_key_public " + 
					"FROM identities i WHERE " + where);
				
				for( Map<String, String> row : rows )
				{
					Certificate c = new Certificate(row.get("identity_key_public"));
					Principal p = c.x509().getSubjectX500Principal();
					if( PrincipalUtil.isOrg(p) )
						p = Crypto.generatePrincipal(PrincipalUtil.extractFromDN("CN", p.getName()), PrincipalUtil.orgName(p));
					else
						p = Crypto.generatePrincipal(PrincipalUtil.extractFromDN("CN", p.getName()), null);
					
					
					// generate a new certificate with the new CA
					String cert = Crypto.toString(Crypto.generateCertificate(
						p, 
						c.x509().getPublicKey(),
						IdentityStore.getInstance().getAuthority().getSubjectPrincipal(),
						IdentityStore.getInstance().getAuthority().getPrivateKey(),
						IdentityStore.getInstance().getAuthority().getPublicKey(),
						c.x509().getNotBefore(),
						c.x509().getNotAfter()));
					
					Database.getInstance().update("UPDATE identities SET identity_key_public = '" + cert + "', identity_principal = '" + Security.escape(p.getName()) + "' WHERE identity_id = " + row.get("identity_id"));
				}
				
				return "OK";
			}
		};
		
		regen.addMapping(new String[] { "regen", "regenerate" });
		regen.description = "Regenerate user certificates.";
		regen.returnDescription = "OK";
		regen.addGrant(new String[] { Config.gets("com.busit.rest.admin.grant") });
		
		Parameter identity = new Parameter();
		identity.isOptional = true;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity.description = "The identity name, principal or id to regenerate";
		identity.addAlias(new String[]{ "identity", "identity_name", "identity_id", "principal", "identity_principal"});
		regen.addParameter(identity);
		
		index.addOwnHandler(regen);
	}
}