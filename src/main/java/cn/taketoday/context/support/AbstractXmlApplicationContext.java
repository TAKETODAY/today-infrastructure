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

import java.io.IOException;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.ResourceEntityResolver;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;

/**
 * Convenient base class for {@link cn.taketoday.context.ApplicationContext}
 * implementations, drawing configuration from XML documents containing bean definitions
 * understood by an {@link cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader}.
 *
 * <p>Subclasses just have to implement the {@link #getConfigResources} and/or
 * the {@link #getConfigLocations} method. Furthermore, they might override
 * the {@link #getResourceByPath} hook to interpret relative paths in an
 * environment-specific fashion, and/or {@link #getPatternResourceLoader()}
 * for extended pattern resolution.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getConfigResources
 * @see #getConfigLocations
 * @see XmlBeanDefinitionReader
 * @since 4.0 2022/3/6 22:03
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableConfigApplicationContext {

  private boolean validating = true;

  /**
   * Create a new AbstractXmlApplicationContext with no parent.
   */
  public AbstractXmlApplicationContext() {
  }

  /**
   * Create a new AbstractXmlApplicationContext with the given parent context.
   *
   * @param parent the parent context
   */
  public AbstractXmlApplicationContext(@Nullable ApplicationContext parent) {
    super(parent);
  }

  /**
   * Set whether to use XML validation. Default is {@code true}.
   */
  public void setValidating(boolean validating) {
    this.validating = validating;
  }

  /**
   * Loads the bean definitions via an XmlBeanDefinitionReader.
   *
   * @see cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader
   * @see #initBeanDefinitionReader
   * @see #loadBeanDefinitions
   */
  @Override
  protected void loadBeanDefinitions(StandardBeanFactory beanFactory) throws BeansException, IOException {

    // Create a new XmlBeanDefinitionReader for the given BeanFactory.
    XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

    // Configure the bean definition reader with this context's
    // resource loading environment.
    beanDefinitionReader.setEnvironment(this.getEnvironment());
    beanDefinitionReader.setResourceLoader(this);
    beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

    // Allow a subclass to provide custom initialization of the reader,
    // then proceed with actually loading the bean definitions.
    initBeanDefinitionReader(beanDefinitionReader);
    loadBeanDefinitions(beanDefinitionReader);
  }

  /**
   * Initialize the bean definition reader used for loading the bean
   * definitions of this context. Default implementation is empty.
   * <p>Can be overridden in subclasses, e.g. for turning off XML validation
   * or using a different XmlBeanDefinitionParser implementation.
   *
   * @param reader the bean definition reader used by this context
   * @see cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader#setDocumentReaderClass
   */
  protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
    reader.setValidating(this.validating);
  }

  /**
   * Load the bean definitions with the given XmlBeanDefinitionReader.
   * <p>The lifecycle of the bean factory is handled by the {@link #refreshBeanFactory}
   * method; hence this method is just supposed to load and/or register bean definitions.
   *
   * @param reader the XmlBeanDefinitionReader to use
   * @throws BeansException in case of bean registration errors
   * @throws IOException if the required XML document isn't found
   * @see #refreshBeanFactory
   * @see #getConfigLocations
   * @see #getResources
   * @see #getPatternResourceLoader()
   */
  protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
    Resource[] configResources = getConfigResources();
    if (configResources != null) {
      reader.loadBeanDefinitions(configResources);
    }
    String[] configLocations = getConfigLocations();
    if (configLocations != null) {
      reader.loadBeanDefinitions(configLocations);
    }
  }

  /**
   * Return an array of Resource objects, referring to the XML bean definition
   * files that this context should be built with.
   * <p>The default implementation returns {@code null}. Subclasses can override
   * this to provide pre-built Resource objects rather than location Strings.
   *
   * @return an array of Resource objects, or {@code null} if none
   * @see #getConfigLocations()
   */
  @Nullable
  protected Resource[] getConfigResources() {
    return null;
  }

}

