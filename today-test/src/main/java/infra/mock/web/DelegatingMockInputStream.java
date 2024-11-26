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

package infra.mock.web;

import java.io.IOException;
import java.io.InputStream;

import infra.lang.Assert;
import infra.mock.api.MockInputStream;
import infra.mock.api.ReadListener;

/**
 * Delegating implementation of {@link MockInputStream}.
 *
 * <p>Used by {@link HttpMockRequestImpl}; typically not directly
 * used for testing application controllers.
 *
 * @author Juergen Hoeller
 * @see HttpMockRequestImpl
 * @since 4.0
 */
public class DelegatingMockInputStream extends MockInputStream {

  private final InputStream sourceStream;

  private boolean finished = false;

  /**
   * Create a DelegatingServletInputStream for the given source stream.
   *
   * @param sourceStream the source stream (never {@code null})
   */
  public DelegatingMockInputStream(InputStream sourceStream) {
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
