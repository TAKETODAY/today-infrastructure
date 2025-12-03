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

import org.apache.commons.io.input.BoundedInputStream;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;

import infra.http.HttpHeaders;

/**
 * Default implementation of {@link FileItemInput}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class FileItemInputImpl implements FileItemInput {

  /**
   * The file items content type.
   */
  private final @Nullable String contentType;

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
  private HttpHeaders headers;

  /**
   * Creates a new instance.
   *
   * @param iterator The {@link FileItemInputIteratorImpl iterator}, which returned this file item.
   * @param fileName The items file name, or null.
   * @param fieldName The items field name.
   * @param contentType The items content type, or null.
   * @param formField Whether the item is a form field.
   * @param contentLength The items content length, if known, or -1
   * @throws IOException Creating the file item failed.
   * @throws FileUploadException Parsing the incoming data stream failed.
   */
  FileItemInputImpl(final FileItemInputIteratorImpl iterator, final @Nullable String fileName, final @Nullable String fieldName, final @Nullable String contentType,
          final boolean formField, final long contentLength) throws FileUploadException, IOException {
    this.fileName = fileName;
    this.fieldName = fieldName;
    this.contentType = contentType;
    this.formField = formField;
    final var fileSizeMax = iterator.getFileSizeMax();
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
                throw new FileUploadByteCountLimitException(
                        String.format("The field %s exceeds its maximum permitted size of %s bytes.", fieldName, fileSizeMax), count, fileSizeMax, fileName,
                        fieldName);
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
  @Override
  public @Nullable String getContentType() {
    return contentType;
  }

  /**
   * Gets the items field name.
   *
   * @return Field name.
   */
  @Override
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Gets the headers.
   *
   * @return The items header object
   */
  @Override
  public HttpHeaders getHeaders() {
    return headers;
  }

  /**
   * Gets the input stream, which may be used to read the items contents.
   *
   * @return Opened input stream.
   * @throws IOException An I/O error occurred.
   */
  @Override
  public InputStream getInputStream() throws IOException {
    if (inputStreamClosed) {
      throw new FileItemInput.ItemSkippedException("getInputStream()");
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
  @Override
  public String getName() {
    return DiskFileItem.checkFileName(fileName);
  }

  /**
   * Tests whether this is a form field.
   *
   * @return True, if the item is a form field, otherwise false.
   */
  @Override
  public boolean isFormField() {
    return formField;
  }

  /**
   * Sets the file item headers.
   *
   * @param headers The items header object
   */
  @Override
  public void setHeaders(final HttpHeaders headers) {
    this.headers = headers;
  }

}
