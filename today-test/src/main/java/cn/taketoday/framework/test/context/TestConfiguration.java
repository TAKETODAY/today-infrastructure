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

package cn.taketoday.framework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.framework.ApplicationConfiguration;

/**
 * {@link Configuration @Configuration} that can be used to define additional beans or
 * customizations for a test. Unlike regular {@code @Configuration} classes the use of
 * {@code @TestConfiguration} does not prevent auto-detection of
 * {@link ApplicationConfiguration @SpringBootConfiguration}.
 *
 * @author Phillip Webb
 * @see ApplicationTestContextBootstrapper
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@TestComponent
public @interface TestConfiguration {

  /**
   * Explicitly specify the name of the Spring bean definition associated with this
   * Configuration class. See {@link Configuration#value()} for details.
   *
   * @return the specified bean name, if any
   */
  @AliasFor(annotation = Configuration.class)
  String value() default "";

  /**
   * Specify whether {@link Bean @Bean} methods should get proxied in order to enforce
   * bean lifecycle behavior, e.g. to return shared singleton bean instances even in
   * case of direct {@code @Bean} method calls in user code. This feature requires
   * method interception, implemented through a runtime-generated CGLIB subclass which
   * comes with limitations such as the configuration class and its methods not being
   * allowed to declare {@code final}.
   * <p>
   * The default is {@code true}, allowing for 'inter-bean references' within the
   * configuration class as well as for external calls to this configuration's
   * {@code @Bean} methods, e.g. from another configuration class. If this is not needed
   * since each of this particular configuration's {@code @Bean} methods is
   * self-contained and designed as a plain factory method for container use, switch
   * this flag to {@code false} in order to avoid CGLIB subclass processing.
   * <p>
   * Turning off bean method interception effectively processes {@code @Bean} methods
   * individually like when declared on non-{@code @Configuration} classes, a.k.a.
   * "@Bean Lite Mode" (see {@link Bean @Bean's javadoc}). It is therefore behaviorally
   * equivalent to removing the {@code @Configuration} stereotype.
   *
   * @return whether to proxy {@code @Bean} methods
   * @since 4.0
   */
  @AliasFor(annotation = Configuration.class)
  boolean proxyBeanMethods() default true;

}
