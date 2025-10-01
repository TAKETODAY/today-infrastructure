/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.socket.client.support;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import infra.http.HttpHeaders;
import infra.lang.Assert;
import infra.util.DataSize;
import infra.util.StringUtils;
import infra.web.socket.WebSocketExtension;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

/**
 * Default ClientHandshakerFactory
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/7 11:06
 */
public class DefaultClientHandshakerFactory implements ClientHandshakerFactory {

  /**
   * Version of web socket specification to use to connect to the server
   */
  private WebSocketVersion webSocketVersion = WebSocketVersion.V13;

  /**
   * Allow extensions to be used in the reserved bits of the web socket frame
   */
  private boolean allowExtensions = true;

  /**
   * Maximum allowable frame payload length. Setting this value to your application's
   * requirement may reduce denial of service attacks using long data frames.
   */
  private DataSize maxFramePayload = DataSize.ofKilobytes(512);

  /**
   * Whether to mask all written websocket frames. This must be set to true in order to be fully compatible
   * with the websocket specifications. Client applications that communicate with a non-standard server
   * which doesn't require masking might set this to false to achieve a higher performance.
   */
  private boolean performMasking = true;

  /**
   * When set to true, frames which are not masked properly according to the standard will still be
   * accepted.
   */
  private boolean allowMaskMismatch = false;

  /**
   * Close the connection if it was not closed by the server after timeout specified
   */
  @Nullable
  private Duration forceCloseTimeout;

  /**
   * Use an absolute url for the Upgrade request, typically when connecting through an HTTP proxy over
   * clear HTTP
   */
  private boolean absoluteUpgradeUrl;

  /**
   * Allows to generate the `Origin`|`Sec-WebSocket-Origin` header value for handshake request
   * according to the given webSocketURL
   */
  private boolean generateOriginHeader;

  /**
   * Custom HTTP headers to send during the handshake for every websocket connection
   */
  @Nullable
  private HttpHeaders defaultHeaders;

  public void setAllowExtensions(boolean allowExtensions) {
    this.allowExtensions = allowExtensions;
  }

  public void setWebSocketVersion(WebSocketVersion webSocketVersion) {
    Assert.notNull(webSocketVersion, "WebSocket version is required");
    this.webSocketVersion = webSocketVersion;
  }

  public void setMaxFramePayload(DataSize maxFramePayload) {
    Assert.notNull(maxFramePayload, "maxFramePayload size is required");
    this.maxFramePayload = maxFramePayload;
  }

  public void setAbsoluteUpgradeUrl(boolean absoluteUpgradeUrl) {
    this.absoluteUpgradeUrl = absoluteUpgradeUrl;
  }

  public void setAllowMaskMismatch(boolean allowMaskMismatch) {
    this.allowMaskMismatch = allowMaskMismatch;
  }

  public void setDefaultHeaders(@Nullable HttpHeaders defaultHeaders) {
    this.defaultHeaders = defaultHeaders;
  }

  public void setForceCloseTimeout(@Nullable Duration forceCloseTimeout) {
    this.forceCloseTimeout = forceCloseTimeout;
  }

  public void setGenerateOriginHeader(boolean generateOriginHeader) {
    this.generateOriginHeader = generateOriginHeader;
  }

  public void setPerformMasking(boolean performMasking) {
    this.performMasking = performMasking;
  }

  @Override
  public WebSocketClientHandshaker create(URI uri, List<String> subProtocols, List<WebSocketExtension> extensions, HttpHeaders customHeaders) {
    long forceCloseTimeoutMillis = -1;
    if (forceCloseTimeout != null) {
      forceCloseTimeoutMillis = forceCloseTimeout.toMillis();
    }
    return WebSocketClientHandshakerFactory.newHandshaker(uri, webSocketVersion,
            StringUtils.collectionToCommaDelimitedString(subProtocols), allowExtensions,
            createHeaders(customHeaders), maxFramePayload.toBytesInt(), performMasking,
            allowMaskMismatch, forceCloseTimeoutMillis, absoluteUpgradeUrl, generateOriginHeader);
  }

  private DefaultHttpHeaders createHeaders(HttpHeaders headers) {
    DefaultHttpHeaders entries = new DefaultHttpHeaders();
    if (defaultHeaders != null) {
      for (var entry : defaultHeaders.entrySet()) {
        entries.add(entry.getKey(), entry.getValue());
      }
    }

    for (var entry : headers.entrySet()) {
      entries.add(entry.getKey(), entry.getValue());
    }
    return entries;
  }

}
