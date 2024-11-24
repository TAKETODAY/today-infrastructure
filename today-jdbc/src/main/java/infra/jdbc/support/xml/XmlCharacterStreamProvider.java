/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.jdbc.support.xml;

import java.io.IOException;
import java.io.Writer;

/**
 * Interface defining handling involved with providing {@code Writer}
 * data for XML input.
 *
 * @author Thomas Risberg
 * @see Writer
 * @since 4.0
 */
public interface XmlCharacterStreamProvider {

  /**
   * Implementations must implement this method to provide the XML content
   * for the {@code Writer}.
   *
   * @param writer the {@code Writer} object being used to provide the XML input
   * @throws IOException if an I/O error occurs while providing the XML
   */
  void provideXml(Writer writer) throws IOException;

}
