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

import org.apache.commons.io.input.BoundedInputStream;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;

import infra.http.HttpHeaders;
import infra.http.MediaType;

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
class FieldItemInput {

  /**
   * The file items content type.
   */
  private final @Nullable MediaType contentType;

  /**
   * The file items field name.
   */
  private final @Nullable String fieldName;

  /**
   * The file items file name.
   */
  private final @Nullable String fileName;

  /**
   * Whether the file item is a form field.
   */
  private final boolean formField;

  /**
   * The file items input stream.
   */
  private final InputStream inputStream;

  /**
   * The file items input stream closed flag.
   */
  private boolean inputStreamClosed;

  /**
   * The headers, if any.
   */
  private final HttpHeaders headers;

  /**
   * Creates a new instance.
   *
   * @param iterator The {@link FieldItemInputIterator iterator}, which returned this file item.
   * @param fileName The items file name, or null.
   * @param fieldName The items field name.
   * @param contentType The items content type, or null.
   * @param formField Whether the item is a form field.
   * @throws IOException Creating the file item failed.
   * @throws FileUploadException Parsing the incoming data stream failed.
   */
  FieldItemInput(final FieldItemInputIterator iterator, final @Nullable String fileName, final @Nullable String fieldName,
          final @Nullable MediaType contentType, final boolean formField, HttpHeaders headers) throws FileUploadException, IOException {
    this.fileName = fileName;
    this.fieldName = fieldName;
    this.contentType = contentType;
    this.formField = formField;
    this.headers = headers;
    final var fileSizeMax = iterator.getFileSizeMax();
    long contentLength = headers.getContentLength();
    if (fileSizeMax != -1 && contentLength != -1 && contentLength > fileSizeMax) {
      throw new FileUploadByteCountLimitException(String.format("The field %s exceeds its maximum permitted size of %s bytes.", fieldName, fileSizeMax),
              contentLength, fileSizeMax, fileName, fieldName);
    }
    // OK to construct stream now
    final var itemInputStream = iterator.multiPartInput.newInputStream();
    InputStream istream = itemInputStream;
    if (fileSizeMax != -1) {
      // onMaxLength will be called when the length is greater than _or equal to_ the supplied maxLength.
      // Because we only want to throw an exception when the length is greater than fileSizeMax, we
      // increment fileSizeMax by 1.
      istream = BoundedInputStream.builder()
              .setInputStream(istream)
              .setMaxCount(fileSizeMax + 1)
              .setOnMaxCount((max, count) -> {
                itemInputStream.close(true);
                throw new FileUploadByteCountLimitException(String.format(
                        "The field %s exceeds its maximum permitted size of %s bytes.", fieldName, fileSizeMax), count, fileSizeMax, fileName, fieldName);
              })
              .get();
    }
    this.inputStream = istream;
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
   * Gets the content type, or null.
   *
   * @return Content type, if known, or null.
   */
  public @Nullable MediaType getContentType() {
    return contentType;
  }

  /**
   * Gets the items field name.
   *
   * @return Field name.
   */
  public String getName() {
    return fieldName;
  }

  /**
   * Gets the headers.
   *
   * @return The items header object
   */
  public HttpHeaders getHeaders() {
    return headers;
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

  /**
   * Gets the file name.
   *
   * @return File name, if known, or null.
   * @throws InvalidPathException The file name is invalid, for example it contains a NUL character, which might be an indicator of a security attack. If you
   * intend to use the file name anyways, catch the exception and use InvalidPathException#getInput().
   */
  public @Nullable String getFilename() {
    return DefaultPart.checkFileName(fileName);
  }

  /**
   * Tests whether this is a form field.
   *
   * @return True, if the item is a form field, otherwise false.
   */
  public boolean isFormField() {
    return formField;
  }

}
