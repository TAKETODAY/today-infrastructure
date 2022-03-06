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

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;

import cn.taketoday.lang.Nullable;

/**
 * Implementation of the {@code Source} tagging interface for StAX readers. Can be constructed with
 * an {@code XMLEventReader} or an {@code XMLStreamReader}.
 *
 * <p>This class is necessary because there is no implementation of {@code Source} for StAX Readers
 * in JAXP 1.3. There is a {@code StAXSource} in JAXP 1.4 (JDK 1.6), but this class is kept around
 * for backwards compatibility reasons.
 *
 * <p>Even though {@code StaxSource} extends from {@code SAXSource}, calling the methods of
 * {@code SAXSource} is <strong>not supported</strong>. In general, the only supported operation
 * on this class is to use the {@code XMLReader} obtained via {@link #getXMLReader()} to parse the
 * input source obtained via {@link #getInputSource()}. Calling {@link #setXMLReader(XMLReader)}
 * or {@link #setInputSource(InputSource)} will result in {@code UnsupportedOperationException #setInputSource(InputSource)} will result in {@code UnsupportedOperationExceptions}.
 *
 * @author Arjen Poutsma
 * @see XMLEventReader
 * @see XMLStreamReader
 * @see javax.xml.transform.Transformer
 * @since 4.0
 */
class StaxSource extends SAXSource {

  @Nullable
  private XMLEventReader eventReader;

  @Nullable
  private XMLStreamReader streamReader;

  /**
   * Construct a new instance of the {@code StaxSource} with the specified {@code XMLEventReader}.
   * The supplied event reader must be in {@code XMLStreamConstants.START_DOCUMENT} or
   * {@code XMLStreamConstants.START_ELEMENT} state.
   *
   * @param eventReader the {@code XMLEventReader} to read from
   * @throws IllegalStateException if the reader is not at the start of a document or element
   */
  StaxSource(XMLEventReader eventReader) {
    super(new StaxEventXMLReader(eventReader), new InputSource());
    this.eventReader = eventReader;
  }

  /**
   * Construct a new instance of the {@code StaxSource} with the specified {@code XMLStreamReader}.
   * The supplied stream reader must be in {@code XMLStreamConstants.START_DOCUMENT} or
   * {@code XMLStreamConstants.START_ELEMENT} state.
   *
   * @param streamReader the {@code XMLStreamReader} to read from
   * @throws IllegalStateException if the reader is not at the start of a document or element
   */
  StaxSource(XMLStreamReader streamReader) {
    super(new StaxStreamXMLReader(streamReader), new InputSource());
    this.streamReader = streamReader;
  }

  /**
   * Return the {@code XMLEventReader} used by this {@code StaxSource}.
   * <p>If this {@code StaxSource} was created with an {@code XMLStreamReader},
   * the result will be {@code null}.
   *
   * @return the StAX event reader used by this source
   * @see StaxSource#StaxSource(XMLEventReader)
   */
  @Nullable
  XMLEventReader getXMLEventReader() {
    return this.eventReader;
  }

  /**
   * Return the {@code XMLStreamReader} used by this {@code StaxSource}.
   * <p>If this {@code StaxSource} was created with an {@code XMLEventReader},
   * the result will be {@code null}.
   *
   * @return the StAX event reader used by this source
   * @see StaxSource#StaxSource(XMLEventReader)
   */
  @Nullable
  XMLStreamReader getXMLStreamReader() {
    return this.streamReader;
  }

  /**
   * Throws an {@code UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setInputSource(InputSource inputSource) {
    throw new UnsupportedOperationException("setInputSource is not supported");
  }

  /**
   * Throws an {@code UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setXMLReader(XMLReader reader) {
    throw new UnsupportedOperationException("setXMLReader is not supported");
  }

}
