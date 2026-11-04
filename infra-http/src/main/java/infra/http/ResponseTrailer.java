/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.http;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.util.Assert;

/**
 * A response trailer field whose value is evaluated after the response body
 * has been produced.
 *
 * <p>The field name is declared when the trailer is registered so that the
 * transport can announce it before the response headers are committed. The
 * supplier is invoked later, when the body is complete; returning {@code null}
 * from the supplier skips the field for this response.
 *
 * <p>Instances are immutable and can be created with {@link #of(String, Supplier)}.
 * Registering multiple trailers with the same name produces multiple values for
 * that field.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9110#section-6.5">RFC 9110, Section 6.5</a>
 * @since 5.0
 */
public record ResponseTrailer(String name, Supplier<@Nullable String> supplier) {

  /**
   * Validate the field name and the value supplier.
   *
   * @throws IllegalArgumentException if the name is empty
   * @throws IllegalArgumentException if the supplier is {@code null}
   */
  public ResponseTrailer {
    Assert.hasText(name, "Trailer name must not be empty");
    Assert.notNull(supplier, "Trailer supplier is required");
  }

  /**
   * Create a response trailer field.
   *
   * @param name the trailer field name
   * @param supplier evaluates the field value when the body is complete;
   * returns {@code null} to skip the field
   * @return a new {@code ResponseTrailer}
   */
  public static ResponseTrailer of(String name, Supplier<@Nullable String> supplier) {
    return new ResponseTrailer(name, supplier);
  }

}
