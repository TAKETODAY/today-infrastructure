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

package cn.taketoday.core.serializer.support;

import cn.taketoday.core.NestedRuntimeException;

/**
 * Wrapper for the native IOException (or similar) when a
 * {@link cn.taketoday.core.serializer.Serializer} or
 * {@link cn.taketoday.core.serializer.Deserializer} failed.
 * Thrown by {@link SerializingConverter} and {@link DeserializingConverter}.
 *
 * @author Gary Russell
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SerializationFailedException extends NestedRuntimeException {

  /**
   * Construct a {@code SerializationException} with the specified detail message.
   *
   * @param message the detail message
   */
  public SerializationFailedException(String message) {
    super(message);
  }

  /**
   * Construct a {@code SerializationException} with the specified detail message
   * and nested exception.
   *
   * @param message the detail message
   * @param cause the nested exception
   */
  public SerializationFailedException(String message, Throwable cause) {
    super(message, cause);
  }

}
