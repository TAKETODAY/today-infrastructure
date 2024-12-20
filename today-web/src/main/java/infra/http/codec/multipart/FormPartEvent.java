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

package infra.http.codec.multipart;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import infra.http.ContentDisposition;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.lang.Nullable;
import reactor.core.publisher.Mono;

/**
 * Represents an event triggered for a form field. Contains the
 * {@linkplain #value() value}, besides the {@linkplain #headers() headers}
 * exposed through {@link PartEvent}.
 *
 * <p>Multipart form fields trigger one {@code FormPartEvent}, as
 * {@linkplain PartEvent described here}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface FormPartEvent extends PartEvent {

  /**
   * Return the form field value.
   */
  String value();

  /**
   * Creates a stream with a single {@code FormPartEven} based on the given
   * {@linkplain PartEvent#name() name} and {@linkplain #value() value}.
   *
   * @param name the name of the part
   * @param value the form field value
   * @return a single event stream
   */
  static Mono<FormPartEvent> create(String name, String value) {
    return create(name, value, null);
  }

  /**
   * Creates a stream with a single {@code FormPartEven} based on the given
   * {@linkplain PartEvent#name() name} and {@linkplain #value() value}.
   *
   * @param name the name of the part
   * @param value the form field value
   * @param headersConsumer used to change default headers. Can be {@code null}.
   * @return a single event stream
   */
  static Mono<FormPartEvent> create(String name, String value, @Nullable Consumer<HttpHeaders> headersConsumer) {
    Assert.hasLength(name, "Name must not be empty");
    Assert.notNull(value, "Value is required");

    return Mono.fromCallable(() -> {
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setContentType(MediaType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
      headers.setContentDisposition(ContentDisposition.formData().
              name(name)
              .build());
      if (headersConsumer != null) {
        headersConsumer.accept(headers);
      }
      return DefaultPartEvents.form(headers, value);
    });
  }

}
