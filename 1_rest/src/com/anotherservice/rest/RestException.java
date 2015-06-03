package com.anotherservice.rest;

import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.sql.SQLException;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.lang.IndexOutOfBoundsException;
import java.lang.NullPointerException;
import java.lang.SecurityException;
import java.lang.StackTraceElement;

public class RestException extends RuntimeException implements Jsonizable
{
	public static final int NOT_RECOVERABLE =	0x80000000;
	public static final int CAUSE_CONFIG = 		0x40000000;
	public static final int CAUSE_RUNTIME = 	0x20000000;
	public static final int CAUSE_USER = 		0x10000000;
	public static final int DUE_TO_DATABASE = 	0x8000000;
	public static final int DUE_TO_PARAMETER = 	0x4000000;
	public static final int DUE_TO_SECURITY = 	0x2000000;
	public static final int DUE_TO_ROUTING = 	0x1000000;
	
	/**
	 * The code is structured as follows :
	 * (0000 0000) (0000 0000 0000) (0000 0000 0000)
	 * The highest 8 bits give information about the type of error
	 * The next 12 bits determine which class emits the error  (max 4095 values)
	 * The lowest 12 bits are specific to the class (max 4095 values)
	 *
	 * Example: -939511795 = 0xC800300D = (1100 1000) (0000 0000 0011) (0000 0000 1101)
	 * GROUP 1 : This is an error that cannot recover (trying again even with different 
	 * 			 parameters will produce the same error) about the configuration of the database
	 * GROUP 2 : That is triggered by the class ID 0x3
	 * GROUP 3 : With an internal error code of 0xD
	 */
	protected int code = -1;
	protected Any data = Any.empty();
	
	public RestException(String message) { super(message); }
	public RestException(String message, Throwable cause) { super(message, cause); }
	public RestException(String message, int code) { super(message); this.code = code; }
	public RestException(String message, int code, Throwable cause) { super(message, cause); this.code = code; }
	public RestException(String message, int code, Any data) { super(message); this.code = code; this.data = data; }
	public RestException(String message, int code, Any data, Throwable cause) { super(message, cause); this.code = code; this.data = data; }
	public RestException(String message, Any data) { super(message); this.data = data; }
	public RestException(String message, Any data, Throwable cause) { super(message, cause); this.data = data; }
	public RestException(Throwable cause) { super(cause); }
	public RestException(Throwable cause, int code) { super(cause); this.code = code; }
	public RestException(Throwable cause, int code, Any data) { super(cause); this.code = code; this.data = data; }
	public RestException(Throwable cause, Any data) { super(cause); this.data = data; }
	
	public int getCode() { return this.code; }
	public RestException setCode(int code) { this.code = code; return this; }
	
	public Any getData() { return this.data; }
	public RestException setData(Any data) { this.data = data; return this; }
	public RestException addData(Any data) { this.data.addAll(data); return this; }
	public RestException addData(String key, Object value) { this.data.put(key, value); return this; }
	
	public String toString()
	{
		return "Error " + code + " : " + super.toString() + "\nData:\n" + data.toJson();
	}
	
	public String toJson()
	{
		Any a = Any.empty();
		a.put("message", (getMessage() == null ? "null" : getMessage()));
		a.put("code", code);
		a.put("data", data);
		
		if( Logger.instance().level <= 500 /* Level.FINE */ )
		{
			StackTrace.removeLastUntil(this, Handler.class.getCanonicalName()); // do not show tomcat and routing stuff
			StackTrace.keepLastUntil(this, Database.class.getCanonicalName()); // do not show internal database driver stuff
			
			Any t = Any.empty();
			for( StackTraceElement s : getStackTrace() )
			{
				Any line = Any.empty();
				line.put("file", (s.getFileName() == null ? "unknown" : s.getFileName()));
				line.put("line", s.getLineNumber());
				line.put("class", (s.getClassName() == null ? "unknown" : s.getClassName()));
				line.put("method", (s.getMethodName() == null ? "unknown" : s.getMethodName()));
				t.add(line);
			}		
			a.put("trace", t);
		}
		
		return a.toJson();
	}
}