package com.busit.security;

import java.util.*;

public interface IIdentityChecker
{
	public boolean userExists(String user) throws Exception;
	public boolean orgExists(String org) throws Exception;
	public boolean isUserOrgMember(String user, String org) throws Exception;
	public boolean isIdentityOrgMember(String identity, String org) throws Exception;
	public boolean isUserOrgAdmin(String user, String org) throws Exception;
	public boolean isIdentityOrgAdmin(String identity, String org) throws Exception;
	public boolean isUserIdentityAdmin(String user, String identity) throws Exception;
	public String userOrgMemberVia(String user, String org) throws Exception;
	public boolean isUserIdentity(String user, String identity) throws Exception;
	public boolean isOrgIdentity(String org, String identity) throws Exception;
	public boolean isUserImpersonate(String user, String identity) throws Exception;
	public String userImpersonateVia(String user, String identity) throws Exception;
	public boolean isIdentityImpersonate(String identity_from, String identity_to) throws Exception;
	public boolean isUserIdentityOrImpersonate(String user, String identity) throws Exception;
	public Collection<Map<String, String>> userIdentities(String user) throws Exception;
	
	public boolean isUserInstance(String user, String instance) throws Exception;
	public boolean isUserInstanceAdmin(String user, String instance) throws Exception;
	public boolean isUserInstanceUser(String user, String instance) throws Exception;
	
	public boolean isUserConnector(String user, String connector) throws Exception;
	public boolean isUserBill(String user, String bill) throws Exception;
	
	public boolean isUserSpace(String user, String space) throws Exception;
	public boolean isOrgSpace(String space) throws Exception;
	public boolean isOrgSpace(String org, String space) throws Exception;
	public boolean isUserSpaceAdmin(String user, String space) throws Exception;
	public boolean isUserSpaceUser(String user, String space) throws Exception;
	
	public boolean isOrg(String user) throws Exception;
	public boolean isLastOrgAdmin(String user, String org) throws Exception;
	
	public Identity defaultIdentity(String user) throws Exception;
	public int defaultIdentityId(String user) throws Exception;
}