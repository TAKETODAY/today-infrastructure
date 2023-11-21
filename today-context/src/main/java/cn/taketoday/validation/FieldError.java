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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.validation;

import java.io.Serial;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Encapsulates a field error, that is, a reason for rejecting a specific
 * field value.
 *
 * <p>See the {@link DefaultMessageCodesResolver} javadoc for details on
 * how a message code list is built for a {@code FieldError}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultMessageCodesResolver
 * @since 4.0
 */
public class FieldError extends ObjectError {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String field;

  @Nullable
  private final Object rejectedValue;

  private final boolean bindingFailure;

  /**
   * Create a new FieldError instance.
   *
   * @param objectName the name of the affected object
   * @param field the affected field of the object
   * @param defaultMessage the default message to be used to resolve this message
   */
  public FieldError(String objectName, String field, String defaultMessage) {
    this(objectName, field, null, false, null, null, defaultMessage);
  }

  /**
   * Create a new FieldError instance.
   *
   * @param objectName the name of the affected object
   * @param field the affected field of the object
   * @param rejectedValue the rejected field value
   * @param bindingFailure whether this error represents a binding failure
   * (like a type mismatch); else, it is a validation failure
   * @param codes the codes to be used to resolve this message
   * @param arguments the array of arguments to be used to resolve this message
   * @param defaultMessage the default message to be used to resolve this message
   */
  public FieldError(String objectName, String field, @Nullable Object rejectedValue, boolean bindingFailure,
          @Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {

    super(objectName, codes, arguments, defaultMessage);
    Assert.notNull(field, "Field is required");
    this.field = field;
    this.rejectedValue = rejectedValue;
    this.bindingFailure = bindingFailure;
  }

  /**
   * Return the affected field of the object.
   */
  public String getField() {
    return this.field;
  }

  /**
   * Return the rejected field value.
   */
  @Nullable
  public Object getRejectedValue() {
    return this.rejectedValue;
  }

  /**
   * Return whether this error represents a binding failure
   * (like a type mismatch); otherwise it is a validation failure.
   */
  public boolean isBindingFailure() {
    return this.bindingFailure;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!super.equals(other)) {
      return false;
    }
    FieldError otherError = (FieldError) other;
    return getField().equals(otherError.getField())
            && ObjectUtils.nullSafeEquals(getRejectedValue(), otherError.getRejectedValue())
            && isBindingFailure() == otherError.isBindingFailure();
  }

  @Override
  public int hashCode() {
    int hashCode = super.hashCode();
    hashCode = 29 * hashCode + getField().hashCode();
    hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getRejectedValue());
    hashCode = 29 * hashCode + (isBindingFailure() ? 1 : 0);
    return hashCode;
  }

  @Override
  public String toString() {
    // We would preferably use ObjectUtils.nullSafeConciseToString(rejectedValue) here but
    // keep including the full nullSafeToString representation for backwards compatibility.
    return "Field error in object '" + getObjectName() + "' on field '" + this.field +
            "': rejected value [" + ObjectUtils.nullSafeToString(this.rejectedValue) + "]; " +
            resolvableToString();
  }

}
