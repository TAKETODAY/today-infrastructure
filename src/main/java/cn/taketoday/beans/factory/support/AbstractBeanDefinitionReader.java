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

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.EnvironmentCapable;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Abstract base class for bean definition readers which implement
 * the {@link BeanDefinitionReader} interface.
 *
 * <p>Provides common properties like the bean factory to work on
 * and the class loader to use for loading bean classes.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanDefinitionReaderUtils
 * @since 4.0 2022/3/6 22:20
 */
public abstract class AbstractBeanDefinitionReader implements BeanDefinitionReader, EnvironmentCapable {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final BeanDefinitionRegistry registry;

  @Nullable
  private ResourceLoader resourceLoader;

  @Nullable
  private ClassLoader beanClassLoader;

  private Environment environment;

  private BeanNamePopulator beanNameGenerator = DefaultBeanNamePopulator.INSTANCE;

  /**
   * Create a new BeanDefinitionReader for the given bean factory.
   * <p>If the passed-in bean factory does not only implement the BeanDefinitionRegistry
   * interface but also the ResourceLoader interface, it will be used as default
   * ResourceLoader as well. This will usually be the case for
   * {@link cn.taketoday.context.ApplicationContext} implementations.
   * <p>If given a plain BeanDefinitionRegistry, the default ResourceLoader will be a
   * {@link cn.taketoday.core.io.PathMatchingPatternResourceLoader}.
   * <p>If the passed-in bean factory also implements {@link EnvironmentCapable} its
   * environment will be used by this reader.  Otherwise, the reader will initialize and
   * use a {@link StandardEnvironment}. All ApplicationContext implementations are
   * EnvironmentCapable, while normal BeanFactory implementations are not.
   *
   * @param registry the BeanFactory to load bean definitions into,
   * in the form of a BeanDefinitionRegistry
   * @see #setResourceLoader
   * @see #setEnvironment
   */
  protected AbstractBeanDefinitionReader(BeanDefinitionRegistry registry) {
    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
    this.registry = registry;

    // Determine ResourceLoader to use.
    if (this.registry instanceof ResourceLoader) {
      this.resourceLoader = (ResourceLoader) this.registry;
    }
    else {
      this.resourceLoader = new PathMatchingPatternResourceLoader();
    }

    // Inherit Environment if possible
    if (this.registry instanceof EnvironmentCapable) {
      this.environment = ((EnvironmentCapable) this.registry).getEnvironment();
    }
    else {
      this.environment = new StandardEnvironment();
    }
  }

  @Override
  public final BeanDefinitionRegistry getRegistry() {
    return this.registry;
  }

  /**
   * Set the ResourceLoader to use for resource locations.
   * If specifying a ResourcePatternResolver, the bean definition reader
   * will be capable of resolving resource patterns to Resource arrays.
   * <p>Default is PathMatchingResourcePatternResolver, also capable of
   * resource pattern resolving through the ResourcePatternResolver interface.
   * <p>Setting this to {@code null} suggests that absolute resource loading
   * is not available for this bean definition reader.
   *
   * @see cn.taketoday.core.io.PatternResourceLoader
   * @see cn.taketoday.core.io.PathMatchingPatternResourceLoader
   */
  public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Override
  @Nullable
  public ResourceLoader getResourceLoader() {
    return this.resourceLoader;
  }

  /**
   * Set the ClassLoader to use for bean classes.
   * <p>Default is {@code null}, which suggests to not load bean classes
   * eagerly but rather to just register bean definitions with class names,
   * with the corresponding Classes to be resolved later (or never).
   *
   * @see Thread#getContextClassLoader()
   */
  public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }

  @Override
  @Nullable
  public ClassLoader getBeanClassLoader() {
    return this.beanClassLoader;
  }

  /**
   * Set the Environment to use when reading bean definitions. Most often used
   * for evaluating profile information to determine which bean definitions
   * should be read and which should be omitted.
   */
  public void setEnvironment(Environment environment) {
    Assert.notNull(environment, "Environment must not be null");
    this.environment = environment;
  }

  @Override
  public Environment getEnvironment() {
    return this.environment;
  }

  /**
   * Set the BeanNameGenerator to use for anonymous beans
   * (without explicit bean name specified).
   * <p>Default is a {@link DefaultBeanNamePopulator}.
   */
  public void setBeanNameGenerator(@Nullable BeanNamePopulator beanNameGenerator) {
    this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : DefaultBeanNamePopulator.INSTANCE);
  }

  @Override
  public BeanNamePopulator getBeanNamePopulator() {
    return this.beanNameGenerator;
  }

  @Override
  public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
    Assert.notNull(resources, "Resource array must not be null");
    int count = 0;
    for (Resource resource : resources) {
      count += loadBeanDefinitions(resource);
    }
    return count;
  }

  @Override
  public int loadBeanDefinitions(String location) throws BeanDefinitionStoreException {
    return loadBeanDefinitions(location, null);
  }

  /**
   * Load bean definitions from the specified resource location.
   * <p>The location can also be a location pattern, provided that the
   * ResourceLoader of this bean definition reader is a ResourcePatternResolver.
   *
   * @param location the resource location, to be loaded with the ResourceLoader
   * (or ResourcePatternResolver) of this bean definition reader
   * @param actualResources a Set to be filled with the actual Resource objects
   * that have been resolved during the loading process. May be {@code null}
   * to indicate that the caller is not interested in those Resource objects.
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   * @see #getResourceLoader()
   * @see #loadBeanDefinitions(cn.taketoday.core.io.Resource)
   * @see #loadBeanDefinitions(cn.taketoday.core.io.Resource[])
   */
  public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {
    ResourceLoader resourceLoader = getResourceLoader();
    if (resourceLoader == null) {
      throw new BeanDefinitionStoreException(
              "Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
    }

    if (resourceLoader instanceof PatternResourceLoader loader) {
      // Resource pattern matching available.
      try {
        Resource[] resources = loader.getResourcesArray(location);
        int count = loadBeanDefinitions(resources);
        if (actualResources != null) {
          Collections.addAll(actualResources, resources);
        }
        if (logger.isTraceEnabled()) {
          logger.trace("Loaded {} bean definitions from location pattern [{}]", count, location);
        }
        return count;
      }
      catch (IOException ex) {
        throw new BeanDefinitionStoreException(
                "Could not resolve bean definition resource pattern [" + location + "]", ex);
      }
    }
    else {
      // Can only load single resources by absolute URL.
      Resource resource = resourceLoader.getResource(location);
      int count = loadBeanDefinitions(resource);
      if (actualResources != null) {
        actualResources.add(resource);
      }
      if (logger.isTraceEnabled()) {
        logger.trace("Loaded {} bean definitions from location [{}]", count, location);
      }
      return count;
    }
  }

  @Override
  public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
    Assert.notNull(locations, "Location array must not be null");
    int count = 0;
    for (String location : locations) {
      count += loadBeanDefinitions(location);
    }
    return count;
  }

}

