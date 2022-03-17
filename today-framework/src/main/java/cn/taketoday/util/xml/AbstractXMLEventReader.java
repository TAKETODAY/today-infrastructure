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

package cn.taketoday.util.xml;

import java.util.NoSuchElementException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import cn.taketoday.util.ClassUtils;

/**
 * Abstract base class for {@code XMLEventReader}s.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 4.0
 */
abstract class AbstractXMLEventReader implements XMLEventReader {

  private boolean closed;

  @Override
  public Object next() {
    try {
      return nextEvent();
    }
    catch (XMLStreamException ex) {
      throw new NoSuchElementException(ex.getMessage());
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException(
            "remove not supported on " + ClassUtils.getShortName(getClass()));
  }

  /**
   * This implementation throws an {@code IllegalArgumentException} for any property.
   *
   * @throws IllegalArgumentException when called
   */
  @Override
  public Object getProperty(String name) throws IllegalArgumentException {
    throw new IllegalArgumentException("Property not supported: [" + name + "]");
  }

  @Override
  public void close() {
    this.closed = true;
  }

  /**
   * Check if the reader is closed, and throws a {@code XMLStreamException} if so.
   *
   * @throws XMLStreamException if the reader is closed
   * @see #close()
   */
  protected void checkIfClosed() throws XMLStreamException {
    if (this.closed) {
      throw new XMLStreamException("XMLEventReader has been closed");
    }
  }

}
