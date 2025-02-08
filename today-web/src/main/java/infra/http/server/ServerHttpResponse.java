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

package infra.http.server;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;

import infra.http.HttpOutputMessage;
import infra.http.HttpStatusCode;

/**
 * Represents a server-side HTTP response.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0
 */
public interface ServerHttpResponse extends HttpOutputMessage, Flushable, Closeable {

  /**
   * Set the HTTP status code of the response.
   *
   * @param status the HTTP status as an HttpStatus enum value
   */
  void setStatusCode(HttpStatusCode status);

  /**
   * Ensure that the headers and the content of the response are written out.
   * <p>After the first flush, headers can no longer be changed.
   * Only further content writing and content flushing is possible.
   * <p>
   * NOTE: Not recommended to use {@link OutputStream#flush() getBody().flush()}
   */
  @Override
  void flush() throws IOException;

  /**
   * Close this response, freeing any resources created.
   */
  @Override
  void close();

}
