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

import java.io.IOException;

import javax.xml.transform.Result;

/**
 * Defines the contract for Object XML Mapping Marshallers. Implementations of this interface
 * can serialize a given Object to an XML Stream.
 *
 * <p>Although the {@code marshal} method accepts a {@code java.lang.Object} as its
 * first parameter, most {@code Marshaller} implementations cannot handle arbitrary
 * {@code Object}s. Instead, an object class must be registered with the marshaller,
 * or have a common base class.
 *
 * @author Arjen Poutsma
 * @see Unmarshaller
 * @since 4.0
 */
public interface Marshaller {

  /**
   * Indicate whether this marshaller can marshal instances of the supplied type.
   *
   * @param clazz the class that this marshaller is being asked if it can marshal
   * @return {@code true} if this marshaller can indeed marshal instances of the supplied class;
   * {@code false} otherwise
   */
  boolean supports(Class<?> clazz);

  /**
   * Marshal the object graph with the given root into the provided {@link Result}.
   *
   * @param graph the root of the object graph to marshal
   * @param result the result to marshal to
   * @throws IOException if an I/O error occurs
   * @throws XmlMappingException if the given object cannot be marshalled to the result
   */
  void marshal(Object graph, Result result) throws IOException, XmlMappingException;

}
