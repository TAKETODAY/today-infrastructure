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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.http.DefaultHttpHeaders;
import infra.http.HttpHeaders;
import infra.mock.api.http.Part;
import infra.util.ExceptionUtils;
import infra.util.FileCopyUtils;
import infra.util.StreamUtils;
import infra.web.multipart.support.AbstractPart;

/**
 * Servlet based {@link AbstractPart}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/13 11:05
 */
public final class MockFormData extends AbstractPart {
  private final Part part;

  @Nullable
  private String value;

  public MockFormData(Part part) {
    this.part = part;
  }

  @Override
  public String getContentAsString() {
    String value = this.value;
    if (value == null) {
      try {
        value = StreamUtils.copyToString(part.getInputStream());
      }
      catch (IOException e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
      this.value = value;
    }
    return value;
  }

  @Override
  public byte[] getContentAsByteArray() throws IOException {
    return FileCopyUtils.copyToByteArray(part.getInputStream());
  }

  @Override
  public boolean isFormField() {
    return true;
  }

  @Override
  public String getName() {
    return part.getName();
  }

  @Override
  public void cleanup() throws IOException {
    part.delete();
  }

  @Override
  protected HttpHeaders createHttpHeaders() {
    return createHeaders(part);
  }

  static DefaultHttpHeaders createHeaders(Part part) {
    DefaultHttpHeaders headers = HttpHeaders.forWritable();
    for (String headerName : part.getHeaderNames()) {
      headers.addAll(headerName, part.getHeaders(headerName));
    }

    if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
      headers.setOrRemove(HttpHeaders.CONTENT_TYPE, part.getContentType());
    }
    return headers;
  }

}
