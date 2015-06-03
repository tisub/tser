package com.busit.routing;

import com.anotherservice.util.*;
import java.util.*;

public class Path extends LinkedList<Step>
{
	public Path()
	{
	}
	
	public Path(String json) throws Exception
	{
		this(Json.decode(json));
	}
	
	public Path(Any json)
	{	
		for( Any s : json )
			this.add(Step.uncheckedParse(s));
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		Iterator<Step> i = this.iterator();
		while( i.hasNext() )
		{
			sb.append(",");
			sb.append(i.next().toString());
		}
		
		return "[" + sb.substring(1) + "]";
	}
	
	public String toString(long processingTime, long outputSize, String outputHash)
	{
		StringBuilder sb = new StringBuilder();
		Iterator<Step> i = this.iterator();
		Step t;
		while( i.hasNext() )
		{
			sb.append(",");
			t = i.next();
			if( !i.hasNext() )
			{
				t.processingTime = processingTime;
				t.outputSize = outputSize;
				t.outputHash = outputHash;
			}
			sb.append(t.toString());
		}
		
		return "[" + sb.substring(1) + "]";
	}
}