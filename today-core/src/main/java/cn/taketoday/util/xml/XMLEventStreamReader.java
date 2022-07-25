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

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.XMLEvent;

import cn.taketoday.lang.Nullable;

/**
 * Implementation of the {@link javax.xml.stream.XMLStreamReader} interface that wraps a
 * {@link XMLEventReader}. Useful because the StAX {@link javax.xml.stream.XMLInputFactory}
 * allows one to create a event reader from a stream reader, but not vice-versa.
 *
 * @author Arjen Poutsma
 * @see StaxUtils#createEventStreamReader(XMLEventReader)
 * @since 4.0
 */
class XMLEventStreamReader extends AbstractXMLStreamReader {

  private XMLEvent event;

  private final XMLEventReader eventReader;

  public XMLEventStreamReader(XMLEventReader eventReader) throws XMLStreamException {
    this.eventReader = eventReader;
    this.event = eventReader.nextEvent();
  }

  @Override
  public QName getName() {
    if (this.event.isStartElement()) {
      return this.event.asStartElement().getName();
    }
    else if (this.event.isEndElement()) {
      return this.event.asEndElement().getName();
    }
    else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Location getLocation() {
    return this.event.getLocation();
  }

  @Override
  public int getEventType() {
    return this.event.getEventType();
  }

  @Override
  @Nullable
  public String getVersion() {
    if (this.event.isStartDocument()) {
      return ((StartDocument) this.event).getVersion();
    }
    else {
      return null;
    }
  }

  @Override
  public Object getProperty(String name) throws IllegalArgumentException {
    return this.eventReader.getProperty(name);
  }

  @Override
  public boolean isStandalone() {
    if (this.event.isStartDocument()) {
      return ((StartDocument) this.event).isStandalone();
    }
    else {
      throw new IllegalStateException();
    }
  }

  @Override
  public boolean standaloneSet() {
    if (this.event.isStartDocument()) {
      return ((StartDocument) this.event).standaloneSet();
    }
    else {
      throw new IllegalStateException();
    }
  }

  @Override
  @Nullable
  public String getEncoding() {
    return null;
  }

  @Override
  @Nullable
  public String getCharacterEncodingScheme() {
    return null;
  }

  @Override
  public String getPITarget() {
    if (this.event.isProcessingInstruction()) {
      return ((ProcessingInstruction) this.event).getTarget();
    }
    else {
      throw new IllegalStateException();
    }
  }

  @Override
  public String getPIData() {
    if (this.event.isProcessingInstruction()) {
      return ((ProcessingInstruction) this.event).getData();
    }
    else {
      throw new IllegalStateException();
    }
  }

  @Override
  public int getTextStart() {
    return 0;
  }

  @Override
  public String getText() {
    if (this.event.isCharacters()) {
      return this.event.asCharacters().getData();
    }
    else if (this.event.getEventType() == COMMENT) {
      return ((Comment) this.event).getText();
    }
    else {
      throw new IllegalStateException();
    }
  }

  @Override
  public int getAttributeCount() {
    if (!this.event.isStartElement()) {
      throw new IllegalStateException();
    }
    Iterator<Attribute> attributes = this.event.asStartElement().getAttributes();
    return countIterator(attributes);
  }

  @Override
  public boolean isAttributeSpecified(int index) {
    return getAttribute(index).isSpecified();
  }

  @Override
  public QName getAttributeName(int index) {
    return getAttribute(index).getName();
  }

  @Override
  public String getAttributeType(int index) {
    return getAttribute(index).getDTDType();
  }

  @Override
  public String getAttributeValue(int index) {
    return getAttribute(index).getValue();
  }

  private Attribute getAttribute(int index) {
    if (!this.event.isStartElement()) {
      throw new IllegalStateException();
    }
    int count = 0;
    Iterator<Attribute> attributes = this.event.asStartElement().getAttributes();
    while (attributes.hasNext()) {
      Attribute attribute = attributes.next();
      if (count == index) {
        return attribute;
      }
      else {
        count++;
      }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    if (this.event.isStartElement()) {
      return this.event.asStartElement().getNamespaceContext();
    }
    else {
      throw new IllegalStateException();
    }
  }

  @Override
  @SuppressWarnings("rawtypes")
  public int getNamespaceCount() {
    Iterator namespaces;
    if (this.event.isStartElement()) {
      namespaces = this.event.asStartElement().getNamespaces();
    }
    else if (this.event.isEndElement()) {
      namespaces = this.event.asEndElement().getNamespaces();
    }
    else {
      throw new IllegalStateException();
    }
    return countIterator(namespaces);
  }

  @Override
  public String getNamespacePrefix(int index) {
    return getNamespace(index).getPrefix();
  }

  @Override
  public String getNamespaceURI(int index) {
    return getNamespace(index).getNamespaceURI();
  }

  @SuppressWarnings("rawtypes")
  private Namespace getNamespace(int index) {
    Iterator namespaces;
    if (this.event.isStartElement()) {
      namespaces = this.event.asStartElement().getNamespaces();
    }
    else if (this.event.isEndElement()) {
      namespaces = this.event.asEndElement().getNamespaces();
    }
    else {
      throw new IllegalStateException();
    }
    int count = 0;
    while (namespaces.hasNext()) {
      Namespace namespace = (Namespace) namespaces.next();
      if (count == index) {
        return namespace;
      }
      else {
        count++;
      }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public int next() throws XMLStreamException {
    this.event = this.eventReader.nextEvent();
    return this.event.getEventType();
  }

  @Override
  public void close() throws XMLStreamException {
    this.eventReader.close();
  }

  @SuppressWarnings("rawtypes")
  private static int countIterator(Iterator iterator) {
    int count = 0;
    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }
    return count;
  }

}
