/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import cn.taketoday.context.support.DefaultMessageSourceResolvable;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Encapsulates an object error, that is, a global reason for rejecting
 * an object.
 *
 * <p>See the {@link DefaultMessageCodesResolver} javadoc for details on
 * how a message code list is built for an {@code ObjectError}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FieldError
 * @see DefaultMessageCodesResolver
 * @since 4.0
 */
public class ObjectError extends DefaultMessageSourceResolvable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String objectName;

  @Nullable
  private transient Object source;

  /**
   * Create a new instance of the ObjectError class.
   *
   * @param objectName the name of the affected object
   * @param defaultMessage the default message to be used to resolve this message
   */
  public ObjectError(String objectName, String defaultMessage) {
    this(objectName, null, null, defaultMessage);
  }

  /**
   * Create a new instance of the ObjectError class.
   *
   * @param objectName the name of the affected object
   * @param codes the codes to be used to resolve this message
   * @param arguments the array of arguments to be used to resolve this message
   * @param defaultMessage the default message to be used to resolve this message
   */
  public ObjectError(
          String objectName, @Nullable String[] codes,
          @Nullable Object[] arguments, @Nullable String defaultMessage) {

    super(codes, arguments, defaultMessage);
    Assert.notNull(objectName, "Object name is required");
    this.objectName = objectName;
  }

  /**
   * Return the name of the affected object.
   */
  public String getObjectName() {
    return this.objectName;
  }

  /**
   * Preserve the source behind this error: possibly an {@link Exception}
   * (typically {@link cn.taketoday.beans.PropertyAccessException})
   * or a Bean Validation {@link jakarta.validation.ConstraintViolation}.
   * <p>Note that any such source object is being stored as transient:
   * that is, it won't be part of a serialized error representation.
   *
   * @param source the source object
   */
  public void wrap(Object source) {
    if (this.source != null) {
      throw new IllegalStateException("Already wrapping " + this.source);
    }
    this.source = source;
  }

  /**
   * Unwrap the source behind this error: possibly an {@link Exception}
   * (typically {@link cn.taketoday.beans.PropertyAccessException})
   * or a Bean Validation {@link jakarta.validation.ConstraintViolation}.
   * <p>The cause of the outermost exception will be introspected as well,
   * e.g. the underlying conversion exception or exception thrown from a setter
   * (instead of having to unwrap the {@code PropertyAccessException} in turn).
   *
   * @return the source object of the given type
   * @throws IllegalArgumentException if no such source object is available
   * (i.e. none specified or not available anymore after deserialization)
   */
  public <T> T unwrap(Class<T> sourceType) {
    if (sourceType.isInstance(this.source)) {
      return sourceType.cast(this.source);
    }
    else if (this.source instanceof Throwable) {
      Throwable cause = ((Throwable) this.source).getCause();
      if (sourceType.isInstance(cause)) {
        return sourceType.cast(cause);
      }
    }
    throw new IllegalArgumentException("No source object of the given type available: " + sourceType);
  }

  /**
   * Check the source behind this error: possibly an {@link Exception}
   * (typically {@link cn.taketoday.beans.PropertyAccessException})
   * or a Bean Validation {@link jakarta.validation.ConstraintViolation}.
   * <p>The cause of the outermost exception will be introspected as well,
   * e.g. the underlying conversion exception or exception thrown from a setter
   * (instead of having to unwrap the {@code PropertyAccessException} in turn).
   *
   * @return whether this error has been caused by a source object of the given type
   */
  public boolean contains(Class<?> sourceType) {
    return (sourceType.isInstance(this.source) ||
            (this.source instanceof Throwable && sourceType.isInstance(((Throwable) this.source).getCause())));
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || other.getClass() != getClass() || !super.equals(other)) {
      return false;
    }
    ObjectError otherError = (ObjectError) other;
    return getObjectName().equals(otherError.getObjectName());
  }

  @Override
  public int hashCode() {
    return (29 * super.hashCode() + getObjectName().hashCode());
  }

  @Override
  public String toString() {
    return "Error in object '" + this.objectName + "': " + resolvableToString();
  }

}
