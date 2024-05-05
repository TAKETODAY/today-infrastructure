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

package cn.taketoday.test.context.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.web.mock.WebApplicationContext;

/**
 * {@code @WebAppConfiguration} is a class-level annotation that is used to
 * declare that the {@code ApplicationContext} loaded for an integration test
 * should be a {@link WebApplicationContext WebApplicationContext}.
 *
 * <p>The presence of {@code @WebAppConfiguration} on a test class indicates that
 * a {@code WebApplicationContext} should be loaded for the test using a default
 * for the path to the root of the web application. To override the default,
 * specify an explicit resource path via the {@link #value} attribute.
 *
 * <p>Note that {@code @WebAppConfiguration} must be used in conjunction with
 * {@link ContextConfiguration @ContextConfiguration},
 * either within a single test class or within a test class hierarchy.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p> this annotation will be inherited from an
 * enclosing test class by default. See
 * {@link NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * @author Sam Brannen
 * @see WebApplicationContext
 * @see ContextConfiguration
 * @see ServletTestExecutionListener
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface WebAppConfiguration {

  /**
   * The resource path to the root directory of the web application.
   * <p>A path that does not include a Infra resource prefix (e.g., {@code classpath:},
   * {@code file:}, etc.) will be interpreted as a file system resource, and a
   * path should not end with a slash.
   * <p>Defaults to {@code "src/main/webapp"} as a file system resource. Note
   * that this is the standard directory for the root of a web application in
   * a project that follows the standard Maven project layout for a WAR.
   */
  String value() default "src/main/webapp";

}
