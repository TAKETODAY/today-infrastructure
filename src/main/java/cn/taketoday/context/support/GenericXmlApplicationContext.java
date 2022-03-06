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

package cn.taketoday.context.support;

import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;

/**
 * Convenient application context with built-in XML support.
 * This is a flexible alternative to {@link ClassPathXmlApplicationContext}
 * and {@link FileSystemXmlApplicationContext}, to be configured via setters,
 * with an eventual {@link #refresh()} call activating the context.
 *
 * <p>In case of multiple configuration files, bean definitions in later files
 * will override those defined in earlier files. This can be leveraged to
 * intentionally override certain bean definitions via an extra configuration
 * file appended to the list.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #load
 * @see XmlBeanDefinitionReader
 * @see cn.taketoday.context.annotation.AnnotationConfigApplicationContext
 * @since 4.0 2022/3/6 22:26
 */
public class GenericXmlApplicationContext extends GenericApplicationContext {

  private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);

  /**
   * Create a new GenericXmlApplicationContext that needs to be
   * {@link #load loaded} and then manually {@link #refresh refreshed}.
   */
  public GenericXmlApplicationContext() {
  }

  /**
   * Create a new GenericXmlApplicationContext, loading bean definitions
   * from the given resources and automatically refreshing the context.
   *
   * @param resources the resources to load from
   */
  public GenericXmlApplicationContext(Resource... resources) {
    load(resources);
    refresh();
  }

  /**
   * Create a new GenericXmlApplicationContext, loading bean definitions
   * from the given resource locations and automatically refreshing the context.
   *
   * @param resourceLocations the resources to load from
   */
  public GenericXmlApplicationContext(String... resourceLocations) {
    load(resourceLocations);
    refresh();
  }

  /**
   * Create a new GenericXmlApplicationContext, loading bean definitions
   * from the given resource locations and automatically refreshing the context.
   *
   * @param relativeClass class whose package will be used as a prefix when
   * loading each specified resource name
   * @param resourceNames relatively-qualified names of resources to load
   */
  public GenericXmlApplicationContext(Class<?> relativeClass, String... resourceNames) {
    load(relativeClass, resourceNames);
    refresh();
  }

  /**
   * Exposes the underlying {@link XmlBeanDefinitionReader} for additional
   * configuration facilities and {@code loadBeanDefinition} variations.
   */
  public final XmlBeanDefinitionReader getReader() {
    return this.reader;
  }

  /**
   * Set whether to use XML validation. Default is {@code true}.
   */
  public void setValidating(boolean validating) {
    this.reader.setValidating(validating);
  }

  /**
   * Delegates the given environment to underlying {@link XmlBeanDefinitionReader}.
   * Should be called before any call to {@code #load}.
   */
  @Override
  public void setEnvironment(ConfigurableEnvironment environment) {
    super.setEnvironment(environment);
    this.reader.setEnvironment(getEnvironment());
  }

  //---------------------------------------------------------------------
  // Convenient methods for loading XML bean definition files
  //---------------------------------------------------------------------

  /**
   * Load bean definitions from the given XML resources.
   *
   * @param resources one or more resources to load from
   */
  public void load(Resource... resources) {
    this.reader.loadBeanDefinitions(resources);
  }

  /**
   * Load bean definitions from the given XML resources.
   *
   * @param resourceLocations one or more resource locations to load from
   */
  public void load(String... resourceLocations) {
    this.reader.loadBeanDefinitions(resourceLocations);
  }

  /**
   * Load bean definitions from the given XML resources.
   *
   * @param relativeClass class whose package will be used as a prefix when
   * loading each specified resource name
   * @param resourceNames relatively-qualified names of resources to load
   */
  public void load(Class<?> relativeClass, String... resourceNames) {
    Resource[] resources = new Resource[resourceNames.length];
    for (int i = 0; i < resourceNames.length; i++) {
      resources[i] = new ClassPathResource(resourceNames[i], relativeClass);
    }
    this.load(resources);
  }

}
