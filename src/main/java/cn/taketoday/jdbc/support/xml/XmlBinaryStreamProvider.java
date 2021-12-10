/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.support.xml;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface defining handling involved with providing {@code OutputStream}
 * data for XML input.
 *
 * @author Thomas Risberg
 * @see OutputStream
 * @since 4.0
 */
public interface XmlBinaryStreamProvider {

  /**
   * Implementations must implement this method to provide the XML content
   * for the {@code OutputStream}.
   *
   * @param outputStream the {@code OutputStream} object being used to provide the XML input
   * @throws IOException if an I/O error occurs while providing the XML
   */
  void provideXml(OutputStream outputStream) throws IOException;

}
