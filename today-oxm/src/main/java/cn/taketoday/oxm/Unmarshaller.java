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

import javax.xml.transform.Source;

/**
 * Defines the contract for Object XML Mapping unmarshallers. Implementations of this
 * interface can deserialize a given XML Stream to an Object graph.
 *
 * @author Arjen Poutsma
 * @see Marshaller
 * @since 4.0
 */
public interface Unmarshaller {

  /**
   * Indicate whether this unmarshaller can unmarshal instances of the supplied type.
   *
   * @param clazz the class that this unmarshaller is being asked if it can marshal
   * @return {@code true} if this unmarshaller can indeed unmarshal to the supplied class;
   * {@code false} otherwise
   */
  boolean supports(Class<?> clazz);

  /**
   * Unmarshal the given {@link Source} into an object graph.
   *
   * @param source the source to marshal from
   * @return the object graph
   * @throws IOException if an I/O error occurs
   * @throws XmlMappingException if the given source cannot be mapped to an object
   */
  Object unmarshal(Source source) throws IOException, XmlMappingException;

}
