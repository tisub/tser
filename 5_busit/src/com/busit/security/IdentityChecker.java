package com.busit.security;

public class IdentityChecker
{
	private IdentityChecker() { }
	private static IIdentityChecker implementation;
	/**
	 * Returns the <code>IIdentityChecker</code> instance
	 * @return	the unique <code>IIdentityChecker</code> instance
	 * @throws	RuntimeException	if the instance has not been set
	 */
	public synchronized static IIdentityChecker getInstance()
	{
		if( implementation == null )
			throw new RuntimeException("Identity checker implementation not defined");
		
		return implementation;
	}
	
	/**
	 * Sets the <code>IIdentityChecker</code> instance
	 * @param	instance	the unique <code>IIdentityChecker</code> instance
	 * @throws	RuntimeException	if the instance has already been set
	 */
	public synchronized static void setInstance(IIdentityChecker instance)
	{
		if( implementation != null )
			throw new RuntimeException("Identity checker implementation is already defined");
		
		implementation = instance;
	}
}