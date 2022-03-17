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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class StaxUtilsTests {

  @Test
  void isStaxSourceInvalid() throws Exception {
    Assertions.assertThat(StaxUtils.isStaxSource(new DOMSource())).as("A StAX Source").isFalse();
    assertThat(StaxUtils.isStaxSource(new SAXSource())).as("A StAX Source").isFalse();
    assertThat(StaxUtils.isStaxSource(new StreamSource())).as("A StAX Source").isFalse();
  }

  @Test
  void isStaxSource() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    String expected = "<element/>";
    XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(expected));
    Source source = StaxUtils.createCustomStaxSource(streamReader);

    assertThat(StaxUtils.isStaxSource(source)).as("Not a StAX Source").isTrue();
  }

  @Test
  void isStaxSourceJaxp14() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    String expected = "<element/>";
    XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(expected));
    StAXSource source = new StAXSource(streamReader);

    assertThat(StaxUtils.isStaxSource(source)).as("Not a StAX Source").isTrue();
  }

  @Test
  void isStaxResultInvalid() throws Exception {
    assertThat(StaxUtils.isStaxResult(new DOMResult())).as("A StAX Result").isFalse();
    assertThat(StaxUtils.isStaxResult(new SAXResult())).as("A StAX Result").isFalse();
    assertThat(StaxUtils.isStaxResult(new StreamResult())).as("A StAX Result").isFalse();
  }

  @Test
  void isStaxResult() throws Exception {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(new StringWriter());
    Result result = StaxUtils.createCustomStaxResult(streamWriter);

    assertThat(StaxUtils.isStaxResult(result)).as("Not a StAX Result").isTrue();
  }

  @Test
  void isStaxResultJaxp14() throws Exception {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(new StringWriter());
    StAXResult result = new StAXResult(streamWriter);

    assertThat(StaxUtils.isStaxResult(result)).as("Not a StAX Result").isTrue();
  }

}
