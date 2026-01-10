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

/**
 * Allowing for different storage strategies such as in-memory, database-backed,
 * or distributed cache-based implementations.
 * <p>
 * Implementations of this interface are responsible for the entire lifecycle of a
 * session, including its creation, retrieval, update, and deletion. They must
 * also handle session expiration to ensure that stale sessions are properly
 * cleaned up.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Session
 * @since 2019-09-28 10:26
 */
public interface SessionRepository {

  /**
   * Create a new Session.
   * <p>Note that this does nothing more than create a new instance.
   * The session can later be started explicitly via {@link Session#start()}
   * or implicitly by adding attributes -- and then persisted via
   * {@link Session#save()}.
   *
   * @return the created session instance
   * @since 4.0
   */
  Session createSession();

  /**
   * Create a new Session with given session id.
   * <p>Note that this does nothing more than create a new instance.
   * The session can later be started explicitly via {@link Session#start()}
   * or implicitly by adding attributes -- and then persisted via
   * {@link Session#save()}.
   *
   * @return the created session instance
   * @since 4.0
   */
  Session createSession(String id);

  /**
   * Return the Session for the given id.
   * <p><strong>Note:</strong> This method should perform an expiration check,
   * and if it has expired remove the session and return empty. This method
   * should also update the lastAccessTime of retrieved sessions.
   *
   * @param sessionId the session to load
   * @return the session
   */
  @Nullable
  Session retrieveSession(String sessionId);

  /**
   * Remove the Session for the specified instance.
   *
   * @param session the instance of the session to remove
   */
  void removeSession(Session session);

  /**
   * Remove the Session for the specified id.
   *
   * @param sessionId the id of the session to remove
   * @return an old session
   */
  @Nullable
  Session removeSession(String sessionId);

  /**
   * Update the last accessed timestamp to "now".
   *
   * @param session the session to update
   * @since 4.0
   */
  void updateLastAccessTime(Session session);

  boolean contains(String id);

  /**
   * @return the count of sessions
   * @since 4.0
   */
  int getSessionCount();

  /**
   * @return all session ids
   * @since 4.0
   */
  String[] getIdentifiers();

}
