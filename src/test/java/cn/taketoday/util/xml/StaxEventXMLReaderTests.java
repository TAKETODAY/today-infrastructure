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

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.io.StringReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StaxEventXMLReaderTests extends AbstractStaxXMLReaderTests {

  public static final String CONTENT = "<root xmlns='http://springframework.org/spring-ws'><child/></root>";

  @Override
  protected AbstractStaxXMLReader createStaxXmlReader(InputStream inputStream) throws XMLStreamException {
    return new StaxEventXMLReader(inputFactory.createXMLEventReader(inputStream));
  }

  @Test
  void partial() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(CONTENT));
    eventReader.nextTag();  // skip to root
    StaxEventXMLReader xmlReader = new StaxEventXMLReader(eventReader);
    ContentHandler contentHandler = mock(ContentHandler.class);
    xmlReader.setContentHandler(contentHandler);
    xmlReader.parse(new InputSource());
    verify(contentHandler).startDocument();
    verify(contentHandler).startElement(eq("http://springframework.org/spring-ws"), eq("child"), eq("child"), any(Attributes.class));
    verify(contentHandler).endElement("http://springframework.org/spring-ws", "child", "child");
    verify(contentHandler).endDocument();
  }

}
