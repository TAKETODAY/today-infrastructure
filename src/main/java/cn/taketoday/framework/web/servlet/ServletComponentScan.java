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

package cn.taketoday.framework.web.servlet;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;

/**
 * Enables scanning for Servlet components ({@link WebFilter filters}, {@link WebServlet
 * servlets}, and {@link WebListener listeners}). Scanning is only performed when using an
 * embedded web server.
 * <p>
 * Typically, one of {@code value}, {@code basePackages}, or {@code basePackageClasses}
 * should be specified to control the packages to be scanned for components. In their
 * absence, scanning will be performed from the package of the class with the annotation.
 *
 * @author Andy Wilkinson
 * @see WebServlet
 * @see WebFilter
 * @see WebListener
 * @since 1.3.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ServletComponentScanRegistrar.class)
public @interface ServletComponentScan {

  /**
   * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
   * declarations e.g.: {@code @ServletComponentScan("org.my.pkg")} instead of
   * {@code @ServletComponentScan(basePackages="org.my.pkg")}.
   *
   * @return the base packages to scan
   */
  @AliasFor("basePackages")
  String[] value() default {};

  /**
   * Base packages to scan for annotated servlet components. {@link #value()} is an
   * alias for (and mutually exclusive with) this attribute.
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
   * scan for annotated servlet components. The package of each class specified will be
   * scanned.
   *
   * @return classes from the base packages to scan
   */
  Class<?>[] basePackageClasses() default {};

}
