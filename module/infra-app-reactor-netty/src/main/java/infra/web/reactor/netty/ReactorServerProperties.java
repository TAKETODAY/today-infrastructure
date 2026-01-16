/*
 * Copyright 2012-present the original author or authors.
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

package infra.web.reactor.netty;

import org.jspecify.annotations.Nullable;

import java.time.Duration;

import infra.context.properties.ConfigurationProperties;
import infra.util.DataSize;

/**
 * ReactorNetty properties.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/13 17:10
 */
@ConfigurationProperties("server.reactor-netty")
public class ReactorServerProperties {

  /**
   * Connection timeout of the Netty channel.
   */
  public @Nullable Duration connectionTimeout;

  /**
   * Maximum content length of an H2C upgrade request.
   */
  public DataSize h2cMaxContentLength = DataSize.ofBytes(0);

  /**
   * Initial buffer size for HTTP request decoding.
   */
  public DataSize initialBufferSize = DataSize.ofBytes(128);

  /**
   * Maximum chunk size that can be decoded for an HTTP request.
   */
  public DataSize maxChunkSize = DataSize.ofKilobytes(8);

  /**
   * Maximum size of the HTTP message header.
   */
  public DataSize maxHeaderSize = DataSize.ofKilobytes(8);

  /**
   * Maximum length that can be decoded for an HTTP request's initial line.
   */
  public DataSize maxInitialLineLength = DataSize.ofKilobytes(4);

  /**
   * Maximum number of requests that can be made per connection. By default, a
   * connection serves unlimited number of requests.
   */
  public @Nullable Integer maxKeepAliveRequests;

  /**
   * Whether to validate headers when decoding requests.
   */
  public boolean validateHeaders = true;

  /**
   * Idle timeout of the Netty channel. When not specified, an infinite timeout is
   * used.
   */
  public @Nullable Duration idleTimeout;

}
