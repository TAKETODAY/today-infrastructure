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
package cn.taketoday.mock.api.fileupload;

import java.io.IOException;
import java.util.List;

import cn.taketoday.mock.api.fileupload.impl.FileSizeLimitExceededException;
import cn.taketoday.mock.api.fileupload.impl.SizeLimitExceededException;

/**
 * An iterator, as returned by
 * {@link FileUploadBase#getItemIterator(RequestContext)}.
 */
public interface FileItemIterator {
  /**
   * Returns the maximum size of a single file. An {@link FileSizeLimitExceededException}
   * will be thrown, if there is an uploaded file, which is exceeding this value.
   * By default, this value will be copied from the {@link FileUploadBase#getFileSizeMax()
   * FileUploadBase} object, however, the user may replace the default value with a
   * request specific value by invoking {@link #setFileSizeMax(long)} on this object.
   *
   * @return The maximum size of a single, uploaded file. The value -1 indicates "unlimited".
   */
  long getFileSizeMax();

  /**
   * Sets the maximum size of a single file. An {@link FileSizeLimitExceededException}
   * will be thrown, if there is an uploaded file, which is exceeding this value.
   * By default, this value will be copied from the {@link FileUploadBase#getFileSizeMax()
   * FileUploadBase} object, however, the user may replace the default value with a
   * request specific value by invoking {@link #setFileSizeMax(long)} on this object, so
   * there is no need to configure it here.
   * <em>Note:</em>Changing this value doesn't affect files, that have already been uploaded.
   *
   * @param pFileSizeMax The maximum size of a single, uploaded file. The value -1 indicates "unlimited".
   */
  void setFileSizeMax(long pFileSizeMax);

  /**
   * Returns the maximum size of the complete HTTP request. A {@link SizeLimitExceededException}
   * will be thrown, if the HTTP request will exceed this value.
   * By default, this value will be copied from the {@link FileUploadBase#getSizeMax()
   * FileUploadBase} object, however, the user may replace the default value with a
   * request specific value by invoking {@link #setSizeMax(long)} on this object.
   *
   * @return The maximum size of the complete HTTP request. The value -1 indicates "unlimited".
   */
  long getSizeMax();

  /**
   * Returns the maximum size of the complete HTTP request. A {@link SizeLimitExceededException}
   * will be thrown, if the HTTP request will exceed this value.
   * By default, this value will be copied from the {@link FileUploadBase#getSizeMax()
   * FileUploadBase} object, however, the user may replace the default value with a
   * request specific value by invoking {@link #setSizeMax(long)} on this object.
   * <em>Note:</em> Setting the maximum size on this object will work only, if the iterator is not
   * yet initialized. In other words: If the methods {@link #hasNext()}, {@link #next()} have not
   * yet been invoked.
   *
   * @param pSizeMax The maximum size of the complete HTTP request. The value -1 indicates "unlimited".
   */
  void setSizeMax(long pSizeMax);

  /**
   * Returns, whether another instance of {@link FileItemStream}
   * is available.
   *
   * @return True, if one or more additional file items
   * are available, otherwise false.
   * @throws FileUploadException Parsing or processing the
   * file item failed.
   * @throws IOException Reading the file item failed.
   */
  boolean hasNext() throws FileUploadException, IOException;

  /**
   * Returns the next available {@link FileItemStream}.
   *
   * @return FileItemStream instance, which provides
   * access to the next file item.
   * @throws java.util.NoSuchElementException No more items are available. Use
   * {@link #hasNext()} to prevent this exception.
   * @throws FileUploadException Parsing or processing the
   * file item failed.
   * @throws IOException Reading the file item failed.
   */
  FileItemStream next() throws FileUploadException, IOException;

  List<FileItem> getFileItems() throws FileUploadException, IOException;
}
