/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.annotation;

import cn.taketoday.beans.factory.BeanDefinitionRegistryPostProcessor;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.core.type.AnnotationMetadata;

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
 * {@link cn.taketoday.beans.factory.Aware Aware} interfaces, and their respective
 * methods will be called prior to {@link #registerBeanDefinitions}:
 * <ul>
 * <li>{@link cn.taketoday.context.aware.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link cn.taketoday.beans.factory.BeanFactoryAware BeanFactoryAware}
 * <li>{@link cn.taketoday.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}
 * <li>{@link cn.taketoday.context.aware.ResourceLoaderAware ResourceLoaderAware}
 * </ul>
 *
 * <p>Alternatively, the class may provide a single constructor with one or more of
 * the following supported parameter types:
 * <ul>
 * <li>{@link cn.taketoday.core.env.Environment Environment}</li>
 * <li>{@link cn.taketoday.beans.factory.BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link cn.taketoday.core.io.ResourceLoader ResourceLoader}</li>
 * </ul>
 *
 * <p>See implementations and associated unit tests for usage examples.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author TODAY 2019-10-01 19:08
 * @see Import
 * @see ImportSelector
 * @see Configuration
 */
@FunctionalInterface
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
  void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context);

}
