package com.busit;

/** This interface specifies that the connector can emit a new message out of the blue.
 * <br /><pre>
 * public class Clock extends Connector implements Producer
 * {
 *      public IMessage produce(Interface out)
 *      {
 *           IMessage message = Factory.message();
 *           IContent data = Factory.content(0);
 *           data.put("data", new SimpleDateFormat(out.value).format(new Date()));
 *           message.content(data);
 *           return message;
 *      }
 *
 *      public IMessage sample(Interface out)
 *      {
 *           return produce(out);
 *      }
 * 
 *      public boolean test() throws Exception
 *      {
 * 	         return true;
 *      }
 * }
 * </pre>
 */
public interface Producer
{
	/** Produce a message.
	 * @param out the target connector output
	 * @return	the new message or <code>null</code> if nothing is produced
	 */
	public IMessage produce(Interface out);
	
	/** Produce a sample test message.
	 * @param out the target connector output
	 * @return	the new message filled with sample data
	 */
	public IMessage sample(Interface out);
	
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
