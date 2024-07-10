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

package cn.taketoday.aot.hint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aot.hint.MemberCategory;

/**
 * Register reflection hints against an arbitrary number of target classes.
 *
 * <p>When using this annotation directly, only the defined
 * {@linkplain #memberCategories() member categories} are registered for each
 * target class. The target classes can be specified by class or class names.
 * When both are specified, they are all considered. If no target class is
 * specified, the current class is used.
 *
 * <p>This annotation can be used as a meta-annotation to customize how hints
 * are registered against each target class.
 *
 * <p>The annotated element can be any bean:
 * <pre>{@code
 * @Configuration
 * @RegisterReflection(classes = CustomerEntry.class, memberCategories = PUBLIC_FIELDS)
 * public class MyConfig {
 *     // ...
 * }}</pre>
 *
 * <p>To register reflection hints for the type itself, only member categories
 * should be specified:<pre>{@code
 * @Component
 * @RegisterReflection(memberCategories = INVOKE_PUBLIC_METHODS)
 * public class MyComponent {
 *     // ...
 * }
 * }</pre>
 *
 * <p>Reflection hints can be registered from a method. In this case, at least
 * one target class should be specified:<pre>{@code
 * @Component
 * public class MyComponent {
 *
 *     @RegisterReflection(classes = CustomerEntry.class, memberCategories = PUBLIC_FIELDS)
 *     CustomerEntry process() { ... }
 *     // ...
 * }
 * }</pre>
 *
 * <p>If the class is not available, {@link #classNames()} allows to specify the
 * fully qualified name, rather than the {@link Class} reference.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective(RegisterReflectionReflectiveProcessor.class)
public @interface RegisterReflection {

  /**
   * Classes for which reflection hints should be registered. Consider using
   * {@link #classNames()} for classes that are not public in the current
   * scope. If both {@code classes} and {@code classNames} are specified, they
   * are merged in a single set.
   * <p>
   * By default, the annotated type is the target of the registration. When
   * placed on a method, at least one class must be specified.
   *
   * @see #classNames()
   */
  Class<?>[] classes() default {};

  /**
   * Alternative to {@link #classes()} to specify the classes as class names.
   *
   * @see #classes()
   */
  String[] classNames() default {};

  /**
   * Specify the {@linkplain MemberCategory member categories} to enable.
   */
  MemberCategory[] memberCategories() default {};

}
