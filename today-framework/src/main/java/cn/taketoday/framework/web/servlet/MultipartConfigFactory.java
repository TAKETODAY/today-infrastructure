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

package cn.taketoday.framework.web.servlet;

import cn.taketoday.util.DataSize;
import cn.taketoday.web.multipart.MultipartConfig;
import jakarta.servlet.MultipartConfigElement;

/**
 * Factory that can be used to create a {@link MultipartConfigElement}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MultipartConfigFactory extends MultipartConfig {

  /**
   * Create a new {@link MultipartConfigElement} instance.
   *
   * @return the multipart config element
   */
  public MultipartConfigElement createMultipartConfig() {
    long maxFileSizeBytes = convertToBytes(getMaxFileSize(), -1);
    long maxRequestSizeBytes = convertToBytes(getMaxRequestSize(), -1);
    long fileSizeThresholdBytes = convertToBytes(getFileSizeThreshold(), 0);
    return new MultipartConfigElement(getLocation(), maxFileSizeBytes, maxRequestSizeBytes, (int) fileSizeThresholdBytes);
  }

  /**
   * Return the amount of bytes from the specified {@link DataSize size}. If the size is
   * {@code null} or negative, returns {@code defaultValue}.
   *
   * @param size the data size to handle
   * @param defaultValue the default value if the size is {@code null} or negative
   * @return the amount of bytes to use
   */
  private long convertToBytes(DataSize size, int defaultValue) {
    if (size != null && !size.isNegative()) {
      return size.toBytes();
    }
    return defaultValue;
  }

}
