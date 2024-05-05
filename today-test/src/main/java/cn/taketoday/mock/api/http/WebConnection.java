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

package cn.taketoday.mock.api.http;

import java.io.IOException;

import cn.taketoday.mock.api.MockInputStream;
import cn.taketoday.mock.api.MockOutputStream;

/**
 * This interface encapsulates the connection for an upgrade request. It allows the protocol handler to send service
 * requests and status queries to the container.
 */
public interface WebConnection extends AutoCloseable {
  /**
   * Returns an input stream for this web connection.
   *
   * @return a ServletInputStream for reading binary data
   * @throws IOException if an I/O error occurs
   */
  public MockInputStream getInputStream() throws IOException;

  /**
   * Returns an output stream for this web connection.
   *
   * @return a ServletOutputStream for writing binary data
   * @throws IOException if an I/O error occurs
   */
  public MockOutputStream getOutputStream() throws IOException;
}
