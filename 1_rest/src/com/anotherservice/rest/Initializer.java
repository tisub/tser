package com.anotherservice.rest;

/**
 * <p>This is the REST service initializer interface. Custom services discovered by the {@link com.anotherservice.rest.core.ServiceMonitor ServiceMonitor} will call the
 * <code>initialize()</code> method on <strong>a new instance using an empty constructor</strong> instead of using a factory (see discussion below).
 * </p><p><br />
 * <big><strong>Discussion on the empty constructor vs factory</strong></big><br />
 * Typically, in order to obtain an instance of a class which may be extended (or an interface) one shall use a <em>factory method</em>.<br />
 * Such method would have the following signature : <br /><code>public Initializer create()</code><br />
 * Then, one can easily return a different implementation of the Initializer interface. Easy enough.
 * </p><p><br />
 * The factory pattern is motivated by the fact that a <code>constructor</code> is part of the implementation of a class. Thus,
 * none can know in advance the constructor signature (and this is why interfaces cannot define a constructor signature). <em>"What if
 * my specific implementation required a different constructor !?"</em> Then, in order to avoid this issue, the creation of the instance
 * is isolated in a <code>factory method</code> at the sole discretion of the implementor.
 * </p><p><br />
 * However, how do we call this factory method ?
 * Either it is a <code>static</code> method, which enables us to call it from anywhere (very convenient), but in this case, it means that
 * the factory class is predefined ! And so is the static factory method... which is totally against the pattern. (interfaces for static methods are not possible).<br />
 * Or, second option, is to create some sort of factory for the factory class such that anyone can implement its own factory method...<br />
 * But how do we get this factory class instance ? From a factory itself... and we go in an infinite loop.
 * </p><p><br />
 * Hence, typical factory mechanisms imply that from the executing code, one shall <em>register</em> its own factory implementation (either as a singleton or else)
 * using a static method like this one : <br /><code>public static void register(Factory instance)</code>.<br />
 * Therefore, from anywhere, we can get the singleton factory and call the <code>create()</code> method to get an instance of <code>Initializer</code>.<br />
 * Tadaaam, problem solved !
 * </p><p><br />
 * - But huh... wait a second, <em>"from the executing code"</em> you said... But in this webservice context, where we <u>do not</u> have executing code in advance,
 * how can we achieve this !?<br />
 * - Indeed, services implementation reside in a jar file which is <strong>not loaded</strong> until used. <br />
 * - And when it is being loaded, how do we know where to start !? <br />
 * - Hey, using an <code>Initializer</code> interface, then we get an instance and simply call the <code>initialize()</code> method against it.<br />
 * - But in order to get the <code>Initializer</code> implementation, we should use a factory, right ? A factory to get the initializer of the factory to 
 * register the factory to get the <code>Initializer</code> of the service...<br />
 * - Hmmm... it seems we are running into a stupid pattern loop here, isnt't it !?<br />
 * </p><p><br />
 * <big><strong>How do <em>others</em> solve this ?</strong></big><br />
 * They use a static method to get the factory <em>from somewhere</em> ! This is the only option to call something without constructing a new factory manually and running in the above problem.<br />
 * So either they use a <code>static void main()</code>-like method defined in the <em>manifest</em> of the jar. But relying on a manifest file is not very 
 * convenient and not very robust(, but it turns out to be a bit like our solution but ours is better).<br />
 * Or, they rely on the name of the class in the jar file... which is totally stupid because a class name does not imply anything about its content.<br />
 * Or, they use annontations such that when a proper annotated class is <em>found</em>, we can call the static method and get the factory. How is the class <em>"found"</em> then !?<br />
 * Via reflection. This means that they should <strong>scan and load into memory every class in every jar file</strong> in order to process them all to try to find 
 * a proper class that has the good annotation. And what if there are two !? Then we run into problems.<br /><br />
 * <em>JavaBeans</em> are not a valid option either because it relies on (I) internal method naming, and (II) reflection as stated above.
 * </p><p><br />
 * <big><strong>Solution</strong></big><br />
 * Therefore, in order to avoid all this factory mess, and be much lighter and smarter than others, lets agree that <strong>implementations of <code>Initializer</code> 
 * simply should have a default empty constructor</strong> so we can instanciate it without problems and we just call the <code>initialize()</code> method.<br />
 * Point.</p>
 * 
 * @author  Simon Uyttendaele <em>- By the way, I'm open to discussions on this at any time ;)</em>
 * @see com.anotherservice.rest.core.ServiceMonitor
 */
public interface Initializer
{
	/**
	 * Initializes the service. One may create resources, instanciate Handlers and register them at this point.
	 * @note	<ul><li>Every <code>Initializer</code> instance is created and run in its own <code>ClassLoader</code> in order to avoid library collusions. 
	 * {@link com.anotherservice.rest.Handler Handlers} and other resources are therefore also created in this context</li>
	 * <li>There is no guarantee that this method will be called only once. A single instance may be called several times and different instances may be created. Thus,
	 * one should take proper actions to handle this behavior.</li></ul>
	 * @see	com.anotherservice.util.Context
	 * @see	com.anotherservice.rest.InitializerOnce
	 */
	public void initialize();
}