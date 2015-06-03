package com.busit;

import java.util.Map;

/**
 * This is the basic routable interface for connectors. See {@link com.busit.Connector}.
 */
public interface IConnector
{
	public static final long VERSION = 3L;
	
	/**
	 * Step 1. This method is called by the broker in order to initialize this connector instance.
	 * You should consider this method as the constructor and you should not perform any process before
	 * {@link #cron(IMessage, String)} or {@link #setInput(IMessage, String)} !
	 * @param	config	the key/value pair of all configuration parameters
	 * @param	inputs	the list of all <strong>input</strong> interfaces (see note for format)
	 * @param	outputs	the list of all <strong>output</strong> interfaces (see note for format)
	 * @throws	RuntimeException 	you should throw if anything goes wrong so that the user can be alerted.
	 * @note	The interface (input and output) format is as follows :
	 * <ul><li>The <code>Map</code> key is the <strong>name</strong> of the interface as given by the user. 
	 * This is the <code>interfaceId</code> that will be used in {@link #cron(IMessage, String)}, {@link #getOutput(String)} and {@link #setInput(IMessage, String)}.</li>
	 * <li>Each <code>Map</code> value is again a <code>Map</code> containing :
	 * <ul><li><em>key</em> : the internal name of the interface as you defined it in the connector configuration</li>
	 * <li><em>value</em> : the eventual value of the interface if it is a dynamic one, else <code>null</code></li></ul>
	 * </li></ul>
	 */
	public void init(Map<String, String> config, Map<String, Map<String, String>> inputs, Map<String, Map<String, String>> outputs);
	
	/**
	 * Step 2a. This method is called by the broker when an automatic timer occurs as setup by the user.
	 * This method is called first in order to prepare required data and then <strong>only the corresponding output</strong> is
	 * considered when calling {@link #getOutput(String)}, all other links on other output interfaces are not triggered.
	 * @param	interfaceId	the name of the <strong>output</strong> interface to consider
	 * @throws	UnsupportedOperationException 	you should throw if you do not support cron.
	 * @throws	RuntimeException 	you should throw if anything goes wrong so that the user can be alerted.
	 */
	public void cron(IMessage message, String interfaceId);
	
	/**
	 * Step 2b. This method is called by the broker when a message is sent by the user.
	 * This method is called first in order to prepare required data and then all possible output interfaces linked by the user are
	 * triggered by calling {@link #getOutput(String)}.
	 * @param	message	the encrypted message of the user
	 * @param	interfaceId	the name of the <strong>input</strong> interface to consider
	 * @throws	UnsupportedOperationException 	you should throw if the <code>interfaceId</code> is invalid.
	 * @throws	RuntimeException 	you should throw if anything goes wrong so that the user can be alerted.
	 * @see		com.busit.IMessage
	 */
	public void setInput(IMessage message, String interfaceId);
	
	/**
	 * Step 3. This method is called by the broker in order to retrieve the message to forward to the next linked connector.
	 * This method is always called after either {@link #cron(IMessage, String)} or {@link #setInput(IMessage, String)}
	 * @param	interfaceId	the name of the <strong>output</strong> interface to consider
	 * @throws	UnsupportedOperationException 	you should throw if the <code>interfaceId</code> is invalid.
	 * @throws	RuntimeException 	you should throw if anything goes wrong so that the user can be alerted.
	 * @return	The message with proper content, or <code>null</code> if there is no data.
	 * @see		com.busit.IMessage
	 */
	public IMessage getOutput(String interfaceId);
	
	/**
	 * This method is called when the user wants to simulate data production in order to test his scenario. 
	 * This method should return a static hard-coded message. If your connector does not produce any data, just return <code>null</code>.
	 * Note that this metod will always be called after {@link #init(Map, Map, Map)} so you can rely on a fully populated configuration.
	 * @param	message	an empty template message to fill-in
	 * @param	interfaceId	the name of the <strong>output</strong> interface to consider
	 * @throws	UnsupportedOperationException 	you should throw if the <code>interfaceId</code> is invalid.
	 * @throws	RuntimeException 	you should throw if anything goes wrong so that the user can be alerted.
	 * @return	The message with proper sample content, or <code>null</code> if there is no data.
	 * @see		com.busit.IMessage
	 */
	public IMessage getSampleData(IMessage message, String interfaceId);
	
	/**
	 * This method is called internally for time to time in order to test if the conector works. 
	 * You should build unit tests for your connector in this method and check for external resources or anything you rely on.
	 * Note that this method will be called just after the constructor, thus the {@link #init(Map, Map, Map)} method will <strong>not</strong> be called.
	 * Instead, you will be provided the config, inputs and outputs parameters directly. 
	 * This method is intented to test if your connector works as is, not as part of a scenario. Thus the runtime instance of your implementation is to be
	 * considered as an <em>anonymous</em> instance : the config, inputs and outputs will be defaults as you declared them, 
	 * and <strong>not</strong> customized by the user. Thus if you rely on OAuth or specific configuration process values or else, those will <strong>not</strong> be available.
	 * @param	config	the key/value pair of all configuration parameters. The value will be null unless you have provided defaults.
	 * @param	inputs	the list of all <strong>input</strong> interfaces, the name is the same as the key and the value is null unless you have provided defaults.
	 * @param	outputs	the list of all <strong>output</strong> interfaces, the name is the same as the key and the value is null unless you have provided defaults.
	 * @throws	Exception 	Any exception is allowed to be thrown and is considered as a failure equivalent to returning <code>false</code>. 
	 * 		However, throwing an exception allows to mention a specific cause that will eventually help troubleshooting.
	 * @return	<code>true</code> if the connector should work properly, <code>false</code> otherwise.
	 */
	public boolean isFunctional(Map<String, String> config, Map<String, Map<String, String>> inputs, Map<String, Map<String, String>> outputs) throws Exception;
	
	/**
	 * This method should be implemented to report errors or warnings to the end user.
	 * @param	message	the message to report to the user (consider translations)
	 */
	public void notifyUser(String message);
	
	/**
	 * This method should be implemented to report errors or warnings to the owner of the implementation.
	 * @param	message	the message to report to the owner (consider translations)
	 * @param	data	any additionnal data that can be useful
	 */
	public void notifyOwner(String message, Map<String, Object> data);
}