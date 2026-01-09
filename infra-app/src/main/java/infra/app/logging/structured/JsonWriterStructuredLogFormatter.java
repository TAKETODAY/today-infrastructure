/*
 * Copyright 2012-present the original author or authors.
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
