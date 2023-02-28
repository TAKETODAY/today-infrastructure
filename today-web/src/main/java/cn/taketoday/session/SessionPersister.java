/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.io.IOException;

import cn.taketoday.lang.Nullable;

/**
 * A <b>SessionPersister</b> that provides persistent storage and loading
 * of Sessions and their associated user data.
 * Implementations are free to save and load the Sessions to any media they
 * wish, but it is assumed that saved Sessions are persistent across
 * server or context restarts.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/26 23:04
 */
public interface SessionPersister {

  /**
   * Remove all Sessions from this Store.
   *
   * @throws IOException if an input/output error occurs
   */
  void clear() throws IOException;

  /**
   * Remove the Session with the specified session identifier from
   * this Store, if present.  If no such Session is present, this method
   * takes no action.
   *
   * @param id Session identifier of the Session to be removed
   */
  void remove(String id) throws IOException;

  /**
   * @return an array containing the session identifiers of all Sessions
   * currently saved in this Store.  If there are no such Sessions, a
   * zero-length array is returned.
   */
  String[] keys();

  /**
   * Load and return the Session associated with the specified session
   * identifier from this Store, without removing it.  If there is no
   * such stored Session, return <code>null</code>.
   *
   * @param id Session identifier of the session to load
   * @return the loaded Session instance
   * @throws ClassNotFoundException if a deserialization error occurs
   * @throws IOException if an input/output error occurs
   */
  @Nullable
  WebSession load(String id) throws ClassNotFoundException, IOException;

  /**
   * Save the specified Session into this Store.  Any previously saved
   * information for the associated session identifier is replaced.
   *
   * @param session Session to be saved
   * @throws IOException if an input/output error occurs
   */
  void save(WebSession session) throws IOException;

}
