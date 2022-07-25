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

package cn.taketoday.context.annotation.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Import;

/**
 * Registers packages with {@link AutoConfigurationPackages}. When no {@link #basePackages
 * base packages} or {@link #basePackageClasses base package classes} are specified, the
 * package of the annotated class is registered.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AutoConfigurationPackages
 * @since 4.0 2022/2/1 02:36
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AutoConfigurationPackages.Registrar.class)
public @interface AutoConfigurationPackage {

  /**
   * Base packages that should be registered with {@link AutoConfigurationPackages}.
   * <p>
   * Use {@link #basePackageClasses} for a type-safe alternative to String-based package
   * names.
   *
   * @return the back package names
   */
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages} for specifying the packages to be
   * registered with {@link AutoConfigurationPackages}.
   * <p>
   * Consider creating a special no-op marker class or interface in each package that
   * serves no purpose other than being referenced by this attribute.
   *
   * @return the base package classes
   */
  Class<?>[] basePackageClasses() default {};

}
