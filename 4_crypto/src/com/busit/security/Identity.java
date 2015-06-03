package com.busit.security;

import com.busit.*;
import java.security.*;
import java.util.*;
import com.anotherservice.util.*;

public class Identity implements IIdentity
{
	private PrivateKey key = null;
	private Certificate certificate = null;
	
	public Identity(Certificate certificate)
	{
		this.certificate = certificate;
	}
	
	public Identity(String certificate) throws Exception
	{
		this(Crypto.parseCertificate(certificate));
	}
	
	public Identity(PrivateKey key, Certificate certificate)
	{
		this.key = key;
		this.certificate = certificate;
	}
	
	public Identity(String key, String certificate) throws Exception
	{
		this(Crypto.parsePrivateKey(key), Crypto.parseCertificate(certificate));
	}
	
	public String certificate()
	{
		return getCertificate().toString();
	}
	
	public Certificate getCertificate()
	{
		return this.certificate;
	}
	
	public PublicKey getPublicKey()
	{
		return this.certificate.x509().getPublicKey();
	}
	
	public PrivateKey getPrivateKey()
	{
		return this.key;
	}
	
	public Identity getIssuer()
	{
		if( this.certificate.x509().getIssuerX500Principal().equals(this.certificate.x509().getSubjectX500Principal()) )
			return null;

		return IdentityStore.getInstance().getIdentity(this.getIssuerPrincipal());
	}
	
	public String getIssuerName()
	{
		if( this.certificate.x509().getIssuerX500Principal().equals(this.certificate.x509().getSubjectX500Principal()) )
			return null;
			
		return PrincipalUtil.shortName(this.certificate.x509().getIssuerX500Principal());
	}
	
	public Principal getIssuerPrincipal()
	{
		if( this.certificate.x509().getIssuerX500Principal().equals(this.certificate.x509().getSubjectX500Principal()) )
			return null;

		return this.certificate.x509().getIssuerX500Principal();
	}
	
	public String getName()
	{
		return getSubjectName();
	}
	
	public String toString()
	{
		return getName();
	}
	
	public String getSubjectName()
	{
		return PrincipalUtil.shortName(this.certificate.x509().getSubjectX500Principal());
	}
	
	public Principal getSubjectPrincipal()
	{
		return this.certificate.x509().getSubjectX500Principal();
	}
	
	public Date validFrom()
	{
		return this.certificate.x509().getNotBefore();
	}
	
	public Date validTo()
	{
		return this.certificate.x509().getNotAfter();
	}
	
	public boolean isUnchecked()
	{
		return PrincipalUtil.equals(getIssuerPrincipal(), IdentityStore.getInstance().getUnchecked().getIssuerPrincipal());
	}
	
	public boolean isAuthentic()
	{
		return (PrincipalUtil.equals(getIssuerPrincipal(), IdentityStore.getInstance().getAuthentic().getIssuerPrincipal()) || isAuthority());
	}
	
	public boolean isAuthority()
	{
		return PrincipalUtil.equals(getIssuerPrincipal(), IdentityStore.getInstance().getAuthority().getIssuerPrincipal());
	}
	
	public boolean isLocal()
	{
		return PrincipalUtil.isLocal(this.getSubjectPrincipal());
	}
	
	public boolean isRemote()
	{
		return !isLocal();
	}

	public boolean equals(Object o)
	{
		try
		{
			if( o == null )
				return false;
			if( o instanceof Identity )
				return PrincipalUtil.equals(this.getSubjectPrincipal(), ((Identity)o).getSubjectPrincipal());
			if( o instanceof Principal )
				return PrincipalUtil.equals(this.getSubjectPrincipal(), (Principal)o);
			if( o instanceof String )
			{
				if( PrincipalUtil.isPrincipal((String)o) )
					return PrincipalUtil.equals(this.getSubjectPrincipal(), (String)o);
			}
			return false;
		}
		catch(Exception e)
		{
			return false;
		}
	}
}