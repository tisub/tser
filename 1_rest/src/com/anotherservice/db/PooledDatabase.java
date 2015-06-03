package com.anotherservice.db;

import java.lang.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.anotherservice.util.*;
import java.sql.SQLException;

/**
 * This class is a generic {@link com.anotherservice.db.Database} (possibly blocking) pool.
 * It creates new instances when required and cleans-up old instances when idle. 
 * @note	New connections are created using the {@link com.anotherservice.db.Database#clone()} method and are stored as a stack.
 * @author	Simon Uyttendaele
 */
public class PooledDatabase extends Database
{
	/**
	 * Determines if the pool should wait for a instance to be available or throw an exception otherwise.
	 * <br />Default : <code>true</code>
	 * @see	#blockTimeout
	 */
	public boolean blocking = true;
	/**
	 * Maximum number of connections in the pool.
	 * <br />Default : <code>10</code>
	 */
	public int maxActiveConnections = 10;
	/**
	 * Minimum number of connections in the pool.
	 * <br />Default : <code>1</code>
	 */
	public int minActiveConnections = 1;
	/**
	 * The maximum number of milliseconds a thread can be waiting to obtain a connection.
	 * If the time is reached, an <code>IllegalStateException</code> is thrown. If the <code>blockTimeout</code> is <code>0</code> then the thread waits until 
	 * a new connection is returned to the pool.
	 * <br />Default : <code>0</code>
	 */
	public long blockTimeout = 0;
	/**
	 * The checking interval in milliseconds that a blocked thread checks for an available connection.
	 * <br />Default : <code>50</code>
	 */
	public long checkInterval = 50;
	
	private ConcurrentLinkedQueue<Database> pool = new ConcurrentLinkedQueue<Database>();
	private Integer instanceCount = 0;
	private Database template = null;
	private boolean closing = false;
	
	private PooledDatabase() { }
	
	/**
	 * Constructor with the template <code>Database</code> that should be cloned to populate the pool.
	 * @note that the template instance will be closed.
	 */
	public PooledDatabase(Database template)
	{
		this.template = template;
		try { template.close(); } catch(Exception e) {}
	}
	
	/**
	 * Overrides {@link com.anotherservice.db.Database#query(String, Database.Mode)} in order to query the next available connection in the pool.
	 * @param	sql	The SQL request
	 * @param	mode	The query type
	 * @return	The query result
	 * @throws	RuntimeException	if the pool has been closed.
	 * @throws	IllegalStateException	if the individual connection gotten from the pool is no longer alive.
	 * @throws	NoSuchElementException	if a connection could not be retrieved within allowed time (timeout).
	 */
	public Object query(String sql, Database.Mode mode) throws SQLException
	{
		if( closing )
			throw new RuntimeException("The pool is closed");
			
		Database db = this.getDatabase();
		
		try
		{
			return db.query(sql, mode);
		}
		finally
		{
			putDatabase(db);
		}
	}
	
	/**
	 * Cloning this instance is not allowed.
	 * @return	<code>null</code>
	 */
	public Database clone()
	{
		return null;
	}
	
	/**
	 * Overrides {@link com.anotherservice.db.Database#close()} in order to close all instances in the pool.
	 * <br />Active connections that are not back in the pool yet will be closed upon return.
	 * @note	Closing the pool cannot be reverted.
	 */
	public void close() throws SQLException
	{
		closing = true;
		Database db;
		while( (db = pool.poll()) != null )
			db.close();
	}
	
	private void putDatabase(Database db)
	{
		if( closing )
		{
			try { db.close(); } catch(Exception e) {}
			return;
		}
		
		// too many instances ?
		synchronized(instanceCount)
		{
			if( instanceCount > maxActiveConnections )
			{
				Logger.finest("Too many connection. Not returning to pool ["+db.hashCode()+"].");
					
				if( db != null && db.isAlive() )
					try { db.close(); } catch(Exception e) {}
				instanceCount--;
				return;
			}
		}
		
		// put back the db in the pool if it is still active
		if( db != null && db.isAlive() )
		{
			Logger.finest("Returning to pool ["+db.hashCode()+"].");

			pool.offer(db);
		}
		else
		{
			Logger.finest("Disconnected. Not returning to pool ["+db.hashCode()+"].");

			synchronized(instanceCount)
			{
				instanceCount--;
			}
			checkMinInstances();
		}
	}
	
	private void checkMinInstances()
	{
		Database db = null;
		
		// initialize MIN instances
		synchronized(instanceCount)
		{
			while( instanceCount < minActiveConnections )
			{
				db = template.clone();
				pool.offer(db);
				instanceCount++;
				
				Logger.finest("Not enough connections. Pooling ["+db.hashCode()+"].");
			}
		}
	}
	
	private Database getDatabase()
	{
		checkMinInstances();
		
		Database db = null;
		
		// get from pool (blocking)
		if( blocking )
		{
			long timeout_start = System.currentTimeMillis();
			while( (db = pool.poll()) == null )
			{
				// check if we reached the max connections
				synchronized(instanceCount)
				{
					if( instanceCount < maxActiveConnections )
					{
						db = template.clone();
						instanceCount++;
						break;
					}
				}
				
				try
				{
					long timeout_now = System.currentTimeMillis() - timeout_start;
					if( blockTimeout > 0 && timeout_now >= blockTimeout )
						throw new Exception("Timeout");
					
					Thread.sleep(checkInterval);
				}
				catch(Exception e) { throw new NoSuchElementException("Could not get a connection from the pool. Timeout."); }
			}
		}
		else
		{
			db = pool.poll();
			
			if( db == null )
				throw new IllegalStateException("Current connection is not available");
		}
		
		Logger.finest("Got connection ["+db.hashCode()+"]");
		
		return db;
	}
	
	protected void connect() throws SQLException
	{
		throw new SQLException("Cannot invoke the connect() method on a PooledDatabase");
	}
}