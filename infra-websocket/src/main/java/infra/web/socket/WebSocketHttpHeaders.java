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

package infra.web.socket;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.http.DefaultHttpHeaders;
import infra.http.HttpHeaders;
import infra.util.CollectionUtils;

/**
 * An {@link HttpHeaders} variant that adds support for
 * the HTTP headers defined by the WebSocket specification RFC 6455.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/12 16:15
 */
public class WebSocketHttpHeaders extends DefaultHttpHeaders {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Create a new instance.
   */
  public WebSocketHttpHeaders() {
  }

  /**
   * Create an instance that wraps the given pre-existing HttpHeaders and also
   * propagate all changes to it.
   *
   * @param headers the HTTP headers to wrap
   */
  public WebSocketHttpHeaders(HttpHeaders headers) {
    super(headers);
  }

  /**
   * Sets the (new) value of the {@code Sec-WebSocket-Accept} header.
   *
   * @param secWebSocketAccept the value of the header
   */
  public void setSecWebSocketAccept(@Nullable String secWebSocketAccept) {
    setOrRemove(SEC_WEBSOCKET_ACCEPT, secWebSocketAccept);
  }

  /**
   * Returns the value of the {@code Sec-WebSocket-Accept} header.
   *
   * @return the value of the header
   */
  public @Nullable String getSecWebSocketAccept() {
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
    setOrRemove(SEC_WEBSOCKET_EXTENSIONS, toCommaDelimitedString(result));
  }

  /**
   * Sets the (new) value of the {@code Sec-WebSocket-Key} header.
   *
   * @param secWebSocketKey the value of the header
   */
  public void setSecWebSocketKey(@Nullable String secWebSocketKey) {
    setOrRemove(SEC_WEBSOCKET_KEY, secWebSocketKey);
  }

  /**
   * Returns the value of the {@code Sec-WebSocket-Key} header.
   *
   * @return the value of the header
   */
  public @Nullable String getSecWebSocketKey() {
    return getFirst(SEC_WEBSOCKET_KEY);
  }

  /**
   * Sets the (new) value of the {@code Sec-WebSocket-Protocol} header.
   *
   * @param secWebSocketProtocol the value of the header
   */
  public void setSecWebSocketProtocol(@Nullable String secWebSocketProtocol) {
    setOrRemove(SEC_WEBSOCKET_PROTOCOL, secWebSocketProtocol);
  }

  /**
   * Sets the (new) value of the {@code Sec-WebSocket-Protocol} header.
   *
   * @param secWebSocketProtocols the value of the header
   */
  public void setSecWebSocketProtocol(@Nullable List<String> secWebSocketProtocols) {
    setOrRemove(SEC_WEBSOCKET_PROTOCOL, toCommaDelimitedString(secWebSocketProtocols));
  }

  /**
   * Returns the value of the {@code Sec-WebSocket-Protocol} header.
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
    setOrRemove(SEC_WEBSOCKET_VERSION, secWebSocketVersion);
  }

  /**
   * Returns the value of the {@code Sec-WebSocket-Version} header.
   *
   * @return the value of the header
   */
  public @Nullable String getSecWebSocketVersion() {
    return getFirst(SEC_WEBSOCKET_VERSION);
  }

}
