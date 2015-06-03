package com.anotherservice.db;

import java.sql.*;
import java.util.*;

/**
 * This class is an abstraction layer for easy access to databases using the <code>java.sql</code> framework. 
 * All connections, resources, cleanups and such are thus transparent.
 * @author	Simon Uyttendaele
 */
public abstract class Database implements Cloneable
{
	private static Database instance = null;
	
	/**
	 * Singleton getter for an instance of this class.
	 * @return	the single instance registered
	 * @note	the instance <em>must</em> be set using the {@link #setInstance(Database)} method.
	 */
	public static Database getInstance()
	{
		if( instance == null )
			throw new RuntimeException("Database instance not set");
		return instance;
	}
	
	/**
	 * Singleton setter for an instance of this class.
	 * @param	db	the instance
	 */
	public static void setInstance(Database db)
	{
		instance = db;
	}
	
	/**
	 * The database host
	 */
	protected String host;
	/**
	 * The database port
	 */
	protected String port;
	/**
	 * The database name
	 */
	protected String database;
	/**
	 * The database user
	 */
	protected String user;
	/**
	 * The database password
	 */
	protected String password;
	/**
	 * The database dummy SELECT statement.
	 * This SQL query will be performed before every other request in order to check that the connection is stil alive. 
	 * <br />Default value : "SELECT 1 FROM DUAL"
	 */
	protected String testSelect = "SELECT 1 FROM DUAL";
	/**
	 * The original <code>java.sql.Connection</code>
	 */
	protected Connection connection;
	
	/**
	 * Getter for the {@link #host}. 
	 * @note Once the connection is established, it should not be changed.
	 */
	public String host() { return host; }
	/**
	 * Getter for the {@link #port}. 
	 * @note Once the connection is established, it should not be changed.
	 */
	public String port() { return port; }
	/**
	 * Getter for the {@link #database}. 
	 * @note Once the connection is established, it should not be changed.
	 */
	public String database() { return database; }
	/**
	 * Getter for the {@link #user}. 
	 * @note Once the connection is established, it should not be changed.
	 */
	public String user() { return user; }
	/**
	 * Getter for the {@link #password}. 
	 * @note Once the connection is established, it should not be changed.
	 */
	public String password() { return password; }
	
	/**
	 * Checks whether or not the database is ready to process requests. 
	 * @return	<code>true</code> if the <code>Connection</code> is not <code>isClosed()</code>. <code>false</code> otherwise.
	 */
	public boolean isAlive()
	{
		try 
		{
			return this.connection instanceof Connection && !this.connection.isClosed() && this.connection.isValid(10);
		}
		catch(SQLException e)
		{
			return false;
		}
	}
	
	/**
	 * Closes the database connection. 
	 * @see #close()
	 */
	protected void finalize()
	{
		try { close(); } catch( Exception e ) { }
	}
	
	/**
	 * Closes the database connection.
	 */
	public synchronized void close() throws SQLException
	{
		if( this.connection instanceof Connection && !this.connection.isClosed() )
			this.connection.close();
	}
	
	/**
	 * This method should be implemented in order to (re)establish the connection.
	 * This method will be called when a dropped connection is discovered in order
	 * to reconnect to the database. (SQLState 08*)
	 */
	protected abstract void connect() throws SQLException;
	
	/**
	 * The database request type. 
	 * @see #query(String, Database.Mode)
	 */
	public static enum Mode
	{
		/**
		 * Targets a request from which no result should be returned. 
		 * Typically INSERT, UPDATE, DELETE
		 */
		NO_ROW,
		/**
		 * Targets a request from which only one row should be returned. 
		 * Typically SELECT
		 */
		ONE_ROW,
		/**
		 * Targets a request from which possibly many rows should be returned. 
		 * Typically SELECT
		 */
		ANY_ROW
	};
	
	/**
	 * Clones this database instance.
	 * The returned clone should use the same <em>host, port, database, user, password</em>.
	 */
	abstract public Database clone();
	
	/**
	 * Shortcut for an INSERT request. 
	 * @param	sql	The SQL request
	 * @return	the first inserted numeric primary key
	 * @note	this method simply calls {@link #query(String, Database.Mode)}
	 */
	public synchronized Long insert(String sql) throws SQLException { return (Long) query(sql, Mode.NO_ROW); }
	
	/**
	 * Shortcut for an UPDATE request. 
	 * @param	sql	The SQL request
	 * @return	the number of rows updated
	 * @note	this method simply calls {@link #query(String, Database.Mode)} and ignores the return value
	 */
	public synchronized Long update(String sql) throws SQLException { return (Long) query(sql, Mode.NO_ROW); }
	
	/**
	 * Shortcut for a DELETE request. 
	 * @param	sql	The SQL request
	 * @return	the number of rows deleted
	 * @note	this method simply calls {@link #query(String, Database.Mode)} and ignores the return value
	 */
	public synchronized Long delete(String sql) throws SQLException { return (Long) query(sql, Mode.NO_ROW); }
	
	/**
	 * Shortcut for a SELECT request with {@link Mode#ONE_ROW}. 
	 * @param	sql	The SQL request
	 * @return	the result of the request
	 * @note	this method simply calls {@link #query(String, Database.Mode)} and returns the value
	 */
	public synchronized Map<String, String> selectOne(String sql) throws SQLException { return (Map<String, String>) query(sql, Mode.ONE_ROW); }
	
	/**
	 * Shortcut for a SELECT request with {@link Mode#ANY_ROW}. 
	 * @param	sql	The SQL request
	 * @return	the result of the request
	 * @note	this method simply calls {@link #query(String, Database.Mode)} and returns the value
	 */
	public synchronized Vector<Map<String, String>> select(String sql) throws SQLException { return (Vector<Map<String, String>>) query(sql, Mode.ANY_ROW); }
	
	/**
	 * Performs the provided request against the underlying database represented by this object. 
	 * This method creates a new <code>java.sql.Statement</code> and performs an <ul><li>
	 * <code>executeQuery()</code> if the <em>mode</em> is {@link Mode#ONE_ROW} or {@link Mode#ANY_ROW}</li>
	 * <li><code>executeUpdate()</code> if the <em>mode</em> is {@link Mode#NO_ROW}</li></ul>
	 * @note an implicit <code>commit()</code> is performed against the <code>Connection</code> after every statement.
	 * @param	sql	The SQL request
	 * @param	mode	The query type
	 * @return	<ul><li>the last inserted <code>java.lang.Long</code> primary key (INSERT) or the number of affected rows (UPDATE, DELETE) if the <em>mode</em> is {@link Mode#NO_ROW}</li>
	 * <li>a <code>Map<String, String></code> with column names as keys and their matching values as string if the <em>mode</em> is {@link Mode#ONE_ROW}</li>
	 * <li>a <code>Vector<Map<String, String>></code> for each of the result rows if the <em>mode</em> is {@link Mode#ANY_ROW}</li></ul>
	 */
	public synchronized Object query(String sql, Database.Mode mode) throws SQLException
	{
		if( !(this.connection instanceof Connection) )
			throw new SQLException("No active connection");
		
		if( !isAlive() )
		{
			this.connect();
			return query(sql, mode);
		}
		
		Statement statement = this.connection.createStatement();
		
		// in order to prevent a loss of connection,
		// first try a dummy select.
		// if the connection has been lost, the SQLException will be caught and
		// then we can reconnect.
		// Because if we reconnect directly, it does NOT detect that the connection is lost
		// until some query is made. 
		try { statement.executeQuery(testSelect); } catch(SQLException se)
		{
			if( se.getSQLState().startsWith("08") )
			{
				this.connect();
				return query(sql, mode);
			}
		}
		
		ResultSet r = null;
		
		try
		{
		switch(mode)
		{
			case NO_ROW:
				Integer count = statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
				
				Long id = null;
				if( sql.matches("^(?i)(?s)(UPDATE|DELETE) .*") )
					id = count.longValue();

				try
				{
					r = statement.getGeneratedKeys();
					if( r.next() )
						id = r.getLong(1);
				}
				catch(Exception e) { id = null; /* if the generated ID does not exist or is not numeric... */ }
				
				try { this.connection.commit(); }
				catch(Exception e) { /* Can't call commit when autocommit=true */ }

				return id;
			
			default:
			case ONE_ROW:
				r = statement.executeQuery(sql);
				HashMap<String, String> h_result = new HashMap<String, String>();
				if( r.next() )
				{
					ResultSetMetaData rsmd = r.getMetaData();
					for(int i=1; i <= rsmd.getColumnCount(); i++)
					{
						if( rsmd.getColumnType(i) == java.sql.Types.BLOB )
						{
							byte[] bytes = r.getBytes(i);
							if( bytes == null || bytes.length == 0 )
								h_result.put(rsmd.getColumnLabel(i), null);
							else
								h_result.put(rsmd.getColumnLabel(i), new String(bytes));
						}
						else
							h_result.put(rsmd.getColumnLabel(i), r.getString(i));
					}
				}

				return h_result;
				
			case ANY_ROW:
				r = statement.executeQuery(sql);
				Vector<HashMap<String, String>> v_result = new Vector<HashMap<String, String>>();
				while( r.next() )
				{
					HashMap<String, String> row = new HashMap<String, String>();
					
					ResultSetMetaData rsmd = r.getMetaData();
					for(int i=1; i <= rsmd.getColumnCount(); i++)
					{
						if( rsmd.getColumnType(i) == java.sql.Types.BLOB )
						{
							byte[] bytes = r.getBytes(i);
							if( bytes == null || bytes.length == 0 )
								row.put(rsmd.getColumnLabel(i), null);
							else
								row.put(rsmd.getColumnLabel(i), new String(bytes));
						}
						else
							row.put(rsmd.getColumnLabel(i), r.getString(i));
					}
						
					v_result.add(row);
				}

				return v_result;
		}
		}
		finally
		{
			if( r != null )
				r.close();
			if( statement != null )
				statement.close();
		}
	}
}
