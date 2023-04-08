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

package cn.taketoday.oxm.jaxb;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;

/**
 * Used by {@link cn.taketoday.oxm.jaxb.Jaxb2MarshallerTests}.
 *
 * @author Arjen Poutsma
 */
public class Primitives {

  private static final QName NAME = new QName("https://springframework.org/oxm-test", "primitives");

  // following methods are used to test support for primitives
  public JAXBElement<Boolean> primitiveBoolean() {
    return new JAXBElement<>(NAME, Boolean.class, true);
  }

  public JAXBElement<Byte> primitiveByte() {
    return new JAXBElement<>(NAME, Byte.class, (byte) 42);
  }

  public JAXBElement<Short> primitiveShort() {
    return new JAXBElement<>(NAME, Short.class, (short) 42);
  }

  public JAXBElement<Integer> primitiveInteger() {
    return new JAXBElement<>(NAME, Integer.class, 42);
  }

  public JAXBElement<Long> primitiveLong() {
    return new JAXBElement<>(NAME, Long.class, 42L);
  }

  public JAXBElement<Double> primitiveDouble() {
    return new JAXBElement<>(NAME, Double.class, 42D);
  }

  public JAXBElement<byte[]> primitiveByteArray() {
    return new JAXBElement<>(NAME, byte[].class, new byte[] { 42 });
  }

}
