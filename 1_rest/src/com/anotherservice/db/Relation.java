package com.anotherservice.db;
import java.util.*;

public class Relation
{
	public static final int RESTRICT = 0;
	public static final int CASCADE = 1;
	public static final int SETNULL = 2;
	
	public String name;
	public Map<Column, Column> columns = new HashMap<Column, Column>();
	
	public int onUpdate = 0;
	public int onDelete = 0;
}