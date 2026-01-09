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

package infra.context.annotation;

import infra.beans.factory.Aware;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import infra.beans.factory.support.BeanNameGenerator;
import infra.context.BootstrapContext;
import infra.context.EnvironmentAware;
import infra.context.ResourceLoaderAware;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.core.type.AnnotationMetadata;

/**
 * Interface to be implemented by types that register additional bean definitions when
 * processing @{@link Configuration} classes. Useful when operating at the bean definition
 * level (as opposed to {@code @Bean} method/instance level) is desired or necessary.
 *
 * <p>Along with {@code @Configuration} and {@link ImportSelector}, classes of this type
 * may be provided to the @{@link Import} annotation (or may also be returned from an
 * {@code ImportSelector}).
 *
 * <p>An {@link ImportBeanDefinitionRegistrar} may implement any of the following
 * {@link Aware Aware} interfaces, and their respective
 * methods will be called prior to {@link #registerBeanDefinitions}:
 * <ul>
 * <li>{@link EnvironmentAware EnvironmentAware}</li>
 * <li>{@link BeanFactoryAware BeanFactoryAware}
 * <li>{@link BeanClassLoaderAware BeanClassLoaderAware}
 * <li>{@link ResourceLoaderAware ResourceLoaderAware}
 * </ul>
 *
 * <p>Alternatively, the class may provide a single constructor with one or more of
 * the following supported parameter types:
 * <ul>
 * <li>{@link Environment Environment}</li>
 * <li>{@link BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link ResourceLoader ResourceLoader}</li>
 * </ul>
 *
 * <p>See implementations and associated unit tests for usage examples.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Import
 * @see ImportSelector
 * @see Configuration
 * @since 2019-10-01 19:08
 */
public interface ImportBeanDefinitionRegistrar {

  /**
   * Register bean definitions as necessary based on the given annotation metadata of
   * the importing {@code @Configuration} class.
   * <p>Note that {@link BeanDefinitionRegistryPostProcessor} types may <em>not</em> be
   * registered here, due to lifecycle constraints related to {@code @Configuration}
   * class processing.
   * <p>The default implementation is empty.
   *
   * @param importMetadata annotation metadata of the importing class
   * @param context Bean definition loading context
   */
  default void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {

  }

  /**
   * Register bean definitions as necessary based on the given annotation metadata of
   * the importing {@code @Configuration} class.
   * <p>Note that {@link BeanDefinitionRegistryPostProcessor} types may <em>not</em> be
   * registered here, due to lifecycle constraints related to {@code @Configuration}
   * class processing.
   * <p>The default implementation delegates to
   * {@link #registerBeanDefinitions(AnnotationMetadata, BootstrapContext)}.
   *
   * @param importingClassMetadata annotation metadata of the importing class
   * @param context current bean definition registry
   * @param importBeanNameGenerator the bean name generator strategy for imported beans:
   * {@link ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR} by default, or a
   * user-provided one if {@link ConfigurationClassPostProcessor#setBeanNameGenerator}
   * has been set. In the latter case, the passed-in strategy will be the same used for
   * component scanning in the containing application context (otherwise, the default
   * component-scan naming strategy is {@link AnnotationBeanNameGenerator#INSTANCE}).
   * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
   * @see ConfigurationClassPostProcessor#setBeanNameGenerator
   * @since 5.0
   */
  default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BootstrapContext context,
          BeanNameGenerator importBeanNameGenerator) {

    registerBeanDefinitions(importingClassMetadata, context);
  }
}
