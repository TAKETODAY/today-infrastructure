/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.oxm;

import java.lang.reflect.Type;

/**
 * Subinterface of {@link Unmarshaller} that has support for generics.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public interface GenericUnmarshaller extends Unmarshaller {

  /**
   * Indicates whether this marshaller can marshal instances of the supplied generic type.
   *
   * @param genericType the type that this marshaller is being asked if it can marshal
   * @return {@code true} if this marshaller can indeed marshal instances of the supplied type;
   * {@code false} otherwise
   */
  boolean supports(Type genericType);

}
