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

package infra.web.server.netty;

import io.netty.handler.codec.http.HttpVersion;

/**
 * Transport protocol used by a Netty request channel.
 * <p>
 * The protocol is resolved once, when the channel pipeline is configured, and is
 * carried explicitly through the request context instead of being inferred from
 * the presence of a particular codec in the pipeline.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public enum HttpProtocol {

  /**
   * HTTP/1.1 over plaintext or TLS.
   */
  HTTP_1_1(HttpVersion.HTTP_1_1),

  /**
   * HTTP/2 over h2c (prior knowledge or upgrade) or TLS with ALPN.
   */
  HTTP_2(HttpVersion.valueOf("HTTP/2.0"));

  private final HttpVersion version;

  HttpProtocol(HttpVersion version) {
    this.version = version;
  }

  /**
   * Return the Netty {@link HttpVersion} that corresponds to this protocol.
   */
  public HttpVersion version() {
    return this.version;
  }

  /**
   * Return whether this protocol is HTTP/2.
   */
  public boolean isHttp2() {
    return this == HTTP_2;
  }

}
