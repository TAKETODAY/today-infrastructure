/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import cn.taketoday.lang.Constant;

/**
 * Simple interface for objects that are sources for an {@link InputStream}.
 *
 * <p>This is the base interface for Infra more extensive {@link Resource} interface.
 *
 * <p>For single-use streams, {@link InputStreamResource} can be used for any
 * given {@code InputStream}. Infra {@link ByteArrayResource} or any
 * file-based {@code Resource} implementation can be used as a concrete
 * instance, allowing one to read the underlying content stream multiple times.
 * This makes this interface useful as an abstract content source for mail
 * attachments, for example.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.io.InputStream
 * @see Resource
 * @see InputStreamResource
 * @see ByteArrayResource
 * @since 2.1.6 2019-07-08 00:12
 */
@FunctionalInterface
public interface InputStreamSource {

  /**
   * Return an {@link InputStream} for the content of an underlying resource.
   * <p>It is expected that each call creates a <i>fresh</i> stream.
   * <p>This requirement is particularly important when you consider an API such
   * as JavaMail, which needs to be able to read the stream multiple times when
   * creating mail attachments. For such a use case, it is <i>required</i>
   * that each {@code getInputStream()} call returns a fresh stream.
   *
   * @return the input stream for the underlying resource (must not be {@code null})
   * @throws java.io.FileNotFoundException if the underlying resource does not exist
   * @throws IOException if the content stream could not be opened
   * @see Resource#isReadable()
   */
  InputStream getInputStream() throws IOException;

  /**
   * Get {@link Reader}
   *
   * @throws IOException If an input exception occurs
   */
  default Reader getReader() throws IOException {
    return getReader(Constant.DEFAULT_ENCODING);
  }

  /**
   * Get {@link Reader}
   *
   * @param encoding Charset string
   * @throws IOException If an input exception occurs
   */
  default Reader getReader(String encoding) throws IOException {
    return new InputStreamReader(getInputStream(), encoding);
  }

  /**
   * Return a {@link ReadableByteChannel}.
   * <p>
   * It is expected that each call creates a <i>fresh</i> channel.
   * <p>
   * The default implementation returns {@link Channels#newChannel(InputStream)}
   * with the result of {@link #getInputStream()}.
   *
   * @return the byte channel for the underlying resource (must not be
   * {@code null})
   * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
   * @throws IOException if the content channel could not be opened
   * @see #getInputStream()
   */
  default ReadableByteChannel readableChannel() throws IOException {
    return Channels.newChannel(getInputStream());
  }

}
