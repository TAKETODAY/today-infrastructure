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
package cn.taketoday.web.mock.fileupload;

/**
 * The {@link ProgressListener} may be used to display a progress bar
 * or do stuff like that.
 */
public interface ProgressListener {

  /**
   * Updates the listeners status information.
   *
   * @param pBytesRead The total number of bytes, which have been read
   * so far.
   * @param pContentLength The total number of bytes, which are being
   * read. May be -1, if this number is unknown.
   * @param pItems The number of the field, which is currently being
   * read. (0 = no item so far, 1 = first item is being read, ...)
   */
  void update(long pBytesRead, long pContentLength, int pItems);

}
