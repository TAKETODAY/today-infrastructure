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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

import infra.web.RequestContext;

/**
 * Provides access to a file or form item that was received within a {@code multipart/form-data} POST request.
 * <p>
 * The items contents are retrieved by calling {@link #getInputStream()}.
 * </p>
 * <p>
 * Instances of this class are created by accessing the iterator, returned by {@link DefaultMultipartParser#getItemIterator(RequestContext)}.
 * </p>
 * <p>
 * <em>Note</em>: There is an interaction between the iterator and its associated instances of {@link FileItemInput}: By invoking
 * {@link java.util.Iterator#hasNext()} on the iterator, you discard all data, which hasn't been read so far from the previous data.
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface FileItemInput extends FileItemHeadersProvider {

  /**
   * This exception is thrown, if an attempt is made to read data from the {@link InputStream}, which has been returned by
   * {@link FileItemInput#getInputStream()}, after {@link java.util.Iterator#hasNext()} has been invoked on the iterator, which created the
   * {@link FileItemInput}.
   */
  class ItemSkippedException extends FileUploadException {

    /**
     * The exceptions serial version UID, which is being used when serializing an exception instance.
     */
    private static final long serialVersionUID = 2;

    /**
     * Constructs an instance with a given detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    ItemSkippedException(final String message) {
      super(message);
    }

  }

  /**
   * Gets the content type passed by the browser or {@code null} if not defined.
   *
   * @return The content type passed by the browser or {@code null} if not defined.
   */
  @Nullable
  String getContentType();

  /**
   * Gets the name of the field in the multipart form corresponding to this file item.
   *
   * @return The name of the form field.
   */
  String getFieldName();

  /**
   * Opens an {@link InputStream}, which allows to read the items contents.
   *
   * @return The input stream, from which the items data may be read.
   * @throws IllegalStateException The method was already invoked on this item. It is not possible to recreate the data stream.
   * @throws IOException An I/O error occurred.
   * @see ItemSkippedException
   */
  InputStream getInputStream() throws IOException;

  /**
   * Gets the original file name in the client's file system, as provided by the browser (or other client software). In most cases, this will be the base file
   * name, without path information. However, some clients, such as the Opera browser, do include path information.
   *
   * @return The original file name in the client's file system.
   */
  String getName();

  /**
   * Tests whether or not a {@code FileItem} instance represents a simple form field.
   *
   * @return {@code true} if the instance represents a simple form field; {@code false} if it represents an uploaded file.
   */
  boolean isFormField();

}
