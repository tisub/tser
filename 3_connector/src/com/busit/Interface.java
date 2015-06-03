package com.busit;

import com.anotherservice.util.*;

/** This class maps the configuration of a connector interface (input or output) in corresponding fields
 */
public class Interface
{
	/** The interface technical name given by the developper
	 */
	public String key;
	/** The interface value (if any) configured by the user
	 */
	public String value;
	/** The interface friendly name given by the user
	 */
	public String name;
	
	public Interface(String id, Any i)
	{
		name = id;
		key = i.<String>value("key");
		value = i.<String>value("value");
	}
}