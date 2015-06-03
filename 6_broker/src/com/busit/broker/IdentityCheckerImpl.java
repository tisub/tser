package com.busit.broker;

import java.util.*;
import com.busit.security.*;

public class IdentityCheckerImpl implements IIdentityChecker
{
	public boolean userExists(String user) throws Exception												{ throw new UnsupportedOperationException(); }
	public boolean orgExists(String org) throws Exception												{ throw new UnsupportedOperationException(); }
	public boolean isUserOrgMember(String user, String org) throws Exception							{ throw new UnsupportedOperationException(); }
	public boolean isIdentityOrgMember(String identity, String org) throws Exception					{ throw new UnsupportedOperationException(); }
	public boolean isUserOrgAdmin(String user, String org) throws Exception								{ throw new UnsupportedOperationException(); }
	public boolean isIdentityOrgAdmin(String identity, String org) throws Exception						{ throw new UnsupportedOperationException(); }
	public boolean isUserIdentityAdmin(String user, String identity) throws Exception					{ throw new UnsupportedOperationException(); }
	public String userOrgMemberVia(String user, String org) throws Exception							{ throw new UnsupportedOperationException(); }
	public boolean isUserIdentity(String user, String identity) throws Exception						{ throw new UnsupportedOperationException(); }
	public boolean isOrgIdentity(String org, String identity) throws Exception							{ throw new UnsupportedOperationException(); }
	public boolean isUserImpersonate(String user, String identity) throws Exception						{ throw new UnsupportedOperationException(); }
	public String userImpersonateVia(String user, String identity) throws Exception						{ throw new UnsupportedOperationException(); }
	public boolean isIdentityImpersonate(String identity_from, String identity_to) throws Exception		{ throw new UnsupportedOperationException(); }
	public boolean isUserIdentityOrImpersonate(String user, String identity) throws Exception 			{ return true; }
	public Collection<Map<String, String>> userIdentities(String user) throws Exception					{ throw new UnsupportedOperationException(); }
	
	public boolean isUserInstance(String user, String instance) throws Exception						{ throw new UnsupportedOperationException(); }
	public boolean isUserInstanceAdmin(String user, String space) throws Exception						{ throw new UnsupportedOperationException(); }
	public boolean isUserInstanceUser(String user, String instance) throws Exception					{ throw new UnsupportedOperationException(); }
	
	public boolean isUserConnector(String user, String connector) throws Exception						{ throw new UnsupportedOperationException(); }
	public boolean isUserBill(String user, String bill) throws Exception								{ throw new UnsupportedOperationException(); }
	
	public boolean isUserSpace(String user, String space) throws Exception								{ throw new UnsupportedOperationException(); }
	public boolean isOrgSpace(String space) throws Exception											{ throw new UnsupportedOperationException(); }
	public boolean isOrgSpace(String org, String space) throws Exception								{ throw new UnsupportedOperationException(); }
	public boolean isUserSpaceAdmin(String user, String space) throws Exception							{ throw new UnsupportedOperationException(); }
	public boolean isUserSpaceUser(String user, String space) throws Exception							{ throw new UnsupportedOperationException(); }
	
	public boolean isOrg(String user) throws Exception													{ throw new UnsupportedOperationException(); }
	public boolean isLastOrgAdmin(String user, String org) throws Exception								{ throw new UnsupportedOperationException(); }
	
	public Identity defaultIdentity(String user) throws Exception										{ throw new UnsupportedOperationException(); }
	public int defaultIdentityId(String user) throws Exception											{ throw new UnsupportedOperationException(); }
}