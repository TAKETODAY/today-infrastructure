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

package cn.taketoday.http;

import java.io.IOException;
import java.io.OutputStream;

import cn.taketoday.util.StreamUtils;

/**
 * Represents an HTTP output message that allows for setting a streaming body.
 * Note that such messages typically do not support {@link #getBody()} access.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setBody
 * @since 4.0
 */
public interface StreamingHttpOutputMessage extends HttpOutputMessage {

  /**
   * Set the streaming body callback for this message.
   *
   * @param body the streaming body callback
   */
  void setBody(Body body);

  /**
   * write body
   */
  static void writeBody(HttpOutputMessage outputMessage, byte[] body) throws IOException {
    if (outputMessage instanceof StreamingHttpOutputMessage streaming) {
      streaming.setBody(new Body() {

        @Override
        public void writeTo(OutputStream outputStream) throws IOException {
          StreamUtils.copy(body, outputStream);
        }

        @Override
        public boolean repeatable() {
          return true;
        }
      });
    }
    else {
      StreamUtils.copy(body, outputMessage.getBody());
    }
  }

  /**
   * Defines the contract for bodies that can be written directly to an
   * {@link OutputStream}. Useful with HTTP client libraries that provide
   * indirect access to an {@link OutputStream} via a callback mechanism.
   */
  @FunctionalInterface
  interface Body {

    /**
     * Write this body to the given {@link OutputStream}.
     *
     * @param outputStream the output stream to write to
     * @throws IOException in case of I/O errors
     */
    void writeTo(OutputStream outputStream) throws IOException;

    /**
     * Indicates whether this body is capable of
     * {@linkplain #writeTo(OutputStream) writing its data} more than
     * once. Default implementation returns {@code false}.
     *
     * @return {@code true} if this body can be written repeatedly,
     * {@code false} otherwise
     */
    default boolean repeatable() {
      return false;
    }
  }

}
