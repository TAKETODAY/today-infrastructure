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

package infra.web.server;

import infra.lang.Nullable;
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

  @Nullable
  private String[] excludedUserAgents = null;

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

  @Nullable
  public String[] getExcludedUserAgents() {
    return this.excludedUserAgents;
  }

  public void setExcludedUserAgents(@Nullable String[] excludedUserAgents) {
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
  public static boolean isEnabled(@Nullable Compression compression) {
    return compression != null && compression.enabled;
  }

}
