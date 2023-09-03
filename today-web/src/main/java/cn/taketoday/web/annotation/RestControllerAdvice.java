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

package cn.taketoday.web.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;

/**
 * A convenience annotation that is itself annotated with
 * {@link ControllerAdvice @ControllerAdvice}
 * and {@link ResponseBody @ResponseBody}.
 *
 * <p>Types that carry this annotation are treated as controller advice where
 * {@link ExceptionHandler @ExceptionHandler} methods assume
 * {@link ResponseBody @ResponseBody} semantics by default.
 *
 * <p><b>NOTE:</b> {@code @RestControllerAdvice} is processed if an appropriate
 * {@code HandlerMapping}-{@code HandlerAdapter} pair is configured such as the
 * {@code RequestMappingHandlerMapping}-{@code RequestMappingHandlerAdapter} pair
 * which are the default in the MVC Java config and the MVC namespace.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RestController
 * @see ControllerAdvice
 * @since 2.3.7 2019-06-18 14:27
 */
@Documented
@ResponseBody
@ControllerAdvice
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface RestControllerAdvice {

  /**
   * Alias for {@link ControllerAdvice#name}.
   *
   * @since 4.0
   */
  @AliasFor(annotation = ControllerAdvice.class)
  String[] name() default {};

  /**
   * Alias for the {@link #basePackages} attribute.
   * <p>Allows for more concise annotation declarations &mdash; for example,
   * {@code @RestControllerAdvice("org.my.pkg")} is equivalent to
   * {@code @RestControllerAdvice(basePackages = "org.my.pkg")}.
   *
   * @see #basePackages
   */
  @AliasFor(annotation = ControllerAdvice.class)
  String[] value() default {};

  /**
   * Array of base packages.
   * <p>Controllers that belong to those base packages or sub-packages thereof
   * will be included &mdash; for example,
   * {@code @RestControllerAdvice(basePackages = "org.my.pkg")} or
   * {@code @RestControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}.
   * <p>{@link #value} is an alias for this attribute, simply allowing for
   * more concise use of the annotation.
   * <p>Also consider using {@link #basePackageClasses} as a type-safe
   * alternative to String-based package names.
   */
  @AliasFor(annotation = ControllerAdvice.class)
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages} for specifying the packages
   * in which to select controllers to be advised by the {@code @RestControllerAdvice}
   * annotated class.
   * <p>Consider creating a special no-op marker class or interface in each package
   * that serves no purpose other than being referenced by this attribute.
   */
  @AliasFor(annotation = ControllerAdvice.class)
  Class<?>[] basePackageClasses() default {};

  /**
   * Array of classes.
   * <p>Controllers that are assignable to at least one of the given types
   * will be advised by the {@code @RestControllerAdvice} annotated class.
   */
  @AliasFor(annotation = ControllerAdvice.class)
  Class<?>[] assignableTypes() default {};

  /**
   * Array of annotations.
   * <p>Controllers that are annotated with at least one of the supplied annotation
   * types will be advised by the {@code @RestControllerAdvice} annotated class.
   * <p>Consider creating a custom composed annotation or use a predefined one,
   * like {@link RestController @RestController}.
   */
  @AliasFor(annotation = ControllerAdvice.class)
  Class<? extends Annotation>[] annotations() default {};

}

