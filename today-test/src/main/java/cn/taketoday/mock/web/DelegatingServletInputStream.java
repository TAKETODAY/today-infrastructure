/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.mock.web;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.lang.Assert;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * Delegating implementation of {@link ServletInputStream}.
 *
 * <p>Used by {@link MockHttpServletRequest}; typically not directly
 * used for testing application controllers.
 *
 * @author Juergen Hoeller
 * @see MockHttpServletRequest
 * @since 4.0
 */
public class DelegatingServletInputStream extends ServletInputStream {

  private final InputStream sourceStream;

  private boolean finished = false;

  /**
   * Create a DelegatingServletInputStream for the given source stream.
   *
   * @param sourceStream the source stream (never {@code null})
   */
  public DelegatingServletInputStream(InputStream sourceStream) {
    Assert.notNull(sourceStream, "Source InputStream is required");
    this.sourceStream = sourceStream;
  }

  /**
   * Return the underlying source stream (never {@code null}).
   */
  public final InputStream getSourceStream() {
    return this.sourceStream;
  }

  @Override
  public int read() throws IOException {
    int data = this.sourceStream.read();
    if (data == -1) {
      this.finished = true;
    }
    return data;
  }

  @Override
  public int available() throws IOException {
    return this.sourceStream.available();
  }

  @Override
  public void close() throws IOException {
    super.close();
    this.sourceStream.close();
  }

  @Override
  public boolean isFinished() {
    return this.finished;
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setReadListener(ReadListener readListener) {
    throw new UnsupportedOperationException();
  }

}
