/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import infra.core.io.WritableResource;
import infra.lang.Assert;

/**
 * JSON content that can be written out.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/22 17:31
 */
@FunctionalInterface
public interface WritableJson {

  /**
   * Write the JSON to the provided {@link Appendable}.
   *
   * @param out the {@link Appendable} to receive the JSON
   * @throws IOException on IO error
   */
  void to(Appendable out) throws IOException;

  /**
   * Write the JSON to a {@link String}.
   *
   * @return the JSON string
   */
  default String toJsonString() {
    try {
      StringBuilder stringBuilder = new StringBuilder();
      to(stringBuilder);
      return stringBuilder.toString();
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Write the JSON to a UTF-8 encoded byte array.
   *
   * @return the JSON bytes
   */
  default byte[] toByteArray() {
    return toByteArray(StandardCharsets.UTF_8);
  }

  /**
   * Write the JSON to a byte array.
   *
   * @param charset the charset
   * @return the JSON bytes
   */
  default byte[] toByteArray(Charset charset) {
    Assert.notNull(charset, "'charset' is required");
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      toWriter(new OutputStreamWriter(out, charset));
      return out.toByteArray();
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Write the JSON to the provided {@link WritableResource} using
   * {@link StandardCharsets#UTF_8 UTF8} encoding.
   *
   * @param out the {@link OutputStream} to receive the JSON
   * @throws IOException on IO error
   */
  default void toResource(WritableResource out) throws IOException {
    Assert.notNull(out, "'out' is required");
    try (OutputStream outputStream = out.getOutputStream()) {
      toOutputStream(outputStream);
    }
  }

  /**
   * Write the JSON to the provided {@link WritableResource} using the given
   * {@link Charset}.
   *
   * @param out the {@link OutputStream} to receive the JSON
   * @param charset the charset to use
   * @throws IOException on IO error
   */
  default void toResource(WritableResource out, Charset charset) throws IOException {
    Assert.notNull(out, "'out' is required");
    Assert.notNull(charset, "'charset' is required");
    try (OutputStream outputStream = out.getOutputStream()) {
      toOutputStream(outputStream, charset);
    }
  }

  /**
   * Write the JSON to the provided {@link OutputStream} using
   * {@link StandardCharsets#UTF_8 UTF8} encoding. The output stream will not be
   * closed.
   *
   * @param out the {@link OutputStream} to receive the JSON
   * @throws IOException on IO error
   * @see #toOutputStream(OutputStream, Charset)
   */
  default void toOutputStream(OutputStream out) throws IOException {
    toOutputStream(out, StandardCharsets.UTF_8);
  }

  /**
   * Write the JSON to the provided {@link OutputStream} using the given
   * {@link Charset}. The output stream will not be closed.
   *
   * @param out the {@link OutputStream} to receive the JSON
   * @param charset the charset to use
   * @throws IOException on IO error
   */
  default void toOutputStream(OutputStream out, Charset charset) throws IOException {
    Assert.notNull(out, "'out' is required");
    Assert.notNull(charset, "'charset' is required");
    toWriter(new OutputStreamWriter(out, charset));
  }

  /**
   * Write the JSON to the provided {@link Writer}. The writer will be flushed but
   * not closed.
   *
   * @param out the {@link Writer} to receive the JSON
   * @throws IOException on IO error
   * @see #toOutputStream(OutputStream, Charset)
   */
  default void toWriter(Writer out) throws IOException {
    Assert.notNull(out, "'out' is required");
    to(out);
    out.flush();
  }

  /**
   * Factory method used to create a {@link WritableJson} with a sensible
   * {@link Object#toString()} that delegate to {@link WritableJson#toJsonString()}.
   *
   * @param writableJson the source {@link WritableJson}
   * @return a new {@link WritableJson} with a sensible {@link Object#toString()}.
   */
  static WritableJson of(WritableJson writableJson) {
    return new WritableJson() {

      @Override
      public void to(Appendable out) throws IOException {
        writableJson.to(out);
      }

      @Override
      public String toString() {
        return toJsonString();
      }

    };
  }

}