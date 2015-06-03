package com.anotherservice.db;
import java.util.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.security.*;

public class Schema implements Iterable<Table>
{
	private static Schema instance = null;
	public static void setInstance(Schema s) { instance = s; }
	public static Schema getInstance() { return instance; }
	
	public static Schema discover(Database db, String name) throws Exception
	{
		Any a = Any.wrap(db.select("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + 
			Security.escape(name) + "'"));
		
		Schema s = new Schema();
		s.db = db;
		if( a.size() == 0 )
			s.name = name;
		else
			s.parse(a);
		return s;
	}
	void parse(Any a)
	{
		this.name = a.get(0).<String>value("TABLE_SCHEMA");
		
		Any t = Any.empty();
		for( Any c : a ) // group columns by table
		{
			Any d = t.get(c.<String>value("TABLE_NAME"));
			if( d == null )
			{
				d = Any.empty();
				t.put(c.<String>value("TABLE_NAME"), d);
			}
			d.add(c);
		}
		
		for( Any v : t.values() )
		{
			Table tab = new Table();
			tab.schema = this;
			tab.parse(v);
			this.tables.add(tab);
		}
	}
	public void discoverConstraints() throws Exception
	{
		Any a = Any.wrap(this.db.select("SELECT k.*, r.UPDATE_RULE, r.DELETE_RULE FROM KEY_COLUMN_USAGE k " + 
			"LEFT JOIN REFERENTIAL_CONSTRAINTS r ON(r.TABLE_NAME = k.TABLE_NAME AND r.CONSTAINT_SCHEMA = k.TABLE_SCHEMA) " + 
			"WHERE k.TABLE_SCHEMA = '" + 
			Security.escape(this.name) + "'"));
		
		parseConstraints(a);
	}
	void parseConstraints(Any a)
	{
		Any t = Any.empty();
		for( Any c : a ) // group columns by table
		{
			Any d = t.get(c.<String>value("TABLE_NAME"));
			if( d == null )
			{
				d = Any.empty();
				t.put(c.<String>value("TABLE_NAME"), d);
			}
			d.add(c);
		}
		
		for( Any v : t.values() )
		{
			Table tab = this.table(v.get(0).<String>value("TABLE_NAME"));
			tab.parseConstraints(v);
		}
	}
	
	public List<Table> tables = new Vector<Table>();
	public Table table(String name) { for( Table t : tables ) if( t.name.equals(name) ) return t; return null; }
	public Column column(String tableName, String columnName) { Table t = table(tableName); if( t == null ) return null; return t.column(columnName); }
	
	public String name;
	
	public Database db;
	
	public void startTransaction() throws Exception
	{
		db.query("START TRANSACTION", Database.Mode.NO_ROW);
	}
	
	public void rollback() throws Exception
	{
		db.query("ROLLBACK", Database.Mode.NO_ROW);
	}
	
	public void commit() throws Exception
	{
		db.query("COMMIT", Database.Mode.NO_ROW);
	}
	
	public Iterator<Table> iterator() { return tables.iterator(); }
}