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

import java.util.Collection;
import java.util.List;

/**
 * A decorating {@link HttpMessage} implementation that delegates
 * all calls to another {@link HttpMessage}.
 * <p>Provides a convenient base for wrapping {@link HttpMessage} instances,
 * delegating all method calls to the wrapped instance by default.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/10 21:58
 */
public class DecoratingHttpMessage implements HttpMessage {

  private final HttpMessage delegate;

  protected DecoratingHttpMessage(HttpMessage delegate) {
    this.delegate = delegate;
  }

  public HttpMessage delegate() {
    return delegate;
  }

  @Override
  public HttpHeaders getHeaders() {
    return delegate.getHeaders();
  }

  @Override
  public @Nullable String getHeader(String name) {
    return delegate.getHeader(name);
  }

  @Override
  public boolean containsHeader(String name) {
    return delegate.containsHeader(name);
  }

  @Override
  public List<String> getHeaders(String name) {
    return delegate.getHeaders(name);
  }

  @Override
  public Collection<String> getHeaderNames() {
    return delegate.getHeaderNames();
  }

  @Override
  public @Nullable MediaType getContentType() {
    return delegate.getContentType();
  }

  @Override
  public @Nullable String getContentTypeAsString() {
    return delegate.getContentTypeAsString();
  }

  @Override
  public long getContentLength() {
    return delegate.getContentLength();
  }

  @Override
  public String toString() {
    return "%s [delegate=%s]".formatted(getClass().getSimpleName(), delegate);
  }

}
