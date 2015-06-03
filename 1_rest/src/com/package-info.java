/**
 *  -- START HERE -- : Explanation of the REST service principles and implementation scheme.
 * 
 * <h1 style="border-bottom: 1px solid #99BBCC; color: #668899">Goal</h1>
 * <p>The main goal of this REST service is to provide an easy, intuitive and flexible way of 
 * offering a set of functionalities to the end-user without becoming uderly complex. 
 * Hence, some bricks of the current framework are voluntarily simplified or hardcoded 
 * in line with the <em>CoC</em> design pattern (convention over configuration)(aka: prefer
 * using well-known simpified code rather than fully customizable complex stuff).</p>
 *
 * <h1 style="border-bottom: 1px solid #99BBCC; color: #668899">Protocol</h1>
 * <h2 style="margin-left: 30px; color: #668899">REST</h2>
 * <p>A REST web api is basically a resource you can call via HTTP, eventually passing in
 * some parameters, and then get a response. However, there is no strict definition or
 * enforcement apart from some <em>W3C</em> a posteriori attempts.</p>
 *
 * <h2 style="margin-left: 30px; color: #668899">HTTP verb</h2>
 * <p>In this implementation, we deviate from the "commonly accepted" REST paradigm in that
 * <strong>we do not care about the HTTP verb</strong> that is used. Why is that ?! Because
 * in the concept, REST is using HTTP because it is easy and most platforms, programming 
 * languages, browsers can talk HTTP, so it makes it very convenient to use. In theory,
 * the HTTP protocol comes with some <em>verbs</em> (<code>GET, POST, PUT, DELETE,...</code>)
 * however, in practice, only <code>GET</code> and <code>POST</code> are really used. The 
 * <code>GET</code> verb is used when you type in a URL in your browser, what follows the question mark
 * (<em>http://example.com/<strong>?x=y</strong></em>) are the parameters passed to the server. On
 * the other hand, the <code>POST</code> verb is used when you submit a form in your browser and the
 * parameters are the form data.<br />
 * Of course, some random other stuff use HTTP with wierd verbs, but this is absolutely not representative
 * in terms of ratio compared to <code>GET</code> and <code>POST</code>.<br />
 * Hence, what is truly very easy, is that anyone can test, try and send requests using his browser
 * and thus using the <code>GET</code> verb. There is currently no easy way of sending anything else than
 * <code>GET</code> when typing in the URL you want to access. So we decided that we would not consider the
 * HTTP verb for our REST web api ! Moreover, we allow any to be used or even combined : you can send in a 
 * <code>POST</code> request with form data along URL parameters (<code>GET</code>) that will all be combined transparently
 * (see combination rules below).</p>
 *
 * <h2 style="margin-left: 30px; color: #668899">URL based</h2>
 * <p>Even though REST does not enforce anything about the URLs and the way to use them, we try to make everything
 * as clear and intuitive as possible. For instance, some REST web apis have only one valid URL which is the root ("/")
 * and everything else is passed as parameter. (I.e.: </em>/?action=addCustomer&companyName=MyCompany&...</em>)<br />
 * On our side, we decided to enable the user to <em>guess</em> the URL as it is so easy to understand. For instance, if
 * you want to add a new customer of your company, you may type in something like <code>/MyCompany/add/customer/John</code>.
 * <br />Of course, this depends on what you, programmer, will implement using this framework ;-)<br />
 * Moreover, the framework offers the possibility to use aliases in order to designate the same resource. This is simply, once
 * again, an ease for the end user : why <code>/customer/list</code> would not be valid when <code>/clients/show</code> would...
 * So we say that <code>customer = clients</code> and that <code>list = show</code> and thus, this is exactly the same 
 * functionality behind the scenes.
 * <br /><br />
 * As a recap, considering the previous point explaining that we do not use the HTTP verb, we consider that the URL should be
 * self-descriptive on what it does. Hence, <code>/user/delete</code> indeed deletes a user... There is no need
 * to build complex requests using untestable HTTP verbs as seen in many other REST api implementations.</p>
 *
 * <h2 style="margin-left: 30px; color: #668899">Stateless</h2>
 * <p>One important yet sometimes annoying fact about REST is that it is <em>supposed to be</em> stateless. This means : no session.
 * Hence, this implies that successive calls to the web api cannot share info about the previous call. Each resource should be
 * fully standalone. Remember that this is an API, not a GUI : this means that you may build complex multi-step interfaces and then
 * execute one single request to the API behind the scenes.
 * <br />With respect to security, the fact that REST is stateless means that either we do not care about authentication, or that
 * valid credentials should be passed along every request. (no no, do not fall into generating a session ID that the user must 
 * pass : that is not stateless! (even though in practice, you have the possibility to do so...))</p>
 *
 * <h2 style="margin-left: 30px; color: #668899">Self descriptive</h2>
 * The api is built in such a way that it is rather easy for the designer to document the functionalities, the parameters and so on.
 * There is an anto-generated help page that exposes this information such that any user may browse and understand how to use your methods.
 *
 * <h1 style="border-bottom: 1px solid #99BBCC; color: #668899">Workflow</h1>
 * <h2 style="margin-left: 30px; color: #668899">Initialization</h2>
 * <ol>
 * 		<li>When the very first request comes in, the java application engine (i.e.: Tomcat)
 * 			initializes the servlet. This means that for the first request, all <em>asynchronous</em> 
 * 			initialization may not be complete! Anyhow, the {@link com.anotherservice.rest.core.Servlet#init()} is called
 * 			according to the <code>web.xml</code> file in the <code>servlet-class</code> tag.</li>
 * 		<li>The servlet registers all other <code>init-param</code> of the <code>web.xml</code> using the {@link com.anotherservice.util.Config} class.
 * 			Those variables are registered under the name <code>com.anotherservice.rest.core.*</code>. Thus, if you want some of those to be available
 * 			you may set them here. However, consider using your own config file as explained below.</li>
 * 		<li>The {@link com.anotherservice.util.Logger} is then initialized.</li>
 * 		<li>The {@link com.anotherservice.rest.core.ServiceMonitor} is started to load your custom functionalities.</li>
 * 		<ol>
 * 			<li>It browses for all folders in the <em>services</em> directory (set in <code>web.xml</code>) and in a separate context each time
 * 				, it loads all the jar files present such that there is no conflict with other classes in other folders.</li>
 * 			<li>It looks for the default config file (set in <code>web.xml</code>) and looks for the <em>initializer</em> section inside it.</li>
 * 			<li><strong>Asynchronousely</strong> (in a separate Thread), it creates a new instance of your {@link com.anotherservice.rest.Initializer}.</li>
 * 			<li>You can access your own config file using {@link com.anotherservice.util.JarLoader#getResourceAsStream(String)} that will find it for you.</li>
 * 			<li>You are then supposed to register some {@link com.anotherservice.rest.Action} or {@link com.anotherservice.rest.Index} using 
 * 				{@link com.anotherservice.rest.Handler#addHandler(String, Handler)}.</li>
 * 		</ol>
 * </ol>
 *
 * <h2 style="margin-left: 30px; color: #668899">Requests</h2>
 * <ol>
 * 		<li>First of all, every request is served in a new thread. This means that at all times, you should be very careful about variables and resources you use.
 * 			Even more when knowing that all your handlers will run in a single class instance.</li>
 * 		<li>When a request comes in, independently of the HTTP verb, the incomming parameters are normalized in the {@link com.anotherservice.rest.core.Request}.
 * 			If <code>GET</code> and <code>POST</code> parameters have the same name, they are merged using a semicolon. Also, the URL is split into <em>action</em> parts.</li>
 * 		<li>The {@link com.anotherservice.rest.core.Router#route()} is then called in order to call the apropriate registered handler.</li>
 * 		<li>We switch back to the context of your handler, with your classes, your config and such, and we check the security with 
 * 			{@link com.anotherservice.rest.security.ISecurity#hasGrants(Collection)} using {@link com.anotherservice.rest.Handler#getGrants()}.
 * 			Be sure you have defined some sort of {@link com.anotherservice.rest.security.Security#setInstance(ISecurity)} otherwise, this will raise an exception.
 * 			Remember that security is essential, anywhere, anytime ;-)</li>
 * 		<li>Your method {@link com.anotherservice.rest.Handler#execute()} is called...</li>
 * 		<ol>
 * 			<li>You can get the request parameters using {@link com.anotherservice.rest.Parameter#getValue()}</li>
 * 			<li>Those are then checked using all sort of criteria, multiple values, optional, min/max length and so on...</li>
 * 			<li>You may insert some stuff in the {@link com.anotherservice.db.Database} safely using {@link com.anotherservice.rest.security.Security#escape(String)}.</li>
 * 			<li>You should return the result of your job. Base types are okay, more complex objects should be returned with care.</li>
 * 		</ol>
 * 		<li>Finally, the {@link com.anotherservice.rest.core.Responder} will send your return value in the proper format.</li>
 * </ol>
 *
 * <h2 style="margin-left: 30px; color: #668899">Examples</h2>
 * Now that you have a better view on the workflow, you can just give it a try using the examples provided in the {@link com.anotherservice.rest} package documentation.
 */
package com;
