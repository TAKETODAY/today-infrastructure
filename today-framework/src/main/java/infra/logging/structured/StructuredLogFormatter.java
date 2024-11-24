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

package infra.logging.structured;

import java.nio.charset.Charset;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import infra.core.env.Environment;

/**
 * Formats a log event to a structured log message.
 * <p>
 * Implementing classes can declare the following parameter types in the constructor:
 * <ul>
 * <li>{@link Environment}</li>
 * </ul>
 * When using Logback, implementing classes can also use the following parameter types in
 * the constructor:
 * <ul>
 * <li>{@link ThrowableProxyConverter}</li>
 * </ul>
 *
 * @param <E> the log event type
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface StructuredLogFormatter<E> {

  /**
   * Formats the given log event to a String.
   *
   * @param event the log event to write
   * @return the formatted log event String
   */
  String format(E event);

  /**
   * Formats the given log event to a byte array.
   *
   * @param event the log event to write
   * @param charset the charset
   * @return the formatted log event bytes
   */
  default byte[] formatAsBytes(E event, Charset charset) {
    return format(event).getBytes(charset);
  }

}
