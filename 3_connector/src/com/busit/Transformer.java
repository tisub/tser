package com.busit;

/** This interface specifies that the connector receives a message and forwards it to continue the chain.
 * <br /><pre>
 * public class Substring extends Connector implements Transformer
 * {
 *      public IMessage transform(IMessage message, Interface in, Interface out);
 *      {
 *           String text = message.content().get("data");
 *           text = text.substring(0, Integer.parseInt(in.value));
 *           message.content().put("data", text);
 *           return message;
 *      }
 *
 *      public boolean test() throws Exception
 *      {
 *           return "TEST".substring(0, Integer.parseInt("2")).equals("TE");
 *      }
 * }
 */
public interface Transformer
{
	/** Transform a message.
	 * @param message the original message transform and return
	 * @param in the target connector input
	 * @param out the target connector output
	 * @return	the transformed message or <code>null</code> in order to break the chain
	 */
	public IMessage transform(IMessage message, Interface in, Interface out);
	
	/**
	 * Tests if the connector works. 
	 * You should build unit tests for your connector in this method and check for external resources or anything you rely on.
	 * This method is intented to test if your connector works as is, not as part of a scenario. Thus the runtime instance of your implementation is to be
	 * considered as an <em>anonymous</em> instance : the config, inputs and outputs will be defaults as you declared them, 
	 * and <strong>not</strong> customized by the user. Thus if you rely on externally populated or specific configuration process values or else, 
	 * those will <strong>not</strong> be available.
	 * @throws	Exception 	Any exception is allowed to be thrown and is considered as a failure equivalent to returning <code>false</code>. 
	 * 		However, throwing an exception allows to mention a specific cause that will eventually help troubleshooting.
	 * @return	<code>true</code> if the connector should work properly, <code>false</code> otherwise.
	 */
	public boolean test() throws Exception;
}