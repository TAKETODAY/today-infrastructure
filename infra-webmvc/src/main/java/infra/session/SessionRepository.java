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
   * Remove the specified session.
   * <p>This method removes the session from the repository and invalidates it.
   * Implementations should ensure that any associated resources are cleaned up.
   *
   * @param session the session instance to remove
   */
  void remove(Session session);

  /**
   * Remove the session with the specified id.
   * <p>This method removes the session from the repository and invalidates it.
   * Implementations should ensure that any associated resources are cleaned up.
   *
   * @param sessionId the id of the session to remove
   * @return the removed session, or {@code null} if no session was found with the given id
   */
  @Nullable
  Session remove(String sessionId);

  /**
   * Save or update the specified session.
   * <p>This method handles both creating new sessions and updating existing ones.
   * For new sessions, the session must have been started (via {@link Session#start()}
   * or by adding attributes). For existing sessions, this updates the persisted state.
   * <p><strong>Note:</strong> If the session ID has been changed via
   * {@link Session#changeSessionId()}, this method will handle the necessary
   * cleanup of the old session identifier.
   *
   * @param session the session to save or update
   * @since 5.0
   */
  void saveOrUpdate(Session session);

  /**
   * Check if a session with the given id exists.
   *
   * @param id the session id to check
   * @return {@code true} if the session exists, {@code false} otherwise
   * @since 4.0
   */
  boolean contains(String id);

  /**
   * Return the number of active sessions.
   *
   * @return the count of sessions
   * @since 4.0
   */
  int getSessionCount();

  /**
   * Return all active session identifiers.
   *
   * @return an array of session ids
   * @since 4.0
   */
  String[] getIdentifiers();

  /**
   * Update the last accessed time of the given session to the current time.
   * <p>This method is typically called to refresh the session's activity timestamp,
   * preventing it from expiring due to inactivity. Implementations should ensure
   * that this update is persisted if the session is stored in an external store.
   *
   * @param session the session whose last accessed time needs to be updated
   * @since 4.0
   */
  void updateLastAccessTime(Session session);

}
