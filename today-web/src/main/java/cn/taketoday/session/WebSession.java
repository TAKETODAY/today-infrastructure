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

import cn.taketoday.core.AttributeAccessor;
import reactor.core.publisher.Mono;

/**
 * Main contract for using a server-side session that provides access to session
 * attributes across HTTP requests.
 *
 * <p>The creation of a {@code WebSession} instance does not automatically start
 * a session thus causing the session id to be sent to the client (typically via
 * a cookie). A session starts implicitly when session attributes are added.
 * A session may also be created explicitly via {@link #start()}.
 *
 * @author TODAY <br>
 * @since 2019-09-27 20:16
 */
public interface WebSession extends AttributeAccessor {

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
   * Save the session through the {@code WebSessionStore} as follows:
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

}
