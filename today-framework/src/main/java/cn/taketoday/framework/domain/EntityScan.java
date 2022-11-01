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

package cn.taketoday.framework.domain;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.annotation.AliasFor;

/**
 * Configures the base packages used by auto-configuration when scanning for entity
 * classes.
 * <p>
 * Using {@code @EntityScan} will cause auto-configuration to:
 * <ul>
 * <li>Set the
 * {@link cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean#setPackagesToScan(String...)
 * packages scanned} for JPA entities.</li>
 * </ul>
 * <p>
 * One of {@link #basePackageClasses()}, {@link #basePackages()} or its alias
 * {@link #value()} may be specified to define specific packages to scan. If specific
 * packages are not defined scanning will occur from the package of the class with this
 * annotation.
 *
 * @author Phillip Webb
 * @see EntityScanPackages
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EntityScanPackages.Registrar.class)
public @interface EntityScan {

  /**
   * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
   * declarations e.g.: {@code @EntityScan("org.my.pkg")} instead of
   * {@code @EntityScan(basePackages="org.my.pkg")}.
   *
   * @return the base packages to scan
   */
  @AliasFor("basePackages")
  String[] value() default {};

  /**
   * Base packages to scan for entities. {@link #value()} is an alias for (and mutually
   * exclusive with) this attribute.
   * <p>
   * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
   * package names.
   *
   * @return the base packages to scan
   */
  @AliasFor("value")
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages()} for specifying the packages to
   * scan for entities. The package of each class specified will be scanned.
   * <p>
   * Consider creating a special no-op marker class or interface in each package that
   * serves no purpose other than being referenced by this attribute.
   *
   * @return classes from the base packages to scan
   */
  Class<?>[] basePackageClasses() default {};

}
