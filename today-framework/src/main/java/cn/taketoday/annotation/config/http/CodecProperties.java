/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.http;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.util.DataSize;

/**
 * {@link ConfigurationProperties properties} for reactive codecs.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "http.codec")
public class CodecProperties {

  /**
   * Whether to log form data at DEBUG level, and headers at TRACE level.
   */
  private boolean logRequestDetails;

  /**
   * Limit on the number of bytes that can be buffered whenever the input stream needs
   * to be aggregated. This applies only to the auto-configured WebFlux server and
   * WebClient instances. By default this is not set, in which case individual codec
   * defaults apply. Most codecs are limited to 256K by default.
   */
  private DataSize maxInMemorySize;

  public boolean isLogRequestDetails() {
    return this.logRequestDetails;
  }

  public void setLogRequestDetails(boolean logRequestDetails) {
    this.logRequestDetails = logRequestDetails;
  }

  public DataSize getMaxInMemorySize() {
    return this.maxInMemorySize;
  }

  public void setMaxInMemorySize(DataSize maxInMemorySize) {
    this.maxInMemorySize = maxInMemorySize;
  }

}
