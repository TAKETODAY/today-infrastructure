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

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.ext.LexicalHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

import cn.taketoday.lang.Nullable;

/**
 * SAX {@link org.xml.sax.ContentHandler} and {@link LexicalHandler}
 * that writes to a {@link javax.xml.stream.util.XMLEventConsumer}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
class StaxEventHandler extends AbstractStaxHandler {

  private final XMLEventFactory eventFactory;

  private final XMLEventWriter eventWriter;

  /**
   * Construct a new instance of the {@code StaxEventContentHandler} that writes to the
   * given {@code XMLEventWriter}. A default {@code XMLEventFactory} will be created.
   *
   * @param eventWriter the writer to write events to
   */
  public StaxEventHandler(XMLEventWriter eventWriter) {
    this.eventFactory = XMLEventFactory.newInstance();
    this.eventWriter = eventWriter;
  }

  /**
   * Construct a new instance of the {@code StaxEventContentHandler} that uses the given
   * event factory to create events and writes to the given {@code XMLEventConsumer}.
   *
   * @param eventWriter the writer to write events to
   * @param factory the factory used to create events
   */
  public StaxEventHandler(XMLEventWriter eventWriter, XMLEventFactory factory) {
    this.eventFactory = factory;
    this.eventWriter = eventWriter;
  }

  @Override
  public void setDocumentLocator(@Nullable Locator locator) {
    if (locator != null) {
      eventFactory.setLocation(new LocatorLocationAdapter(locator));
    }
  }

  @Override
  protected void startDocumentInternal() throws XMLStreamException {
    eventWriter.add(eventFactory.createStartDocument());
  }

  @Override
  protected void endDocumentInternal() throws XMLStreamException {
    eventWriter.add(eventFactory.createEndDocument());
  }

  @Override
  protected void startElementInternal(QName name, Attributes atts,
          Map<String, String> namespaceMapping) throws XMLStreamException {

    List<Attribute> attributes = getAttributes(atts);
    List<Namespace> namespaces = getNamespaces(namespaceMapping);
    eventWriter.add(eventFactory.createStartElement(name, attributes.iterator(), namespaces.iterator()));

  }

  private List<Namespace> getNamespaces(Map<String, String> namespaceMappings) {
    List<Namespace> result = new ArrayList<>(namespaceMappings.size());
    for (Map.Entry<String, String> entry : namespaceMappings.entrySet()) {
      String prefix = entry.getKey();
      String namespaceUri = entry.getValue();
      result.add(eventFactory.createNamespace(prefix, namespaceUri));
    }
    return result;
  }

  private List<Attribute> getAttributes(Attributes attributes) {
    int attrLength = attributes.getLength();
    ArrayList<Attribute> result = new ArrayList<>(attrLength);
    for (int i = 0; i < attrLength; i++) {
      QName attrName = toQName(attributes.getURI(i), attributes.getQName(i));
      if (!isNamespaceDeclaration(attrName)) {
        result.add(eventFactory.createAttribute(attrName, attributes.getValue(i)));
      }
    }
    return result;
  }

  @Override
  protected void endElementInternal(QName name, Map<String, String> namespaceMapping) throws XMLStreamException {
    List<Namespace> namespaces = getNamespaces(namespaceMapping);
    eventWriter.add(eventFactory.createEndElement(name, namespaces.iterator()));
  }

  @Override
  protected void charactersInternal(String data) throws XMLStreamException {
    eventWriter.add(eventFactory.createCharacters(data));
  }

  @Override
  protected void cDataInternal(String data) throws XMLStreamException {
    eventWriter.add(eventFactory.createCData(data));
  }

  @Override
  protected void ignorableWhitespaceInternal(String data) throws XMLStreamException {
    eventWriter.add(eventFactory.createIgnorableSpace(data));
  }

  @Override
  protected void processingInstructionInternal(String target, String data) throws XMLStreamException {
    eventWriter.add(eventFactory.createProcessingInstruction(target, data));
  }

  @Override
  protected void dtdInternal(String dtd) throws XMLStreamException {
    eventWriter.add(eventFactory.createDTD(dtd));
  }

  @Override
  protected void commentInternal(String comment) throws XMLStreamException {
    eventWriter.add(eventFactory.createComment(comment));
  }

  // Ignored
  @Override
  protected void skippedEntityInternal(String name) {
  }

  private static final class LocatorLocationAdapter implements Location {

    private final Locator locator;

    public LocatorLocationAdapter(Locator locator) {
      this.locator = locator;
    }

    @Override
    public int getLineNumber() {
      return locator.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
      return locator.getColumnNumber();
    }

    @Override
    public int getCharacterOffset() {
      return -1;
    }

    @Override
    public String getPublicId() {
      return locator.getPublicId();
    }

    @Override
    public String getSystemId() {
      return locator.getSystemId();
    }
  }

}
