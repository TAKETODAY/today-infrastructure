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

import infra.beans.factory.config.ExpressionEvaluator;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.core.type.classreading.MetadataReaderFactory;

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
 * <p>{@code BeanRegistrar} implementations are not Infra components: they must have
 * a no-arg constructor and cannot rely on dependency injection or any other
 * component-model feature. They can be used in two distinct ways depending on the
 * application context setup.
 *
 * <h3>With the {@code @Configuration} model</h3>
 *
 * <p>A {@code BeanRegistrar} must be imported via
 * {@link infra.context.annotation.Import @Import} on a
 * {@link infra.context.annotation.Configuration @Configuration} class:
 *
 * <pre>{@code
 * @Configuration
 * @Import(MyBeanRegistrar.class)
 * class MyConfiguration {
 * }}</pre>
 *
 * <p>This is the only mechanism that triggers bean registration in the annotation-based
 * configuration model. Annotating an implementation with {@code @Configuration} or
 * {@code @Component}, or returning an instance from a {@code @Bean} method, registers
 * it as a bean but does <strong>not</strong> invoke its
 * {@link #register(BeanRegistry, Environment) register} method.
 *
 * <p>When imported, the registrar is invoked in the order it is encountered during
 * configuration class processing. It can therefore check for and build on beans that
 * have already been defined, but has no visibility into beans that will be registered
 * by classes processed later.
 *
 * <h3>Programmatic usage</h3>
 *
 * <p>A {@code BeanRegistrar} can also be applied directly to a
 * {@link infra.context.support.GenericApplicationContext}:
 *
 * <pre>{@code
 * GenericApplicationContext context = new GenericApplicationContext();
 * context.register(new MyBeanRegistrar());
 * context.registerBean("myBean", MyBean.class);
 * context.refresh();
 * }</pre>
 *
 * <p>This mode is primarily intended for fully programmatic application context setups.
 * Registrars applied this way are invoked before any {@code @Configuration} class is
 * processed. They can therefore observe beans registered programmatically (e.g., via
 * one of the {@code GenericApplicationContext#registerBean} methods), but will
 * <strong>not</strong> see any beans defined in {@code @Configuration} classes also
 * registered with the context.
 *
 * <p>A {@code BeanRegistrar} implementing {@link infra.context.annotation.ImportAware}
 * can optionally introspect import metadata when used in an import scenario, otherwise the
 * {@code setImportMetadata} method is simply not being called.
 *
 * <p>An {@link BeanRegistrar} may implement any of the following
 * {@link Aware Aware} interfaces,
 * and their respective methods will be called prior to {@link #register}:
 * <ul>
 * <li>{@link infra.context.EnvironmentAware}</li>
 * <li>{@link infra.beans.factory.BeanFactoryAware}</li>
 * <li>{@link infra.beans.factory.BeanClassLoaderAware}</li>
 * <li>{@link infra.context.annotation.ImportAware}</li>
 * <li>{@link infra.context.ResourceLoaderAware}</li>
 * <li>{@link infra.context.BootstrapContextAware}</li>
 * <li>{@link infra.context.ApplicationContextAware}</li>
 * </ul>
 *
 * <p>Alternatively, the class may provide a single constructor with one or more of
 * the following supported parameter types:
 * <ul>
 * <li>{@link Environment Environment}</li>
 * <li>{@link BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link ResourceLoader ResourceLoader}</li>
 * <li>{@link infra.context.BootstrapContext BootstrapContext}</li>
 * <li>{@link infra.context.ApplicationContext ApplicationContext}</li>
 * <li>{@link ExpressionEvaluator ExpressionEvaluator}</li>
 * <li>{@link MetadataReaderFactory MetadataReaderFactory}</li>
 * <li>{@link BeanDefinitionRegistry BeanDefinitionRegistry}</li>
 * </ul>
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
