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

package cn.taketoday.web.mock;

import java.io.IOException;
import java.io.OutputStream;

import cn.taketoday.lang.Assert;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

/**
 * Delegating implementation of {@link jakarta.servlet.ServletOutputStream}.
 *
 * <p>Used by {@link MockHttpServletResponse}; typically not directly
 * used for testing application controllers.
 *
 * @author Juergen Hoeller
 * @see MockHttpServletResponse
 * @since 3.0
 */
public class DelegatingServletOutputStream extends ServletOutputStream {

  private final OutputStream targetStream;

  /**
   * Create a DelegatingServletOutputStream for the given target stream.
   *
   * @param targetStream
   *         the target stream (never {@code null})
   */
  public DelegatingServletOutputStream(OutputStream targetStream) {
    Assert.notNull(targetStream, "Target OutputStream must not be null");
    this.targetStream = targetStream;
  }

  /**
   * Return the underlying target stream (never {@code null}).
   */
  public final OutputStream getTargetStream() {
    return this.targetStream;
  }

  @Override
  public void write(int b) throws IOException {
    this.targetStream.write(b);
  }

  @Override
  public void flush() throws IOException {
    super.flush();
    this.targetStream.flush();
  }

  @Override
  public void close() throws IOException {
    super.close();
    this.targetStream.close();
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setWriteListener(WriteListener writeListener) {
    throw new UnsupportedOperationException();
  }

}
