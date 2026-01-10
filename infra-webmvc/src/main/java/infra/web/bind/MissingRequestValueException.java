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

package infra.web.bind;

import org.jspecify.annotations.Nullable;

import infra.http.ProblemDetail;

/**
 * Base class for {@link RequestBindingException} exceptions that could
 * not bind because the request value is required but is either missing or
 * otherwise resolves to {@code null} after conversion.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MissingRequestValueException extends RequestBindingException {

  private final boolean missingAfterConversion;

  /**
   * Constructor with a message only.
   */
  public MissingRequestValueException(@Nullable String msg) {
    this(msg, false);
  }

  /**
   * Constructor with a message and a flag that indicates whether a value
   * was present but became {@code null} after conversion.
   */
  public MissingRequestValueException(@Nullable String msg, boolean missingAfterConversion) {
    super(msg);
    this.missingAfterConversion = missingAfterConversion;
  }

  /**
   * Constructor with a given {@link ProblemDetail}, and a
   * {@link infra.context.MessageSource} code and arguments to
   * resolve the detail message with.
   *
   * @since 5.0
   */
  protected MissingRequestValueException(String msg, boolean missingAfterConversion,
          @Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments) {

    super(msg, messageDetailCode, messageDetailArguments);
    this.missingAfterConversion = missingAfterConversion;
  }

  /**
   * Whether the request value was present but converted to {@code null}, e.g. via
   * {@code IdToEntityConverter}.
   */
  public boolean isMissingAfterConversion() {
    return this.missingAfterConversion;
  }

}
