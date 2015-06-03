package com.anotherservice.db;
import java.util.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.security.*;

public class Table implements Iterable<Column>
{
	public static Table discover(Schema schema, String name) throws Exception
	{
		Any a = Any.wrap(schema.db.select("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + 
			Security.escape(schema.name) + "' AND TABLE_NAME = '" + 
			Security.escape(name) + "'"));
		
		if( a.size() == 0 )
			return null;
		
		Table t = new Table();
		t.schema = schema;
		t.parse(a);
		t.discoverConstraints();
		
		return t;
	}
	void parse(Any a)
	{
		this.name = a.get(0).<String>value("TABLE_NAME");
		
		for( Any c : a )
		{
			Column col = new Column();
			col.table = this;
			col.parse(c);
			this.columns.add(col);
		}
	}
	void discoverConstraints() throws Exception
	{
		Any a = Any.wrap(schema.db.select("SELECT k.*, r.UPDATE_RULE, r.DELETE_RULE FROM KEY_COLUMN_USAGE k " + 
			"LEFT JOIN REFERENTIAL_CONSTRAINTS r ON(r.TABLE_NAME = k.TABLE_NAME AND r.CONSTAINT_SCHEMA = k.TABLE_SCHEMA) " + 
			"WHERE k.TABLE_SCHEMA = '" + 
			Security.escape(schema.name) + "' AND k.TABLE_NAME = '" + 
			Security.escape(name) + "'"));
		
		parseConstraints(a);
	}
	void parseConstraints(Any a)
	{
		for( Any c : a )
		{
			if( c.<String>value("CONSTRAINT_NAME").equalsIgnoreCase("PRIMARY") )
			{
				if( this.primary == null ) // primary key
				{
					this.primary = new PrimaryKey();
					this.primary.name = c.<String>value("CONSTRAINT_NAME");
				}
				this.primary.columns.add(this.column(c.<String>value("COLUMN_NAME")));
			}
			else if( c.isNull("REFERENCED_TABLE_SCHEMA") ) // unique key
			{
				UniqueKey u = this.unique(c.<String>value("CONSTRAINT_NAME"));
				if( u == null )
				{
					u = new UniqueKey();
					u.name = c.<String>value("CONSTRAINT_NAME");
					this.uniques.add(u);
				}
				
				u.columns.add(this.column(c.<String>value("COLUMN_NAME")));
			}
			else // foreign key
			{
				Relation r = this.relation(c.<String>value("CONSTRAINT_NAME"));
				if( r == null )
				{
					r = new Relation();
					r.name = c.<String>value("CONSTRAINT_NAME");
					this.relations.add(r);
				}
				
				r.columns.put(
					this.column(c.<String>value("COLUMN_NAME")),
					this.schema.column(c.<String>value("REFERENCED_TABLE_NAME"), c.<String>value("REFERENCED_COLUMN_NAME"))
				);
				
				switch(c.<String>value("UPDATE_RULE").toUpperCase())
				{
					case "CASCADE": r.onUpdate = Relation.CASCADE; break;
					case "RESTRICT": case "NO ACTION": r.onUpdate = Relation.RESTRICT; break;
					case "SET DEFAULT": case "SET NULL": r.onUpdate = Relation.SETNULL; break;
				}
				
				switch(c.<String>value("DELETE_RULE").toUpperCase())
				{
					case "CASCADE": r.onDelete = Relation.CASCADE; break;
					case "RESTRICT": case "NO ACTION": r.onDelete = Relation.RESTRICT; break;
					case "SET DEFAULT": case "SET NULL": r.onDelete = Relation.SETNULL; break;
				}
			}
		}
	}
	
	public Schema schema;
	
	public String name;
	public List<Column> columns = new Vector<Column>();
	public Column column(String name) { for( Column c : columns ) if( c.name.equalsIgnoreCase(name) ) return c; return null; }
	
	public PrimaryKey primary = null;
	public List<UniqueKey> uniques = new Vector<UniqueKey>();
	public UniqueKey unique(String name) { for( UniqueKey u : uniques ) if( u.name.equalsIgnoreCase(name) ) return u; return null; }
	public List<Relation> relations = new Vector<Relation>();
	public Relation relation(String name) { for( Relation r : relations ) if( r.name.equalsIgnoreCase(name) ) return r; return null; }
	
	public boolean isUnique(Column c)
	{
		for( UniqueKey u : uniques )
			if( u.columns.contains(c) )
				return true;
		return false;
	}
	
	public boolean isSoleIdentifier(Column c)
	{
		if( uniques.size() == 1 && uniques.get(0).columns.contains(c) )
			return true;
		else if( uniques.size() == 0 && primary != null && primary.columns.contains(c) )
			return true;
		else
			return false;
	}
	
	public List<Trigger> beforeUpdate = new Vector<Trigger>();
	public List<Trigger> afterUpdate = new Vector<Trigger>();
	public List<Trigger> beforeInsert = new Vector<Trigger>();
	public List<Trigger> afterInsert = new Vector<Trigger>();
	public List<Trigger> beforeDelete = new Vector<Trigger>();
	public List<Trigger> afterDelete = new Vector<Trigger>();
	
	public List<Trigger> beforeSelect = new Vector<Trigger>();
	
	public Iterator<Column> iterator() { return columns.iterator(); }
	
	public long insert(Row row) throws Exception
	{
		for( Trigger t : beforeInsert )
			t.apply(this, row);
	
		String sql = "INSERT INTO " + this.name;
		StringBuffer cols = new StringBuffer(" (");
		StringBuffer vals = new StringBuffer(" VALUES (");
		for( Map.Entry<Column, String> value : row.entrySet() )
		{
			cols.append(value.getKey() + ",");
			cols.append(value.getKey().format(value.getValue()) + ",");
		}
		if( cols.length() > 0 )
			cols.deleteCharAt(cols.length()-1);
		if( vals.length() > 0 )
			vals.deleteCharAt(vals.length()-1);
		
		Long id = schema.db.insert(sql + cols + ")" + vals + ")");
		if( id != null && primary != null && !primary.isComposite() )
			row.put(primary.first(), id.toString());
	
		for( Trigger t : afterInsert )
			t.apply(this, row);
		
		return id;
	}
	
	public void update(Row row) throws Exception
	{
		List<Column> id = getIdentifier(row);
		if( row.size() == id.size() )
			return; // nothing to update if we only have the id columns !
		
		for( Trigger t : beforeUpdate )
			t.apply(this, row);
	
		StringBuffer sql = new StringBuffer("UPDATE " + this.name + " SET ");
		for( Map.Entry<Column, String> value : row.entrySet() )
			if( !id.contains(value.getKey()) )
				sql.append(value.getKey() + " = " + value.getKey().format(value.getValue()) + ",");
		sql.deleteCharAt(sql.length()-1);
		
		StringBuffer where = new StringBuffer(" WHERE 1=1");
		for( Column c : id )
			where.append(" AND " + c + " = " + c.format(row.get(c)));
		
		schema.db.update(sql + " " + where);
		
		for( Trigger t : afterUpdate )
			t.apply(this, row);
	}
	
	public void delete(Row row) throws Exception
	{
		// this is a choice : we force to have the id columns. There can be more for the where clause, but we require at least the identifiable
		List<Column> id = getIdentifier(row);
		
		for( Trigger t : beforeDelete )
			t.apply(this, row);
	
		StringBuffer sql = new StringBuffer("DELETE FROM " + this.name + " WHERE 1=1");
		for( Map.Entry<Column, String> value : row.entrySet() )
			sql.append(" AND " + value.getKey() + " = " + value.getKey().format(value.getValue()));
		
		schema.db.delete(sql.toString());
		
		for( Trigger t : afterDelete )
			t.apply(this, row);
	}
	
	public Any select(Row row) throws Exception
	{
		for( Trigger t : beforeSelect )
			t.apply(this, row);
	
		StringBuffer sql = new StringBuffer("SELECT * FROM " + this.name + " WHERE 1=1");
		for( Map.Entry<Column, String> value : row.entrySet() )
			sql.append(" AND " + value.getKey() + " = " + value.getKey().format(value.getValue()));
		
		return Any.wrap(schema.db.select(sql.toString()));
	}
	
	private List<Column> getIdentifier(Row row)
	{
		// do we have sufficient info to know which row to update : primary key OR unique key
		List<Column> identifier = null;
		if( primary != null )
		{
			boolean all = true;
			for( Column c : primary.columns )
			{
				if( !row.containsKey(c) )
				{
					all = false;
					break;
				}
			}
			if( all )
				identifier = primary.columns;
		}
		if( identifier == null )
		{
			for( UniqueKey u : uniques )
			{
				boolean all = true;
				for( Column c : u.columns )
				{
					if( !row.containsKey(c) )
					{
						all = false;
						break;
					}
				}
				if( all )
				{
					identifier = u.columns;
					break;
				}
			}
			if( identifier == null )
				throw new IllegalArgumentException("Row cannot be unambiguously identified based upon provided columns");
		}
		
		return identifier;
	}
}