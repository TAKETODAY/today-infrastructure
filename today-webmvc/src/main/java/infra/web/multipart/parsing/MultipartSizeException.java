/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.multipart.parsing;

import java.io.Serial;

import infra.web.multipart.MultipartException;

/**
 * Signals that a requests permitted size is exceeded.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class MultipartSizeException extends MultipartException {

  @Serial
  private static final long serialVersionUID = 1;

  /**
   * The actual size of the request.
   */
  private final long actual;

  /**
   * The maximum permitted size of the request.
   */
  private final long permitted;

  /**
   * Constructs an instance.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
   * @param permitted The requests size limit.
   * @param actual The actual values for the request.
   */
  public MultipartSizeException(final String message, final long permitted, final long actual) {
    super(message);
    this.permitted = permitted;
    this.actual = actual;
  }

  /**
   * Gets the actual size of the request.
   *
   * @return The actual size of the request.
   */
  public long getActualSize() {
    return actual;
  }

  /**
   * Gets the limit size of the request.
   *
   * @return The limit size of the request.
   */
  public long getPermitted() {
    return permitted;
  }

}
