/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.multipart.upload;

/**
 * Receives progress information. May be used to display a progress bar.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface ProgressListener {

  /**
   * Nop implementation.
   */
  ProgressListener NOP = (bytesRead, contentLength, items) -> {
    // nop
  };

  /**
   * Updates the listeners status information.
   *
   * @param bytesRead The total number of bytes, which have been read so far.
   * @param contentLength The total number of bytes, which are being read. May be -1, if this number is unknown.
   * @param items The number of the field, which is currently being read. (0 = no item so far, 1 = first item is being read, ...)
   */
  void update(long bytesRead, long contentLength, int items);

}
