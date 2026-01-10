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

import java.io.IOException;

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
   * contains session with given id
   */
  boolean contains(String id);

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
  Session findById(String id) throws ClassNotFoundException, IOException;

  /**
   * Save the specified Session into this Store.  Any previously saved
   * information for the associated session identifier is replaced.
   *
   * @param session Session to be saved
   * @throws IOException if an input/output error occurs
   */
  void persist(Session session) throws IOException;

}
