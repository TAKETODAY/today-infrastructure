/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory;

import infra.core.env.Environment;

/**
 * Contract for registering beans programmatically, typically imported with an
 * {@link infra.context.annotation.Import @Import} annotation on
 * a {@link infra.context.annotation.Configuration @Configuration}
 * class.
 * <pre>{@code
 * @Configuration
 * @Import(MyBeanRegistrar.class)
 * class MyConfiguration {
 * }
 * }</pre>
 * Can also be applied to an application context via
 * {@link infra.context.support.GenericApplicationContext#register(BeanRegistrar...)}.
 *
 *
 * <p>Bean registrar implementations use {@link BeanRegistry} and {@link Environment}
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
 * <p>A {@code BeanRegistrar} implementing {@link infra.context.annotation.ImportAware}
 * can optionally introspect import metadata when used in an import scenario, otherwise the
 * {@code setImportMetadata} method is simply not being called.
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
