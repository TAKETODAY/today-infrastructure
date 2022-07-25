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
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import cn.taketoday.lang.Nullable;

/**
 * Abstract base class for SAX {@code XMLReader} implementations.
 * Contains properties as defined in {@link XMLReader}, and does not recognize any features.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see #setContentHandler(ContentHandler)
 * @see #setDTDHandler(DTDHandler)
 * @see #setEntityResolver(EntityResolver)
 * @see #setErrorHandler(ErrorHandler)
 * @since 4.0
 */
abstract class AbstractXMLReader implements XMLReader {

  @Nullable
  private DTDHandler dtdHandler;

  @Nullable
  private ContentHandler contentHandler;

  @Nullable
  private EntityResolver entityResolver;

  @Nullable
  private ErrorHandler errorHandler;

  @Nullable
  private LexicalHandler lexicalHandler;

  @Override
  public void setContentHandler(@Nullable ContentHandler contentHandler) {
    this.contentHandler = contentHandler;
  }

  @Override
  @Nullable
  public ContentHandler getContentHandler() {
    return this.contentHandler;
  }

  @Override
  public void setDTDHandler(@Nullable DTDHandler dtdHandler) {
    this.dtdHandler = dtdHandler;
  }

  @Override
  @Nullable
  public DTDHandler getDTDHandler() {
    return this.dtdHandler;
  }

  @Override
  public void setEntityResolver(@Nullable EntityResolver entityResolver) {
    this.entityResolver = entityResolver;
  }

  @Override
  @Nullable
  public EntityResolver getEntityResolver() {
    return this.entityResolver;
  }

  @Override
  public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  @Override
  @Nullable
  public ErrorHandler getErrorHandler() {
    return this.errorHandler;
  }

  @Nullable
  protected LexicalHandler getLexicalHandler() {
    return this.lexicalHandler;
  }

  /**
   * This implementation throws a {@code SAXNotRecognizedException} exception
   * for any feature outside of the "http://xml.org/sax/features/" namespace
   * and returns {@code false} for any feature within.
   */
  @Override
  public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
    if (name.startsWith("http://xml.org/sax/features/")) {
      return false;
    }
    else {
      throw new SAXNotRecognizedException(name);
    }
  }

  /**
   * This implementation throws a {@code SAXNotRecognizedException} exception
   * for any feature outside of the "http://xml.org/sax/features/" namespace
   * and accepts a {@code false} value for any feature within.
   */
  @Override
  public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
    if (name.startsWith("http://xml.org/sax/features/")) {
      if (value) {
        throw new SAXNotSupportedException(name);
      }
    }
    else {
      throw new SAXNotRecognizedException(name);
    }
  }

  /**
   * Throws a {@code SAXNotRecognizedException} exception when the given property does not signify a lexical
   * handler. The property name for a lexical handler is {@code http://xml.org/sax/properties/lexical-handler}.
   */
  @Override
  @Nullable
  public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
    if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
      return this.lexicalHandler;
    }
    else {
      throw new SAXNotRecognizedException(name);
    }
  }

  /**
   * Throws a {@code SAXNotRecognizedException} exception when the given property does not signify a lexical
   * handler. The property name for a lexical handler is {@code http://xml.org/sax/properties/lexical-handler}.
   */
  @Override
  public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
    if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
      this.lexicalHandler = (LexicalHandler) value;
    }
    else {
      throw new SAXNotRecognizedException(name);
    }
  }

}
