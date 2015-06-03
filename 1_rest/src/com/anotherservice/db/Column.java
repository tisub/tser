package com.anotherservice.db;
import java.util.*;
import java.text.*;
import java.io.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.security.*;

public class Column
{
	public static final int NUMERIC = 1;
	public static final int TEXT = 2;
	public static final int BINARY = 3;
	public static final int BOOL = 4;
	public static final int DATE = 5;
	
	public static Column discover(Table table, String name) throws Exception
	{
		Any a = Any.wrap(table.schema.db.select("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + 
			Security.escape(table.schema.name) + "' AND TABLE_NAME = '" + 
			Security.escape(table.name) + "' AND COLUMN_NAME = '" + 
			Security.escape(name) + "'"));
		
		if( a.size() == 0 )
			return null;
		
		Column c = new Column();
		c.table = table;
		c.parse(a);
		return c;
	}
	void parse(Any a)
	{
		this.name = a.<String>value("COLUMN_NAME");
		this.nullable = a.<String>value("IS_NULLABLE").matches("^(?i)(yes|true|1)$");
		this.defaultValue = a.<String>value("COLUMN_DEFAULT");
		this.autoIncrement = a.<String>value("EXTRA").equalsIgnoreCase("auto_increment");
		
		switch(a.<String>value("DATA_TYPE").toUpperCase())
		{
			case "TINYINT": // this is not really true, but lets suppose so
				this.type = BOOL;
				this.length = 1;
				break;
			case "SMALLINT":
			case "MEDIUMINT":
			case "INT":
			case "INTEGER":
			case "BIGINT":
			case "FLOAT":
			case "DOUBLE":
			case "REAL":
			case "DECIMAL":
			case "NUMERIC":
			case "BIT":
				this.type = NUMERIC;
				this.length = Long.parseLong(a.<String>value("NUMERIC_PRECISION"));
				break;
			case "YEAR":
			case "DATE":
			case "TIME":
			case "DATETIME":
			case "TIMESTAMP":
				this.type = DATE;
				this.length = Long.parseLong(a.<String>value("DATETIME_PRECISION"));
				break;
			case "CHAR":
			case "VARCHAR":
			case "TINYTEXT":
			case "MEDIUMTEXT":
			case "TEXT":
			case "LONGTEXT":
				this.type = TEXT;
				this.length = Long.parseLong(a.<String>value("CHARACTER_MAXIMUM_LENGTH"));
				break;
			case "BINARY":
			case "VARBINARY":
			case "TINYBLOB":
			case "MEDIUMBLOB":
			case "BLOB":
			case "LONGBLOB":
				this.type = BINARY;
				this.length = Long.parseLong(a.<String>value("CHARACTER_MAXIMUM_LENGTH"));
				break;
		}
	}
	
	public Table table;
	
	public String name;
	public int type = 0;
	public boolean nullable = true;
	public long length;
	// non-standard
	public String defaultValue = null;
	public boolean autoIncrement = false;
	
	public boolean acceptsNull()
	{
		// caution, this does not say that the column is NULLABLE, it tells whether the columns accepts a NULL value for insert/update
		// taking into consideration the defaut value or auto-increment
		
		if( table.isUnique(this) )
			return false;
		if( !nullable && !autoIncrement && defaultValue == null )
			return false;
		return true;
	}
	
	public String toString()
	{
		return this.name;
	}
	
	public String canonicalName()
	{
		return this.table.name + "." + this.name;
	}
	
	public String format(Object value) throws Exception
	{
		if( value == null )
		{
			if( autoIncrement )
				return (nullable ? "0" : "NULL");
			else if( nullable )
				return "NULL";
			else if( defaultValue != null )
				return defaultValue;
			else
				throw new IllegalArgumentException("Value cannot be null");
		}
		else if( autoIncrement )
			return (nullable ? "0" : "NULL");
		
		switch(type)
		{
			case NUMERIC:
				if( !(value instanceof Number) )
				{
					if( value.toString().matches("^[a-zA-Z_]+\\(\\)$") )
						return value.toString();
					else
						value = Double.parseDouble(value.toString());
				}
				value = value.toString();
				if( ((String)value).length() > (10 * length - 1) )
					throw new IllegalArgumentException("Value length exceeded");
				return value.toString();
			case BOOL:
				if( value instanceof Boolean )
					return ((Boolean)value ? "TRUE" : "FALSE");
				else
					return (value.toString().matches("^(?i)(yes|true|1)$") ? "TRUE" : "FALSE");
			default:
			case TEXT:
			case BINARY:
				if( value instanceof byte[] )
					value = Hex.toString((byte[])value);
				else if( value instanceof InputStream )
					value = Hex.toString((InputStream)value);
				
				value = value.toString();
				if( ((String)value).length() > (10 * length - 1) )
					throw new IllegalArgumentException("Value length exceeded");
				return "'" + Security.escape(value.toString()) + "'";
			case DATE:
				if( value instanceof Number )
				{
					value = "" + ((Number)value).intValue();
					if( ((String)value).length() < 6 )
						value = "000000".substring(((String)value).length()) + value;
				}
				if( value instanceof Date )
					return "'" + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format((Date)value) + "'";
				if( value.toString().matches("^(\\d{2,4}([:_/-]\\d{1,2}|\\d{2})([:_/-]\\d{1,2}|\\d{2}) )?\\d{2,4}([:_/-]\\d{1,2}|\\d{2})([:_/-]\\d{1,2}|\\d{2})(\\.\\d{1,6})?$") )
					return "'" + value.toString() + "'";
				throw new IllegalArgumentException("Cannot convert value to proper date format");
		}
	}
}