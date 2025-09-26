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

package infra.context.support;

import infra.beans.BeansException;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.context.ApplicationContext;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Standalone XML application context, taking the context definition files
 * from the class path, interpreting plain paths as class path resource names
 * that include the package path (e.g. "mypackage/myresource.txt"). Useful for
 * test harnesses as well as for application contexts embedded within JARs.
 *
 * <p>The config location defaults can be overridden via {@link #getConfigLocations},
 * Config locations can either denote concrete files like "/myfiles/context.xml"
 * or Ant-style patterns like "/myfiles/*-context.xml" (see the
 * {@link infra.util.AntPathMatcher} javadoc for pattern details).
 *
 * <p>Note: In case of multiple config locations, later bean definitions will
 * override ones defined in earlier loaded files. This can be leveraged to
 * deliberately override certain bean definitions via an extra XML file.
 *
 * <p><b>This is a simple, one-stop shop convenience ApplicationContext.
 * Consider using the {@link GenericApplicationContext} class in combination
 * with an {@link XmlBeanDefinitionReader}
 * for more flexible context setup.</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getResource
 * @see #getResourceByPath
 * @see GenericApplicationContext
 * @since 4.0 2022/3/6 22:03
 */
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {

  @Nullable
  private Resource[] configResources;

  /**
   * Create a new ClassPathXmlApplicationContext for bean-style configuration.
   *
   * @see #setConfigLocation
   * @see #setConfigLocations
   * @see #afterPropertiesSet()
   */
  public ClassPathXmlApplicationContext() {
  }

  /**
   * Create a new ClassPathXmlApplicationContext for bean-style configuration.
   *
   * @param parent the parent context
   * @see #setConfigLocation
   * @see #setConfigLocations
   * @see #afterPropertiesSet()
   */
  public ClassPathXmlApplicationContext(ApplicationContext parent) {
    super(parent);
  }

  /**
   * Create a new ClassPathXmlApplicationContext, loading the definitions
   * from the given XML file and automatically refreshing the context.
   *
   * @param configLocation resource location
   * @throws BeansException if context creation failed
   */
  public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
    this(new String[] { configLocation }, true, null);
  }

  /**
   * Create a new ClassPathXmlApplicationContext, loading the definitions
   * from the given XML files and automatically refreshing the context.
   *
   * @param configLocations array of resource locations
   * @throws BeansException if context creation failed
   */
  public ClassPathXmlApplicationContext(String... configLocations) throws BeansException {
    this(configLocations, true, null);
  }

  /**
   * Create a new ClassPathXmlApplicationContext with the given parent,
   * loading the definitions from the given XML files and automatically
   * refreshing the context.
   *
   * @param configLocations array of resource locations
   * @param parent the parent context
   * @throws BeansException if context creation failed
   */
  public ClassPathXmlApplicationContext(String[] configLocations, @Nullable ApplicationContext parent)
          throws BeansException {

    this(configLocations, true, parent);
  }

  /**
   * Create a new ClassPathXmlApplicationContext, loading the definitions
   * from the given XML files.
   *
   * @param configLocations array of resource locations
   * @param refresh whether to automatically refresh the context,
   * loading all bean definitions and creating all singletons.
   * Alternatively, call refresh manually after further configuring the context.
   * @throws BeansException if context creation failed
   * @see #refresh()
   */
  public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
    this(configLocations, refresh, null);
  }

  /**
   * Create a new ClassPathXmlApplicationContext with the given parent,
   * loading the definitions from the given XML files.
   *
   * @param configLocations array of resource locations
   * @param refresh whether to automatically refresh the context,
   * loading all bean definitions and creating all singletons.
   * Alternatively, call refresh manually after further configuring the context.
   * @param parent the parent context
   * @throws BeansException if context creation failed
   * @see #refresh()
   */
  public ClassPathXmlApplicationContext(
          String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
          throws BeansException {

    super(parent);
    setConfigLocations(configLocations);
    if (refresh) {
      refresh();
    }
  }

  /**
   * Create a new ClassPathXmlApplicationContext, loading the definitions
   * from the given XML file and automatically refreshing the context.
   * <p>This is a convenience method to load class path resources relative to a
   * given Class. For full flexibility, consider using a GenericApplicationContext
   * with an XmlBeanDefinitionReader and a ClassPathResource argument.
   *
   * @param path relative (or absolute) path within the class path
   * @param clazz the class to load resources with (basis for the given paths)
   * @throws BeansException if context creation failed
   * @see ClassPathResource#ClassPathResource(String, Class)
   * @see GenericApplicationContext
   * @see XmlBeanDefinitionReader
   */
  public ClassPathXmlApplicationContext(String path, Class<?> clazz) throws BeansException {
    this(new String[] { path }, clazz);
  }

  /**
   * Create a new ClassPathXmlApplicationContext, loading the definitions
   * from the given XML files and automatically refreshing the context.
   *
   * @param paths array of relative (or absolute) paths within the class path
   * @param clazz the class to load resources with (basis for the given paths)
   * @throws BeansException if context creation failed
   * @see ClassPathResource#ClassPathResource(String, Class)
   * @see GenericApplicationContext
   * @see XmlBeanDefinitionReader
   */
  public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz) throws BeansException {
    this(paths, clazz, null);
  }

  /**
   * Create a new ClassPathXmlApplicationContext with the given parent,
   * loading the definitions from the given XML files and automatically
   * refreshing the context.
   *
   * @param paths array of relative (or absolute) paths within the class path
   * @param clazz the class to load resources with (basis for the given paths)
   * @param parent the parent context
   * @throws BeansException if context creation failed
   * @see ClassPathResource#ClassPathResource(String, Class)
   * @see GenericApplicationContext
   * @see XmlBeanDefinitionReader
   */
  public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz, @Nullable ApplicationContext parent)
          throws BeansException {

    super(parent);
    Assert.notNull(paths, "Path array is required");
    Assert.notNull(clazz, "Class argument is required");
    this.configResources = new Resource[paths.length];
    for (int i = 0; i < paths.length; i++) {
      this.configResources[i] = new ClassPathResource(paths[i], clazz);
    }
    refresh();
  }

  @Override
  @Nullable
  protected Resource[] getConfigResources() {
    return this.configResources;
  }

}
