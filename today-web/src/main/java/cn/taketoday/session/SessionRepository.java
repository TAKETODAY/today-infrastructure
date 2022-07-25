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

import cn.taketoday.lang.Nullable;

/**
 * {@link WebSession} Storage
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-09-28 10:26
 */
public interface SessionRepository {

  /**
   * Create a new WebSession.
   * <p>Note that this does nothing more than create a new instance.
   * The session can later be started explicitly via {@link WebSession#start()}
   * or implicitly by adding attributes -- and then persisted via
   * {@link WebSession#save()}.
   *
   * @return the created session instance
   * @since 4.0
   */
  WebSession createSession();

  /**
   * Return the WebSession for the given id.
   * <p><strong>Note:</strong> This method should perform an expiration check,
   * and if it has expired remove the session and return empty. This method
   * should also update the lastAccessTime of retrieved sessions.
   *
   * @param sessionId the session to load
   * @return the session
   */
  @Nullable
  WebSession retrieveSession(String sessionId);

  /**
   * Remove the WebSession for the specified instance.
   *
   * @param session the instance of the session to remove
   */
  default void removeSession(WebSession session) {
    removeSession(session.getId());
  }

  /**
   * Remove the WebSession for the specified id.
   *
   * @param sessionId the id of the session to remove
   * @return an old session
   */
  @Nullable
  WebSession removeSession(String sessionId);

  /**
   * Update the last accessed timestamp to "now".
   *
   * @param webSession the session to update
   * @since 4.0
   */
  void updateLastAccessTime(WebSession webSession);

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
