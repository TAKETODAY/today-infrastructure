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
