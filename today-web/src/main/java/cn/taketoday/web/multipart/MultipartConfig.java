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

package cn.taketoday.web.multipart;

import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.DataSize;

/**
 * Properties to be used in configuring a {@link jakarta.servlet.MultipartConfigElement}.
 * <ul>
 * <li>{@link #getLocation() location} specifies the directory where uploaded files will
 * be stored. When not specified, a temporary directory will be used.</li>
 * <li>{@link #getMaxFileSize() max-file-size} specifies the maximum size permitted for
 * uploaded files. The default is 512MB</li>
 * <li>{@link #getMaxRequestSize() max-request-size} specifies the maximum size allowed
 * for {@literal multipart/form-data} requests. The default is 1GB.</li>
 * <li>{@link #getFileSizeThreshold() file-size-threshold} specifies the size threshold
 * after which files will be written to disk. The default is 0.</li>
 * </ul>
 * <p>
 * These properties are ultimately passed to {@link cn.taketoday.framework.web.servlet.MultipartConfigFactory} which means
 * you may specify numeric values using {@literal long} values or using more readable
 * {@link DataSize} variants.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-11 22:47
 */
public class MultipartConfig {

  /*** temp file upload location */
  @Nullable
  private String location;

  /**
   * Maximum size of a single uploaded file.
   */
  @Nullable
  private DataSize maxFileSize = DataSize.ofMegabytes(512); // every single file

  @Nullable
  private DataSize maxRequestSize = DataSize.ofGigabytes(1); // total size in every single request

  @Nullable
  private DataSize fileSizeThreshold = DataSize.ofBytes(0); // cache

  /**
   * Sets the {@link DataSize size} threshold after which files will be written to disk.
   *
   * @param fileSizeThreshold the file size threshold
   */
  public void setFileSizeThreshold(@Nullable DataSize fileSizeThreshold) {
    this.fileSizeThreshold = fileSizeThreshold;
  }

  /**
   * Sets the directory location where files will be stored.
   *
   * @param location the location
   */
  public void setLocation(@Nullable String location) {
    this.location = location;
  }

  /**
   * Sets the maximum {@link DataSize size} allowed for uploaded files.
   *
   * @param maxFileSize the maximum file size
   */
  public void setMaxFileSize(@Nullable DataSize maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  /**
   * Sets the maximum {@link DataSize} allowed for multipart/form-data requests.
   *
   * @param maxRequestSize the maximum request size
   */
  public void setMaxRequestSize(@Nullable DataSize maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
  }

  @Nullable
  public String getLocation() {
    return location;
  }

  @Nullable
  public DataSize getMaxRequestSize() {
    return maxRequestSize;
  }

  @Nullable
  public DataSize getMaxFileSize() {
    return maxFileSize;
  }

  @Nullable
  public DataSize getFileSizeThreshold() {
    return fileSizeThreshold;
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("location", location)
            .append("maxFileSize", maxFileSize)
            .append("maxRequestSize", maxRequestSize)
            .append("fileSizeThreshold", fileSizeThreshold)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final MultipartConfig that))
      return false;
    return Objects.equals(location, that.location)
            && Objects.equals(maxFileSize, that.maxFileSize)
            && Objects.equals(maxRequestSize, that.maxRequestSize)
            && Objects.equals(fileSizeThreshold, that.fileSizeThreshold);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, maxFileSize, maxRequestSize, fileSizeThreshold);
  }

}
