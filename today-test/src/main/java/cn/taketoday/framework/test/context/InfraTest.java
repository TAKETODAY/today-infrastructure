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

package cn.taketoday.framework.test.context;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationArguments;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.InfraConfiguration;
import cn.taketoday.framework.test.web.server.LocalServerPort;
import cn.taketoday.web.server.context.WebServerApplicationContext;
import cn.taketoday.web.server.reactive.context.ReactiveWebApplicationContext;
import cn.taketoday.test.context.BootstrapWith;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.web.servlet.WebApplicationContext;

/**
 * Annotation that can be specified on a test class that runs Infra based tests.
 * Provides the following features over and above the regular <em>Infra TestContext
 * Framework</em>:
 * <ul>
 * <li>Uses {@link InfraApplicationContextLoader} as the default {@link ContextLoader} when no
 * specific {@link ContextConfiguration#loader() @ContextConfiguration(loader=...)} is
 * defined.</li>
 * <li>Automatically searches for a
 * {@link InfraConfiguration @InfraConfiguration} when nested
 * {@code @Configuration} is not used, and no explicit {@link #classes() classes} are
 * specified.</li>
 * <li>Allows custom {@link Environment} properties to be defined using the
 * {@link #properties() properties attribute}.</li>
 * <li>Allows application arguments to be defined using the {@link #args() args
 * attribute}.</li>
 * <li>Provides support for different {@link #webEnvironment() webEnvironment} modes,
 * including the ability to start a fully running web server listening on a
 * {@link WebEnvironment#DEFINED_PORT defined} or {@link WebEnvironment#RANDOM_PORT
 * random} port.</li>
 * <li>Registers a {@link cn.taketoday.framework.test.web.client.TestRestTemplate
 * TestRestTemplate} and/or
 * {@link cn.taketoday.test.web.reactive.server.WebTestClient WebTestClient} bean
 * for use in web tests that are using a fully running web server.</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see ContextConfiguration
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@BootstrapWith(InfraTestContextBootstrapper.class)
@ExtendWith(InfraExtension.class)
public @interface InfraTest {

  /**
   * Alias for {@link #properties()}.
   *
   * @return the properties to apply
   */
  @AliasFor("properties")
  String[] value() default {};

  /**
   * Properties in form {@literal key=value} that should be added to the Infra
   * {@link Environment} before the test runs.
   *
   * @return the properties to add
   */
  @AliasFor("value")
  String[] properties() default {};

  /**
   * Application arguments that should be passed to the application under test.
   *
   * @return the application arguments to pass to the application under test.
   * @see ApplicationArguments
   * @see Application#run(String...)
   */
  String[] args() default {};

  /**
   * The <em>component classes</em> to use for loading an
   * {@link cn.taketoday.context.ApplicationContext ApplicationContext}. Can also
   * be specified using
   * {@link ContextConfiguration#classes() @ContextConfiguration(classes=...)}. If no
   * explicit classes are defined the test will look for nested
   * {@link Configuration @Configuration} classes, before falling back to a
   * {@link InfraConfiguration @InfraConfiguration} search.
   *
   * @return the component classes used to load the application context
   * @see ContextConfiguration#classes()
   */
  Class<?>[] classes() default {};

  /**
   * The type of web environment to create when applicable. Defaults to
   * {@link WebEnvironment#MOCK}.
   *
   * @return the type of web environment
   */
  WebEnvironment webEnvironment() default WebEnvironment.MOCK;

  /**
   * An enumeration web environment modes.
   */
  enum WebEnvironment {

    /**
     * Creates a {@link WebApplicationContext} with a mock servlet environment if
     * servlet APIs are on the classpath, a {@link ReactiveWebApplicationContext} if
     * Infra WebFlux is on the classpath or a regular {@link ApplicationContext}
     * otherwise.
     */
    MOCK(false),

    /**
     * Creates a web application context (reactive or servlet based) and sets a
     * {@code server.port=0} {@link Environment} property (which usually triggers
     * listening on a random port). Often used in conjunction with a
     * {@link LocalServerPort @LocalServerPort} injected field on the test.
     */
    RANDOM_PORT(true),

    /**
     * Creates a (reactive) web application context without defining any
     * {@code server.port=0} {@link Environment} property.
     */
    DEFINED_PORT(true),

    /**
     * Creates an {@link ApplicationContext} and sets
     * {@link Application#setApplicationType(ApplicationType)} to
     * {@link ApplicationType#NORMAL}.
     */
    NONE(false);

    private final boolean embedded;

    WebEnvironment(boolean embedded) {
      this.embedded = embedded;
    }

    /**
     * Return if the environment uses an {@link WebServerApplicationContext}.
     *
     * @return if an {@link WebServerApplicationContext} is used.
     */
    public boolean isEmbedded() {
      return this.embedded;
    }

  }

}
