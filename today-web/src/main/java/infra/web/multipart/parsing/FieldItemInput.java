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

package infra.web.multipart.parsing;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.web.multipart.MultipartException;

/**
 * Provides access to a file or form item that was received within a {@code multipart/form-data} POST request.
 * <p>
 * The items contents are retrieved by calling {@link #getInputStream()}.
 * </p>
 * <p>
 * <em>Note</em>: There is an interaction between the iterator and its associated instances of {@link FieldItemInput}: By invoking
 * {@link java.util.Iterator#hasNext()} on the iterator, you discard all data, which hasn't been read so far from the previous data.
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class FieldItemInput {

  /**
   * The file items content type.
   */
  public final @Nullable MediaType contentType;

  /**
   * The file items file name.
   */
  public final @Nullable String fileName;

  /**
   * The file items field name.
   */
  public final String fieldName;

  /**
   * Whether the file item is a form field.
   */
  public final boolean formField;

  /**
   * The file items input stream closed flag.
   */
  private boolean inputStreamClosed;

  /**
   * The headers, if any.
   */
  public final HttpHeaders headers;

  /**
   * The file items input stream.
   */
  private final InputStream inputStream;

  /**
   * Creates a new instance.
   *
   * @param iterator The {@link FieldItemInputIterator iterator}, which returned this file item.
   * @param fileName The items file name, or null.
   * @param fieldName The items field name.
   * @param contentType The items content type, or null.
   * @param formField Whether the item is a form field.
   * @throws MultipartException Parsing the incoming data stream failed.
   */
  FieldItemInput(final FieldItemInputIterator iterator, final @Nullable String fileName, final String fieldName,
          final @Nullable MediaType contentType, final boolean formField, HttpHeaders headers) throws MultipartException {
    this.fileName = fileName;
    this.fieldName = fieldName;
    this.contentType = contentType;
    this.formField = formField;
    this.headers = headers;
    // OK to construct stream now
    this.inputStream = iterator.multiPartInput.newInputStream();
  }

  /**
   * Closes the file item.
   *
   * @throws IOException An I/O error occurred.
   */
  public void close() throws IOException {
    inputStream.close();
    inputStreamClosed = true;
  }

  /**
   * Gets the input stream, which may be used to read the items contents.
   *
   * @return Opened input stream.
   * @throws IOException An I/O error occurred.
   */
  public InputStream getInputStream() throws IOException {
    if (inputStreamClosed) {
      throw new ItemSkippedException("getInputStream()");
    }
    return inputStream;
  }

}
