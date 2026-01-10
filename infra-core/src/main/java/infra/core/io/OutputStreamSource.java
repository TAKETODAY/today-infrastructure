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

package infra.core.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import infra.lang.Constant;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2.1.6 2019-07-08 00:11
 */
@FunctionalInterface
public interface OutputStreamSource {

  /**
   * Return an {@link OutputStream} for the underlying resource, allowing to
   * (over-)write its content.
   *
   * @throws IOException if the stream could not be opened
   */
  OutputStream getOutputStream() throws IOException;

  /**
   * Get {@link Writer}
   *
   * @throws IOException if the stream could not be opened
   */
  default Writer getWriter() throws IOException {
    return new OutputStreamWriter(getOutputStream(), Constant.DEFAULT_CHARSET);
  }

  /**
   * Return a {@link WritableByteChannel}.
   * <p>
   * It is expected that each call creates a <i>fresh</i> channel.
   * <p>
   * The default implementation returns {@link Channels#newChannel(OutputStream)}
   * with the result of {@link #getOutputStream()}.
   *
   * @return the byte channel for the underlying resource (must not be
   * {@code null})
   * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
   * @throws IOException if the content channel could not be opened
   * @see #getOutputStream()
   */
  default WritableByteChannel writableChannel() throws IOException {
    return Channels.newChannel(getOutputStream());
  }

}
