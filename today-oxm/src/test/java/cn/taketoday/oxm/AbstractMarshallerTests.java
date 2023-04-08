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
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;

import cn.taketoday.util.xml.StaxUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
public abstract class AbstractMarshallerTests<M extends Marshaller> {

  protected static final String EXPECTED_STRING =
          "<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
                  "<tns:flight><tns:number>42</tns:number></tns:flight></tns:flights>";

  protected M marshaller;

  protected Object flights;

  @BeforeEach
  public final void setUp() throws Exception {
    marshaller = createMarshaller();
    flights = createFlights();
  }

  protected abstract M createMarshaller() throws Exception;

  protected abstract Object createFlights();

  @Test
  public void marshalDOMResult() throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
    Document result = builder.newDocument();
    DOMResult domResult = new DOMResult(result);
    marshaller.marshal(flights, domResult);
    Document expected = builder.newDocument();
    Element flightsElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:flights");
    Attr namespace = expected.createAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:tns");
    namespace.setNodeValue("http://samples.springframework.org/flight");
    flightsElement.setAttributeNode(namespace);
    expected.appendChild(flightsElement);
    Element flightElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:flight");
    flightsElement.appendChild(flightElement);
    Element numberElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:number");
    flightElement.appendChild(numberElement);
    Text text = expected.createTextNode("42");
    numberElement.appendChild(text);
    assertThat(XmlContent.of(result)).isSimilarToIgnoringWhitespace(expected);
  }

  @Test
  public void marshalEmptyDOMResult() throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
    DOMResult domResult = new DOMResult();
    marshaller.marshal(flights, domResult);
    boolean condition = domResult.getNode() instanceof Document;
    assertThat(condition).as("DOMResult does not contain a Document").isTrue();
    Document result = (Document) domResult.getNode();
    Document expected = builder.newDocument();
    Element flightsElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:flights");
    Attr namespace = expected.createAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:tns");
    namespace.setNodeValue("http://samples.springframework.org/flight");
    flightsElement.setAttributeNode(namespace);
    expected.appendChild(flightsElement);
    Element flightElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:flight");
    flightsElement.appendChild(flightElement);
    Element numberElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:number");
    flightElement.appendChild(numberElement);
    Text text = expected.createTextNode("42");
    numberElement.appendChild(text);
    assertThat(XmlContent.of(result)).isSimilarToIgnoringWhitespace(expected);
  }

  @Test
  public void marshalStreamResultWriter() throws Exception {
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    marshaller.marshal(flights, result);
    assertThat(XmlContent.of(writer)).isSimilarToIgnoringWhitespace(EXPECTED_STRING);
  }

  @Test
  public void marshalStreamResultOutputStream() throws Exception {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    StreamResult result = new StreamResult(os);
    marshaller.marshal(flights, result);
    assertThat(XmlContent.of(new String(os.toByteArray(), "UTF-8"))).isSimilarToIgnoringWhitespace(EXPECTED_STRING);
  }

  @Test
  public void marshalStaxResultStreamWriter() throws Exception {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    StringWriter writer = new StringWriter();
    XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
    Result result = StaxUtils.createStaxResult(streamWriter);
    marshaller.marshal(flights, result);
    assertThat(XmlContent.from(writer)).isSimilarToIgnoringWhitespace(EXPECTED_STRING);
  }

  @Test
  public void marshalStaxResultEventWriter() throws Exception {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    StringWriter writer = new StringWriter();
    XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
    Result result = StaxUtils.createStaxResult(eventWriter);
    marshaller.marshal(flights, result);
    assertThat(XmlContent.from(writer)).isSimilarToIgnoringWhitespace(EXPECTED_STRING);
  }

  @Test
  public void marshalJaxp14StaxResultStreamWriter() throws Exception {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    StringWriter writer = new StringWriter();
    XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
    StAXResult result = new StAXResult(streamWriter);
    marshaller.marshal(flights, result);
    assertThat(XmlContent.from(writer)).isSimilarToIgnoringWhitespace(EXPECTED_STRING);
  }

  @Test
  public void marshalJaxp14StaxResultEventWriter() throws Exception {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    StringWriter writer = new StringWriter();
    XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
    StAXResult result = new StAXResult(eventWriter);
    marshaller.marshal(flights, result);
    assertThat(XmlContent.from(writer)).isSimilarToIgnoringWhitespace(EXPECTED_STRING);
  }
}
