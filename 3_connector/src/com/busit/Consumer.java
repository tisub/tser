package com.busit;

/** This interface specifies that the connector can receive a message and end the chain.
 * <br /><pre>
 * public class Dumper extends Connector implements Consumer
 * {
 *      public void consume(IMessage message, Interface in)
 *      {
 *           System.out.println(in.value + " " + message.content().toText());
 *      }
 * 
 *      public boolean test() throws Exception
 *      {
 *           return (System.out != null);
 *      }
 * }
 * </pre>
 */
public interface Consumer
{
	/** Consume a message.
	 * @param message the message
	 * @param in the target connector input
	 */
	public void consume(IMessage message, Interface in);
	
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