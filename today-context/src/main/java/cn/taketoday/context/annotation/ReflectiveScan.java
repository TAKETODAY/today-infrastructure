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

package cn.taketoday.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aot.hint.annotation.Reflective;
import cn.taketoday.aot.hint.annotation.RegisterReflection;
import cn.taketoday.core.annotation.AliasFor;

/**
 * Scan arbitrary types for use of {@link Reflective}. Typically used on
 * {@link Configuration @Configuration} classes but can be added to any bean.
 * Scanning happens during AOT processing, typically at build-time.
 *
 * <p>In the example below, {@code com.example.app} and its subpackages are
 * scanned: <pre>{@code
 * @Configuration
 * @ReflectiveScan("com.example.app")
 * class MyConfiguration {
 *     // ...
 * }}</pre>
 *
 * <p>Either {@link #basePackageClasses} or {@link #basePackages} (or its alias
 * {@link #value}) may be specified to define specific packages to scan. If specific
 * packages are not defined, scanning will occur recursively beginning with the
 * package of the class that declares this annotation.
 *
 * <p>A type does not need to be annotated at class level to be candidate, and
 * this performs a "deep scan" by loading every class in the target packages and
 * search for {@link Reflective} on types, constructors, methods, and fields.
 * Enclosed classes are candidates as well. Classes that fail to load are
 * ignored.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Reflective @Reflective
 * @see RegisterReflection @RegisterReflection
 * @since 5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ReflectiveScan {

  /**
   * Alias for {@link #basePackages}.
   * <p>Allows for more concise annotation declarations if no other attributes
   * are needed &mdash; for example, {@code @ReflectiveScan("org.my.pkg")}
   * instead of {@code @ReflectiveScan(basePackages = "org.my.pkg")}.
   */
  @AliasFor("basePackages")
  String[] value() default {};

  /**
   * Base packages to scan for reflective usage.
   * <p>{@link #value} is an alias for (and mutually exclusive with) this
   * attribute.
   * <p>Use {@link #basePackageClasses} for a type-safe alternative to
   * String-based package names.
   */
  @AliasFor("value")
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages} for specifying the packages
   * to scan for reflection usage. The package of each class specified will be scanned.
   * <p>Consider creating a special no-op marker class or interface in each package
   * that serves no purpose other than being referenced by this attribute.
   */
  Class<?>[] basePackageClasses() default {};

}
