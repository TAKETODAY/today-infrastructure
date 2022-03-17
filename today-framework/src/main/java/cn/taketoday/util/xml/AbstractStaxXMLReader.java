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
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Abstract base class for SAX {@code XMLReader} implementations that use StAX as a basis.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see #setContentHandler(org.xml.sax.ContentHandler)
 * @see #setDTDHandler(org.xml.sax.DTDHandler)
 * @see #setEntityResolver(org.xml.sax.EntityResolver)
 * @see #setErrorHandler(org.xml.sax.ErrorHandler)
 * @since 4.0
 */
abstract class AbstractStaxXMLReader extends AbstractXMLReader {

  private static final String NAMESPACES_FEATURE_NAME = "http://xml.org/sax/features/namespaces";

  private static final String NAMESPACE_PREFIXES_FEATURE_NAME = "http://xml.org/sax/features/namespace-prefixes";

  private static final String IS_STANDALONE_FEATURE_NAME = "http://xml.org/sax/features/is-standalone";

  private boolean namespacesFeature = true;

  private boolean namespacePrefixesFeature = false;

  @Nullable
  private Boolean isStandalone;

  private final Map<String, String> namespaces = new LinkedHashMap<>();

  @Override
  public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
    switch (name) {
      case NAMESPACES_FEATURE_NAME:
        return this.namespacesFeature;
      case NAMESPACE_PREFIXES_FEATURE_NAME:
        return this.namespacePrefixesFeature;
      case IS_STANDALONE_FEATURE_NAME:
        if (this.isStandalone != null) {
          return this.isStandalone;
        }
        else {
          throw new SAXNotSupportedException("startDocument() callback not completed yet");
        }
      default:
        return super.getFeature(name);
    }
  }

  @Override
  public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
    if (NAMESPACES_FEATURE_NAME.equals(name)) {
      this.namespacesFeature = value;
    }
    else if (NAMESPACE_PREFIXES_FEATURE_NAME.equals(name)) {
      this.namespacePrefixesFeature = value;
    }
    else {
      super.setFeature(name, value);
    }
  }

  protected void setStandalone(boolean standalone) {
    this.isStandalone = standalone;
  }

  /**
   * Indicates whether the SAX feature {@code http://xml.org/sax/features/namespaces} is turned on.
   */
  protected boolean hasNamespacesFeature() {
    return this.namespacesFeature;
  }

  /**
   * Indicates whether the SAX feature {@code http://xml.org/sax/features/namespaces-prefixes} is turned on.
   */
  protected boolean hasNamespacePrefixesFeature() {
    return this.namespacePrefixesFeature;
  }

  /**
   * Convert a {@code QName} to a qualified name, as used by DOM and SAX.
   * The returned string has a format of {@code prefix:localName} if the
   * prefix is set, or just {@code localName} if not.
   *
   * @param qName the {@code QName}
   * @return the qualified name
   */
  protected String toQualifiedName(QName qName) {
    String prefix = qName.getPrefix();
    if (StringUtils.isEmpty(prefix)) {
      return qName.getLocalPart();
    }
    else {
      return prefix + ":" + qName.getLocalPart();
    }
  }

  /**
   * Parse the StAX XML reader passed at construction-time.
   * <p><b>NOTE:</b>: The given {@code InputSource} is not read, but ignored.
   *
   * @param ignored is ignored
   * @throws SAXException a SAX exception, possibly wrapping a {@code XMLStreamException}
   */
  @Override
  public final void parse(InputSource ignored) throws SAXException {
    parse();
  }

  /**
   * Parse the StAX XML reader passed at construction-time.
   * <p><b>NOTE:</b>: The given system identifier is not read, but ignored.
   *
   * @param ignored is ignored
   * @throws SAXException a SAX exception, possibly wrapping a {@code XMLStreamException}
   */
  @Override
  public final void parse(String ignored) throws SAXException {
    parse();
  }

  private void parse() throws SAXException {
    try {
      parseInternal();
    }
    catch (XMLStreamException ex) {
      Locator locator = null;
      if (ex.getLocation() != null) {
        locator = new StaxLocator(ex.getLocation());
      }
      SAXParseException saxException = new SAXParseException(ex.getMessage(), locator, ex);
      if (getErrorHandler() != null) {
        getErrorHandler().fatalError(saxException);
      }
      else {
        throw saxException;
      }
    }
  }

  /**
   * Template method that parses the StAX reader passed at construction-time.
   */
  protected abstract void parseInternal() throws SAXException, XMLStreamException;

  /**
   * Start the prefix mapping for the given prefix.
   *
   * @see org.xml.sax.ContentHandler#startPrefixMapping(String, String)
   */
  protected void startPrefixMapping(@Nullable String prefix, String namespace) throws SAXException {
    if (getContentHandler() != null && StringUtils.isNotEmpty(namespace)) {
      if (prefix == null) {
        prefix = "";
      }
      if (!namespace.equals(this.namespaces.get(prefix))) {
        getContentHandler().startPrefixMapping(prefix, namespace);
        this.namespaces.put(prefix, namespace);
      }
    }
  }

  /**
   * End the prefix mapping for the given prefix.
   *
   * @see org.xml.sax.ContentHandler#endPrefixMapping(String)
   */
  protected void endPrefixMapping(String prefix) throws SAXException {
    if (getContentHandler() != null && this.namespaces.containsKey(prefix)) {
      getContentHandler().endPrefixMapping(prefix);
      this.namespaces.remove(prefix);
    }
  }

  /**
   * Implementation of the {@code Locator} interface based on a given StAX {@code Location}.
   *
   * @see Locator
   * @see Location
   */
  private static class StaxLocator implements Locator {

    private final Location location;

    public StaxLocator(Location location) {
      this.location = location;
    }

    @Override
    public String getPublicId() {
      return this.location.getPublicId();
    }

    @Override
    public String getSystemId() {
      return this.location.getSystemId();
    }

    @Override
    public int getLineNumber() {
      return this.location.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
      return this.location.getColumnNumber();
    }
  }

}
