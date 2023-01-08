/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.socket;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * An {@link HttpHeaders} variant that adds support for
 * the HTTP headers defined by the WebSocket specification RFC 6455.
 *
 * @author Rossen Stoyanchev
 * @author TODAY 2021/11/12 16:15
 * @since 4.0
 */
public class WebSocketHttpHeaders extends HttpHeaders {
  @Serial
  private static final long serialVersionUID = -6644521016187828916L;

  private final HttpHeaders headers;

  /**
   * Create a new instance.
   */
  public WebSocketHttpHeaders() {
    this(HttpHeaders.create());
  }

  /**
   * Create an instance that wraps the given pre-existing HttpHeaders and also
   * propagate all changes to it.
   *
   * @param headers the HTTP headers to wrap
   */
  public WebSocketHttpHeaders(HttpHeaders headers) {
    this.headers = headers;
  }

  /**
   * Sets the (new) value of the {@code Sec-WebSocket-Accept} header.
   *
   * @param secWebSocketAccept the value of the header
   */
  public void setSecWebSocketAccept(@Nullable String secWebSocketAccept) {
    set(SEC_WEBSOCKET_ACCEPT, secWebSocketAccept);
  }

  /**
   * Returns the value of the {@code Sec-WebSocket-Accept} header.
   *
   * @return the value of the header
   */
  @Nullable
  public String getSecWebSocketAccept() {
    return getFirst(SEC_WEBSOCKET_ACCEPT);
  }

  /**
   * Returns the value of the {@code Sec-WebSocket-Extensions} header.
   *
   * @return the value of the header
   */
  public List<WebSocketExtension> getSecWebSocketExtensions() {
    List<String> values = get(SEC_WEBSOCKET_EXTENSIONS);
    if (CollectionUtils.isEmpty(values)) {
      return Collections.emptyList();
    }
    else {
      ArrayList<WebSocketExtension> result = new ArrayList<>(values.size());
      for (String value : values) {
        result.addAll(WebSocketExtension.parseExtensions(value));
      }
      result.trimToSize();
      return result;
    }
  }

  /**
   * Sets the (new) value(s) of the {@code Sec-WebSocket-Extensions} header.
   *
   * @param extensions the values for the header
   */
  public void setSecWebSocketExtensions(List<WebSocketExtension> extensions) {
    List<String> result = new ArrayList<>(extensions.size());
    for (WebSocketExtension extension : extensions) {
      result.add(extension.toString());
    }
    set(SEC_WEBSOCKET_EXTENSIONS, toCommaDelimitedString(result));
  }

  /**
   * Sets the (new) value of the {@code Sec-WebSocket-Key} header.
   *
   * @param secWebSocketKey the value of the header
   */
  public void setSecWebSocketKey(@Nullable String secWebSocketKey) {
    set(SEC_WEBSOCKET_KEY, secWebSocketKey);
  }

  /**
   * Returns the value of the {@code Sec-WebSocket-Key} header.
   *
   * @return the value of the header
   */
  @Nullable
  public String getSecWebSocketKey() {
    return getFirst(SEC_WEBSOCKET_KEY);
  }

  /**
   * Sets the (new) value of the {@code Sec-WebSocket-Protocol} header.
   *
   * @param secWebSocketProtocol the value of the header
   */
  public void setSecWebSocketProtocol(String secWebSocketProtocol) {
    set(SEC_WEBSOCKET_PROTOCOL, secWebSocketProtocol);
  }

  /**
   * Sets the (new) value of the {@code Sec-WebSocket-Protocol} header.
   *
   * @param secWebSocketProtocols the value of the header
   */
  public void setSecWebSocketProtocol(List<String> secWebSocketProtocols) {
    set(SEC_WEBSOCKET_PROTOCOL, toCommaDelimitedString(secWebSocketProtocols));
  }

  /**
   * Returns the value of the {@code Sec-WebSocket-Key} header.
   *
   * @return the value of the header
   */
  public List<String> getSecWebSocketProtocol() {
    List<String> values = get(SEC_WEBSOCKET_PROTOCOL);
    if (CollectionUtils.isEmpty(values)) {
      return Collections.emptyList();
    }
    else if (values.size() == 1) {
      return getValuesAsList(SEC_WEBSOCKET_PROTOCOL);
    }
    else {
      return values;
    }
  }

  /**
   * Sets the (new) value of the {@code Sec-WebSocket-Version} header.
   *
   * @param secWebSocketVersion the value of the header
   */
  public void setSecWebSocketVersion(@Nullable String secWebSocketVersion) {
    set(SEC_WEBSOCKET_VERSION, secWebSocketVersion);
  }

  /**
   * Returns the value of the {@code Sec-WebSocket-Version} header.
   *
   * @return the value of the header
   */
  @Nullable
  public String getSecWebSocketVersion() {
    return getFirst(SEC_WEBSOCKET_VERSION);
  }

  // Single string methods

  /**
   * Return the first header value for the given header name, if any.
   *
   * @param headerName the header name
   * @return the first header value; or {@code null}
   */
  @Override
  @Nullable
  public String getFirst(String headerName) {
    return this.headers.getFirst(headerName);
  }

  /**
   * Add the given, single header value under the given name.
   *
   * @param headerName the header name
   * @param headerValue the header value
   * @throws UnsupportedOperationException if adding headers is not supported
   * @see #put(String, List)
   * @see #set(String, String)
   */
  @Override
  public void add(String headerName, @Nullable String headerValue) {
    this.headers.add(headerName, headerValue);
  }

  /**
   * Set the given, single header value under the given name.
   *
   * @param headerName the header name
   * @param headerValue the header value
   * @throws UnsupportedOperationException if adding headers is not supported
   * @see #put(String, List)
   * @see #add(String, String)
   */
  @Override
  public void set(String headerName, @Nullable String headerValue) {
    this.headers.set(headerName, headerValue);
  }

  @Override
  public void setAll(Map<String, String> values) {
    this.headers.setAll(values);
  }

  @Override
  public Map<String, String> toSingleValueMap() {
    return this.headers.toSingleValueMap();
  }

  // Map implementation

  @Override
  public int size() {
    return this.headers.size();
  }

  @Override
  public boolean isEmpty() {
    return this.headers.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return this.headers.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return this.headers.containsValue(value);
  }

  @Override
  public List<String> get(Object key) {
    return this.headers.get(key);
  }

  @Override
  public List<String> put(String key, List<String> value) {
    return this.headers.put(key, value);
  }

  @Override
  public List<String> remove(Object key) {
    return this.headers.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends List<String>> m) {
    this.headers.putAll(m);
  }

  @Override
  public void clear() {
    this.headers.clear();
  }

  @Override
  public Set<String> keySet() {
    return this.headers.keySet();
  }

  @Override
  public Collection<List<String>> values() {
    return this.headers.values();
  }

  @Override
  public Set<Map.Entry<String, List<String>>> entrySet() {
    return this.headers.entrySet();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof WebSocketHttpHeaders otherHeaders)) {
      return false;
    }
    return this.headers.equals(otherHeaders.headers);
  }

  @Override
  public int hashCode() {
    return this.headers.hashCode();
  }

  @Override
  public String toString() {
    return this.headers.toString();
  }

}
