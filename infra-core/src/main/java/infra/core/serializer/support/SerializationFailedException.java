/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.serializer.support;

import infra.core.NestedRuntimeException;
import infra.core.serializer.Deserializer;
import infra.core.serializer.Serializer;

/**
 * Wrapper for the native IOException (or similar) when a
 * {@link Serializer} or
 * {@link Deserializer} failed.
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
