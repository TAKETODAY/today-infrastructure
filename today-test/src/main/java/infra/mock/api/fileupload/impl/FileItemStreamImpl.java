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
package infra.mock.api.fileupload.impl;

import java.io.IOException;
import java.io.InputStream;

import infra.mock.api.fileupload.FileItemHeaders;
import infra.mock.api.fileupload.FileItemStream;
import infra.mock.api.fileupload.FileUploadException;
import infra.mock.api.fileupload.InvalidFileNameException;
import infra.mock.api.fileupload.MultipartStream.ItemInputStream;
import infra.mock.api.fileupload.util.Closeable;
import infra.mock.api.fileupload.util.LimitedInputStream;
import infra.mock.api.fileupload.util.Streams;

/**
 * Default implementation of {@link FileItemStream}.
 */
public class FileItemStreamImpl implements FileItemStream {

  /**
   * The file items content type.
   */
  private final String contentType;

  /**
   * The file items field name.
   */
  private final String fieldName;

  /**
   * The file items file name.
   */
  private final String name;

  /**
   * Whether the file item is a form field.
   */
  private final boolean formField;

  /**
   * The file items input stream.
   */
  private final InputStream stream;

  /**
   * The headers, if any.
   */
  private FileItemHeaders headers;

  /**
   * Creates a new instance.
   *
   * @param pFileItemIterator The {@link FileItemIteratorImpl iterator}, which returned this file
   * item.
   * @param pName The items file name, or null.
   * @param pFieldName The items field name.
   * @param pContentType The items content type, or null.
   * @param pFormField Whether the item is a form field.
   * @param pContentLength The items content length, if known, or -1
   * @throws IOException Creating the file item failed.
   * @throws FileUploadException Parsing the incoming data stream failed.
   */
  public FileItemStreamImpl(final FileItemIteratorImpl pFileItemIterator, final String pName, final String pFieldName,
          final String pContentType, final boolean pFormField,
          final long pContentLength) throws FileUploadException, IOException {
    name = pName;
    fieldName = pFieldName;
    contentType = pContentType;
    formField = pFormField;
    final long fileSizeMax = pFileItemIterator.getFileSizeMax();
    if (fileSizeMax != -1 && pContentLength != -1
            && pContentLength > fileSizeMax) {
      final FileSizeLimitExceededException e =
              new FileSizeLimitExceededException(
                      String.format("The field %s exceeds its maximum permitted size of %s bytes.", fieldName, fileSizeMax),
                      pContentLength, fileSizeMax);
      e.setFileName(pName);
      e.setFieldName(pFieldName);
      throw new FileUploadIOException(e);
    }
    // OK to construct stream now
    final ItemInputStream itemStream = pFileItemIterator.getMultiPartStream().newInputStream();
    InputStream istream = itemStream;
    if (fileSizeMax != -1) {
      istream = new LimitedInputStream(istream, fileSizeMax) {

        @Override
        protected void raiseError(final long pSizeMax, final long pCount) throws IOException {
          itemStream.close(true);
          final FileSizeLimitExceededException e = new FileSizeLimitExceededException(
                  String.format("The field %s exceeds its maximum permitted size of %s bytes.", fieldName, pSizeMax), pCount, pSizeMax);
          e.setFieldName(fieldName);
          e.setFileName(name);
          throw new FileUploadIOException(e);
        }
      };
    }
    stream = istream;
  }

  /**
   * Returns the items content type, or null.
   *
   * @return Content type, if known, or null.
   */
  @Override
  public String getContentType() {
    return contentType;
  }

  /**
   * Returns the items field name.
   *
   * @return Field name.
   */
  @Override
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Returns the items file name.
   *
   * @return File name, if known, or null.
   * @throws InvalidFileNameException The file name contains a NUL character,
   * which might be an indicator of a security attack. If you intend to
   * use the file name anyways, catch the exception and use
   * InvalidFileNameException#getName().
   */
  @Override
  public String getName() {
    return Streams.checkFileName(name);
  }

  /**
   * Returns, whether this is a form field.
   *
   * @return True, if the item is a form field,
   * otherwise false.
   */
  @Override
  public boolean isFormField() {
    return formField;
  }

  /**
   * Returns an input stream, which may be used to
   * read the items contents.
   *
   * @return Opened input stream.
   * @throws IOException An I/O error occurred.
   */
  @Override
  public InputStream openStream() throws IOException {
    if (((Closeable) stream).isClosed()) {
      throw new ItemSkippedException();
    }
    return stream;
  }

  /**
   * Closes the file item.
   *
   * @throws IOException An I/O error occurred.
   */
  public void close() throws IOException {
    stream.close();
  }

  /**
   * Returns the file item headers.
   *
   * @return The items header object
   */
  @Override
  public FileItemHeaders getHeaders() {
    return headers;
  }

  /**
   * Sets the file item headers.
   *
   * @param pHeaders The items header object
   */
  @Override
  public void setHeaders(final FileItemHeaders pHeaders) {
    headers = pHeaders;
  }

}
