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

import infra.lang.Contract;
import infra.util.DataSize;

/**
 * Simple server-independent abstraction for compression configuration.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Compression {

  private boolean enabled = false;

  private String[] mimeTypes = new String[] {
          "text/html", "text/xml", "text/plain", "text/css", "text/javascript",
          "application/javascript", "application/json", "application/xml"
  };

  private String @Nullable [] excludedUserAgents = null;

  private DataSize minResponseSize = DataSize.ofKilobytes(2);

  /**
   * Return whether response compression is enabled.
   *
   * @return {@code true} if response compression is enabled
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Return the MIME types that should be compressed.
   *
   * @return the MIME types that should be compressed
   */
  public String[] getMimeTypes() {
    return this.mimeTypes;
  }

  public void setMimeTypes(String[] mimeTypes) {
    this.mimeTypes = mimeTypes;
  }

  public String @Nullable [] getExcludedUserAgents() {
    return this.excludedUserAgents;
  }

  public void setExcludedUserAgents(String @Nullable [] excludedUserAgents) {
    this.excludedUserAgents = excludedUserAgents;
  }

  /**
   * Return the minimum "Content-Length" value that is required for compression to be
   * performed.
   *
   * @return the minimum content size in bytes that is required for compression
   */
  public DataSize getMinResponseSize() {
    return this.minResponseSize;
  }

  public void setMinResponseSize(DataSize minSize) {
    this.minResponseSize = minSize;
  }

  /**
   * Returns if Http2 is enabled for the given instance.
   *
   * @param compression the {@link Http2} instance or {@code null}
   * @return {@code true} is Http2 is enabled
   */
  @Contract("null -> false")
  public static boolean isEnabled(@Nullable Compression compression) {
    return compression != null && compression.enabled;
  }

}
