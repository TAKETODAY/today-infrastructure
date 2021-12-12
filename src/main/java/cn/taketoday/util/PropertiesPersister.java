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

package cn.taketoday.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * Strategy interface for persisting {@code java.util.Properties},
 * allowing for pluggable parsing strategies.
 *
 * <p>The default implementation is DefaultPropertiesPersister,
 * providing the native parsing of {@code java.util.Properties},
 * but allowing for reading from any Reader and writing to any Writer
 * (which allows to specify an encoding for a properties file).
 *
 * @author Juergen Hoeller
 * @see DefaultPropertiesPersister
 * @see java.util.Properties
 * @since 4.0
 */
public interface PropertiesPersister {

  /**
   * Load properties from the given InputStream into the given
   * Properties object.
   *
   * @param props the Properties object to load into
   * @param is the InputStream to load from
   * @throws IOException in case of I/O errors
   * @see java.util.Properties#load
   */
  void load(Properties props, InputStream is) throws IOException;

  /**
   * Load properties from the given Reader into the given
   * Properties object.
   *
   * @param props the Properties object to load into
   * @param reader the Reader to load from
   * @throws IOException in case of I/O errors
   */
  void load(Properties props, Reader reader) throws IOException;

  /**
   * Write the contents of the given Properties object to the
   * given OutputStream.
   *
   * @param props the Properties object to store
   * @param os the OutputStream to write to
   * @param header the description of the property list
   * @throws IOException in case of I/O errors
   * @see java.util.Properties#store
   */
  void store(Properties props, OutputStream os, String header) throws IOException;

  /**
   * Write the contents of the given Properties object to the
   * given Writer.
   *
   * @param props the Properties object to store
   * @param writer the Writer to write to
   * @param header the description of the property list
   * @throws IOException in case of I/O errors
   */
  void store(Properties props, Writer writer, String header) throws IOException;

  /**
   * Load properties from the given XML InputStream into the
   * given Properties object.
   *
   * @param props the Properties object to load into
   * @param is the InputStream to load from
   * @throws IOException in case of I/O errors
   * @see java.util.Properties#loadFromXML(java.io.InputStream)
   */
  void loadFromXml(Properties props, InputStream is) throws IOException;

  /**
   * Write the contents of the given Properties object to the
   * given XML OutputStream.
   *
   * @param props the Properties object to store
   * @param os the OutputStream to write to
   * @param header the description of the property list
   * @throws IOException in case of I/O errors
   * @see java.util.Properties#storeToXML(java.io.OutputStream, String)
   */
  void storeToXml(Properties props, OutputStream os, String header) throws IOException;

  /**
   * Write the contents of the given Properties object to the
   * given XML OutputStream.
   *
   * @param props the Properties object to store
   * @param os the OutputStream to write to
   * @param encoding the encoding to use
   * @param header the description of the property list
   * @throws IOException in case of I/O errors
   * @see java.util.Properties#storeToXML(java.io.OutputStream, String, String)
   */
  void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException;

}
