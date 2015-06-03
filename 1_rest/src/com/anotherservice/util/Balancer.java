package com.anotherservice.util;

import java.util.*;
import java.lang.reflect.Method;

public class Balancer<T extends Cloneable>
{
	private LinkedList<T> pool;
	private int pointer = 0;
	
	public static <U extends Cloneable> Balancer<U> init(U template, int quantity) throws Exception
	{
		if( quantity == 0 || template == null )
			throw new Exception("Invalid balancer initialization parameters");
			
		Balancer<U> b = new Balancer<U>();
		
		Method clone = template.getClass().getMethod("clone"); 

		b.add(template);
		for( int i = 1; i < quantity; i++ )
			b.add((U) clone.invoke(template));

		return b;
	}
	
	public Balancer()
	{
		this.pool = new LinkedList<T>();
	}
	
	public synchronized void add(T element)
	{
		pool.add(element);
	}
	
	public synchronized void remove(T element)
	{
		pool.remove(element);
	}
	
	public synchronized T next()
	{
		if( pool.size() == 0 )
			return null;

		pointer++;
		if( pointer >= pool.size() )
			pointer = 0;
		
		return pool.get(pointer);
	}
}