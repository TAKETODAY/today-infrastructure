/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.mock.api.http;

import java.io.IOException;

import infra.mock.api.MockInputStream;
import infra.mock.api.MockOutputStream;

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
