package com.anotherservice.rest.security;

public interface IQuota
{
	public void increment(String user, String quota) throws Exception;
	public void increment(String user, String quota, boolean bypass) throws Exception;
	public void add(String user, String quota, int quantity, boolean bypass) throws Exception;
	public void decrement(String user, String quota) throws Exception;
	public void decrement(String user, String quota, boolean bypass) throws Exception;
	public void substract(String user, String quota, int quantity, boolean bypass) throws Exception;
}