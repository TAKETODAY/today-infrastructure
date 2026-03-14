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

package infra.http;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code HttpHeaders} object that can only be read, not written to.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/5 16:46
 */
final class ReadOnlyHttpHeaders extends HttpHeaders {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * An empty {@code HttpHeaders} instance (immutable).
   */
  public static final ReadOnlyHttpHeaders EMPTY = new ReadOnlyHttpHeaders(new DefaultHttpHeaders());

  private final HttpHeaders headers;

  private @Nullable MediaType cachedContentType;

  private @Nullable List<MediaType> cachedAccept;

  ReadOnlyHttpHeaders(HttpHeaders headers) {
    this.headers = headers;
  }

  @Override
  public @Nullable MediaType getContentType() {
    if (this.cachedContentType != null) {
      return this.cachedContentType;
    }
    else {
      MediaType contentType = super.getContentType();
      this.cachedContentType = contentType;
      return contentType;
    }
  }

  @Override
  public List<MediaType> getAccept() {
    if (this.cachedAccept != null) {
      return this.cachedAccept;
    }
    else {
      List<MediaType> accept = super.getAccept();
      this.cachedAccept = accept;
      return accept;
    }
  }

  @Override
  public void clearContentHeaders() {
    // No-op.
  }

  @Override
  public void add(String name, @Nullable String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected @Nullable List<String> setHeader(String name, String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(String key, @Nullable Collection<String> values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> setOrRemove(String name, @Nullable String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable List<String> setOrRemove(String name, String @Nullable [] value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable List<String> setOrRemove(String name, @Nullable Collection<String> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAll(@Nullable Map<String, List<String>> values) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected List<String> setHeader(String key, Collection<String> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> remove(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> keySet() {
    return Collections.unmodifiableSet(headers.keySet());
  }

  @Override
  public Set<Map.Entry<String, List<String>>> entrySet() {
    return Collections.unmodifiableSet(headers.entrySet());
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return headers.size();
  }

  @Override
  public boolean isEmpty() {
    return headers.isEmpty();
  }

  @Override
  public @Nullable List<String> get(String name) {
    List<String> values = this.headers.get(name);
    return values != null ? Collections.unmodifiableList(values) : null;
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    return Collections.unmodifiableMap(this.headers.toSingleValueMap());
  }

  @Override
  public @Nullable String getFirst(String name) {
    return headers.getFirst(name);
  }

  @Override
  public HttpHeaders asWritable() {
    return new DefaultHttpHeaders(headers);
  }

  @Override
  public HttpHeaders asReadOnly() {
    return this;
  }

}
