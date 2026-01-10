/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.client.reactive;

import org.jspecify.annotations.Nullable;

import infra.core.NestedRuntimeException;

/**
 * Abstract base class for exception published by {@link WebClient} in case of errors.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class WebClientException extends NestedRuntimeException {

  /**
   * Construct a new instance of {@code WebClientException} with the given message.
   *
   * @param msg the message
   */
  public WebClientException(@Nullable String msg) {
    super(msg);
  }

  /**
   * Construct a new instance of {@code WebClientException} with the given message
   * and exception.
   *
   * @param msg the message
   * @param ex the exception
   */
  public WebClientException(@Nullable String msg, @Nullable Throwable ex) {
    super(msg, ex);
  }

}
