package com.anotherservice.rest.security;

import com.anotherservice.rest.core.*;
import com.anotherservice.rest.*;

/**
 * This interface defines the most basic operations that the REST service filter should implement. 
 * <br />Only one instance of <code>IFilter</code> is allowed at once for the whole system.
 * <br />This ensures that the filtering mechanism is consolidated across every service deployed.
 * @see com.anotherservice.rest.security.Filter
 *
 * @author  Simon Uyttendaele
 */
public interface IFilter
{
	/**
	 * Filters the current request before the routing process.
	 * @throws Exception to prevent execution of the handler
	 */
	public void preFilter() throws Exception;
	
	/**
	 * Filters the current request after the routing process and allows to potentially alter the result.
	 * @param result the result of the routing process
	 * @return the <em>eventually modified</em> result to send to the client
	 * @throws Exception to send to the client
	 * @note the return value is <strong>mandatory</strong> otherwise a null response will be sent to the client.
	 * If you do not want to alter the response, just return it.
	 */
	public Object postFilter(Object result) throws Exception;
}