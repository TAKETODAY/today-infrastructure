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

package infra.http.converter;

import org.jspecify.annotations.Nullable;

import infra.http.HttpInputMessage;
import infra.lang.Assert;

/**
 * Thrown by {@link HttpMessageConverter} implementations when the
 * {@link HttpMessageConverter#read} method fails.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpMessageNotReadableException extends HttpMessageConversionException {

  @Nullable
  private final HttpInputMessage httpInputMessage;

  /**
   * Create a new HttpMessageNotReadableException.
   *
   * @param msg the detail message
   * @param httpInputMessage the original HTTP message
   */
  public HttpMessageNotReadableException(@Nullable String msg, @Nullable HttpInputMessage httpInputMessage) {
    super(msg);
    this.httpInputMessage = httpInputMessage;
  }

  /**
   * Create a new HttpMessageNotReadableException.
   *
   * @param msg the detail message
   * @param cause the root cause (if any)
   * @param httpInputMessage the original HTTP message
   */
  public HttpMessageNotReadableException(@Nullable String msg, @Nullable Throwable cause, @Nullable HttpInputMessage httpInputMessage) {
    super(msg, cause);
    this.httpInputMessage = httpInputMessage;
  }

  /**
   * Return the original HTTP message.
   */
  public HttpInputMessage getHttpInputMessage() {
    Assert.state(this.httpInputMessage != null, "No HttpInputMessage available - use non-deprecated constructors");
    return this.httpInputMessage;
  }

}
