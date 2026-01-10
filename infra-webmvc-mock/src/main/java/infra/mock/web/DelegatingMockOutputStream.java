/*
 * Copyright 2002-present the original author or authors.
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

package infra.mock.web;

import java.io.IOException;
import java.io.OutputStream;

import infra.lang.Assert;
import infra.mock.api.MockOutputStream;
import infra.mock.api.WriteListener;

/**
 * Delegating implementation of {@link MockOutputStream}.
 *
 * <p>Used by {@link MockHttpResponseImpl}; typically not directly
 * used for testing application controllers.
 *
 * @author Juergen Hoeller
 * @see MockHttpResponseImpl
 * @since 4.0
 */
public class DelegatingMockOutputStream extends MockOutputStream {

  private final OutputStream targetStream;

  /**
   * Create a DelegatingServletOutputStream for the given target stream.
   *
   * @param targetStream the target stream (never {@code null})
   */
  public DelegatingMockOutputStream(OutputStream targetStream) {
    Assert.notNull(targetStream, "Target OutputStream is required");
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
