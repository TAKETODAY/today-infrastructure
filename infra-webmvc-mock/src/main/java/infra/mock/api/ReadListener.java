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

package infra.mock.api;

import java.io.IOException;
import java.util.EventListener;

/**
 * <p>
 * This class represents a call-back mechanism that will notify implementations as HTTP request data becomes available
 * to be read without blocking.
 * </p>
 */
public interface ReadListener extends EventListener {

  /**
   * When an instance of the <code>ReadListener</code> is registered with a {@link MockInputStream}, this method will
   * be invoked by the container the first time when it is possible to read data. Subsequently the container will invoke
   * this method if and only if the {@link MockInputStream#isReady()} method has been called and has
   * returned a value of <code>false</code> <em>and</em> data has subsequently become available to read.
   *
   * @throws IOException if an I/O related error has occurred during processing
   */
  void onDataAvailable() throws IOException;

  /**
   * Invoked when all data for the current request has been read.
   *
   * @throws IOException if an I/O related error has occurred during processing
   */
  void onAllDataRead() throws IOException;

  /**
   * Invoked when an error occurs processing the request.
   *
   * @param t the throwable to indicate why the read operation failed
   */
  void onError(Throwable t);

}
