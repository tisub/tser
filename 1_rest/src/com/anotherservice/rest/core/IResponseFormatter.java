package com.anotherservice.rest.core;

import com.anotherservice.rest.*;

/**
 * This interface defines the two methods that will format the Handler response to be sent to the client. 
 * <br />You should register your implementation using {@link Responder#register(IResponseFormatter, String)}.
 * If the format method throws an exception, a HTTP 500 error will be sent.
 * @see com.anotherservice.rest.Handler
 * @see com.anotherservice.rest.core.Responder
 *
 * @author  Simon Uyttendaele
 */
public interface IResponseFormatter
{
	public byte[] format(Object o);
	public byte[] format(Throwable t);
	public byte[] format(Handler h);
}