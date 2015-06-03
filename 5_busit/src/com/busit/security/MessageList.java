package com.busit.security;

import com.busit.*;
import java.io.*;
import java.util.*;

public class MessageList extends ArrayList<IMessage> implements IMessageList
{
	public InputStream file(String name)				{ throw new UnsupportedOperationException(); }
	public void file(String name, InputStream data)		{ throw new UnsupportedOperationException(); }
	public Map<String, InputStream> files() 			{ throw new UnsupportedOperationException(); }
	public IContent content() 							{ throw new UnsupportedOperationException(); }
	public void content(IContent data) 					{ throw new UnsupportedOperationException(); }
	public String from() 								{ throw new UnsupportedOperationException(); }
	public String to() 									{ throw new UnsupportedOperationException(); }
	public IMessage copy() 								{ MessageList ml = new MessageList(); for( IMessage m : this ) ml.add(m); return ml; }
}