package com.anotherservice.db;

import java.sql.*;
import java.util.*;

/**
 * Oracle {@link com.anotherservice.db.Database} implementation.
 * @author	Simon Uyttendaele
 */
public class Oracle extends Database
{
	private static Oracle instance = null;
	
	/**
	 * Singleton getter for an instance of this class.
	 * @return	the single instance registered
	 * @note	the instance <em>must</em> be set using the {@link #init(String, String, String, String, String)} method.
	 */
	public static Oracle getInstance()
	{
		if( instance == null )
			throw new RuntimeException("Oracle must be initialized via the init() method prior to getting an instance");

		return instance;
	}
	
	/**
	 * Singleton setter for an instance of this class.
	 * If an instance if this class is already registered, calling this method <strong>will not replace the current singleton instance</strong> ; 
	 * the new call will be ignored and the original instance will be returned instead.
	 * @param	host	the database host. See {@link com.anotherservice.db.Database#host}.
	 * @param	port	the database port. See {@link com.anotherservice.db.Database#port}.
	 * @param	database	the database name. See {@link com.anotherservice.db.Database#database}.
	 * @param	user	the database user. See {@link com.anotherservice.db.Database#user}.
	 * @param	password	the database password. See {@link com.anotherservice.db.Database#password}.
	 * @return	the newly created instance
	 */
	public static Oracle init(String host, String port, String database, String user, String password) throws SQLException
	{
		if( instance == null )
			instance = new Oracle(host, port, database, user, password);
		
		return instance;
	}
	
	/**
	 * Constructor with required parameters
	 */
	public Oracle(String host, String port, String database, String user, String password) throws SQLException
	{
		super();
		
		this.host = host;
		this.port = port;
		this.database = database;
		this.user = user;
		this.password = password;
		
		connect();
	}
	
	/**
	 * Clones this instance
	 */
	public Database clone()
	{
		try
		{
			return new Oracle(host, port, database, user, password);
		}
		catch(SQLException e)
		{
			return null;
		}
	}
	
	protected synchronized void connect() throws SQLException
	{
		try
		{
			if( !isAlive() )
			{
				Class.forName("oracle.jdbc.driver.OracleDriver");
				String url = "jdbc:oracle:thin:@//" + host + ":" + port + "/" + database;
				
				this.connection = DriverManager.getConnection(url, user, password);
				this.connection.setAutoCommit(true);
			}
		}
		catch(Exception e)
		{
			throw new SQLException(e);
		}
	}
}