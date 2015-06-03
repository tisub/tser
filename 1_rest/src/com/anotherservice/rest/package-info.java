/**
 * This package englobes all the bricks to build a REST web service.
 * <br />Have a look to the different classes it contains, but for a quick deployment,
 * here are two basic samples :<br />
 * <pre>
 * public class HelloWorldService extends InitializerOnce
 * {
 *	public void initialize()
 *	{
 *		Action action = new Action("hello", "This is a simple service", "returns Hello World")
 *		{
 *			public Object execute() throws Exception
 *			{
 *				return "Hello World!";
 *			}
 *		};
 *		Handler.addHandler("/", action);
 *	}
 * }
 * </pre>
 * <br />You can then call this service using the URL : http:// <span style="color: #999999; font-style: italic;">[server]</span> /
 * <span style="color: #999999; font-style: italic;">[servlet]</span> /hello<br />
 * You can get the help page of this service using the URL : http:// <span style="color: #999999; font-style: italic;">[server]</span> /
 * <span style="color: #999999; font-style: italic;">[servlet]</span> /hello/help
 * <hr />
 * <pre>
 * public class ParameterizedService extends InitializerOnce
 * {
 *	public void initialize()
 *	{
 *		Action action = new Action("greet", "This service greets you", "returns Hello [you]")
 *		{
 *			public Object execute() throws Exception
 *			{
 *				return "Hello " + getParameter("name").getValue();
 *			}
 *		};
 *
 *		action.addParameter(new Parameter("name"));
 *		Handler.addHandler("/", action);
 *	}
 * }
 * </pre>
 * <br />You can then call this service using the URL : http:// <span style="color: #999999; font-style: italic;">[server]</span> /
 * <span style="color: #999999; font-style: italic;">[servlet]</span> /greet?name=John<br />
 * You can get the help page of this service using the URL : http:// <span style="color: #999999; font-style: italic;">[server]</span> /
 * <span style="color: #999999; font-style: italic;">[servlet]</span> /greet/help
 */
package com.anotherservice.rest;
