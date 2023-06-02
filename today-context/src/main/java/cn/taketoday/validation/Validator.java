/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import cn.taketoday.lang.Assert;

/**
 * A validator for application-specific objects.
 *
 * <p>This interface is totally divorced from any infrastructure
 * or context; that is to say it is not coupled to validating
 * only objects in the web tier, the data-access tier, or the
 * whatever-tier. As such it is amenable to being used in any layer
 * of an application, and supports the encapsulation of validation
 * logic as a first-class citizen in its own right.
 *
 * <p>Find below a simple but complete {@code Validator}
 * implementation, which validates that the various {@link String}
 * properties of a {@code UserLogin} instance are not empty
 * (that is they are not {@code null} and do not consist
 * wholly of whitespace), and that any password that is present is
 * at least {@code 'MINIMUM_PASSWORD_LENGTH'} characters in length.
 *
 * <pre class="code">public class UserLoginValidator implements Validator {
 *
 *    private static final int MINIMUM_PASSWORD_LENGTH = 6;
 *
 *    public boolean supports(Class clazz) {
 *       return UserLogin.class.isAssignableFrom(clazz);
 *    }
 *
 *    public void validate(Object target, Errors errors) {
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userName", "field.required");
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "field.required");
 *       UserLogin login = (UserLogin) target;
 *       if (login.getPassword() != null
 *             &amp;&amp; login.getPassword().trim().length() &lt; MINIMUM_PASSWORD_LENGTH) {
 *          errors.rejectValue("password", "field.min.length",
 *                new Object[]{Integer.valueOf(MINIMUM_PASSWORD_LENGTH)},
 *                "The password must be at least [" + MINIMUM_PASSWORD_LENGTH + "] characters in length.");
 *       }
 *    }
 * }</pre>
 *
 * <p>See also the Framework reference manual for a fuller discussion of
 * the {@code Validator} interface and its role in an enterprise
 * application.
 *
 * @author Rod Johnson
 * @see SmartValidator
 * @see Errors
 * @see ValidationUtils
 * @since 4.0
 */
public interface Validator {

  /**
   * Can this {@link Validator} {@link #validate(Object, Errors) validate}
   * instances of the supplied {@code clazz}?
   * <p>This method is <i>typically</i> implemented like so:
   * <pre class="code">return Foo.class.isAssignableFrom(clazz);</pre>
   * (Where {@code Foo} is the class (or superclass) of the actual
   * object instance that is to be {@link #validate(Object, Errors) validated}.)
   *
   * @param clazz the {@link Class} that this {@link Validator} is
   * being asked if it can {@link #validate(Object, Errors) validate}
   * @return {@code true} if this {@link Validator} can indeed
   * {@link #validate(Object, Errors) validate} instances of the
   * supplied {@code clazz}
   */
  boolean supports(Class<?> clazz);

  /**
   * Validate the supplied {@code target} object, which must be
   * of a {@link Class} for which the {@link #supports(Class)} method
   * typically has (or would) return {@code true}.
   * <p>The supplied {@link Errors errors} instance can be used to report
   * any resulting validation errors.
   *
   * @param target the object that is to be validated
   * @param errors contextual state about the validation process
   * @see ValidationUtils
   */
  void validate(Object target, Errors errors);

  /**
   * Return a {@code Validator} that checks whether the target object
   * {@linkplain Class#isAssignableFrom(Class) is an instance of}
   * {@code targetClass}, resorting to {@code delegate} to populate
   * {@link Errors} if it is.
   *
   * <p>For instance:
   * <pre>{@code
   * Validator passwordEqualsValidator = Validator.forInstanceOf(PasswordResetForm.class, (form, errors) -> {
   *   if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
   * 	   errors.rejectValue("confirmPassword",
   * 	         "PasswordEqualsValidator.passwordResetForm.password",
   * 	         "password and confirm password must be same.");
   *     }
   *   });
   * }</pre>
   *
   * @param targetClass the class supported by the returned validator
   * @param delegate function invoked with the target object, if it is an
   * instance of type T
   * @param <T> the target object type
   * @return the created {@code Validator}
   */
  static <T> Validator forInstanceOf(Class<T> targetClass, BiConsumer<T, Errors> delegate) {
    return new TypedValidator<>(targetClass, targetClass::isAssignableFrom, delegate);
  }

  /**
   * Return a {@code Validator} that checks whether the target object's class
   * is identical to {@code targetClass}, resorting to {@code delegate} to
   * populate {@link Errors} if it is.
   *
   * <p>For instance:
   * <pre>{@code
   * Validator passwordEqualsValidator = Validator.forType(PasswordResetForm.class, (form, errors) -> {
   *   if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
   * 	   errors.rejectValue("confirmPassword",
   * 	         "PasswordEqualsValidator.passwordResetForm.password",
   * 	         "password and confirm password must be same.");
   *     }
   *   });
   * }</pre>
   *
   * @param targetClass the exact class supported by the returned validator (no subclasses)
   * @param delegate function invoked with the target object, if it is an
   * instance of type T
   * @param <T> the target object type
   * @return the created {@code Validator}
   */
  static <T> Validator forType(Class<T> targetClass, BiConsumer<T, Errors> delegate) {
    return new TypedValidator<>(targetClass, targetClass::equals, delegate);
  }

  /**
   * Validator instance returned by {@link Validator#forInstanceOf(Class, BiConsumer)}
   * and {@link Validator#forType(Class, BiConsumer)}.
   *
   * @param <T> the target object type
   * @author Toshiaki Maki
   * @author Arjen Poutsma
   */
  final class TypedValidator<T> implements Validator {

    private final Class<T> targetClass;
    private final Predicate<Class<?>> supports;
    private final BiConsumer<T, Errors> validate;

    public TypedValidator(Class<T> targetClass, Predicate<Class<?>> supports, BiConsumer<T, Errors> validate) {
      Assert.notNull(targetClass, "TargetClass is required");
      Assert.notNull(supports, "Supports function is required");
      Assert.notNull(validate, "Validate function is required");

      this.targetClass = targetClass;
      this.supports = supports;
      this.validate = validate;
    }

    @Override
    public boolean supports(Class<?> clazz) {
      return this.supports.test(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
      this.validate.accept(this.targetClass.cast(target), errors);
    }

  }

}
