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

package cn.taketoday.oxm.xstream;

import com.thoughtworks.xstream.security.AnyTypePermission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import cn.taketoday.util.xml.StaxUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class XStreamUnmarshallerTests {

  protected static final String INPUT_STRING = "<flight><flightNumber>42</flightNumber></flight>";

  private XStreamMarshaller unmarshaller;

  @BeforeEach
  public void createUnmarshaller() {
    unmarshaller = new XStreamMarshaller();
    unmarshaller.setTypePermissions(AnyTypePermission.ANY);
    Map<String, Class<?>> aliases = new HashMap<>();
    aliases.put("flight", Flight.class);
    unmarshaller.setAliases(aliases);
  }

  @Test
  public void unmarshalDomSource() throws Exception {
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(new InputSource(new StringReader(INPUT_STRING)));
    DOMSource source = new DOMSource(document);
    Object flight = unmarshaller.unmarshal(source);
    testFlight(flight);
  }

  @Test
  public void unmarshalStaxSourceXmlStreamReader() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
    Source source = StaxUtils.createStaxSource(streamReader);
    Object flights = unmarshaller.unmarshal(source);
    testFlight(flights);
  }

  @Test
  public void unmarshalStreamSourceInputStream() throws Exception {
    StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING.getBytes(StandardCharsets.UTF_8)));
    Object flights = unmarshaller.unmarshal(source);
    testFlight(flights);
  }

  @Test
  public void unmarshalStreamSourceReader() throws Exception {
    StreamSource source = new StreamSource(new StringReader(INPUT_STRING));
    Object flights = unmarshaller.unmarshal(source);
    testFlight(flights);
  }

  private void testFlight(Object o) {
    boolean condition = o instanceof Flight;
    assertThat(condition).as("Unmarshalled object is not Flights").isTrue();
    Flight flight = (Flight) o;
    assertThat(flight).as("Flight is null").isNotNull();
    assertThat(flight.getFlightNumber()).as("Number is invalid").isEqualTo(42L);
  }

}

