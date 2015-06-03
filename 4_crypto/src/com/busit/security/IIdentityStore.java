package com.busit.security;

import java.security.Principal;

public interface IIdentityStore
{
	Identity getAuthority();
	Identity getAuthentic();
	Identity getUnchecked();
	
	Identity getIdentity(String anything);
	Identity getIdentity(Principal p);
}