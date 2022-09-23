/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.validation.beanvalidation.InfraValidatorAdapter;

/**
 * Variant of JSR-303's {@link jakarta.validation.Valid}, supporting the
 * specification of validation groups. Designed for convenient use with
 * Framework's JSR-303 support but not JSR-303 specific.
 *
 * <p>Can be used e.g. with Framework MVC handler methods arguments.
 * Supported through {@link cn.taketoday.validation.SmartValidator}'s
 * validation hint concept, with validation group classes acting as hint objects.
 *
 * <p>Can also be used with method level validation, indicating that a specific
 * class is supposed to be validated at the method level (acting as a pointcut
 * for the corresponding validation interceptor), but also optionally specifying
 * the validation groups for method-level validation in the annotated class.
 * Applying this annotation at the method level allows for overriding the
 * validation groups for a specific method but does not serve as a pointcut;
 * a class-level annotation is nevertheless necessary to trigger method validation
 * for a specific bean to begin with. Can also be used as a meta-annotation on a
 * custom stereotype annotation or a custom group-specific validated annotation.
 *
 * @author Juergen Hoeller
 * @see jakarta.validation.Validator#validate(Object, Class[])
 * @see cn.taketoday.validation.SmartValidator#validate(Object, cn.taketoday.validation.Errors, Object...)
 * @see InfraValidatorAdapter
 * @see cn.taketoday.validation.beanvalidation.MethodValidationPostProcessor
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Validated {

  /**
   * Specify one or more validation groups to apply to the validation step
   * kicked off by this annotation.
   * <p>JSR-303 defines validation groups as custom annotations which an application declares
   * for the sole purpose of using them as type-safe group arguments, as implemented in
   * {@link InfraValidatorAdapter}.
   * <p>Other {@link cn.taketoday.validation.SmartValidator} implementations may
   * support class arguments in other ways as well.
   */
  Class<?>[] value() default {};

}
