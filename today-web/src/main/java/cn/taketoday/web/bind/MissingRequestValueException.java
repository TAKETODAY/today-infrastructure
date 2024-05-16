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

package cn.taketoday.web.bind;

import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Nullable;

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
   * {@link cn.taketoday.context.MessageSource} code and arguments to
   * resolve the detail message with.
   *
   * @since 5.0
   */
  protected MissingRequestValueException(String msg, boolean missingAfterConversion,
          @Nullable String messageDetailCode, @Nullable Object[] messageDetailArguments) {

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
