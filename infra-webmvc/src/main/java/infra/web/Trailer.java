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

package infra.web;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.util.Assert;

/**
 * A response trailer field whose value is evaluated after the response body
 * has been produced and before the terminating message is sent.
 *
 * <p>Trailer fields are sent after the message body, as described in
 * <a href="https://datatracker.ietf.org/doc/html/rfc9110#section-6.5">RFC 9110,
 * Section 6.5</a>. The field name is declared when the trailer is registered,
 * allowing the transport to announce it in the {@code Trailer} response header
 * before the headers are committed. The supplier is invoked later, once the
 * body is complete; returning {@code null} skips this field for the current
 * response.
 *
 * <p>Instances are immutable records and can be created with
 * {@link #field(String, Supplier)} or the canonical constructor. Register them
 * through {@link HttpContext#setTrailerFields(Trailer...)}. Registering multiple
 * trailers with the same name produces multiple values for that field.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HttpContext#setTrailerFields(Trailer...)
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9110#section-6.5">RFC 9110, Section 6.5</a>
 * @since 5.0
 */
public record Trailer(String name, Supplier<@Nullable String> supplier) {

  /**
   * Creates a trailer field and validates its components.
   *
   * @param name the trailer field name; must not be blank
   * @param supplier evaluates the field value after the body is complete;
   * returns {@code null} to skip the field
   * @throws IllegalArgumentException if the name is blank or the supplier is
   * {@code null}
   */
  public Trailer {
    Assert.hasText(name, "Trailer name must not be empty");
    Assert.notNull(supplier, "Trailer supplier is required");
  }

  /**
   * Create a response trailer field.
   *
   * @param name the trailer field name; must not be blank
   * @param supplier evaluates the field value after the body is complete;
   * returns {@code null} to skip the field
   * @return a new {@code Trailer}
   * @throws IllegalArgumentException if the name is blank or the supplier is
   * {@code null}
   */
  public static Trailer field(String name, Supplier<@Nullable String> supplier) {
    return new Trailer(name, supplier);
  }

}
