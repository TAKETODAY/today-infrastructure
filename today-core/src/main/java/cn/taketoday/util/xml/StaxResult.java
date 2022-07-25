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

import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXResult;

import cn.taketoday.lang.Nullable;

/**
 * Implementation of the {@code Result} tagging interface for StAX writers. Can be constructed with
 * an {@code XMLEventConsumer} or an {@code XMLStreamWriter}.
 *
 * <p>This class is necessary because there is no implementation of {@code Source} for StaxReaders
 * in JAXP 1.3. There is a {@code StAXResult} in JAXP 1.4 (JDK 1.6), but this class is kept around
 * for backwards compatibility reasons.
 *
 * <p>Even though {@code StaxResult} extends from {@code SAXResult}, calling the methods of
 * {@code SAXResult} is <strong>not supported</strong>. In general, the only supported operation
 * on this class is to use the {@code ContentHandler} obtained via {@link #getHandler()} to parse an
 * input source using an {@code XMLReader}. Calling {@link #setHandler(ContentHandler)}
 * or {@link #setLexicalHandler(LexicalHandler)} will result in
 * {@code UnsupportedOperationException}s.
 *
 * @author Arjen Poutsma
 * @see XMLEventWriter
 * @see XMLStreamWriter
 * @see javax.xml.transform.Transformer
 * @since 4.0
 */
class StaxResult extends SAXResult {

  @Nullable
  private XMLEventWriter eventWriter;

  @Nullable
  private XMLStreamWriter streamWriter;

  /**
   * Construct a new instance of the {@code StaxResult} with the specified {@code XMLEventWriter}.
   *
   * @param eventWriter the {@code XMLEventWriter} to write to
   */
  public StaxResult(XMLEventWriter eventWriter) {
    StaxEventHandler handler = new StaxEventHandler(eventWriter);
    super.setHandler(handler);
    super.setLexicalHandler(handler);
    this.eventWriter = eventWriter;
  }

  /**
   * Construct a new instance of the {@code StaxResult} with the specified {@code XMLStreamWriter}.
   *
   * @param streamWriter the {@code XMLStreamWriter} to write to
   */
  public StaxResult(XMLStreamWriter streamWriter) {
    StaxStreamHandler handler = new StaxStreamHandler(streamWriter);
    super.setHandler(handler);
    super.setLexicalHandler(handler);
    this.streamWriter = streamWriter;
  }

  /**
   * Return the {@code XMLEventWriter} used by this {@code StaxResult}.
   * <p>If this {@code StaxResult} was created with an {@code XMLStreamWriter},
   * the result will be {@code null}.
   *
   * @return the StAX event writer used by this result
   * @see #StaxResult(XMLEventWriter)
   */
  @Nullable
  public XMLEventWriter getXMLEventWriter() {
    return this.eventWriter;
  }

  /**
   * Return the {@code XMLStreamWriter} used by this {@code StaxResult}.
   * <p>If this {@code StaxResult} was created with an {@code XMLEventConsumer},
   * the result will be {@code null}.
   *
   * @return the StAX stream writer used by this result
   * @see #StaxResult(XMLStreamWriter)
   */
  @Nullable
  public XMLStreamWriter getXMLStreamWriter() {
    return this.streamWriter;
  }

  /**
   * Throws an {@code UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setHandler(ContentHandler handler) {
    throw new UnsupportedOperationException("setHandler is not supported");
  }

  /**
   * Throws an {@code UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setLexicalHandler(LexicalHandler handler) {
    throw new UnsupportedOperationException("setLexicalHandler is not supported");
  }

}
