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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import cn.taketoday.core.testfixture.xml.XmlContent;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Arjen Poutsma
 * @author Andrzej Hołowko
 */
class ListBasedXMLEventReaderTests {

  private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

  private final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();

  @Test
  void standard() throws Exception {
    String xml = "<foo><bar>baz</bar></foo>";
    List<XMLEvent> events = readEvents(xml);

    ListBasedXMLEventReader reader = new ListBasedXMLEventReader(events);

    StringWriter resultWriter = new StringWriter();
    XMLEventWriter writer = this.outputFactory.createXMLEventWriter(resultWriter);
    writer.add(reader);

    assertThat(XmlContent.from(resultWriter)).isSimilarTo(xml);
  }

  @Test
  void getElementText() throws Exception {
    String xml = "<foo><bar>baz</bar></foo>";
    List<XMLEvent> events = readEvents(xml);

    ListBasedXMLEventReader reader = new ListBasedXMLEventReader(events);

    assertThat(reader.nextEvent().getEventType()).isEqualTo(START_DOCUMENT);
    assertThat(reader.nextEvent().getEventType()).isEqualTo(START_ELEMENT);
    assertThat(reader.nextEvent().getEventType()).isEqualTo(START_ELEMENT);
    assertThat(reader.getElementText()).isEqualTo("baz");
    assertThat(reader.nextEvent().getEventType()).isEqualTo(END_ELEMENT);
    assertThat(reader.nextEvent().getEventType()).isEqualTo(END_DOCUMENT);
  }

  @Test
  void getElementTextThrowsExceptionAtWrongPosition() throws Exception {
    String xml = "<foo><bar>baz</bar></foo>";
    List<XMLEvent> events = readEvents(xml);

    ListBasedXMLEventReader reader = new ListBasedXMLEventReader(events);

    assertThat(reader.nextEvent().getEventType()).isEqualTo(START_DOCUMENT);

    assertThatExceptionOfType(XMLStreamException.class).isThrownBy(
                    reader::getElementText)
            .withMessageStartingWith("Not at START_ELEMENT");
  }

  private List<XMLEvent> readEvents(String xml) throws XMLStreamException {
    XMLEventReader reader = this.inputFactory.createXMLEventReader(new StringReader(xml));
    List<XMLEvent> events = new ArrayList<>();
    while (reader.hasNext()) {
      events.add(reader.nextEvent());
    }
    return events;
  }

}
