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

package cn.taketoday.orm.mybatis;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.core.annotation.AliasFor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/18 22:27
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@ComponentScan(loadingStrategies = MybatisConfiguration.class)
public @interface MapperScan {

  /**
   * Alias for {@link #basePackages}.
   * <p>Allows for more concise annotation declarations if no other attributes
   * are needed &mdash; for example, {@code @ComponentScan("org.my.pkg")}
   * instead of {@code @ComponentScan(basePackages = "org.my.pkg")}.
   */
  @AliasFor(attribute = "basePackages", annotation = ComponentScan.class)
  String[] value() default {};

  /**
   * Base packages to scan for annotated components.
   * <p>{@link #value} is an alias for (and mutually exclusive with) this
   * attribute.
   * <p>Use {@link #basePackageClasses} for a type-safe alternative to
   * String-based package names.
   */
  @AliasFor(attribute = "value", annotation = ComponentScan.class)
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages} for specifying the packages
   * to scan for annotated components. The package of each class specified will be scanned.
   * <p>Consider creating a special no-op marker class or interface in each package
   * that serves no purpose other than being referenced by this attribute.
   */
  @AliasFor(attribute = "basePackageClasses", annotation = ComponentScan.class)
  Class<?>[] basePackageClasses() default {};

}
