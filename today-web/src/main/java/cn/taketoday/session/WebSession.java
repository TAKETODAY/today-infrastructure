/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ArrayIterator;

/**
 * Main contract for using a server-side session that provides access to session
 * attributes across HTTP requests.
 *
 * <p>The creation of a {@code WebSession} instance does not automatically start
 * a session thus causing the session id to be sent to the client (typically via
 * a cookie). A session starts implicitly when session attributes are added.
 * A session may also be created explicitly via {@link #start()}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AttributeBindingListener
 * @since 2019-09-27 20:16
 */
public interface WebSession {

  /**
   * returns this session's id
   */
  String getId();

  /**
   * Force the creation of a session causing the session id to be sent when
   * {@link #save()} is called.
   */
  void start();

  /**
   * Whether a session with the client has been started explicitly via
   * {@link #start()} or implicitly by adding session attributes.
   * If "false" then the session id is not sent to the client and the
   * {@link #save()} method is essentially a no-op.
   */
  boolean isStarted();

  /**
   * Generate a new id for the session and update the underlying session
   * storage to reflect the new id. After a successful call {@link #getId()}
   * reflects the new session id.
   */
  void changeSessionId();

  /**
   * Save the session through the {@code SessionRepository} as follows:
   * <ul>
   * <li>If the session is new (i.e. created but never persisted), it must have
   * been started explicitly via {@link #start()} or implicitly by adding
   * attributes, or otherwise this method should have no effect.
   * <li>If the session was retrieved through the {@code WebSessionStore},
   * the implementation for this method must check whether the session was
   * {@link #invalidate() invalidated} and if so return an error.
   * </ul>
   * <p>Note that this method is not intended for direct use by applications.
   * Instead it is automatically invoked just before the response is
   * committed.
   */
  void save();

  /**
   * Invalidate the current session and clear session storage.
   */
  void invalidate();

  /**
   * Return {@code true} if the session expired after {@link #getMaxIdleTime()
   * maxIdleTime} elapsed.
   * <p>Typically expiration checks should be automatically made when a session
   * is accessed, a new {@code WebSession} instance created if necessary, at
   * the start of request processing so that applications don't have to worry
   * about expired session by default.
   */
  boolean isExpired();

  /**
   * Return the time when the session was created.
   */
  Instant getCreationTime();

  /**
   * Return the last time of session access as a result of user activity such
   * as an HTTP request. Together with {@link #getMaxIdleTime()
   * maxIdleTimeInSeconds} this helps to determine when a session is
   * {@link #isExpired() expired}.
   */
  Instant getLastAccessTime();

  /**
   * Sets the last accessed time.
   *
   * @param lastAccessTime the last accessed time
   */
  void setLastAccessTime(Instant lastAccessTime);

  /**
   * Configure the max amount of time that may elapse after the
   * {@link #getLastAccessTime() lastAccessTime} before a session is considered
   * expired. A negative value indicates the session should not expire.
   */
  void setMaxIdleTime(Duration maxIdleTime);

  /**
   * Return the maximum time after the {@link #getLastAccessTime()
   * lastAccessTime} before a session expires. A negative time indicates the
   * session doesn't expire.
   */
  Duration getMaxIdleTime();

  // attribute

  /**
   * Binds an object to this session, using the name specified.
   * If an object of the same name is already bound to the session,
   * the object is replaced.
   *
   * <p>
   * After this method executes, and if the new object implements
   * <code>AttributeBindingListener</code>, the container calls
   * <code>AttributeBindingListener.valueBound</code>. The container
   * then notifies any <code>HttpSessionAttributeListener</code>s
   * in the web application.
   *
   * <p>
   * If an object was already bound to this session of this name that
   * implements <code>AttributeBindingListener</code>, its <code>
   * AttributeBindingListener.valueUnbound</code> method is called.
   *
   * <p>
   * If the value passed in is null, this has the same effect as calling
   * <code>removeAttribute()</code>.
   *
   * @param name the name to which the object is bound; cannot be null
   * @param value the object to be bound
   */
  void setAttribute(String name, @Nullable Object value);

  /**
   * Returns the object bound with the specified name in this session,
   * or <code>null</code> if no object is bound under the name.
   *
   * @param name a string specifying the name of the object
   * @return the object with the specified name
   */
  @Nullable
  Object getAttribute(String name);

  /**
   * Removes the object bound with the specified name from this session.
   * If the session does not have an object bound with the specified name,
   * this method does nothing.
   *
   * <p>
   * After this method executes, and if the object implements <code>
   * AttributeBindingListener</code>, the container calls <code>
   * AttributeBindingListener.valueUnbound </code>. The container
   * then notifies any <code>WebSessionAttributeListener</code>s
   * in the web application.
   *
   * @param name the name of the object to remove from this session
   */
  @Nullable
  Object removeAttribute(String name);

  /**
   * Return {@code true} if the attribute identified by {@code name} exists.
   * Otherwise return {@code false}.
   *
   * @param name the unique attribute key
   */
  boolean hasAttribute(String name);

  /**
   * Return the names of all attributes.
   */
  String[] getAttributeNames();

  /**
   * Return the names Iterator.
   *
   * @since 4.0
   */
  default Iterator<String> attributeNames() {
    return new ArrayIterator<>(getAttributeNames());
  }

  /**
   * Returns {@code true} if this map contains no key-value mappings.
   *
   * @return {@code true} if this map contains no key-value mappings
   * @since 4.0
   */
  boolean hasAttributes();

  /**
   * Return attributes map
   *
   * @return attributes map
   */
  Map<String, Object> getAttributes();

}
