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

package infra.context.support;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.beans.BeansException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.ResourceEntityResolver;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.context.ApplicationContext;
import infra.core.io.Resource;

/**
 * Convenient base class for {@link infra.context.ApplicationContext}
 * implementations, drawing configuration from XML documents containing bean definitions
 * understood by an {@link XmlBeanDefinitionReader}.
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
   * @see XmlBeanDefinitionReader
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
   * @see XmlBeanDefinitionReader#setDocumentReaderClass
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
  protected Resource @Nullable [] getConfigResources() {
    return null;
  }

}

