/**
 * This package exposes the basic classes and interfaces accessible to connector developpers.
 * <br /><br /><span style="font-size: 5em; color: red;">Read this !</span>
 * <br /><br /><strong>First step :</strong> take a look at what you should implement using 
 * <ul>
 * <li>{@link com.busit.Consumer}</li>
 * <li>{@link com.busit.Producer}</li>
 * <li>{@link com.busit.Transformer}</li>
 * </ul>
 * <strong>Second step :</strong> take a look at message types for maximum compatibility with {@link com.busit.IContent}.
 * <br /><br /><strong>Advanced topic :</strong> if you wish to return multiple messages at once, you can use the {@link com.busit.IMessageList} 
 * which is itself an {@link com.busit.IMessage}.
 * <br />.
 */
package com.busit;