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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/27 22:42
 */
public interface SerializableSession {

  /**
   * Write a serialized version of the contents of this session object to
   * the specified object output stream, without requiring that the
   * StandardSession itself have been serialized.
   *
   * @param stream The object output stream to write to
   * @throws IOException if an input/output error occurs
   */
  void writeObjectData(ObjectOutputStream stream)
          throws IOException;

  /**
   * Read a serialized version of the contents of this session object from
   * the specified object input stream, without requiring that the
   * StandardSession itself have been serialized.
   *
   * @param stream The object input stream to read from
   * @throws ClassNotFoundException if an unknown class is specified
   * @throws IOException if an input/output error occurs
   */
  void readObjectData(ObjectInputStream stream)
          throws ClassNotFoundException, IOException;

}
