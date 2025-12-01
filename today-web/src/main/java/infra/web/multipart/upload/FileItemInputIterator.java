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

import java.io.IOException;

/**
 * An iterator, as returned by {@link FileUploadParser#getItemIterator(infra.web.RequestContext)}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface FileItemInputIterator {

  /**
   * Gets the maximum size of a single file. An {@link FileUploadByteCountLimitException} will be thrown, if there is an uploaded file, which is exceeding
   * this value. By default, this value will be copied from the {@link FileUploadParser#getMaxFileSize() FileUploadBase} object, however, the user may
   * replace the default value with a request specific value by invoking {@link #setFileSizeMax(long)} on this object.
   *
   * @return The maximum size of a single, uploaded file. The value -1 indicates "unlimited".
   */
  long getFileSizeMax();

  /**
   * Tests whether another instance of {@link FileItemInput} is available.
   *
   * @return True, if one or more additional file items are available, otherwise false.
   * @throws FileUploadException Parsing or processing the file item failed.
   */
  boolean hasNext() throws IOException;

  /**
   * Returns the next available {@link FileItemInput}.
   *
   * @return FileItemInput instance, which provides access to the next file item.
   * @throws java.util.NoSuchElementException No more items are available. Use {@link #hasNext()} to prevent this exception.
   * @throws FileUploadException Parsing or processing the file item failed.
   */
  FileItemInput next() throws IOException;

}
