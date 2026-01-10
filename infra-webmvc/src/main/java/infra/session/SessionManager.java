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

import infra.web.RequestContext;

/**
 * Main class for access to the {@link Session} for an HTTP request.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SessionIdResolver
 * @see SessionRepository
 * @since 2019-09-27 20:24
 */
public interface SessionManager {

  /**
   * default bean name
   */
  String BEAN_NAME = "sessionManager";

  /**
   * create a new session
   */
  Session createSession();

  /**
   * create a new session associated with {@link RequestContext}
   */
  Session createSession(RequestContext context);

  /**
   * Get a session with given session id
   * <p>
   * If there is not a session,create one.
   * </p>
   */
  @Nullable
  Session getSession(@Nullable String sessionId);

  /**
   * Returns the current session associated with this request, or if the request
   * does not have a session, creates one.
   *
   * @param context Current request
   * @return the <code>Session</code> associated with this request
   * @see #getSession(RequestContext, boolean)
   */
  Session getSession(RequestContext context);

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
  Session getSession(RequestContext context, boolean create);

}
