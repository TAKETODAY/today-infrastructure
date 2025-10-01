/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.session;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * SessionManager
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
   * @return the <code>WebSession</code> associated with this request
   * @see #getSession(RequestContext, boolean)
   */
  public WebSession getSession(RequestContext context) {
    return sessionManager.getSession(context);
  }

  /**
   * Returns the current <code>WebSession</code> associated with this request or,
   * if there is no current session and <code>create</code> is true, returns a new
   * session.
   *
   * <p>
   * If <code>create</code> is <code>false</code> and the request has no valid
   * <code>WebSession</code>, this method returns <code>null</code>.
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
   * @return the <code>WebSession</code> associated with this request or
   * <code>null</code> if <code>create</code> is <code>false</code> and
   * the request has no valid session
   * @see #getSession(RequestContext)
   */
  @Nullable
  public WebSession getSession(RequestContext context, boolean create) {
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
  public Object getAttribute(WebSession session, String name) {
    return session.getAttribute(name);
  }

  @Nullable
  public Object getAttribute(RequestContext context, String name) {
    WebSession session = getSession(context, false);
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
    WebSession session = getSession(context, false);
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
    WebSession session = getSession(context, false);
    if (session != null) {
      return session.removeAttribute(name);
    }
    return null;
  }
}
