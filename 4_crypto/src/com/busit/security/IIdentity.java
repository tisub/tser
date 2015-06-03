package com.busit.security;

import java.security.Principal;
import java.util.*;

public interface IIdentity
{
	public String getIssuerName();
	public Principal getIssuerPrincipal();
	
	public String getSubjectName();
	public Principal getSubjectPrincipal();
	
	public String certificate();
	
	public Date validFrom();
	public Date validTo();
	
	public boolean isAuthentic();
	public boolean equals(Object o);
}