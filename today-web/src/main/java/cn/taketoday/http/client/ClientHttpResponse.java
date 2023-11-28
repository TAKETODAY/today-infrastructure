/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.client;

import java.io.Closeable;
import java.io.IOException;

import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpStatusCode;

/**
 * Represents a client-side HTTP response.
 *
 * <p>Obtained via an invocation of {@link ClientHttpRequest#execute()}.
 *
 * <p>A {@code ClientHttpResponse} must be {@linkplain #close() closed},
 * typically in a {@code finally} block.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ClientHttpResponse extends HttpInputMessage, Closeable {

  /**
   * Get the HTTP status code as an {@link HttpStatusCode}.
   *
   * @return the HTTP status as {@code HttpStatusCode} value (never {@code null})
   * @throws IOException in case of I/O errors
   */
  HttpStatusCode getStatusCode() throws IOException;

  /**
   * Get the HTTP status code (potentially non-standard and not
   * resolvable through the {@link HttpStatusCode} enum) as an integer.
   *
   * @return the HTTP status as an integer value
   * @throws IOException in case of I/O errors
   * @see #getStatusCode()
   * @see HttpStatusCode#valueOf(int)
   */
  default int getRawStatusCode() throws IOException {
    return getStatusCode().value();
  }

  /**
   * Get the HTTP status text of the response.
   *
   * @return the HTTP status text
   * @throws IOException in case of I/O errors
   */
  String getStatusText() throws IOException;

  /**
   * Close this response, freeing any resources created.
   */
  @Override
  void close();

}
