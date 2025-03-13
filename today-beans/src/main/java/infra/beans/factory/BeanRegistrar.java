/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory;

import infra.core.env.Environment;

/**
 * Contract for registering beans programmatically.
 *
 * <p>Typically imported with an {@link infra.context.annotation.Import @Import}
 * annotation on {@link infra.context.annotation.Configuration @Configuration}
 * classes.
 * <pre>{@code
 * @Configuration
 * @Import(MyBeanRegistrar.class)
 * class MyConfiguration {
 * }}</pre>
 *
 * <p>The bean registrar implementation uses {@link BeanRegistry} and {@link Environment}
 * APIs to register beans programmatically in a concise and flexible way.
 * <pre>{@code
 * class MyBeanRegistrar implements BeanRegistrar {
 *
 *     @Override
 *     public void register(BeanRegistry registry, Environment env) {
 *         registry.registerBean("foo", Foo.class);
 *         registry.registerBean("bar", Bar.class, spec -> spec
 *                 .prototype()
 *                 .lazyInit()
 *                 .description("Custom description")
 *                 .supplier(context -> new Bar(context.bean(Foo.class))));
 *         if (env.matchesProfiles("baz")) {
 *             registry.registerBean(Baz.class, spec -> spec
 *                     .supplier(context -> new Baz("Hello World!")));
 *         }
 *     }
 * }}</pre>
 *
 * <p>In Kotlin, it is recommended to use {@code BeanRegistrarDsl} instead of
 * implementing {@code BeanRegistrar}.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@FunctionalInterface
public interface BeanRegistrar {

  /**
   * Register beans on the given {@link BeanRegistry} in a programmatic way.
   *
   * @param registry the bean registry to operate on
   * @param env the environment that can be used to get the active profile or some properties
   */
  void register(BeanRegistry registry, Environment env);
}
