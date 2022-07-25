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
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import cn.taketoday.core.testfixture.xml.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DomContentHandler}.
 */
class DomContentHandlerTests {

  private static final String XML_1 =
          "<?xml version='1.0' encoding='UTF-8'?>" + "<?pi content?>" + "<root xmlns='namespace'>" +
                  "<prefix:child xmlns:prefix='namespace2' xmlns:prefix2='namespace3' prefix2:attr='value'>content</prefix:child>" +
                  "</root>";

  private static final String XML_2_EXPECTED =
          "<?xml version='1.0' encoding='UTF-8'?>" + "<root xmlns='namespace'>" + "<child xmlns='namespace2' />" +
                  "</root>";

  private static final String XML_2_SNIPPET =
          "<?xml version='1.0' encoding='UTF-8'?>" + "<child xmlns='namespace2' />";

  private Document expected;

  private DomContentHandler handler;

  private Document result;

  private XMLReader xmlReader;

  private DocumentBuilder documentBuilder;

  @BeforeEach
  void setUp() throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilder = documentBuilderFactory.newDocumentBuilder();
    result = documentBuilder.newDocument();
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    SAXParser saxParser = saxParserFactory.newSAXParser();
    xmlReader = saxParser.getXMLReader();
  }

  @Test
  void contentHandlerDocumentNamespacePrefixes() throws Exception {
    xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
    handler = new DomContentHandler(result);
    expected = documentBuilder.parse(new InputSource(new StringReader(XML_1)));
    xmlReader.setContentHandler(handler);
    xmlReader.parse(new InputSource(new StringReader(XML_1)));
    assertThat(XmlContent.of(result)).as("Invalid result").isSimilarTo(expected);
  }

  @Test
  void contentHandlerDocumentNoNamespacePrefixes() throws Exception {
    handler = new DomContentHandler(result);
    expected = documentBuilder.parse(new InputSource(new StringReader(XML_1)));
    xmlReader.setContentHandler(handler);
    xmlReader.parse(new InputSource(new StringReader(XML_1)));
    assertThat(XmlContent.of(result)).as("Invalid result").isSimilarTo(expected);
  }

  @Test
  void contentHandlerElement() throws Exception {
    Element rootElement = result.createElementNS("namespace", "root");
    result.appendChild(rootElement);
    handler = new DomContentHandler(rootElement);
    expected = documentBuilder.parse(new InputSource(new StringReader(XML_2_EXPECTED)));
    xmlReader.setContentHandler(handler);
    xmlReader.parse(new InputSource(new StringReader(XML_2_SNIPPET)));
    assertThat(XmlContent.of(result)).as("Invalid result").isSimilarTo(expected);
  }

}
