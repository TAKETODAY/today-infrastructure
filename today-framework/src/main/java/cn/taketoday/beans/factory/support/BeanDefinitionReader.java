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

package cn.taketoday.beans.factory.support;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Nullable;

/**
 * Simple interface for bean definition readers that specifies load methods with
 * {@link Resource} and {@link String} location parameters.
 *
 * <p>Concrete bean definition readers can of course add additional
 * load and register methods for bean definitions, specific to
 * their bean definition format.
 *
 * <p>Note that a bean definition reader does not have to implement
 * this interface. It only serves as a suggestion for bean definition
 * readers that want to follow standard naming conventions.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.core.io.Resource
 * @since 4.0 2022/3/6 22:20
 */
public interface BeanDefinitionReader {

  /**
   * Return the bean factory to register the bean definitions with.
   * <p>The factory is exposed through the {@link BeanDefinitionRegistry} interface,
   * encapsulating the methods that are relevant for bean definition handling.
   */
  BeanDefinitionRegistry getRegistry();

  /**
   * Return the {@link ResourceLoader} to use for resource locations.
   * <p>Can be checked for the {@code ResourcePatternResolver} interface and cast
   * accordingly, for loading multiple resources for a given resource pattern.
   * <p>A {@code null} return value suggests that absolute resource loading
   * is not available for this bean definition reader.
   * <p>This is mainly meant to be used for importing further resources
   * from within a bean definition resource, for example via the "import"
   * tag in XML bean definitions. It is recommended, however, to apply
   * such imports relative to the defining resource; only explicit full
   * resource locations will trigger absolute path based resource loading.
   * <p>There is also a {@code loadBeanDefinitions(String)} method available,
   * for loading bean definitions from a resource location (or location pattern).
   * This is a convenience to avoid explicit {@code ResourceLoader} handling.
   *
   * @see #loadBeanDefinitions(String)
   * @see cn.taketoday.core.io.PatternResourceLoader
   */
  @Nullable
  ResourceLoader getResourceLoader();

  /**
   * Return the class loader to use for bean classes.
   * <p>{@code null} suggests to not load bean classes eagerly
   * but rather to just register bean definitions with class names,
   * with the corresponding classes to be resolved later (or never).
   */
  @Nullable
  ClassLoader getBeanClassLoader();

  /**
   * Return the {@link BeanNameGenerator} to use for anonymous beans
   * (without explicit bean name specified).
   */
  BeanNameGenerator getBeanNameGenerator();

  /**
   * Load bean definitions from the specified resource.
   *
   * @param resource the resource descriptor
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   */
  int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException;

  /**
   * Load bean definitions from the specified resources.
   *
   * @param resources the resource descriptors
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   */
  int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException;

  /**
   * Load bean definitions from the specified resource location.
   * <p>The location can also be a location pattern, provided that the
   * {@link ResourceLoader} of this bean definition reader is a
   * {@code ResourcePatternResolver}.
   *
   * @param location the resource location, to be loaded with the {@code ResourceLoader}
   * (or {@code ResourcePatternResolver}) of this bean definition reader
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   * @see #getResourceLoader()
   * @see #loadBeanDefinitions(cn.taketoday.core.io.Resource)
   * @see #loadBeanDefinitions(cn.taketoday.core.io.Resource[])
   */
  int loadBeanDefinitions(String location) throws BeanDefinitionStoreException;

  /**
   * Load bean definitions from the specified resource locations.
   *
   * @param locations the resource locations, to be loaded with the {@code ResourceLoader}
   * (or {@code ResourcePatternResolver}) of this bean definition reader
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   */
  int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException;

}
