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

package infra.validation;

import infra.lang.Nullable;

/**
 * Extended variant of the {@link Validator} interface, adding support for
 * validation 'hints'.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface SmartValidator extends Validator {

  /**
   * Validate the supplied {@code target} object, which must be of a type of {@link Class}
   * for which the {@link #supports(Class)} method typically returns {@code true}.
   * <p>The supplied {@link Errors errors} instance can be used to report any
   * resulting validation errors.
   * <p><b>This variant of {@code validate()} supports validation hints, such as
   * validation groups against a JSR-303 provider</b> (in which case, the provided hint
   * objects need to be annotation arguments of type {@code Class}).
   * <p>Note: Validation hints may get ignored by the actual target {@code Validator},
   * in which case this method should behave just like its regular
   * {@link #validate(Object, Errors)} sibling.
   *
   * @param target the object that is to be validated
   * @param errors contextual state about the validation process
   * @param validationHints one or more hint objects to be passed to the validation engine
   * @see jakarta.validation.Validator#validate(Object, Class[])
   */
  void validate(Object target, Errors errors, Object... validationHints);

  /**
   * Validate the supplied value for the specified field on the target type,
   * reporting the same validation errors as if the value would be bound to
   * the field on an instance of the target class.
   *
   * @param targetType the target type
   * @param fieldName the name of the field
   * @param value the candidate value
   * @param errors contextual state about the validation process
   * @param validationHints one or more hint objects to be passed to the validation engine
   * @see jakarta.validation.Validator#validateValue(Class, String, Object, Class[])
   */
  default void validateValue(
          Class<?> targetType, String fieldName,
          @Nullable Object value, Errors errors, Object... validationHints) {

    throw new IllegalArgumentException("Cannot validate individual value for " + targetType);
  }

  /**
   * Return a contained validator instance of the specified type, unwrapping
   * as far as necessary.
   *
   * @param type the class of the object to return
   * @param <T> the type of the object to return
   * @return a validator instance of the specified type; {@code null} if there
   * isn't a nested validator; an exception may be raised if the specified
   * validator type does not match.
   */
  @Nullable
  default <T> T unwrap(@Nullable Class<T> type) {
    return null;
  }

}
