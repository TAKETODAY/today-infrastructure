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

package cn.taketoday.web.mock.fileupload.servlet;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.web.mock.fileupload.FileUploadBase;
import cn.taketoday.web.mock.fileupload.UploadContext;
import cn.taketoday.web.mock.http.HttpServletRequest;

/**
 * <p>Provides access to the request information needed for a request made to
 * an HTTP servlet.</p>
 *
 * @since FileUpload 1.1
 */
public class MockRequestContext implements UploadContext {

  // ----------------------------------------------------- Instance Variables

  /**
   * The request for which the context is being provided.
   */
  private final HttpServletRequest request;

  // ----------------------------------------------------------- Constructors

  /**
   * Construct a context for this request.
   *
   * @param request The request to which this context applies.
   */
  public MockRequestContext(final HttpServletRequest request) {
    this.request = request;
  }

  // --------------------------------------------------------- Public Methods

  /**
   * Retrieve the character encoding for the request.
   *
   * @return The character encoding for the request.
   */
  @Override
  public String getCharacterEncoding() {
    return request.getCharacterEncoding();
  }

  /**
   * Retrieve the content type of the request.
   *
   * @return The content type of the request.
   */
  @Override
  public String getContentType() {
    return request.getContentType();
  }

  /**
   * Retrieve the content length of the request.
   *
   * @return The content length of the request.
   * @since FileUpload 1.3
   */
  @Override
  public long contentLength() {
    long size;
    try {
      size = Long.parseLong(request.getHeader(FileUploadBase.CONTENT_LENGTH));
    }
    catch (final NumberFormatException e) {
      size = request.getContentLength();
    }
    return size;
  }

  /**
   * Retrieve the input stream for the request.
   *
   * @return The input stream for the request.
   * @throws IOException if a problem occurs.
   */
  @Override
  public InputStream getInputStream() throws IOException {
    return request.getInputStream();
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object.
   */
  @Override
  public String toString() {
    return String.format("ContentLength=%s, ContentType=%s",
            this.contentLength(), this.getContentType());
  }

}
