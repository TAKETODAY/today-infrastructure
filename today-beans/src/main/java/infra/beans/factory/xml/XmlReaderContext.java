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

package infra.beans.factory.xml;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.StringReader;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.parsing.ProblemReporter;
import infra.beans.factory.parsing.ReaderContext;
import infra.beans.factory.parsing.ReaderEventListener;
import infra.beans.factory.parsing.SourceExtractor;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanNameGenerator;
import infra.core.env.Environment;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;

/**
 * Extension of {@link ReaderContext},
 * specific to use with an {@link XmlBeanDefinitionReader}. Provides access to the
 * {@link NamespaceHandlerResolver} configured in the {@link XmlBeanDefinitionReader}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class XmlReaderContext extends ReaderContext {

  private final XmlBeanDefinitionReader reader;

  private final NamespaceHandlerResolver namespaceHandlerResolver;

  /**
   * Construct a new {@code XmlReaderContext}.
   *
   * @param resource the XML bean definition resource
   * @param problemReporter the problem reporter in use
   * @param eventListener the event listener in use
   * @param sourceExtractor the source extractor in use
   * @param reader the XML bean definition reader in use
   * @param namespaceHandlerResolver the XML namespace resolver
   */
  public XmlReaderContext(
          Resource resource, ProblemReporter problemReporter,
          ReaderEventListener eventListener, SourceExtractor sourceExtractor,
          XmlBeanDefinitionReader reader, NamespaceHandlerResolver namespaceHandlerResolver) {

    super(resource, problemReporter, eventListener, sourceExtractor);
    this.reader = reader;
    this.namespaceHandlerResolver = namespaceHandlerResolver;
  }

  /**
   * Return the XML bean definition reader in use.
   */
  public final XmlBeanDefinitionReader getReader() {
    return this.reader;
  }

  /**
   * Return the bean definition registry to use.
   *
   * @see XmlBeanDefinitionReader#XmlBeanDefinitionReader(BeanDefinitionRegistry)
   */
  public final BeanDefinitionRegistry getRegistry() {
    return this.reader.getRegistry();
  }

  /**
   * Return the resource loader to use, if any.
   * <p>This will be non-null in regular scenarios,
   * also allowing access to the resource class loader.
   *
   * @see XmlBeanDefinitionReader#setResourceLoader
   * @see ResourceLoader#getClassLoader()
   */
  @Nullable
  public final ResourceLoader getResourceLoader() {
    return this.reader.getResourceLoader();
  }

  /**
   * Return the bean class loader to use, if any.
   * <p>Note that this will be null in regular scenarios,
   * as an indication to lazily resolve bean classes.
   *
   * @see XmlBeanDefinitionReader#setBeanClassLoader
   */
  @Nullable
  public final ClassLoader getBeanClassLoader() {
    return this.reader.getBeanClassLoader();
  }

  /**
   * Return the environment to use.
   *
   * @see XmlBeanDefinitionReader#setEnvironment
   */
  public final Environment getEnvironment() {
    return this.reader.getEnvironment();
  }

  /**
   * Return the namespace resolver.
   *
   * @see XmlBeanDefinitionReader#setNamespaceHandlerResolver
   */
  public final NamespaceHandlerResolver getNamespaceHandlerResolver() {
    return this.namespaceHandlerResolver;
  }

  // Convenience methods to delegate to

  /**
   * Call the bean name generator for the given bean definition.
   *
   * @see XmlBeanDefinitionReader#getBeanNameGenerator()
   * @see BeanNameGenerator
   */
  public String generateBeanName(BeanDefinition beanDefinition) {
    return this.reader.getBeanNameGenerator().generateBeanName(beanDefinition, getRegistry());
  }

  /**
   * Call the bean name generator for the given bean definition
   * and register the bean definition under the generated name.
   *
   * @see XmlBeanDefinitionReader#getBeanNameGenerator()
   * @see BeanNameGenerator#generateBeanName
   * @see BeanDefinitionRegistry#registerBeanDefinition
   */
  public String registerWithGeneratedName(BeanDefinition beanDefinition) {
    String generatedName = generateBeanName(beanDefinition);
    getRegistry().registerBeanDefinition(generatedName, beanDefinition);
    return generatedName;
  }

  /**
   * Read an XML document from the given String.
   *
   * @see #getReader()
   */
  public Document readDocumentFromString(String documentContent) {
    InputSource is = new InputSource(new StringReader(documentContent));
    try {
      return this.reader.doLoadDocument(is, getResource());
    }
    catch (Exception ex) {
      throw new BeanDefinitionStoreException("Failed to read XML document", ex);
    }
  }

}
