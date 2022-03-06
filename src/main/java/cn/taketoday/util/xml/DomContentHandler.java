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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

import java.util.ArrayList;

/**
 * SAX {@code ContentHandler} that transforms callback calls to DOM {@code Node}s.
 *
 * @author Arjen Poutsma
 * @see Node
 * @since 4.0
 */
class DomContentHandler implements ContentHandler {

  private final Document document;

  private final ArrayList<Element> elements = new ArrayList<>();

  private final Node node;

  /**
   * Create a new instance of the {@code DomContentHandler} with the given node.
   *
   * @param node the node to publish events to
   */
  DomContentHandler(Node node) {
    this.node = node;
    if (node instanceof Document) {
      this.document = (Document) node;
    }
    else {
      this.document = node.getOwnerDocument();
    }
  }

  private Node getParent() {
    if (!this.elements.isEmpty()) {
      return this.elements.get(this.elements.size() - 1);
    }
    else {
      return this.node;
    }
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    Node parent = getParent();
    Element element = this.document.createElementNS(uri, qName);
    int length = attributes.getLength();
    for (int i = 0; i < length; i++) {
      String attrUri = attributes.getURI(i);
      String attrQname = attributes.getQName(i);
      String value = attributes.getValue(i);
      if (!attrQname.startsWith("xmlns")) {
        element.setAttributeNS(attrUri, attrQname, value);
      }
    }
    element = (Element) parent.appendChild(element);
    this.elements.add(element);
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    this.elements.remove(this.elements.size() - 1);
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    String data = new String(ch, start, length);
    Node parent = getParent();
    Node lastChild = parent.getLastChild();
    if (lastChild != null && lastChild.getNodeType() == Node.TEXT_NODE) {
      ((Text) lastChild).appendData(data);
    }
    else {
      Text text = this.document.createTextNode(data);
      parent.appendChild(text);
    }
  }

  @Override
  public void processingInstruction(String target, String data) {
    Node parent = getParent();
    ProcessingInstruction pi = this.document.createProcessingInstruction(target, data);
    parent.appendChild(pi);
  }

  // Unsupported

  @Override
  public void setDocumentLocator(Locator locator) {
  }

  @Override
  public void startDocument() {
  }

  @Override
  public void endDocument() {
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) {
  }

  @Override
  public void endPrefixMapping(String prefix) {
  }

  @Override
  public void ignorableWhitespace(char[] ch, int start, int length) {
  }

  @Override
  public void skippedEntity(String name) {
  }

}
