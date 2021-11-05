/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.http.codec;

import cn.taketoday.logging.Logger;
import cn.taketoday.web.http.HttpLogging;

/**
 * Base class for {@link cn.taketoday.core.codec.Encoder},
 * {@link cn.taketoday.core.codec.Decoder}, {@link HttpMessageReader}, or
 * {@link HttpMessageWriter} that uses a logger and shows potentially sensitive
 * request data.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class LoggingCodecSupport {
  protected final Logger logger = HttpLogging.forLogName(getClass());

  /** Whether to log potentially sensitive info (form data at DEBUG and headers at TRACE). */
  private boolean enableLoggingRequestDetails = false;

  /**
   * Whether to log form data at DEBUG level, and headers at TRACE level.
   * Both may contain sensitive information.
   * <p>By default set to {@code false} so that request details are not shown.
   *
   * @param enable whether to enable or not
   */
  public void setEnableLoggingRequestDetails(boolean enable) {
    this.enableLoggingRequestDetails = enable;
  }

  /**
   * Whether any logging of values being encoded or decoded is explicitly
   * disabled regardless of log level.
   */
  public boolean isEnableLoggingRequestDetails() {
    return this.enableLoggingRequestDetails;
  }

}
