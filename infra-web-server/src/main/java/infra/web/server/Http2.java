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

package infra.web.server;

import org.jspecify.annotations.Nullable;

import infra.context.properties.NestedConfigurationProperty;
import infra.util.DataSize;

/**
 * Simple server-independent abstraction for HTTP/2 configuration.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Http2 {

  private boolean enabled = false;

  /**
   * HTTP/2 initial settings for the connection.
   */
  @NestedConfigurationProperty
  public final InitialSettings initialSettings = new InitialSettings();

  /**
   * Return whether to enable HTTP/2 support, if the current environment supports it.
   *
   * @return {@code true} to enable HTTP/2 support
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns if Http2 is enabled for the given instance.
   *
   * @param http2 the {@link Http2} instance or {@code null}
   * @return {@code true} is Http2 is enabled
   */
  public static boolean isEnabled(@Nullable Http2 http2) {
    return http2 != null && http2.enabled;
  }

  /**
   * HTTP/2 initial settings for the connection.
   */
  public static class InitialSettings {

    /**
     * The maximum size of HTTP/2 frames that the peer is allowed to send.
     */
    public @Nullable DataSize maxFrameSize;

    /**
     * The maximum size of the header compression table used to decode header blocks.
     */
    public @Nullable DataSize maxHeaderListSize = DataSize.ofKilobytes(8);

    /**
     * The initial window size used by the peer to control the amount of data that can be sent.
     */
    public @Nullable Integer initialWindowSize;

    /**
     * The maximum number of concurrent streams that the peer allows.
     */
    public @Nullable Integer maxConcurrentStreams;

    /**
     * The size of the dynamic header table used for HPACK compression.
     */
    public @Nullable Integer headerTableSize;

    /**
     * Whether server push is enabled.
     */
    public @Nullable Boolean pushEnabled;

    /**
     * Whether CONNECT protocol is enabled.
     */
    public @Nullable Boolean connectProtocolEnabled;

  }

}
