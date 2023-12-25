/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.io.Serial;

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

  @Serial
  private static final long serialVersionUID = 1L;

  private final boolean missingAfterConversion;

  public MissingRequestValueException(String msg) {
    this(msg, false);
  }

  public MissingRequestValueException(String msg, boolean missingAfterConversion) {
    super(msg);
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
