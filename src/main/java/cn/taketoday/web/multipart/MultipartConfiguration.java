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
package cn.taketoday.web.multipart;

import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.DataSize;

/**
 * @author TODAY 2019-07-11 22:47
 */
public class MultipartConfiguration {

  /*** temp file upload location */
  private String location = System.getProperty("java.io.tmpdir");

  private String encoding = Constant.DEFAULT_ENCODING;

  /**
   * Maximum size of a single uploaded file.
   */
  private DataSize maxFileSize = DataSize.ofMegabytes(512); // every single file
  private DataSize maxRequestSize = DataSize.ofGigabytes(1); // total size in every single request
  private DataSize fileSizeThreshold = DataSize.ofGigabytes(1); // cache

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void setFileSizeThreshold(DataSize fileSizeThreshold) {
    this.fileSizeThreshold = fileSizeThreshold;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public void setMaxFileSize(DataSize maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  public void setMaxRequestSize(DataSize maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
  }

  public String getLocation() {
    return location;
  }

  public String getEncoding() {
    return encoding;
  }

  public DataSize getMaxRequestSize() {
    return maxRequestSize;
  }

  public DataSize getMaxFileSize() {
    return maxFileSize;
  }

  public DataSize getFileSizeThreshold() {
    return fileSizeThreshold;
  }

  @Override
  public String toString() {
    return ToStringBuilder.valueOf(this)
            .append("encoding", encoding)
            .append("maxFileSize", maxFileSize)
            .append("maxRequestSize", maxRequestSize)
            .append("fileSizeThreshold", fileSizeThreshold)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final MultipartConfiguration that))
      return false;
    return Objects.equals(location, that.location)
            && Objects.equals(encoding, that.encoding)
            && Objects.equals(maxFileSize, that.maxFileSize)
            && Objects.equals(maxRequestSize, that.maxRequestSize)
            && Objects.equals(fileSizeThreshold, that.fileSizeThreshold);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, encoding, maxFileSize, maxRequestSize, fileSizeThreshold);
  }

}
