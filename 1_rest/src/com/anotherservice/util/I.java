package com.anotherservice.util;

import java.util.*;
import java.lang.*;

/**
 * <p>This class is a very short shortcut for using <em>foreach</em> loops with stuff that don't support it natively.
 * Of course, this is a lazy shortcut because official alternatives exists with regular <em>for</em> or <em>while</em> loops.
 * </p><p><br />
 * This works :<br />
 * <pre>
 * Iterable&lt;int&gt; items = new ArrayList&lt;int&gt;();
 * for( int i : items )
 * {
 * 	// do stuff
 * }
 * </pre>
 * <br />But it doesnt work for Enumerations like <code>Hashtable.keys()</code> for instance, or for any <code>Object[]</code> neither...
 * So you can use the following way :<br />
 * <pre>
 * Hashtable&lt;int, Object&gt; hash = new Hashtable&lt;int, Object&gt;();
 * for( int i : I.iterable(hash.keys()) )
 * {
 * 	// do stuff
 * }
 * 
 * int[] items = new int[]{ 42 };
 * for( int i : I.iterable(items) )
 * {
 * 	// do stuff
 * }
 * </pre>
 * </p>
 *
 * @author  Simon Uyttendaele
 */
public class I
{
	private I() {}
	
	/**
	 * Transforms an <em>Enumeration</em> into an <em>Iterable</em> object 
	 * @throws	UnsupportedOperationException	if <code>remove()</code> method of the <code>Iterator</code> is called
	 * @param	e	the target enumeration
	 * @return	a read-only <em>Iterable</em> representation of the provided <em>Enumeration</em>
	 */
	public static final <T> Iterable<T> iterable(final Enumeration<T> e)
	{
		return new Iterable<T>()
		{
			public final Iterator<T> iterator()
			{
				return new Iterator<T>()
				{
					public final boolean hasNext()
					{
						return e.hasMoreElements();
					}
					
					public final T next()
					{
						return e.nextElement();
					}
									
					public final void remove()
					{
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
	
	/**
	 * Transforms an <em>Array</em> into an <em>Iterable</em> object 
	 * @throws	UnsupportedOperationException	if <code>remove()</code> method of the <code>Iterator</code> is called
	 * @param	array	the target object array
	 * @return	a read-only <em>Iterable</em> representation of the provided <em>Array</em>
	 */
	public static final <T> Iterable<T> iterable(final T[] array)
	{
		return new Iterable<T>()
		{
			public final Iterator<T> iterator()
			{
				return new Iterator<T>()
				{
					private int counter = 0;
					
					public final boolean hasNext()
					{
						return array.length > counter;
					}
					
					public final T next()
					{
						return array[counter++];
					}
									
					public final void remove()
					{
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
}