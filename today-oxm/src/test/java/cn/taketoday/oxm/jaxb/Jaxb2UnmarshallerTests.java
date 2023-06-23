/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.oxm.jaxb;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.oxm.AbstractUnmarshallerTests;
import cn.taketoday.oxm.jaxb.test.FlightType;
import cn.taketoday.oxm.jaxb.test.Flights;
import cn.taketoday.oxm.mime.MimeContainer;
import cn.taketoday.util.xml.StaxUtils;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.xml.bind.JAXBElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 * @author Biju Kunjummen
 * @author Sam Brannen
 */
public class Jaxb2UnmarshallerTests extends AbstractUnmarshallerTests<Jaxb2Marshaller> {

  private static final String INPUT_STRING = "<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
          "<tns:flight><tns:number>42</tns:number></tns:flight></tns:flights>";

  @Override
  protected Jaxb2Marshaller createUnmarshaller() throws Exception {
    Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
    unmarshaller.setContextPath("cn.taketoday.oxm.jaxb.test");
    unmarshaller.setSchema(new FileSystemResource("src/test/schema/flight.xsd"));
    unmarshaller.afterPropertiesSet();
    return unmarshaller;
  }

  @Override
  protected void testFlights(Object o) {
    Flights flights = (Flights) o;
    assertThat(flights).as("Flights is null").isNotNull();
    assertThat(flights.getFlight().size()).as("Invalid amount of flight elements").isEqualTo(1);
    testFlight(flights.getFlight().get(0));
  }

  @Override
  protected void testFlight(Object o) {
    FlightType flight = (FlightType) o;
    assertThat(flight).as("Flight is null").isNotNull();
    assertThat(flight.getNumber()).as("Number is invalid").isEqualTo(42L);
  }

  @Test
  public void marshalAttachments() throws Exception {
    unmarshaller = new Jaxb2Marshaller();
    unmarshaller.setClassesToBeBound(BinaryObject.class);
    unmarshaller.setMtomEnabled(true);
    unmarshaller.afterPropertiesSet();
    MimeContainer mimeContainer = mock();

    Resource logo = new ClassPathResource("spring-ws.png", getClass());
    DataHandler dataHandler = new DataHandler(new FileDataSource(logo.getFile()));

    given(mimeContainer.isXopPackage()).willReturn(true);
    given(mimeContainer.getAttachment("<6b76528d-7a9c-4def-8e13-095ab89e9bb7@http://springframework.org/spring-ws>")).willReturn(dataHandler);
    given(mimeContainer.getAttachment("<99bd1592-0521-41a2-9688-a8bfb40192fb@http://springframework.org/spring-ws>")).willReturn(dataHandler);
    given(mimeContainer.getAttachment("696cfb9a-4d2d-402f-bb5c-59fa69e7f0b3@spring-ws.png")).willReturn(dataHandler);
    String content = "<binaryObject xmlns='http://springframework.org/spring-ws'>" + "<bytes>" +
            "<xop:Include href='cid:6b76528d-7a9c-4def-8e13-095ab89e9bb7@http://springframework.org/spring-ws' xmlns:xop='http://www.w3.org/2004/08/xop/include'/>" +
            "</bytes>" + "<dataHandler>" +
            "<xop:Include href='cid:99bd1592-0521-41a2-9688-a8bfb40192fb@http://springframework.org/spring-ws' xmlns:xop='http://www.w3.org/2004/08/xop/include'/>" +
            "</dataHandler>" +
            "<swaDataHandler>696cfb9a-4d2d-402f-bb5c-59fa69e7f0b3@spring-ws.png</swaDataHandler>" +
            "</binaryObject>";

    StringReader reader = new StringReader(content);
    Object result = unmarshaller.unmarshal(new StreamSource(reader), mimeContainer);
    boolean condition = result instanceof BinaryObject;
    assertThat(condition).as("Result is not a BinaryObject").isTrue();
    BinaryObject object = (BinaryObject) result;
    assertThat(object.getBytes()).as("bytes property not set").isNotNull();
    assertThat(object.getBytes().length > 0).as("bytes property not set").isTrue();
    assertThat(object.getSwaDataHandler()).as("datahandler property not set").isNotNull();
  }

  @Test
  @Override
  @SuppressWarnings("unchecked")
  public void unmarshalPartialStaxSourceXmlStreamReader() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
    streamReader.nextTag(); // skip to flights
    streamReader.nextTag(); // skip to flight
    Source source = StaxUtils.createStaxSource(streamReader);
    JAXBElement<FlightType> element = (JAXBElement<FlightType>) unmarshaller.unmarshal(source);
    FlightType flight = element.getValue();
    testFlight(flight);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void unmarshalAnXmlReferringToAWrappedXmlElementDecl() throws Exception {
    // SPR-10714
    unmarshaller = new Jaxb2Marshaller();
    unmarshaller.setPackagesToScan(new String[] { "cn.taketoday.oxm.jaxb" });
    unmarshaller.afterPropertiesSet();
    Source source = new StreamSource(new StringReader(
            "<brand-airplane><name>test</name></brand-airplane>"));
    JAXBElement<Airplane> airplane = (JAXBElement<Airplane>) unmarshaller.unmarshal(source);
    assertThat(airplane.getValue().getName()).as("Unmarshalling via explicit @XmlRegistry tag should return correct type").isEqualTo("test");
  }

  @Test
  public void unmarshalFile() throws IOException {
    Resource resource = new ClassPathResource("jaxb2.xml", getClass());
    File file = resource.getFile();

    Flights f = (Flights) unmarshaller.unmarshal(new StreamSource(file));
    testFlights(f);
  }

}
