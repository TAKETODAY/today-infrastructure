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

package cn.taketoday.framework.web.servlet.context;

import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.web.context.support.XmlWebApplicationContext;

/**
 * {@link ServletWebServerApplicationContext} which takes its configuration from XML
 * documents, understood by an {@link XmlBeanDefinitionReader}.
 * <p>
 * Note: In case of multiple config locations, later bean definitions will override ones
 * defined in earlier loaded files. This can be leveraged to deliberately override certain
 * bean definitions via an extra XML file.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setNamespace
 * @see #setConfigLocations
 * @see ServletWebServerApplicationContext
 * @see XmlWebApplicationContext
 * @since 4.0 2022/4/6 22:51
 */
public class XmlServletWebServerApplicationContext extends ServletWebServerApplicationContext {

  private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);

  /**
   * Create a new {@link XmlServletWebServerApplicationContext} that needs to be
   * {@linkplain #load loaded} and then manually {@link #refresh refreshed}.
   */
  public XmlServletWebServerApplicationContext() {
    this.reader.setEnvironment(getEnvironment());
  }

  /**
   * Create a new {@link XmlServletWebServerApplicationContext}, loading bean
   * definitions from the given resources and automatically refreshing the context.
   *
   * @param resources the resources to load from
   */
  public XmlServletWebServerApplicationContext(Resource... resources) {
    load(resources);
    refresh();
  }

  /**
   * Create a new {@link XmlServletWebServerApplicationContext}, loading bean
   * definitions from the given resource locations and automatically refreshing the
   * context.
   *
   * @param resourceLocations the resources to load from
   */
  public XmlServletWebServerApplicationContext(String... resourceLocations) {
    load(resourceLocations);
    refresh();
  }

  /**
   * Create a new {@link XmlServletWebServerApplicationContext}, loading bean
   * definitions from the given resource locations and automatically refreshing the
   * context.
   *
   * @param relativeClass class whose package will be used as a prefix when loading each
   * specified resource name
   * @param resourceNames relatively-qualified names of resources to load
   */
  public XmlServletWebServerApplicationContext(Class<?> relativeClass, String... resourceNames) {
    load(relativeClass, resourceNames);
    refresh();
  }

  /**
   * Set whether to use XML validation. Default is {@code true}.
   *
   * @param validating if validating the XML
   */
  public void setValidating(boolean validating) {
    this.reader.setValidating(validating);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Delegates the given environment to underlying {@link XmlBeanDefinitionReader}.
   * Should be called before any call to {@link #load}.
   */
  @Override
  public void setEnvironment(ConfigurableEnvironment environment) {
    super.setEnvironment(environment);
    this.reader.setEnvironment(getEnvironment());
  }

  /**
   * Load bean definitions from the given XML resources.
   *
   * @param resources one or more resources to load from
   */
  public final void load(Resource... resources) {
    this.reader.loadBeanDefinitions(resources);
  }

  /**
   * Load bean definitions from the given XML resources.
   *
   * @param resourceLocations one or more resource locations to load from
   */
  public final void load(String... resourceLocations) {
    this.reader.loadBeanDefinitions(resourceLocations);
  }

  /**
   * Load bean definitions from the given XML resources.
   *
   * @param relativeClass class whose package will be used as a prefix when loading each
   * specified resource name
   * @param resourceNames relatively-qualified names of resources to load
   */
  public final void load(Class<?> relativeClass, String... resourceNames) {
    Resource[] resources = new Resource[resourceNames.length];
    for (int i = 0; i < resourceNames.length; i++) {
      resources[i] = new ClassPathResource(resourceNames[i], relativeClass);
    }
    this.reader.loadBeanDefinitions(resources);
  }

}
