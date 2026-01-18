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

package infra.http;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Abstract base class for {@link HttpOutputMessage} decorators.
 *
 * <p>Provides a convenient base for wrapping {@link HttpOutputMessage} instances,
 * delegating all method calls to the wrapped instance by default.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/12 16:21
 */
public class HttpOutputMessageDecorator extends HttpMessageDecorator implements HttpOutputMessage {

  private final HttpOutputMessage delegate;

  protected HttpOutputMessageDecorator(HttpOutputMessage delegate) {
    super(delegate);
    this.delegate = delegate;
  }

  @Override
  public boolean supportsZeroCopy() {
    return delegate.supportsZeroCopy();
  }

  @Override
  public void setHeaders(@Nullable HttpHeaders headers) {
    delegate.setHeaders(headers);
  }

  @Override
  public void setHeader(String name, @Nullable String value) {
    delegate.setHeader(name, value);
  }

  @Override
  public void setContentType(@Nullable MediaType mediaType) {
    delegate.setContentType(mediaType);
  }

  @Override
  public void setContentLength(long length) {
    delegate.setContentLength(length);
  }

  @Override
  public void sendFile(Path file, long position, long count) throws IOException {
    delegate.sendFile(file, position, count);
  }

  @Override
  public void sendFile(File file, long position, long count) throws IOException {
    delegate.sendFile(file, position, count);
  }

  @Override
  public void sendFile(File file) throws IOException {
    delegate.sendFile(file);
  }

  @Override
  public boolean removeHeader(String name) {
    return delegate.removeHeader(name);
  }

  @Override
  public OutputStream getBody() throws IOException {
    return delegate.getBody();
  }

  @Override
  public void addHeaders(@Nullable HttpHeaders headers) {
    delegate.addHeaders(headers);
  }

  @Override
  public void addHeader(String name, @Nullable String value) {
    delegate.addHeader(name, value);
  }

}
