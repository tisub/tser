package com.anotherservice.rest;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;

public class Generator
{
	private static final int ERROR_CLASS_ID = 0xC000;
	
	public static final byte INSERT = 1;
	public static final byte UPDATE = 2;
	public static final byte DELETE = 4;
	public static final byte SELECT = 8;
	
	static class GeneratedAction extends Action
	{
		private Table table = null;
		private byte mode = 0;
		
		public GeneratedAction(Table t, byte m)
		{
			super();
			
			this.table = t;
			this.mode = m;
			
			switch(mode)
			{
				case INSERT: 
					this.description = "Standard INSERT mapping for " + table.name;
					this.returnDescription = "The newly inserted ID if applicable";
					this.addMapping(new String[] { "insert", "add" });
					this.addGrant(new String[] { "access", table.name + "_insert" });
					break;
				case UPDATE: 
					this.description = "Standard UPDATE mapping for " + table.name;
					this.returnDescription = "OK";
					this.addMapping(new String[] { "update", "modify" });
					this.addGrant(new String[] { "access", table.name + "_update" });
					break;
				case DELETE: 
					this.description = "Standard DELETE mapping for " + table.name;
					this.returnDescription = "OK";
					this.addMapping(new String[] { "delete", "remove" });
					this.addGrant(new String[] { "access", table.name + "_delete" });
					break;
				case SELECT: 
					this.description = "Standard SELECT mapping for " + table.name;
					this.returnDescription = "All rows matching the input conditions";
					this.addMapping(new String[] { "select", "list" });
					this.addGrant(new String[] { "access", table.name + "_select" });
					break;
			}
			
			for( Column c : table )
			{
				Parameter p = new Parameter();
				p.minLength = 0;
				p.maxLength = (int)c.length;
				p.description = "Field " + c.name;
				p.addAlias(new String[]{ c.name });
				
				switch(mode)
				{
					case INSERT: 
						p.isOptional = c.acceptsNull();
						if( c.autoIncrement )
							p.description += ". [AutoIncrement]";
						if( c.defaultValue != null )
							p.description += ". [Default: " + c.defaultValue + "]";
						if( !p.isOptional && table.primary != null && table.primary.columns.contains(c) )
							p.description += ". CAUTION: if your primary key is a numeric auto generated value, you SHOULD set it to the proper value (usually 0).";
						break;
					case UPDATE: p.isOptional = !table.isSoleIdentifier(c); break;
					case DELETE: p.isOptional = !table.isSoleIdentifier(c); break;
					case SELECT: p.isOptional = true; break;
				}
				
				switch(c.type)
				{
					case Column.NUMERIC: p.mustMatch = "^-?[0-9]+((\\.|,)[0-9]+)?$"; break;
					case Column.TEXT: p.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL); break;
					case Column.BINARY: break; // no restriction on binary
					case Column.BOOL: p.mustMatch = "^(?i)(yes|true|1|no|false|0)$"; break;
					case Column.DATE: p.mustMatch = "^(\\d{2,4}([:_/-]\\d{1,2}|\\d{2})([:_/-]\\d{1,2}|\\d{2}) )?\\d{2,4}([:_/-]\\d{1,2}|\\d{2})([:_/-]\\d{1,2}|\\d{2})(\\.\\d{1,6})?$"; break;
				}
				
				this.addParameter(p);
			}
		}
		
		public Object execute() throws Exception
		{
			try
			{
				Row row = new Row();
				for( Parameter p : this.getParameters() )
				{
					String v = p.getValue();
					Column c = table.column(p.getAlias());
					if( v == null || c == null ) continue;
					row.put(c, v);
				}
				
				switch(mode)
				{
					case INSERT: return table.insert(row);
					case UPDATE: table.update(row); return "OK";
					case DELETE: table.delete(row); return "OK";
					case SELECT: return table.select(row);
				}
				
				throw new IllegalStateException("Theoritically unreachable statement reached");
			}
			catch(Exception e)
			{
				if( e instanceof RestException )
					throw e;
				else
					throw new RestException(e.getMessage(), RestException.CAUSE_RUNTIME|ERROR_CLASS_ID|0x1, Any.empty().pipe("table", table.name).pipe("mode", mode), e);
			}
		}
	}
	
	public static void generate(Schema schema) { generate(schema, (byte)(INSERT|UPDATE|DELETE|SELECT)); }
	
	public static void generate(Schema schema, byte mode)
	{
		Index index = new Index();
		index.addMapping(new String[] { schema.name });
		Handler.addHandler("/", index);
		
		generate(schema, mode, index);
	}
	
	public static void generate(Schema schema, Index attach) { generate(schema, (byte)(INSERT|UPDATE|DELETE|SELECT), attach); }
	
	public static void generate(Schema schema, byte mode, Index attach)
	{
		for( Table t : schema )
		{
			Index index = new Index();
			index.addMapping(new String[] { t.name });
			attach.addOwnHandler(index);
		
			generate(t, mode, index);
		}
	}
	
	public static void generate(Table table) { generate(table, (byte)(INSERT|UPDATE|DELETE|SELECT)); }
	
	public static void generate(Table table, byte mode)
	{
		Index index = new Index();
		index.addMapping(new String[] { "schema" });
		Handler.addHandler("/", index);
		
		generate(table, mode, index);
	}
	
	public static void generate(Table table, Index attach) { generate(table, (byte)(INSERT|UPDATE|DELETE|SELECT), attach); }
	
	public static void generate(Table table, byte mode, Index attach)
	{
		if( (mode & INSERT) > 0 )
			attach.addOwnHandler(generateAction(table, INSERT));
		if( (mode & UPDATE) > 0 )
			attach.addOwnHandler(generateAction(table, UPDATE));
		if( (mode & DELETE) > 0 )
			attach.addOwnHandler(generateAction(table, DELETE));
		if( (mode & SELECT) > 0 )
			attach.addOwnHandler(generateAction(table, SELECT));
	}
	
	public static Action generateAction(Table table, byte mode)
	{
		return new GeneratedAction(table, mode);
	}
}