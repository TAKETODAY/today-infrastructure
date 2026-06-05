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
   * Creates a new {@link Session} with a generated unique identifier.
   * <p>The generated identifier is guaranteed to be unique within the scope of this repository.
   * The newly created session is not persisted until {@link #saveOrUpdate(Session)} is called,
   * depending on the implementation strategy.
   *
   * @return the newly created session instance
   * @see #saveOrUpdate(Session)
   * @since 4.0
   */
  Session createSession();

  /**
   * Creates a new {@link Session} with the specified identifier.
   * <p>This method allows for explicit control over the session identifier, which can be useful
   * for session fixation protection or migrating existing sessions. The provided identifier must
   * be unique; if a session with the same ID already exists, the behavior is implementation-specific
   * (typically it may overwrite or throw an exception).
   *
   * @param id the unique identifier for the new session
   * @return the newly created session instance
   * @throws IllegalArgumentException if the id is {@code null} or empty
   * @see #saveOrUpdate(Session)
   * @since 4.0
   */
  Session createSession(String id);

  /**
   * Return the Session for the given id.
   * <p><strong>Note:</strong> This method should perform an expiration check,
   * and if it has expired remove the session and return empty. This method
   * should also update the lastAccessTime of retrieved sessions.
   *
   * @param id the session to load
   * @return the session
   */
  @Nullable
  Session retrieveSession(String id);

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
   * @param id the id of the session to remove
   * @return the removed session, or {@code null} if no session was found with the given id
   */
  @Nullable
  Session remove(String id);

  /**
   * Saves or updates the specified session in the repository.
   * <p>This method persists the current state of the session, including any attribute changes
   * and metadata updates (such as last accessed time). If the session is new, it will be created;
   * if it already exists, its state will be updated.
   *
   * @param session the session instance to save or update; must not be {@code null}
   * @throws IllegalArgumentException if the session is {@code null}
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
