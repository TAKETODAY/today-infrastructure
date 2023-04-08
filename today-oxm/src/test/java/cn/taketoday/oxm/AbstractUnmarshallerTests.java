/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.oxm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import cn.taketoday.util.xml.StaxUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
public abstract class AbstractUnmarshallerTests<U extends Unmarshaller> {

  protected U unmarshaller;

  protected static final String INPUT_STRING =
          "<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
                  "<tns:flight><tns:number>42</tns:number></tns:flight></tns:flights>";

  @BeforeEach
  public final void setUp() throws Exception {
    unmarshaller = createUnmarshaller();
  }

  protected abstract U createUnmarshaller() throws Exception;

  protected abstract void testFlights(Object o);

  protected abstract void testFlight(Object o);

  @Test
  public void unmarshalDomSource() throws Exception {
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.newDocument();
    Element flightsElement = document.createElementNS("http://samples.springframework.org/flight", "tns:flights");
    document.appendChild(flightsElement);
    Element flightElement = document.createElementNS("http://samples.springframework.org/flight", "tns:flight");
    flightsElement.appendChild(flightElement);
    Element numberElement = document.createElementNS("http://samples.springframework.org/flight", "tns:number");
    flightElement.appendChild(numberElement);
    Text text = document.createTextNode("42");
    numberElement.appendChild(text);
    DOMSource source = new DOMSource(document);
    Object flights = unmarshaller.unmarshal(source);
    testFlights(flights);
  }

  @Test
  public void unmarshalStreamSourceReader() throws Exception {
    StreamSource source = new StreamSource(new StringReader(INPUT_STRING));
    Object flights = unmarshaller.unmarshal(source);
    testFlights(flights);
  }

  @Test
  public void unmarshalStreamSourceInputStream() throws Exception {
    StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING.getBytes("UTF-8")));
    Object flights = unmarshaller.unmarshal(source);
    testFlights(flights);
  }

  @Test
  public void unmarshalSAXSource() throws Exception {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    SAXParser saxParser = saxParserFactory.newSAXParser();
    XMLReader reader = saxParser.getXMLReader();
    SAXSource source = new SAXSource(reader, new InputSource(new StringReader(INPUT_STRING)));
    Object flights = unmarshaller.unmarshal(source);
    testFlights(flights);
  }

  @Test
  public void unmarshalStaxSourceXmlStreamReader() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
    Source source = StaxUtils.createStaxSource(streamReader);
    Object flights = unmarshaller.unmarshal(source);
    testFlights(flights);
  }

  @Test
  public void unmarshalStaxSourceXmlEventReader() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(INPUT_STRING));
    Source source = StaxUtils.createStaxSource(eventReader);
    Object flights = unmarshaller.unmarshal(source);
    testFlights(flights);
  }

  @Test
  public void unmarshalJaxp14StaxSourceXmlStreamReader() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
    StAXSource source = new StAXSource(streamReader);
    Object flights = unmarshaller.unmarshal(source);
    testFlights(flights);
  }

  @Test
  public void unmarshalJaxp14StaxSourceXmlEventReader() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(INPUT_STRING));
    StAXSource source = new StAXSource(eventReader);
    Object flights = unmarshaller.unmarshal(source);
    testFlights(flights);
  }

  @Test
  public void unmarshalPartialStaxSourceXmlStreamReader() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
    streamReader.nextTag(); // skip to flights
    assertThat(streamReader.getName()).as("Invalid element").isEqualTo(new QName("http://samples.springframework.org/flight", "flights"));
    streamReader.nextTag(); // skip to flight
    assertThat(streamReader.getName()).as("Invalid element").isEqualTo(new QName("http://samples.springframework.org/flight", "flight"));
    Source source = StaxUtils.createStaxSource(streamReader);
    Object flight = unmarshaller.unmarshal(source);
    testFlight(flight);
  }

}
