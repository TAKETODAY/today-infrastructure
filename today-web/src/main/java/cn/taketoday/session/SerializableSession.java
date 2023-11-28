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
