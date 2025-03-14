/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.mock.api.http;

import java.util.Enumeration;

import infra.mock.api.MockContext;

/**
 * Provides a way to identify a user across more than one page request or visit to a Web site and to store information
 * about that user.
 *
 * <p>
 * The servlet container uses this interface to create a session between an HTTP client and an HTTP server. The session
 * persists for a specified time period, across more than one connection or page request from the user. A session
 * usually corresponds to one user, who may visit a site many times. The server can maintain a session in many ways such
 * as using cookies or rewriting URLs.
 *
 * <p>
 * This interface allows servlets to
 * <ul>
 * <li>View and manipulate information about a session, such as the session identifier, creation time, and last accessed
 * time
 * <li>Bind objects to sessions, allowing user information to persist across multiple user connections
 * </ul>
 *
 * <p>
 * When an application stores an object in or removes an object from a session, the session checks whether the object
 * implements {@link HttpSessionBindingListener}. If it does, the servlet notifies the object that it has been bound to
 * or unbound from the session. Notifications are sent after the binding methods complete. For session that are
 * invalidated or expire, notifications are sent after the session has been invalidated or expired.
 *
 * <p>
 * When container migrates a session between VMs in a distributed container setting, all session attributes implementing
 * the {@link HttpSessionActivationListener} interface are notified.
 *
 * <p>
 * A servlet should be able to handle cases in which the client does not choose to join a session, such as when cookies
 * are intentionally turned off. Until the client joins the session, <code>isNew</code> returns <code>true</code>. If
 * the client chooses not to join the session, <code>getSession</code> will return a different session on each request,
 * and <code>isNew</code> will always return <code>true</code>.
 *
 * <p>
 * Session information is scoped only to the current web application (<code>MockContext</code>), so information
 * stored in one context will not be directly visible in another.
 *
 * @author Various
 * @see HttpSessionBindingListener
 */
public interface HttpSession {

  /**
   * Returns the time when this session was created, measured in milliseconds since midnight January 1, 1970 GMT.
   *
   * @return a <code>long</code> specifying when this session was created, expressed in milliseconds since 1/1/1970 GMT
   * @throws IllegalStateException if this method is called on an invalidated session
   */
  long getCreationTime();

  /**
   * Returns a string containing the unique identifier assigned to this session. The identifier is assigned by the servlet
   * container and is implementation dependent.
   *
   * @return a string specifying the identifier assigned to this session
   */
  String getId();

  /**
   * Returns the last time the client sent a request associated with this session, as the number of milliseconds since
   * midnight January 1, 1970 GMT, and marked by the time the container received the request.
   *
   * <p>
   * Actions that your application takes, such as getting or setting a value associated with the session, do not affect
   * the access time.
   *
   * @return a <code>long</code> representing the last time the client sent a request associated with this session,
   * expressed in milliseconds since 1/1/1970 GMT
   * @throws IllegalStateException if this method is called on an invalidated session
   */
  long getLastAccessedTime();

  /**
   * Returns the MockContext to which this session belongs.
   *
   * @return The MockContext object for the web application
   */
  MockContext getMockContext();

  /**
   * Specifies the time, in seconds, between client requests before the servlet container will invalidate this session.
   *
   * <p>
   * An <tt>interval</tt> value of zero or less indicates that the session should never timeout.
   *
   * @param interval An integer specifying the number of seconds
   */
  void setMaxInactiveInterval(int interval);

  /**
   * Returns the maximum time interval, in seconds, that the servlet container will keep this session open between client
   * accesses. After this interval, the servlet container will invalidate the session. The maximum time interval can be
   * set with the <code>setMaxInactiveInterval</code> method.
   *
   * <p>
   * A return value of zero or less indicates that the session will never timeout.
   *
   * @return an integer specifying the number of seconds this session remains open between client requests
   * @see #setMaxInactiveInterval
   */
  int getMaxInactiveInterval();

  /**
   * Returns the object bound with the specified name in this session, or <code>null</code> if no object is bound under
   * the name.
   *
   * @param name a string specifying the name of the object
   * @return the object with the specified name
   * @throws IllegalStateException if this method is called on an invalidated session
   */
  Object getAttribute(String name);

  /**
   * Returns an <code>Enumeration</code> of <code>String</code> objects containing the names of all the objects bound to
   * this session.
   *
   * @return an <code>Enumeration</code> of <code>String</code> objects specifying the names of all the objects bound to
   * this session
   * @throws IllegalStateException if this method is called on an invalidated session
   */
  Enumeration<String> getAttributeNames();

  /**
   * Binds an object to this session, using the name specified. If an object of the same name is already bound to the
   * session, the object is replaced.
   *
   * <p>
   * After this method executes, and if the new object implements <code>HttpSessionBindingListener</code>, the container
   * calls <code>HttpSessionBindingListener.valueBound</code>. The container then notifies any
   * <code>HttpSessionAttributeListener</code>s in the web application.
   *
   * <p>
   * If an object was already bound to this session of this name that implements <code>HttpSessionBindingListener</code>,
   * its <code>HttpSessionBindingListener.valueUnbound</code> method is called.
   *
   * <p>
   * If the value passed in is null, this has the same effect as calling <code>removeAttribute()</code>.
   *
   * @param name the name to which the object is bound; cannot be null
   * @param value the object to be bound
   * @throws IllegalStateException if this method is called on an invalidated session
   */
  void setAttribute(String name, Object value);

  /**
   * Removes the object bound with the specified name from this session. If the session does not have an object bound with
   * the specified name, this method does nothing.
   *
   * <p>
   * After this method executes, and if the object implements <code>HttpSessionBindingListener</code>, the container calls
   * <code>HttpSessionBindingListener.valueUnbound</code>. The container then notifies any
   * <code>HttpSessionAttributeListener</code>s in the web application.
   *
   * @param name the name of the object to remove from this session
   * @throws IllegalStateException if this method is called on an invalidated session
   */
  void removeAttribute(String name);

  /**
   * Invalidates this session then unbinds any objects bound to it.
   *
   * @throws IllegalStateException if this method is called on an already invalidated session
   */
  void invalidate();

  /**
   * Returns <code>true</code> if the client does not yet know about the session or if the client chooses not to join the
   * session. For example, if the server used only cookie-based sessions, and the client had disabled the use of cookies,
   * then a session would be new on each request.
   *
   * @return <code>true</code> if the server has created a session, but the client has not yet joined
   * @throws IllegalStateException if this method is called on an already invalidated session
   */
  boolean isNew();

}
