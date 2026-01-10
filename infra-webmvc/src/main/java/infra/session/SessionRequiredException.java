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

package infra.session;

import org.jspecify.annotations.Nullable;

import infra.core.NestedRuntimeException;

/**
 * Exception thrown when an HTTP request handler requires a pre-existing session.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:42
 */
public class SessionRequiredException extends NestedRuntimeException {

  @Nullable
  private final String expectedAttribute;

  /**
   * Create a new SessionRequiredException.
   *
   * @param msg the detail message
   */
  public SessionRequiredException(String msg) {
    super(msg);
    this.expectedAttribute = null;
  }

  /**
   * Create a new SessionRequiredException.
   *
   * @param msg the detail message
   * @param expectedAttribute the name of the expected session attribute
   */
  public SessionRequiredException(String msg, String expectedAttribute) {
    super(msg);
    this.expectedAttribute = expectedAttribute;
  }

  /**
   * Return the name of the expected session attribute, if any.
   */
  @Nullable
  public String getExpectedAttribute() {
    return this.expectedAttribute;
  }

}
