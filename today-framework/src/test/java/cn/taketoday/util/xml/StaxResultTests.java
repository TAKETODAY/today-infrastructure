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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import cn.taketoday.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class StaxResultTests {

  private static final String XML = "<root xmlns='namespace'><child/></root>";

  private Transformer transformer;

  private XMLOutputFactory inputFactory;

  @BeforeEach
  void setUp() throws Exception {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformer = transformerFactory.newTransformer();
    inputFactory = XMLOutputFactory.newInstance();
  }

  @Test
  void streamWriterSource() throws Exception {
    StringWriter stringWriter = new StringWriter();
    XMLStreamWriter streamWriter = inputFactory.createXMLStreamWriter(stringWriter);
    Reader reader = new StringReader(XML);
    Source source = new StreamSource(reader);
    StaxResult result = new StaxResult(streamWriter);
    assertThat(result.getXMLStreamWriter()).as("Invalid streamWriter returned").isEqualTo(streamWriter);
    assertThat(result.getXMLEventWriter()).as("EventWriter returned").isNull();
    transformer.transform(source, result);
    assertThat(XmlContent.from(stringWriter)).as("Invalid result").isSimilarTo(XML);
  }

  @Test
  void eventWriterSource() throws Exception {
    StringWriter stringWriter = new StringWriter();
    XMLEventWriter eventWriter = inputFactory.createXMLEventWriter(stringWriter);
    Reader reader = new StringReader(XML);
    Source source = new StreamSource(reader);
    StaxResult result = new StaxResult(eventWriter);
    assertThat(result.getXMLEventWriter()).as("Invalid eventWriter returned").isEqualTo(eventWriter);
    assertThat(result.getXMLStreamWriter()).as("StreamWriter returned").isNull();
    transformer.transform(source, result);
    assertThat(XmlContent.from(stringWriter)).as("Invalid result").isSimilarTo(XML);
  }

}
