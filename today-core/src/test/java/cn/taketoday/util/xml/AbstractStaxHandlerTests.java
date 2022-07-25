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
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xmlunit.util.Predicate;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;

import cn.taketoday.core.testfixture.xml.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
abstract class AbstractStaxHandlerTests {

  private static final String COMPLEX_XML =
          "<?xml version='1.0' encoding='UTF-8'?>" +
                  "<!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN 2.0//EN\" \"https://www.springframework.org/dtd/spring-beans-2.0.dtd\">" +
                  "<?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2' prefix:attr='value'>characters <![CDATA[cdata]]></prefix:child>" +
                  "<!-- comment -->" +
                  "</root>";

  private static final String SIMPLE_XML = "<?xml version='1.0' encoding='UTF-8'?>" +
          "<?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2' prefix:attr='value'>content</prefix:child>" +
          "</root>";

  private static final Predicate<Node> nodeFilter = (n -> n.getNodeType() != Node.COMMENT_NODE &&
          n.getNodeType() != Node.DOCUMENT_TYPE_NODE && n.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE);

  private XMLReader xmlReader;

  @BeforeEach
  void createXMLReader() throws Exception {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    SAXParser saxParser = saxParserFactory.newSAXParser();
    xmlReader = saxParser.getXMLReader();
    xmlReader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
  }

  @Test
  void noNamespacePrefixes() throws Exception {
    StringWriter stringWriter = new StringWriter();
    AbstractStaxHandler handler = createStaxHandler(new StreamResult(stringWriter));
    xmlReader.setContentHandler(handler);
    xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

    xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
    xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);

    xmlReader.parse(new InputSource(new StringReader(COMPLEX_XML)));

    assertThat(XmlContent.from(stringWriter)).isSimilarTo(COMPLEX_XML, nodeFilter);
  }

  @Test
  void namespacePrefixes() throws Exception {
    StringWriter stringWriter = new StringWriter();
    AbstractStaxHandler handler = createStaxHandler(new StreamResult(stringWriter));
    xmlReader.setContentHandler(handler);
    xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

    xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
    xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

    xmlReader.parse(new InputSource(new StringReader(COMPLEX_XML)));

    assertThat(XmlContent.from(stringWriter)).isSimilarTo(COMPLEX_XML, nodeFilter);
  }

  @Test
  void noNamespacePrefixesDom() throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

    Document expected = documentBuilder.parse(new InputSource(new StringReader(SIMPLE_XML)));

    Document result = documentBuilder.newDocument();
    AbstractStaxHandler handler = createStaxHandler(new DOMResult(result));
    xmlReader.setContentHandler(handler);
    xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

    xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
    xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);

    xmlReader.parse(new InputSource(new StringReader(SIMPLE_XML)));

    assertThat(XmlContent.of(result)).isSimilarTo(expected, nodeFilter);
  }

  @Test
  void namespacePrefixesDom() throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

    Document expected = documentBuilder.parse(new InputSource(new StringReader(SIMPLE_XML)));

    Document result = documentBuilder.newDocument();
    AbstractStaxHandler handler = createStaxHandler(new DOMResult(result));
    xmlReader.setContentHandler(handler);
    xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

    xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
    xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

    xmlReader.parse(new InputSource(new StringReader(SIMPLE_XML)));

    assertThat(XmlContent.of(result)).isSimilarTo(expected, nodeFilter);
  }

  protected abstract AbstractStaxHandler createStaxHandler(Result result) throws XMLStreamException;

}
