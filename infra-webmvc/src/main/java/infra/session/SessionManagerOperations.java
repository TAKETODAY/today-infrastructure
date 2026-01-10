/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.session;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * Central abstraction for session operations.
 *
 * <p>This class provides a convenient base for session management operations,
 * delegating to a configured {@link SessionManager} instance. It offers
 * methods for session retrieval, attribute management, and other common
 * session operations.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2021/4/30 23:01
 */
public class SessionManagerOperations {

  protected final SessionManager sessionManager;

  public SessionManagerOperations(SessionManager sessionManager) {
    Assert.notNull(sessionManager, "SessionManager is required");
    this.sessionManager = sessionManager;
  }

  public final SessionManager getSessionManager() {
    return sessionManager;
  }

  /**
   * Returns the current session associated with this request, or if the request
   * does not have a session, creates one.
   *
   * @param context Current request
   * @return the <code>Session</code> associated with this request
   * @see #getSession(RequestContext, boolean)
   */
  public Session getSession(RequestContext context) {
    return sessionManager.getSession(context);
  }

  /**
   * Returns the current <code>Session</code> associated with this request or,
   * if there is no current session and <code>create</code> is true, returns a new
   * session.
   *
   * <p>
   * If <code>create</code> is <code>false</code> and the request has no valid
   * <code>Session</code>, this method returns <code>null</code>.
   *
   * <p>
   * To make sure the session is properly maintained, you must call this method
   * before the response is committed. If the container is using cookies to
   * maintain session integrity and is asked to create a new session when the
   * response is committed, an IllegalStateException is thrown.
   *
   * @param context Current request
   * @param create <code>true</code> to create a new session for this request if
   * necessary; <code>false</code> to return <code>null</code> if
   * there's no current session
   * @return the <code>Session</code> associated with this request or
   * <code>null</code> if <code>create</code> is <code>false</code> and
   * the request has no valid session
   * @see #getSession(RequestContext)
   */
  @Nullable
  public Session getSession(RequestContext context, boolean create) {
    return sessionManager.getSession(context, create);
  }

  /**
   * Get the value of the attribute identified by {@code name}. Return
   * {@code null} if the attribute doesn't exist.
   *
   * @param name the unique attribute key
   * @return the current value of the attribute, if any
   */
  @Nullable
  public Object getAttribute(Session session, String name) {
    return session.getAttribute(name);
  }

  @Nullable
  public Object getAttribute(RequestContext context, String name) {
    Session session = getSession(context, false);
    if (session != null) {
      return getAttribute(session, name);
    }
    return null;
  }

  /**
   * Set the attribute defined by {@code name} to the supplied {@code value}. If
   * {@code value} is {@code null}, the attribute is {@link #removeAttribute
   * removed}.
   * <p>
   * In general, users should take care to prevent overlaps with other metadata
   * attributes by using fully-qualified names, perhaps using class or package
   * names as prefix.
   *
   * @param name the unique attribute key
   * @param attribute the attribute value to be attached
   * @since 4.0
   */
  public void setAttribute(RequestContext context, String name, @Nullable Object attribute) {
    Session session = getSession(context, false);
    if (session != null) {
      session.setAttribute(name, attribute);
    }
  }

  /**
   * Remove the attribute identified by {@code name} and return its value. Return
   * {@code null} if no attribute under {@code name} is found.
   *
   * @param name the unique attribute key
   * @return the last value of the attribute, if any
   * @since 4.0
   */
  @Nullable
  public Object removeAttribute(RequestContext context, String name) {
    Session session = getSession(context, false);
    if (session != null) {
      return session.removeAttribute(name);
    }
    return null;
  }
}
