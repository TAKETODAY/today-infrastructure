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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import infra.util.MultiValueMap;

/**
 * {@code HttpHeaders} object that can only be read, not written to.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/5 16:46
 */
class ReadOnlyHttpHeaders extends DefaultHttpHeaders {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * An empty {@code HttpHeaders} instance (immutable).
   */
  @SuppressWarnings("unchecked")
  public static final ReadOnlyHttpHeaders EMPTY = new ReadOnlyHttpHeaders(MultiValueMap.EMPTY);

  @Nullable
  private MediaType cachedContentType;

  @Nullable
  private List<MediaType> cachedAccept;

  ReadOnlyHttpHeaders(MultiValueMap<String, String> headers) {
    super(headers);
  }

  @Nullable
  @Override
  public MediaType getContentType() {
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

  @Nullable
  @Override
  public List<String> get(Object name) {
    List<String> values = this.headers.get(name);
    return (values != null ? Collections.unmodifiableList(values) : null);
  }

  @Override
  public void add(String name, @Nullable String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAll(String key, @Nullable Collection<? extends String> values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAll(String key, @Nullable Enumeration<? extends String> values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> setOrRemove(String name, @Nullable String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAll(@Nullable Map<String, List<String>> values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> put(String key, List<String> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> remove(Object name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends String, ? extends List<String>> map) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    return Collections.unmodifiableMap(this.headers.toSingleValueMap());
  }

  @Override
  public Map<String, String> asSingleValueMap() {
    return Collections.unmodifiableMap(this.headers.asSingleValueMap());
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
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
