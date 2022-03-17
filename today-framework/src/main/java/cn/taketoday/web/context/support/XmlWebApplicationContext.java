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

package cn.taketoday.web.context.support;

import java.io.IOException;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.ResourceEntityResolver;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * {@link cn.taketoday.web.WebApplicationContext} implementation
 * which takes its configuration from XML documents, understood by an
 * {@link cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader}.
 * This is essentially the equivalent of
 * {@link cn.taketoday.context.support.GenericXmlApplicationContext}
 * for a web environment.
 *
 * <p>By default, the configuration will be taken from "/WEB-INF/applicationContext.xml"
 * for the root context, and "/WEB-INF/test-servlet.xml" for a context with the namespace
 * "test-servlet" (like for a DispatcherServlet instance with the servlet-name "test").
 *
 * <p>The config location defaults can be overridden via the "contextConfigLocation"
 * context-param of {@link cn.taketoday.web.context.ContextLoader} and servlet
 * init-param of {@link cn.taketoday.web.servlet.DispatcherServlet}. Config locations
 * can either denote concrete files like "/WEB-INF/context.xml" or Ant-style patterns
 * like "/WEB-INF/*-context.xml" (see {@link cn.taketoday.core.PathMatcher}
 * javadoc for pattern details).
 *
 * <p>Note: In case of multiple config locations, later bean definitions will
 * override ones defined in earlier loaded files. This can be leveraged to
 * deliberately override certain bean definitions via an extra XML file.
 *
 * <p><b>For a WebApplicationContext that reads in a different bean definition format,
 * create an analogous subclass of {@link AbstractRefreshableWebApplicationContext}.</b>
 * Such a context implementation can be specified as "contextClass" context-param
 * for ContextLoader or "contextClass" init-param for FrameworkServlet.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setNamespace
 * @see #setConfigLocations
 * @see cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader
 * @see cn.taketoday.web.context.ContextLoader#initWebApplicationContext
 * @since 4.0 2022/3/6 22:17
 */
public class XmlWebApplicationContext extends AbstractRefreshableWebApplicationContext {

  /** Default config location for the root context. */
  public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.xml";

  /** Default prefix for building a config location for a namespace. */
  public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";

  /** Default suffix for building a config location for a namespace. */
  public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".xml";

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
    beanDefinitionReader.setEnvironment(getEnvironment());
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
   * @param beanDefinitionReader the bean definition reader used by this context
   * @see cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader#setValidationMode
   * @see cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader#setDocumentReaderClass
   */
  protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {

  }

  /**
   * Load the bean definitions with the given XmlBeanDefinitionReader.
   * <p>The lifecycle of the bean factory is handled by the refreshBeanFactory method;
   * therefore this method is just supposed to load and/or register bean definitions.
   * <p>Delegates to a ResourcePatternResolver for resolving location patterns
   * into Resource instances.
   *
   * @throws IOException if the required XML document isn't found
   * @see #refreshBeanFactory
   * @see #getConfigLocations
   * @see #getResources
   * @see #getPatternResourceLoader()
   */
  protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws IOException {
    String[] configLocations = getConfigLocations();
    if (configLocations != null) {
      for (String configLocation : configLocations) {
        reader.loadBeanDefinitions(configLocation);
      }
    }
  }

  /**
   * The default location for the root context is "/WEB-INF/applicationContext.xml",
   * and "/WEB-INF/test-servlet.xml" for a context with the namespace "test-servlet"
   * (like for a DispatcherServlet instance with the servlet-name "test").
   */
  @Override
  protected String[] getDefaultConfigLocations() {
    if (getNamespace() != null) {
      return new String[] { DEFAULT_CONFIG_LOCATION_PREFIX + getNamespace() + DEFAULT_CONFIG_LOCATION_SUFFIX };
    }
    else {
      return new String[] { DEFAULT_CONFIG_LOCATION };
    }
  }

}
