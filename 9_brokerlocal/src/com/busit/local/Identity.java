package com.busit.local;

import com.busit.*;
import com.busit.security.*;
import java.util.*;
import java.security.Principal;

public class Identity implements IIdentity
{
	public String getIssuerName() { return "Busit"; }
	public Principal getIssuerPrincipal() { return PrincipalUtil.parse("CN=Busit,O=Busit"); }
	
	public String getSubjectName() { return "Busit Unchecked"; }
	public Principal getSubjectPrincipal() { return PrincipalUtil.parse("CN=Busit Unchecked,OU=Busit Unchecked,O=Busit"); }
	
	public Date validFrom() { return new Date(); }
	public Date validTo() { return new Date(System.currentTimeMillis() + (1000*60*60)); }
	
	public boolean isAuthentic() { return false; }
	public boolean equals(Object o) { return false; }
}