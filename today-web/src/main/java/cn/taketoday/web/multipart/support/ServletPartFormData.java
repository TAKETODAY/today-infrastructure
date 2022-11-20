/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.multipart.support;

import java.io.IOException;

import cn.taketoday.http.DefaultHttpHeaders;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.web.multipart.FormData;
import jakarta.servlet.http.Part;

/**
 * Servlet based {@link FormData}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/13 11:05
 */
public final class ServletPartFormData extends AbstractMultipart {
  private final Part part;

  private String value;

  public ServletPartFormData(Part part) {
    this.part = part;
  }

  @Override
  public String getValue() {
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
  public boolean isFormField() {
    return true;
  }

  @Override
  public String getName() {
    return part.getName();
  }

  @Override
  public void delete() throws IOException {
    part.delete();
  }

  @Override
  protected HttpHeaders createHttpHeaders() {
    return createHeaders(part);
  }

  static DefaultHttpHeaders createHeaders(Part part) {
    DefaultHttpHeaders headers = HttpHeaders.create();
    for (String headerName : part.getHeaderNames()) {
      headers.addAll(headerName, part.getHeaders(headerName));
    }

    if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
      headers.set(HttpHeaders.CONTENT_TYPE, part.getContentType());
    }
    return headers;
  }

}
