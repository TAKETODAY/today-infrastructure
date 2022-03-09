/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.lang.Component;

/**
 * Specialization of {@link Component @Component} for classes that declare
 * {@link ExceptionHandler @ExceptionHandler} methods to be shared across
 * multiple {@code @Controller} classes.
 *
 * <p>Classes annotated with {@code @ControllerAdvice} can be declared explicitly
 * as Framework beans or auto-detected via classpath scanning. All such beans are
 * sorted based on {@link cn.taketoday.core.Ordered Ordered} semantics or
 * {@link Order @Order} /
 * {@link jakarta.annotation.Priority @Priority} declarations, with {@code Ordered}
 * semantics taking precedence over {@code @Order} / {@code @Priority} declarations.
 * {@code @ControllerAdvice} beans are then applied in that order at runtime.
 * Note, however, that {@code @ControllerAdvice} beans that implement
 * {@link cn.taketoday.core.PriorityOrdered PriorityOrdered} are <em>not</em>
 * given priority over {@code @ControllerAdvice} beans that implement {@code Ordered}.
 * In addition, {@code Ordered} is not honored for scoped {@code @ControllerAdvice}
 * beans &mdash; for example if such a bean has been configured as a request-scoped
 * or session-scoped bean.  For handling exceptions, an {@code @ExceptionHandler}
 * will be picked on the first advice with a matching exception handler method. For
 * model attributes and data binding initialization, {@code @ModelAttribute} and
 * {@code @InitBinder} methods will follow {@code @ControllerAdvice} order.
 *
 * <p>Note: For {@code @ExceptionHandler} methods, a root exception match will be
 * preferred to just matching a cause of the current exception, among the handler
 * methods of a particular advice bean. However, a cause match on a higher-priority
 * advice will still be preferred over any match (whether root or cause level)
 * on a lower-priority advice bean. As a consequence, please declare your primary
 * root exception mappings on a prioritized advice bean with a corresponding order.
 *
 * <p>By default, the methods in an {@code @ControllerAdvice} apply globally to
 * all controllers. Use selectors such as {@link #annotations},
 * {@link #basePackageClasses}, and {@link #basePackages} (or its alias
 * {@link #value}) to define a more narrow subset of targeted controllers.
 * If multiple selectors are declared, boolean {@code OR} logic is applied, meaning
 * selected controllers should match at least one selector. Note that selector checks
 * are performed at runtime, so adding many selectors may negatively impact
 * performance and add complexity.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @author TODAY
 * @see Controller
 * @see RestControllerAdvice
 * @since 2.3.7 2019-06-18 14:27
 */
@Component
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ControllerAdvice {

  /**
   * Alias for the {@link #basePackages} attribute.
   * <p>Allows for more concise annotation declarations &mdash; for example,
   * {@code @ControllerAdvice("org.my.pkg")} is equivalent to
   * {@code @ControllerAdvice(basePackages = "org.my.pkg")}.
   *
   * @see #basePackages
   * @since 4.0
   */
  @AliasFor("basePackages")
  String[] value() default {};

  /**
   * Array of base packages.
   * <p>Controllers that belong to those base packages or sub-packages thereof
   * will be included &mdash; for example,
   * {@code @ControllerAdvice(basePackages = "org.my.pkg")} or
   * {@code @ControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}.
   * <p>{@link #value} is an alias for this attribute, simply allowing for
   * more concise use of the annotation.
   * <p>Also consider using {@link #basePackageClasses} as a type-safe
   * alternative to String-based package names.
   *
   * @since 4.0
   */
  @AliasFor("value")
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages} for specifying the packages
   * in which to select controllers to be advised by the {@code @ControllerAdvice}
   * annotated class.
   * <p>Consider creating a special no-op marker class or interface in each package
   * that serves no purpose other than being referenced by this attribute.
   *
   * @since 4.0
   */
  Class<?>[] basePackageClasses() default {};

  /**
   * Array of classes.
   * <p>Controllers that are assignable to at least one of the given types
   * will be advised by the {@code @ControllerAdvice} annotated class.
   *
   * @since 4.0
   */
  Class<?>[] assignableTypes() default {};

  /**
   * Array of annotation types.
   * <p>Controllers that are annotated with at least one of the supplied annotation
   * types will be advised by the {@code @ControllerAdvice} annotated class.
   * <p>Consider creating a custom composed annotation or use a predefined one,
   * like {@link RestController @RestController}.
   *
   * @since 4.0
   */
  Class<? extends Annotation>[] annotations() default {};

}

