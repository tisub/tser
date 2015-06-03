package com.busit.broker;

import java.util.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.core.*;

public class SecurityImpl implements ISecurity
{
	public boolean isAuthenticated()
	{
		return true;
	}
	
	public String getUser()
	{
		return "BROKER";
	}
	
	public String getCredentials()
	{
		return "";
	}
	
	public long userId()
	{
		return 42;
	}
	
	public long userId(String user)
	{
		if( user == null || !user.equals(getUser()) )
			return -1;
		else
			return userId();
	}
	
	public boolean hasGrant(String grant)
	{
		return true;
	}
	
	public boolean hasGrants(Collection<String> grants)
	{
		return true;
	}
	
	public boolean hasGrant(String grant, String credentials)
	{
		return true;
	}
	
	public boolean hasGrants(Collection<String> grants, String credentials)
	{
		return true;
	}
	
	public boolean hasGrant(String grant, String user, String pass)
	{
		return true;
	}
	
	public boolean hasGrants(Collection<String> grants, String user, String pass)
	{
		return true;
	}
	
	public String hash(String text)
	{
		throw new UnsupportedOperationException();
	}
}