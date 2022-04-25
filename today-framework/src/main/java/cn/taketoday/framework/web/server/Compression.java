/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.server;

import cn.taketoday.util.DataSize;

/**
 * Simple server-independent abstraction for compression configuration.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 4.0
 */
public class Compression {

  private boolean enabled = false;

  private String[] mimeTypes = new String[] {
          "text/html", "text/xml", "text/plain", "text/css", "text/javascript",
          "application/javascript", "application/json", "application/xml"
  };

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

  public String[] getExcludedUserAgents() {
    return this.excludedUserAgents;
  }

  public void setExcludedUserAgents(String[] excludedUserAgents) {
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

}
