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

package infra.context.properties.bind.validation;

import java.io.Serial;

import infra.lang.Nullable;
import infra.origin.Origin;
import infra.origin.OriginProvider;
import infra.validation.FieldError;

/**
 * {@link FieldError} implementation that tracks the source {@link Origin}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class OriginTrackedFieldError extends FieldError implements OriginProvider {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private final Origin origin;

  private OriginTrackedFieldError(FieldError fieldError, @Nullable Origin origin) {
    super(fieldError.getObjectName(), fieldError.getField(), fieldError.getRejectedValue(),
            fieldError.isBindingFailure(), fieldError.getCodes(), fieldError.getArguments(),
            fieldError.getDefaultMessage());
    this.origin = origin;
  }

  @Override
  @Nullable
  public Origin getOrigin() {
    return this.origin;
  }

  @Override
  public String toString() {
    if (this.origin == null) {
      return super.toString();
    }
    return super.toString() + "; origin " + this.origin;
  }

  @Nullable
  static FieldError of(@Nullable FieldError fieldError, @Nullable Origin origin) {
    if (fieldError == null || origin == null) {
      return fieldError;
    }
    return new OriginTrackedFieldError(fieldError, origin);
  }

}
