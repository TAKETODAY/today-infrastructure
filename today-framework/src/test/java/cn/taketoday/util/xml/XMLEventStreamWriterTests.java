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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xmlunit.util.Predicate;

import java.io.StringWriter;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;

import cn.taketoday.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

class XMLEventStreamWriterTests {

  private static final String XML =
          "<?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2'><!--comment-->content</prefix:child></root>";

  private XMLEventStreamWriter streamWriter;

  private StringWriter stringWriter;

  @BeforeEach
  void createStreamReader() throws Exception {
    stringWriter = new StringWriter();
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(stringWriter);
    streamWriter = new XMLEventStreamWriter(eventWriter, XMLEventFactory.newInstance());
  }

  @Test
  void write() throws Exception {
    streamWriter.writeStartDocument();
    streamWriter.writeProcessingInstruction("pi", "content");
    streamWriter.writeStartElement("namespace", "root");
    streamWriter.writeDefaultNamespace("namespace");
    streamWriter.writeStartElement("prefix", "child", "namespace2");
    streamWriter.writeNamespace("prefix", "namespace2");
    streamWriter.writeComment("comment");
    streamWriter.writeCharacters("content");
    streamWriter.writeEndElement();
    streamWriter.writeEndElement();
    streamWriter.writeEndDocument();

    Predicate<Node> nodeFilter = n -> n.getNodeType() != Node.DOCUMENT_TYPE_NODE && n.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE;
    assertThat(XmlContent.from(stringWriter)).isSimilarTo(XML, nodeFilter);
  }

}
