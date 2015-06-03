package com.anotherservice.rest.security;

import java.util.*;

/**
 * This interface defines the most basic operations that the REST service security should implement. 
 * <br />Only one instance of <code>ISecurity</code> is allowed at once for the whole system.
 * <br />This ensures that the security mechanism is consolidated across every service deployed.
 * <br /><br />In order to retrieve the authentication information sent by the client, the easyest
 * approach is to use something like :
 * <pre>String auth = Request.getParam(Config.gets("com.anotherservice.rest.core.authParameter"));</pre>
 * Remember that this is a REST service, so supposed stateless. So some sort of auth/token/... is supposed
 * to be passed in every request.
 * @see com.anotherservice.rest.security.Security
 *
 * @author  Simon Uyttendaele
 */
public interface ISecurity
{
	/**
	 * Checks if there is a user currently authenticated.
	 * @return	<code>true</code> if credentials are valid
	 */
	public boolean isAuthenticated() throws Exception;
	
	/**
	 * Retrieve the current logged user's name. (i.e. for display).
	 * @return	the user name or <code>null</code> if not available (not authenticated or else)
	 */
	public String getUser() throws Exception;
	/**
	 * Retrieve the current logged user's credentials.
	 * That is, the username/password or token,...
	 * Basically, consider that the user should be able to authenticate using <em>only</em> this information.
	 * @return	the user credentials or <code>null</code> if not available (not authenticated or else)
	 */
	public String getCredentials() throws Exception;
	/**
	 * Retrieve the current logged user's ID. (i.e. for database foreign keys).
	 * @return	the user ID or <code>null</code> if not available (not authenticated or else)
	 */
	public long userId() throws Exception;
	/**
	 * Retrieve the provided user's ID. (i.e. for database foreign keys).
	 * @return	the user ID or <code>null</code> if not available
	 */
	public long userId(String user) throws Exception;
	/**
	 * Checks whether or not the current logged user has the provided grants.
	 * Typically calls {@link #hasGrants(Collection, String)} using {@link #getCredentials()}.
	 */
	public boolean hasGrants(Collection<String> grants);
	/**
	 * Checks whether or not the current logged user has the provided grants.
	 * Grants are simple <code>Strings</code> set in different services. Of course,
	 * handling those grants imply some uniformity in all services that should comply to
	 * the same security mechanism.
	 * @param	grants	the list of grants to check
	 * @param	credentials	the user credentials used for authentication
	 * @return	<code>true</code> if the user posses <strong>all</strong> the provided grants. <code>false</code> if any of the grants does not comply.
	 * @note	the behavior of this method may differ from the documentation depending on specific implementations.
	 * <br />For instance, the behavior to adopt when handling <code>null</code> value grants or case sensitivity is not imposed.<br />
	 * Another example is the possible use of a <em>"master"</em>
	 * grant or <em>"hyerarchical"</em> grants which may include or imply other grants, thus alter the behavior of this method.
	 */
	public boolean hasGrants(Collection<String> grants, String credentials);
	/**
	 * Checks whether or not the current logged user has the provided grants.
	 * This method differs from {@link #hasGrants(Collection, String)} in that the credentials may be some sort of parallel (more robust)
	 * authentication mechanism. However, in some situations, it may be useful to fall back on simple user/password authentication. If this is not
	 * supported, either throw a <code>RuntimeException</code> or simply return <code>false</code>.
	 * @param	grants	the list of grants to check
	 * @param	user	the user name or id
	 * @param	pass	the user password
	 * @return	<code>true</code> if the user posses <strong>all</strong> the provided grants. <code>false</code> if any of the grants does not comply.
	 */
	public boolean hasGrants(Collection<String> grants, String user, String pass);
	/**
	 * Checks whether or not the current logged user has the provided grant.
	 * Typically calls {@link #hasGrants(Collection)}.
	 */
	public boolean hasGrant(String grant);
	/**
	 * Checks whether or not the target user has the provided grant.
	 * Typically calls {@link #hasGrants(Collection, String)}.
	 */
	public boolean hasGrant(String grant, String credentials);
	/**
	 * Checks whether or not the target user has the provided grant.
	 * Typically calls {@link #hasGrants(Collection, String, String)}.
	 */
	public boolean hasGrant(String grant, String user, String pass);
	/**
	 * Hashes the provided text.
	 * In order to reduce security risks, you should apply a strong hashing function.
	 * @param	pass	the text to hash
	 * @return	the hashed text
	 */
	public String hash(String pass);
}