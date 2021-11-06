/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.http.server;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import cn.taketoday.http.HttpOutputMessage;
import cn.taketoday.http.HttpStatus;

/**
 * Represents a server-side HTTP response.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ServerHttpResponse extends HttpOutputMessage, Flushable, Closeable {

  /**
   * Set the HTTP status code of the response.
   *
   * @param status the HTTP status as an HttpStatus enum value
   */
  void setStatusCode(HttpStatus status);

  /**
   * Ensure that the headers and the content of the response are written out.
   * <p>After the first flush, headers can no longer be changed.
   * Only further content writing and content flushing is possible.
   */
  @Override
  void flush() throws IOException;

  /**
   * Close this response, freeing any resources created.
   */
  @Override
  void close();

}
