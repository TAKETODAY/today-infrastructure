
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

package cn.taketoday.oxm.jaxb.test;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the org.springframework.oxm.jaxb.test package.
 * &lt;p&gt;An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

  private final static QName _Flight_QNAME = new QName("http://samples.springframework.org/flight", "flight");

  /**
   * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.springframework.oxm.jaxb.test
   */
  public ObjectFactory() {
  }

  /**
   * Create an instance of {@link Flights }
   */
  public Flights createFlights() {
    return new Flights();
  }

  /**
   * Create an instance of {@link FlightType }
   */
  public FlightType createFlightType() {
    return new FlightType();
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link FlightType }{@code >}
   *
   * @param value Java instance representing xml element's value.
   * @return the new instance of {@link JAXBElement }{@code <}{@link FlightType }{@code >}
   */
  @XmlElementDecl(namespace = "http://samples.springframework.org/flight", name = "flight")
  public JAXBElement<FlightType> createFlight(FlightType value) {
    return new JAXBElement<FlightType>(_Flight_QNAME, FlightType.class, null, value);
  }

}
