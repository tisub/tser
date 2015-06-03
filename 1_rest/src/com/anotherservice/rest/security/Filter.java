package com.anotherservice.rest.security;

import com.anotherservice.rest.core.*;
import com.anotherservice.rest.*;

/**
 * This class is a Thread safe Singleton getter for a {@link com.anotherservice.rest.security.IFilter} for the REST service
 * The filter instance cannot be changed after setting it.
 *
 * @author  Simon Uyttendaele
 */
public class Filter
{
	private static final int ERROR_CLASS_ID = 0x3000;
	
	private Filter() { }
	private static IFilter implementation;
	/**
	 * Returns the <code>IFilter</code> instance
	 * @return	the unique <code>IFilter</code> instance. If none has been set, a dummy implementation is returned
	 */
	public synchronized static IFilter getInstance()
	{
		if( implementation == null )
			return new IFilter()
			{
				public void preFilter() throws Exception { }
				public Object postFilter(Object result) throws Exception { return result; }
			};
		
		return implementation;
	}
	
	/**
	 * Sets the <code>IFilter</code> instance
	 * @param	instance	the unique <code>IFilter</code> instance
	 * @throws	RestException	if the instance has already been set
	 */
	public synchronized static void setInstance(IFilter instance)
	{
		if( implementation != null )
			throw new RestException("Filter implementation is already defined", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_CONFIG | ERROR_CLASS_ID | 0x1);
		
		implementation = instance;
	}
}