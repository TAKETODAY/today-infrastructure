/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.framework.server.light;

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.context.utils.MediaType;
import cn.taketoday.web.http.HttpHeaders;

/**
 * The {@code Part} class encapsulates a single part of the multipart.
 *
 * @author TODAY 2021/4/13 10:51
 */
public class Part {

  public String name;
  public String filename;
  public HttpHeaders headers;
  public InputStream body;

  /**
   * Returns the part's name (form field name).
   *
   * @return the part's name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the part's filename (original filename entered in file form field).
   *
   * @return the part's filename, or null if there is none
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Returns the part's headers.
   *
   * @return the part's headers
   */
  public HttpHeaders getHeaders() {
    return headers;
  }

  /**
   * Returns the part's body (form field value).
   *
   * @return the part's body
   */
  public InputStream getBody() {
    return body;
  }

  /***
   * Returns the part's body as a string. If the part
   * headers do not specify a charset, UTF-8 is used.
   *
   * @return the part's body as a string
   * @throws IOException if an IO error occurs
   */
  public String getString() throws IOException {
    final MediaType contentType = headers.getContentType();
    final String charset = contentType.getParameter("charset");
    return Utils.readToken(body, -1, charset == null ? "UTF-8" : charset, 8192);
  }
}
