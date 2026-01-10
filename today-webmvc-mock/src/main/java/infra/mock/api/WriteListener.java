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
 * Callback notification mechanism that signals to the developer it's possible to write content without blocking.
 */
public interface WriteListener extends EventListener {

  /**
   * When an instance of the WriteListener is registered with a {@link MockOutputStream}, this method will be invoked
   * by the container the first time when it is possible to write data. Subsequently the container will invoke this method
   * if and only if the {@link MockOutputStream#isReady()} method has been called and has returned a
   * value of <code>false</code> and a write operation has subsequently become possible.
   *
   * @throws IOException if an I/O related error has occurred during processing
   */
  public void onWritePossible() throws IOException;

  /**
   * Invoked when an error occurs writing data using the non-blocking APIs.
   *
   * @param t the throwable to indicate why the write operation failed
   */
  public void onError(final Throwable t);

}
