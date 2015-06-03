package com.anotherservice.util;

import java.lang.*;
import java.util.*;

public class StackTrace
{
	public static void removeFirst(Throwable e)
	{
		removeFirst(e, 1);
	}
	
	public static void removeFirst(Throwable e, int amount)
	{
		if( e == null || amount <= 0 ) return;
		StackTraceElement[] s = e.getStackTrace();
		if( s == null || s.length == 0 ) return;
		if( s.length <= amount )
		{
			e.setStackTrace(new StackTraceElement[0]);
			return;
		}
		
		LinkedList<StackTraceElement> ns = new LinkedList<StackTraceElement>(Arrays.<StackTraceElement>asList(s));
		for( ; amount > 0; amount-- )
			ns.pollFirst();
		e.setStackTrace(ns.toArray(new StackTraceElement[0]));
		return;
	}
	
	public static void removeLast(Throwable e)
	{
		removeLast(e, 1);
	}
	
	public static void removeLast(Throwable e, int amount)
	{
		if( e == null || amount <= 0 ) return;
		StackTraceElement[] s = e.getStackTrace();
		if( s == null || s.length == 0 ) return;
		if( s.length <= amount )
		{
			e.setStackTrace(new StackTraceElement[0]);
			return;
		}
		
		LinkedList<StackTraceElement> ns = new LinkedList<StackTraceElement>(Arrays.<StackTraceElement>asList(s));
		for( ; amount > 0; amount-- )
			ns.pollLast();
		e.setStackTrace(ns.toArray(new StackTraceElement[0]));
		return;
	}
	
	public static void removeLastUntil(Throwable e, String className)
	{
		removeLastUntil(e, className, null, false);
	}
	
	public static void removeLastUntil(Throwable e, String className, boolean included)
	{
		removeLastUntil(e, className, null, included);
	}
	
	public static void removeLastUntil(Throwable e, String className, String methodName)
	{
		removeLastUntil(e, className, methodName, false);
	}
	
	public static void removeLastUntil(Throwable e, String className, String methodName, boolean included)
	{
		if( e == null ) return;
		StackTraceElement[] s = e.getStackTrace();
		if( s == null || s.length == 0 ) return;
		if( className != null && className.length() == 0 ) className = null;
		if( methodName != null && methodName.length() == 0 ) methodName = null;
		if( className == null && methodName == null ) return;
		
		LinkedList<StackTraceElement> ns = new LinkedList<StackTraceElement>(Arrays.<StackTraceElement>asList(s));
		for( StackTraceElement i = ns.pollLast(); i != null; i = ns.pollLast() )
		{
			if( (className == null || className.equalsIgnoreCase(i.getClassName())) &&
				(methodName == null || methodName.equalsIgnoreCase(i.getMethodName())) )
			{
				if( !included )
					ns.addLast(i);
				break;
			}
		}
		
		e.setStackTrace(ns.toArray(new StackTraceElement[0]));
		return;
	}
	
	public static void removeFirstUntil(Throwable e, String className)
	{
		removeFirstUntil(e, className, null, true);
	}
	
	public static void removeFirstUntil(Throwable e, String className, boolean included)
	{
		removeFirstUntil(e, className, null, included);
	}
	
	public static void removeFirstUntil(Throwable e, String className, String methodName)
	{
		removeFirstUntil(e, className, methodName, true);
	}
	
	public static void removeFirstUntil(Throwable e, String className, String methodName, boolean included)
	{
		if( e == null ) return;
		StackTraceElement[] s = e.getStackTrace();
		if( s == null || s.length == 0 ) return;
		if( className != null && className.length() == 0 ) className = null;
		if( methodName != null && methodName.length() == 0 ) methodName = null;
		if( className == null && methodName == null ) return;
		
		LinkedList<StackTraceElement> ns = new LinkedList<StackTraceElement>(Arrays.<StackTraceElement>asList(s));
		for( StackTraceElement i = ns.pollFirst(); i != null; i = ns.pollFirst() )
		{
			if( (className == null || className.equalsIgnoreCase(i.getClassName())) &&
				(methodName == null || methodName.equalsIgnoreCase(i.getMethodName())) )
			{
				if( !included )
					ns.addFirst(i);
				break;
			}
		}
		
		e.setStackTrace(ns.toArray(new StackTraceElement[0]));
		return;
	}

	public static void keepLastUntil(Throwable e, String className)
	{
		keepLastUntil(e, className, null, true);
	}
	
	public static void keepLastUntil(Throwable e, String className, boolean included)
	{
		keepLastUntil(e, className, null, included);
	}
	
	public static void keepLastUntil(Throwable e, String className, String methodName)
	{
		keepLastUntil(e, className, methodName, true);
	}
	
	public static void keepLastUntil(Throwable e, String className, String methodName, boolean included)
	{
		if( e == null ) return;
		StackTraceElement[] s = e.getStackTrace();
		if( s == null || s.length == 0 ) return;
		if( className != null && className.length() == 0 ) className = null;
		if( methodName != null && methodName.length() == 0 ) methodName = null;
		if( className == null && methodName == null ) return;
		
		LinkedList<StackTraceElement> ns = new LinkedList<StackTraceElement>(Arrays.<StackTraceElement>asList(s));
		for( int i = ns.size() - 1; i >= 0; i-- )
		{
			StackTraceElement ste = ns.get(i);
			if( (className == null || className.equalsIgnoreCase(ste.getClassName())) &&
				(methodName == null || methodName.equalsIgnoreCase(ste.getMethodName())) )
			{
				for( int j = 0; j < (included ? i-1 : i); j++ )
					ns.pollFirst();
				break;
			}
		}
		
		e.setStackTrace(ns.toArray(new StackTraceElement[0]));
		return;
	}
	
	public static void keepFirstUntil(Throwable e, String className)
	{
		keepFirstUntil(e, className, null, false);
	}
	
	public static void keepFirstUntil(Throwable e, String className, boolean included)
	{
		keepFirstUntil(e, className, null, included);
	}
	
	public static void keepFirstUntil(Throwable e, String className, String methodName)
	{
		keepFirstUntil(e, className, methodName, false);
	}
	
	public static void keepFirstUntil(Throwable e, String className, String methodName, boolean included)
	{
		if( e == null ) return;
		StackTraceElement[] s = e.getStackTrace();
		if( s == null || s.length == 0 ) return;
		if( className != null && className.length() == 0 ) className = null;
		if( methodName != null && methodName.length() == 0 ) methodName = null;
		if( className == null && methodName == null ) return;
		
		LinkedList<StackTraceElement> ns = new LinkedList<StackTraceElement>(Arrays.<StackTraceElement>asList(s));
		for( int i = 0; i < ns.size(); i++ )
		{
			StackTraceElement ste = ns.get(i);
			if( (className == null || className.equalsIgnoreCase(ste.getClassName())) &&
				(methodName == null || methodName.equalsIgnoreCase(ste.getMethodName())) )
			{
				for( int j = 0; j < (included ? ns.size()-i-1 : ns.size()-i); j++ )
					ns.pollFirst();
				break;
			}
		}
		
		e.setStackTrace(ns.toArray(new StackTraceElement[0]));
		return;
	}
	
	public static void collapse(Throwable e)
	{
		if( e == null ) return;
		StackTraceElement[] s = e.getStackTrace();
		if( s == null || s.length == 0 ) return;
		LinkedList<StackTraceElement> ns = new LinkedList<StackTraceElement>(Arrays.<StackTraceElement>asList(s));
		
		for( Throwable cause = e.getCause(); cause != null; cause = cause.getCause() )
		{
			StackTraceElement[] s = e.getStackTrace();
			if( s == null || s.length == 0 ) continue;
			
			for( int i = 0; i < s.length; i++ )
				ns.addFirst(s[i]);
		}
		
		e.setStackTrace(ns.toArray(new StackTraceElement[0]));
		return;
	}
}