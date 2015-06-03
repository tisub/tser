package com.busit;

/**
 * This class can be used to get object instances for a set of interfaces.
 */
public final class Factory
{
	private static IFactory _instance = null;
	
	/**
	 * Set the IFactory instance
	 * @param	i	the instance
	 * @throws IllegalStateException	if trying to override a previousely set instance. 
	 */
	public static void instance(IFactory i)
	{
		if( _instance != null )
			throw new IllegalStateException("Instance is already defined and cannot be overridden");
		_instance = i;
	}
	
	/**
	 * Get the IFactory instance
	 * @return	the instance
	 * @throws IllegalStateException	if an instance has not been set.
	 */
	public static IFactory instance()
	{
		if( _instance == null )
			throw new IllegalStateException("Instance is not yet defined");
		return _instance;
	}
	
	/**
	 * This method is a shorthand for {@link #instance()}.{@link com.busit.IFactory#content(int)}.
	 */
	public static IContent content(int id) { return instance().content(id); }
	
	/**
	 * This method is a shorthand for {@link #instance()}.{@link com.busit.IFactory#message()}.
	 */
	public static IMessage message() { return instance().message(); }
	
	/**
	 * This method is a shorthand for {@link #instance()}.{@link com.busit.IFactory#messageList()}.
	 */
	public static IMessageList messageList() { return instance().messageList(); }
}