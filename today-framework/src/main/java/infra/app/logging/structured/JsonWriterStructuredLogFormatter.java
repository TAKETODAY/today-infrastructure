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

package infra.app.logging.structured;

import java.nio.charset.Charset;
import java.util.function.Consumer;

import infra.app.json.JsonWriter;
import infra.app.json.JsonWriter.Members;

/**
 * Base class for {@link StructuredLogFormatter} implementations that generates JSON using
 * a {@link JsonWriter}.
 *
 * @param <E> the log event type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public abstract class JsonWriterStructuredLogFormatter<E> implements StructuredLogFormatter<E> {

  private final JsonWriter<E> jsonWriter;

  /**
   * Create a new {@link JsonWriterStructuredLogFormatter} instance with the given
   * members.
   *
   * @param members a consumer, which should configure the members
   */
  protected JsonWriterStructuredLogFormatter(Consumer<Members<E>> members) {
    this(JsonWriter.of(members).withNewLineAtEnd());
  }

  /**
   * Create a new {@link JsonWriterStructuredLogFormatter} instance with the given
   * {@link JsonWriter}.
   *
   * @param jsonWriter the {@link JsonWriter}
   */
  protected JsonWriterStructuredLogFormatter(JsonWriter<E> jsonWriter) {
    this.jsonWriter = jsonWriter;
  }

  @Override
  public String format(E event) {
    return this.jsonWriter.writeToString(event);
  }

  @Override
  public byte[] formatAsBytes(E event, Charset charset) {
    return this.jsonWriter.write(event).toByteArray(charset);
  }

}
