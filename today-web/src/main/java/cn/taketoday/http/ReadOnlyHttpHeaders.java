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

package cn.taketoday.http;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;

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

  @Override
  public List<String> get(Object key) {
    List<String> values = this.headers.get(key);
    return (values != null ? Collections.unmodifiableList(values) : null);
  }

  @Override
  public void add(String headerName, @Nullable String headerValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAll(String key, @Nullable Collection<? extends String> values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAll(String key, Enumeration<? extends String> values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(String headerName, @Nullable String headerValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAll(Map<String, String> values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> put(String key, List<String> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends String, ? extends List<String>> map) {
    throw new UnsupportedOperationException();
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
