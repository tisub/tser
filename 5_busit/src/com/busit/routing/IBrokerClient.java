package com.busit.routing;

import com.busit.security.*;

public interface IBrokerClient
{
	public void transmit(String message, String user, String language, String local, String sla) throws Exception;
	public void trace(String user, Path path, IIdentity from, IIdentity to, String language, boolean cron) throws Exception;
}