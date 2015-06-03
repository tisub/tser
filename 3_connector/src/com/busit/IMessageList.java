package com.busit;

import java.util.*;

/**
 * This interface represents a list of multiple {@link com.busit.IMessage}.
 * You can only access the <code>List</code> methods.
 * All <code>IMessage</code> methods (except {@link com.busit.IMessage#copy()}) will throw an <code>UnsupportedOperationException</code>.
 * Extending <code>IMessage</code> ensures that you can return an instance of this interface where a single <code>IMessage</code> is expected.
 */
public interface IMessageList extends List<IMessage>, IMessage
{
}