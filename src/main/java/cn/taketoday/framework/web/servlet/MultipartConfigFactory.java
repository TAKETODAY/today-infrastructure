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
import jakarta.servlet.MultipartConfigElement;

/**
 * Factory that can be used to create a {@link MultipartConfigElement}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class MultipartConfigFactory {

  private String location;

  private DataSize maxFileSize;

  private DataSize maxRequestSize;

  private DataSize fileSizeThreshold;

  /**
   * Sets the directory location where files will be stored.
   *
   * @param location the location
   */
  public void setLocation(String location) {
    this.location = location;
  }

  /**
   * Sets the maximum {@link DataSize size} allowed for uploaded files.
   *
   * @param maxFileSize the maximum file size
   */
  public void setMaxFileSize(DataSize maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  /**
   * Sets the maximum {@link DataSize} allowed for multipart/form-data requests.
   *
   * @param maxRequestSize the maximum request size
   */
  public void setMaxRequestSize(DataSize maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
  }

  /**
   * Sets the {@link DataSize size} threshold after which files will be written to disk.
   *
   * @param fileSizeThreshold the file size threshold
   */
  public void setFileSizeThreshold(DataSize fileSizeThreshold) {
    this.fileSizeThreshold = fileSizeThreshold;
  }

  /**
   * Create a new {@link MultipartConfigElement} instance.
   *
   * @return the multipart config element
   */
  public MultipartConfigElement createMultipartConfig() {
    long maxFileSizeBytes = convertToBytes(this.maxFileSize, -1);
    long maxRequestSizeBytes = convertToBytes(this.maxRequestSize, -1);
    long fileSizeThresholdBytes = convertToBytes(this.fileSizeThreshold, 0);
    return new MultipartConfigElement(this.location, maxFileSizeBytes, maxRequestSizeBytes,
            (int) fileSizeThresholdBytes);
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
