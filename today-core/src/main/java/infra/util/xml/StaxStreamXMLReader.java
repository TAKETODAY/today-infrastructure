/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util.xml;

import org.jspecify.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import infra.util.StringUtils;

/**
 * SAX {@code XMLReader} that reads from a StAX {@code XMLStreamReader}. Reads from an
 * {@code XMLStreamReader}, and calls the corresponding methods on the SAX callback interfaces.
 *
 * @author Arjen Poutsma
 * @see XMLStreamReader
 * @see #setContentHandler(ContentHandler)
 * @see #setDTDHandler(org.xml.sax.DTDHandler)
 * @see #setEntityResolver(org.xml.sax.EntityResolver)
 * @see #setErrorHandler(org.xml.sax.ErrorHandler)
 * @since 4.0
 */
class StaxStreamXMLReader extends AbstractStaxXMLReader {

  private static final String DEFAULT_XML_VERSION = "1.0";

  private final XMLStreamReader reader;

  private String xmlVersion = DEFAULT_XML_VERSION;

  @Nullable
  private String encoding;

  /**
   * Construct a new instance of the {@code StaxStreamXmlReader} that reads from the given
   * {@code XMLStreamReader}. The supplied stream reader must be in {@code XMLStreamConstants.START_DOCUMENT}
   * or {@code XMLStreamConstants.START_ELEMENT} state.
   *
   * @param reader the {@code XMLEventReader} to read from
   * @throws IllegalStateException if the reader is not at the start of a document or element
   */
  StaxStreamXMLReader(XMLStreamReader reader) {
    int event = reader.getEventType();
    if (!(event == XMLStreamConstants.START_DOCUMENT || event == XMLStreamConstants.START_ELEMENT)) {
      throw new IllegalStateException("XMLEventReader not at start of document or element");
    }
    this.reader = reader;
  }

  @Override
  protected void parseInternal() throws SAXException, XMLStreamException {
    boolean documentStarted = false;
    boolean documentEnded = false;
    int elementDepth = 0;
    int eventType = this.reader.getEventType();
    while (true) {
      if (eventType != XMLStreamConstants.START_DOCUMENT && eventType != XMLStreamConstants.END_DOCUMENT &&
              !documentStarted) {
        handleStartDocument();
        documentStarted = true;
      }
      switch (eventType) {
        case XMLStreamConstants.START_ELEMENT -> {
          elementDepth++;
          handleStartElement();
        }
        case XMLStreamConstants.END_ELEMENT -> {
          elementDepth--;
          if (elementDepth >= 0) {
            handleEndElement();
          }
        }
        case XMLStreamConstants.PROCESSING_INSTRUCTION -> handleProcessingInstruction();
        case XMLStreamConstants.CHARACTERS,
             XMLStreamConstants.SPACE,
             XMLStreamConstants.CDATA -> handleCharacters();
        case XMLStreamConstants.START_DOCUMENT -> {
          handleStartDocument();
          documentStarted = true;
        }
        case XMLStreamConstants.END_DOCUMENT -> {
          handleEndDocument();
          documentEnded = true;
        }
        case XMLStreamConstants.COMMENT -> handleComment();
        case XMLStreamConstants.DTD -> handleDtd();
        case XMLStreamConstants.ENTITY_REFERENCE -> handleEntityReference();
      }
      if (this.reader.hasNext() && elementDepth >= 0) {
        eventType = this.reader.next();
      }
      else {
        break;
      }
    }
    if (!documentEnded) {
      handleEndDocument();
    }
  }

  private void handleStartDocument() throws SAXException {
    if (XMLStreamConstants.START_DOCUMENT == this.reader.getEventType()) {
      String xmlVersion = this.reader.getVersion();
      if (StringUtils.isNotEmpty(xmlVersion)) {
        this.xmlVersion = xmlVersion;
      }
      this.encoding = this.reader.getCharacterEncodingScheme();
    }

    ContentHandler contentHandler = getContentHandler();
    if (contentHandler != null) {
      Location location = this.reader.getLocation();
      setDocumentLocator(contentHandler, location, xmlVersion, encoding);
      if (this.reader.standaloneSet()) {
        setStandalone(this.reader.isStandalone());
      }
    }
  }

  static void setDocumentLocator(ContentHandler contentHandler, @Nullable Location location,
          String xmlVersion2, @Nullable String encoding) throws SAXException {
    contentHandler.setDocumentLocator(new Locator2() {
      @Override
      public int getColumnNumber() {
        return (location != null ? location.getColumnNumber() : -1);
      }

      @Override
      public int getLineNumber() {
        return (location != null ? location.getLineNumber() : -1);
      }

      @Override
      @Nullable
      public String getPublicId() {
        return (location != null ? location.getPublicId() : null);
      }

      @Override
      @Nullable
      public String getSystemId() {
        return (location != null ? location.getSystemId() : null);
      }

      @Override
      public String getXMLVersion() {
        return xmlVersion2;
      }

      @Override
      @Nullable
      public String getEncoding() {
        return encoding;
      }
    });
    contentHandler.startDocument();
  }

  private void handleStartElement() throws SAXException {
    if (getContentHandler() != null) {
      QName qName = this.reader.getName();
      if (hasNamespacesFeature()) {
        for (int i = 0; i < this.reader.getNamespaceCount(); i++) {
          startPrefixMapping(this.reader.getNamespacePrefix(i), this.reader.getNamespaceURI(i));
        }
        for (int i = 0; i < this.reader.getAttributeCount(); i++) {
          String prefix = this.reader.getAttributePrefix(i);
          String namespace = this.reader.getAttributeNamespace(i);
          if (StringUtils.isNotEmpty(namespace)) {
            startPrefixMapping(prefix, namespace);
          }
        }
        getContentHandler().startElement(qName.getNamespaceURI(), qName.getLocalPart(),
                toQualifiedName(qName), getAttributes());
      }
      else {
        getContentHandler().startElement("", "", toQualifiedName(qName), getAttributes());
      }
    }
  }

  private void handleEndElement() throws SAXException {
    if (getContentHandler() != null) {
      QName qName = this.reader.getName();
      if (hasNamespacesFeature()) {
        getContentHandler().endElement(qName.getNamespaceURI(), qName.getLocalPart(), toQualifiedName(qName));
        for (int i = 0; i < this.reader.getNamespaceCount(); i++) {
          String prefix = this.reader.getNamespacePrefix(i);
          if (prefix == null) {
            prefix = "";
          }
          endPrefixMapping(prefix);
        }
      }
      else {
        getContentHandler().endElement("", "", toQualifiedName(qName));
      }
    }
  }

  private void handleCharacters() throws SAXException {
    if (XMLStreamConstants.CDATA == this.reader.getEventType() && getLexicalHandler() != null) {
      getLexicalHandler().startCDATA();
    }
    if (getContentHandler() != null) {
      getContentHandler().characters(this.reader.getTextCharacters(),
              this.reader.getTextStart(), this.reader.getTextLength());
    }
    if (XMLStreamConstants.CDATA == this.reader.getEventType() && getLexicalHandler() != null) {
      getLexicalHandler().endCDATA();
    }
  }

  private void handleComment() throws SAXException {
    if (getLexicalHandler() != null) {
      getLexicalHandler().comment(this.reader.getTextCharacters(),
              this.reader.getTextStart(), this.reader.getTextLength());
    }
  }

  private void handleDtd() throws SAXException {
    if (getLexicalHandler() != null) {
      Location location = this.reader.getLocation();
      getLexicalHandler().startDTD(null, location.getPublicId(), location.getSystemId());
    }
    if (getLexicalHandler() != null) {
      getLexicalHandler().endDTD();
    }
  }

  private void handleEntityReference() throws SAXException {
    if (getLexicalHandler() != null) {
      getLexicalHandler().startEntity(this.reader.getLocalName());
    }
    if (getLexicalHandler() != null) {
      getLexicalHandler().endEntity(this.reader.getLocalName());
    }
  }

  private void handleEndDocument() throws SAXException {
    if (getContentHandler() != null) {
      getContentHandler().endDocument();
    }
  }

  private void handleProcessingInstruction() throws SAXException {
    if (getContentHandler() != null) {
      getContentHandler().processingInstruction(this.reader.getPITarget(), this.reader.getPIData());
    }
  }

  private Attributes getAttributes() {
    AttributesImpl attributes = new AttributesImpl();
    for (int i = 0; i < this.reader.getAttributeCount(); i++) {
      String namespace = this.reader.getAttributeNamespace(i);
      if (namespace == null || !hasNamespacesFeature()) {
        namespace = "";
      }
      String type = this.reader.getAttributeType(i);
      if (type == null) {
        type = "CDATA";
      }
      attributes.addAttribute(namespace, this.reader.getAttributeLocalName(i),
              toQualifiedName(this.reader.getAttributeName(i)), type, this.reader.getAttributeValue(i));
    }
    if (hasNamespacePrefixesFeature()) {
      for (int i = 0; i < this.reader.getNamespaceCount(); i++) {
        String prefix = this.reader.getNamespacePrefix(i);
        String namespaceUri = this.reader.getNamespaceURI(i);
        String qName;
        if (StringUtils.isNotEmpty(prefix)) {
          qName = "xmlns:" + prefix;
        }
        else {
          qName = "xmlns";
        }
        attributes.addAttribute("", "", qName, "CDATA", namespaceUri);
      }
    }

    return attributes;
  }

}
