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
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * SAX {@link org.xml.sax.ContentHandler} and {@link LexicalHandler}
 * that writes to an {@link XMLStreamWriter}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
class StaxStreamHandler extends AbstractStaxHandler {

  private final XMLStreamWriter streamWriter;

  public StaxStreamHandler(XMLStreamWriter streamWriter) {
    this.streamWriter = streamWriter;
  }

  @Override
  protected void startDocumentInternal() throws XMLStreamException {
    this.streamWriter.writeStartDocument();
  }

  @Override
  protected void endDocumentInternal() throws XMLStreamException {
    this.streamWriter.writeEndDocument();
  }

  @Override
  protected void startElementInternal(QName name, Attributes attributes,
          Map<String, String> namespaceMapping) throws XMLStreamException {

    this.streamWriter.writeStartElement(name.getPrefix(), name.getLocalPart(), name.getNamespaceURI());

    for (Map.Entry<String, String> entry : namespaceMapping.entrySet()) {
      String prefix = entry.getKey();
      String namespaceUri = entry.getValue();
      this.streamWriter.writeNamespace(prefix, namespaceUri);
      if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
        this.streamWriter.setDefaultNamespace(namespaceUri);
      }
      else {
        this.streamWriter.setPrefix(prefix, namespaceUri);
      }
    }
    int length = attributes.getLength();
    for (int i = 0; i < length; i++) {
      QName attrName = toQName(attributes.getURI(i), attributes.getQName(i));
      if (!isNamespaceDeclaration(attrName)) {
        this.streamWriter.writeAttribute(attrName.getPrefix(), attrName.getNamespaceURI(),
                attrName.getLocalPart(), attributes.getValue(i));
      }
    }
  }

  @Override
  protected void endElementInternal(QName name, Map<String, String> namespaceMapping) throws XMLStreamException {
    this.streamWriter.writeEndElement();
  }

  @Override
  protected void charactersInternal(String data) throws XMLStreamException {
    this.streamWriter.writeCharacters(data);
  }

  @Override
  protected void cDataInternal(String data) throws XMLStreamException {
    this.streamWriter.writeCData(data);
  }

  @Override
  protected void ignorableWhitespaceInternal(String data) throws XMLStreamException {
    this.streamWriter.writeCharacters(data);
  }

  @Override
  protected void processingInstructionInternal(String target, String data) throws XMLStreamException {
    this.streamWriter.writeProcessingInstruction(target, data);
  }

  @Override
  protected void dtdInternal(String dtd) throws XMLStreamException {
    this.streamWriter.writeDTD(dtd);
  }

  @Override
  protected void commentInternal(String comment) throws XMLStreamException {
    this.streamWriter.writeComment(comment);
  }

  // Ignored

  @Override
  public void setDocumentLocator(Locator locator) {
  }

  @Override
  public void startEntity(String name) throws SAXException {
  }

  @Override
  public void endEntity(String name) throws SAXException {
  }

  @Override
  protected void skippedEntityInternal(String name) throws XMLStreamException {
  }

}
