package com.anotherservice.rest.security;

import com.anotherservice.rest.*;

public class Quota
{
	private static final int ERROR_CLASS_ID = 0x2000;
	
	private Quota() { }
	private static IQuota implementation;
	public synchronized static IQuota getInstance()
	{
		if( implementation == null )
			throw new RestException("Quota implementation not defined", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_CONFIG | ERROR_CLASS_ID | 0x1);
		
		return implementation;
	}
	
	public synchronized static void setInstance(IQuota instance)
	{
		if( implementation != null )
			throw new RestException("Quota implementation is already defined", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_CONFIG | ERROR_CLASS_ID | 0x1);
		
		implementation = instance;
	}
}