package com.anotherservice.db;
import java.util.*;

public abstract class Key
{
	public String name;
	public List<Column> columns = new Vector<Column>();
	public boolean isComposite() { return columns.size() > 1; }
	public Column first() { if( columns.size() == 0 ) return null; else return columns.get(0); }
}