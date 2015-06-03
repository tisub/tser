package com.anotherservice.db;
import java.util.*;

public class Row extends HashMap<Column, String>
{
	public String value(String column)
	{
		for( Map.Entry<Column, String> col : this.entrySet() )
			if( col.getKey().name.equalsIgnoreCase(column) )
				return col.getValue();
		return null;
	}
	
	public void value(String column, String value)
	{
		for( Map.Entry<Column, String> col : this.entrySet() )
		{
			if( col.getKey().name.equalsIgnoreCase(column) )
			{
				col.setValue(value);
				return;
			}
		}
		
		throw new IllegalArgumentException("Invalid column name");
	}
}