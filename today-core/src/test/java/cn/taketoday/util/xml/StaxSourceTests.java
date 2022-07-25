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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;

import cn.taketoday.core.testfixture.xml.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class StaxSourceTests {

  private static final String XML = "<root xmlns='namespace'><child/></root>";

  private Transformer transformer;

  private XMLInputFactory inputFactory;

  private DocumentBuilder documentBuilder;

  @BeforeEach
  void setUp() throws Exception {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformer = transformerFactory.newTransformer();
    inputFactory = XMLInputFactory.newInstance();
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilder = documentBuilderFactory.newDocumentBuilder();
  }

  @Test
  void streamReaderSourceToStreamResult() throws Exception {
    XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(XML));
    StaxSource source = new StaxSource(streamReader);
    assertThat(source.getXMLStreamReader()).as("Invalid streamReader returned").isEqualTo(streamReader);
    assertThat((Object) source.getXMLEventReader()).as("EventReader returned").isNull();
    StringWriter writer = new StringWriter();
    transformer.transform(source, new StreamResult(writer));
    assertThat(XmlContent.from(writer)).as("Invalid result").isSimilarTo(XML);
  }

  @Test
  void streamReaderSourceToDOMResult() throws Exception {
    XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(XML));
    StaxSource source = new StaxSource(streamReader);
    assertThat(source.getXMLStreamReader()).as("Invalid streamReader returned").isEqualTo(streamReader);
    assertThat((Object) source.getXMLEventReader()).as("EventReader returned").isNull();

    Document expected = documentBuilder.parse(new InputSource(new StringReader(XML)));
    Document result = documentBuilder.newDocument();
    transformer.transform(source, new DOMResult(result));
    assertThat(XmlContent.of(result)).as("Invalid result").isSimilarTo(expected);
  }

  @Test
  void eventReaderSourceToStreamResult() throws Exception {
    XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(XML));
    StaxSource source = new StaxSource(eventReader);
    assertThat((Object) source.getXMLEventReader()).as("Invalid eventReader returned").isEqualTo(eventReader);
    assertThat(source.getXMLStreamReader()).as("StreamReader returned").isNull();
    StringWriter writer = new StringWriter();
    transformer.transform(source, new StreamResult(writer));
    assertThat(XmlContent.from(writer)).as("Invalid result").isSimilarTo(XML);
  }

  @Test
  void eventReaderSourceToDOMResult() throws Exception {
    XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(XML));
    StaxSource source = new StaxSource(eventReader);
    assertThat((Object) source.getXMLEventReader()).as("Invalid eventReader returned").isEqualTo(eventReader);
    assertThat(source.getXMLStreamReader()).as("StreamReader returned").isNull();

    Document expected = documentBuilder.parse(new InputSource(new StringReader(XML)));
    Document result = documentBuilder.newDocument();
    transformer.transform(source, new DOMResult(result));
    assertThat(XmlContent.of(result)).as("Invalid result").isSimilarTo(expected);
  }
}
