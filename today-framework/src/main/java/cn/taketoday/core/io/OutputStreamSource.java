/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import cn.taketoday.lang.Constant;

/**
 * @author TODAY <br>
 * 2019-07-08 00:11
 * @since 2.1.6
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
