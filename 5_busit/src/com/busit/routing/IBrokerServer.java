package com.busit.routing;

import com.anotherservice.util.*;

public interface IBrokerServer
{
	public void initialize(Any data);
	public void start() throws Exception;
	public String receive() throws Exception;
	public void stop();
}