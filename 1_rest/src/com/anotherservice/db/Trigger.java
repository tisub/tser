package com.anotherservice.db;
import java.util.*;

public interface Trigger
{
	public void apply(Table table, Row row);
}