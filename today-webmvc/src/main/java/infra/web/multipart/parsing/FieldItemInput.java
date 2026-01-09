/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
  public final @Nullable String filename;

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
   * @param filename The items file name, or null.
   * @param fieldName The items field name.
   * @param contentType The items content type, or null.
   * @param formField Whether the item is a form field.
   * @throws MultipartException Parsing the incoming data stream failed.
   */
  FieldItemInput(final FieldItemInputIterator iterator, final @Nullable String filename, final String fieldName,
          final @Nullable MediaType contentType, final boolean formField, HttpHeaders headers) throws MultipartException {
    this.filename = filename;
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
