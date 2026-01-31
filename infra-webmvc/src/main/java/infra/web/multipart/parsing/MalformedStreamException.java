/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.multipart.parsing;

import java.io.Serial;

import infra.web.server.MultipartException;

/**
 * Signals that the input stream fails to follow the required syntax.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class MalformedStreamException extends MultipartException {

  @Serial
  private static final long serialVersionUID = 2;

  /**
   * Constructs an {@code MalformedStreamException} with the specified detail message.
   *
   * @param message The detail message.
   */
  public MalformedStreamException(final String message) {
    super(message);
  }

  /**
   * Constructs an {@code MalformedStreamException} with the specified detail message.
   *
   * @param message The detail message.
   * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method). (A null value is permitted, and indicates that the
   * cause is nonexistent or unknown.)
   */
  public MalformedStreamException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
