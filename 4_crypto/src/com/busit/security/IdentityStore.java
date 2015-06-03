package com.busit.security;

public class IdentityStore
{
	private IdentityStore() { }
	private static IIdentityStore implementation;
	/**
	 * Returns the <code>IIdentityStore</code> instance
	 * @return	the unique <code>IIdentityStore</code> instance
	 * @throws	RuntimeException	if the instance has not been set
	 */
	public synchronized static IIdentityStore getInstance()
	{
		if( implementation == null )
			throw new RuntimeException("Identity store implementation not defined");
		
		return implementation;
	}
	
	/**
	 * Sets the <code>IIdentityStore</code> instance
	 * @param	instance	the unique <code>IIdentityStore</code> instance
	 * @throws	RuntimeException	if the instance has already been set
	 */
	public synchronized static void setInstance(IIdentityStore instance)
	{
		if( implementation != null )
			throw new RuntimeException("Identity store implementation is already defined");
		
		implementation = instance;
	}
}