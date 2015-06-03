package com.busit;

public interface IFactory
{
	/**
	 * Get a fully populated {@link com.busit.IContent} instance matching the template <code>id</code> provided.
	 * @param	id	the content type id
	 * @return	a matching <code>IContent</code> instance
	 */
	public IContent content(int id);
	
	/**
	 * Get a clean new {@link com.busit.IMessage} instance.
	 * @return	an <code>IMessage</code> instance
	 * @throws	IllegalStateException if a template message has not previousely been set using {@link #template(IMessage)}.
	 * @see 	#template(IMessage)
	 */
	public IMessage message();
	
	/**
	 * Get a clean new {@link com.busit.IMessageList} instance.
	 * @return	an <code>IMessageList</code> instance
	 */
	public IMessageList messageList();
	
	/**
	 * Sets the message template used to create new messages in the current thread.
	 * @param	message	the message template to create new messages
	 * @throws	IllegalStateException if another template has already been set for the current thread
	 * @see #message()
	 */
	public void template(IMessage message);
}