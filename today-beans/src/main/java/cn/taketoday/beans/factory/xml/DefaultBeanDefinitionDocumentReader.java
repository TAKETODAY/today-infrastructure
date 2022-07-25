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

package cn.taketoday.beans.factory.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.parsing.SourceExtractor;
import cn.taketoday.beans.factory.support.BeanDefinitionReaderUtils;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
 * reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Framework's default XML bean definition format).
 *
 * <p>The structure, elements, and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code <beans>} does not need to be the root
 * element of the XML document: this class will parse all bean definition elements
 * in the XML file, regardless of the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 4.0
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {
  private static final Logger log = LoggerFactory.getLogger(DefaultBeanDefinitionDocumentReader.class);

  public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

  public static final String NESTED_BEANS_ELEMENT = "beans";

  public static final String ALIAS_ELEMENT = "alias";

  public static final String NAME_ATTRIBUTE = "name";

  public static final String ALIAS_ATTRIBUTE = "alias";

  public static final String IMPORT_ELEMENT = "import";

  public static final String RESOURCE_ATTRIBUTE = "resource";

  public static final String PROFILE_ATTRIBUTE = "profile";

  @Nullable
  private XmlReaderContext readerContext;

  @Nullable
  private BeanDefinitionParserDelegate delegate;

  /**
   * This implementation parses bean definitions according to the "spring-beans" XSD
   * (or DTD, historically).
   * <p>Opens a DOM Document; then initializes the default settings
   * specified at the {@code <beans/>} level; then parses the contained bean definitions.
   */
  @Override
  public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
    this.readerContext = readerContext;
    doRegisterBeanDefinitions(doc.getDocumentElement());
  }

  /**
   * Return the descriptor for the XML resource that this parser works on.
   */
  protected final XmlReaderContext getReaderContext() {
    Assert.state(this.readerContext != null, "No XmlReaderContext available");
    return this.readerContext;
  }

  /**
   * Invoke the {@link SourceExtractor}
   * to pull the source metadata from the supplied {@link Element}.
   */
  @Nullable
  protected Object extractSource(Element ele) {
    return getReaderContext().extractSource(ele);
  }

  /**
   * Register each bean definition within the given root {@code <beans/>} element.
   */
  protected void doRegisterBeanDefinitions(Element root) {
    // Any nested <beans> elements will cause recursion in this method. In
    // order to propagate and preserve <beans> default-* attributes correctly,
    // keep track of the current (parent) delegate, which may be null. Create
    // the new (child) delegate with a reference to the parent for fallback purposes,
    // then ultimately reset this.delegate back to its original (parent) reference.
    // this behavior emulates a stack of delegates without actually necessitating one.
    BeanDefinitionParserDelegate parent = this.delegate;
    this.delegate = createDelegate(getReaderContext(), root, parent);

    if (this.delegate.isDefaultNamespace(root)) {
      String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
      if (StringUtils.hasText(profileSpec)) {
        String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
                profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
        // We cannot use Profiles.of(...) since profile expressions are not supported
        // in XML config. See SPR-12458 for details.
        if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
          if (log.isDebugEnabled()) {
            log.debug("Skipped XML bean definition file due to specified profiles [{}] not matching: {}",
                    profileSpec, getReaderContext().getResource());
          }
          return;
        }
      }
    }

    preProcessXml(root);
    parseBeanDefinitions(root, this.delegate);
    postProcessXml(root);

    this.delegate = parent;
  }

  protected BeanDefinitionParserDelegate createDelegate(
          XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {

    BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
    delegate.initDefaults(root, parentDelegate);
    return delegate;
  }

  /**
   * Parse the elements at the root level in the document:
   * "import", "alias", "bean".
   *
   * @param root the DOM root element of the document
   */
  protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
    if (delegate.isDefaultNamespace(root)) {
      NodeList nl = root.getChildNodes();
      for (int i = 0; i < nl.getLength(); i++) {
        Node node = nl.item(i);
        if (node instanceof Element ele) {
          if (delegate.isDefaultNamespace(ele)) {
            parseDefaultElement(ele, delegate);
          }
          else {
            delegate.parseCustomElement(ele);
          }
        }
      }
    }
    else {
      delegate.parseCustomElement(root);
    }
  }

  private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
    if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
      importBeanDefinitionResource(ele);
    }
    else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
      processAliasRegistration(ele);
    }
    else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
      processBeanDefinition(ele, delegate);
    }
    else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
      // recurse
      doRegisterBeanDefinitions(ele);
    }
  }

  /**
   * Parse an "import" element and load the bean definitions
   * from the given resource into the bean factory.
   */
  protected void importBeanDefinitionResource(Element ele) {
    String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
    XmlReaderContext readerContext = getReaderContext();
    if (!StringUtils.hasText(location)) {
      readerContext.error("Resource location must not be empty", ele);
      return;
    }

    // Resolve system properties: e.g. "${user.dir}"
    location = readerContext.getEnvironment().resolveRequiredPlaceholders(location);

    LinkedHashSet<Resource> actualResources = new LinkedHashSet<>(4);

    // Discover whether the location is an absolute or relative URI
    boolean absoluteLocation = false;
    try {
      absoluteLocation = PatternResourceLoader.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
    }
    catch (URISyntaxException ex) {
      // cannot convert to an URI, considering the location relative
      // unless it is the well-known Spring prefix "classpath*:"
    }

    // Absolute or relative?
    if (absoluteLocation) {
      try {
        int importCount = readerContext.getReader().loadBeanDefinitions(location, actualResources);
        if (log.isTraceEnabled()) {
          log.trace("Imported {} bean definitions from URL location [{}]", importCount, location);
        }
      }
      catch (BeanDefinitionStoreException ex) {
        readerContext.error(
                "Failed to import bean definitions from URL location [" + location + "]", ele, ex);
      }
    }
    else {
      // No URL -> considering resource location as relative to the current file.
      try {
        int importCount = 0;
        boolean patternSearch = true;
        try {
          Resource relativeResource = readerContext.getResource().createRelative(location);
          if (relativeResource.exists()) {
            importCount = readerContext.getReader().loadBeanDefinitions(relativeResource);
            actualResources.add(relativeResource);
            patternSearch = false;
          }
        }
        catch (IllegalArgumentException e) {
          // Illegal char <*>
        }

        if (patternSearch) {
          String baseLocation = readerContext.getResource().getURL().toString();
          importCount = readerContext.getReader().loadBeanDefinitions(
                  ResourceUtils.getRelativePath(baseLocation, location), actualResources);
        }

        if (log.isTraceEnabled()) {
          log.trace("Imported {} bean definitions from relative location [{}]", importCount, location);
        }
      }
      catch (IOException ex) {
        readerContext.error("Failed to resolve current resource location", ele, ex);
      }
      catch (BeanDefinitionStoreException ex) {
        readerContext.error(
                "Failed to import bean definitions from relative location [" + location + "]", ele, ex);
      }
    }
    Resource[] actResArray = actualResources.toArray(Resource.EMPTY_ARRAY);
    readerContext.fireImportProcessed(location, actResArray, extractSource(ele));
  }

  /**
   * Process the given alias element, registering the alias with the registry.
   */
  protected void processAliasRegistration(Element ele) {
    String name = ele.getAttribute(NAME_ATTRIBUTE);
    String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
    boolean valid = true;
    if (!StringUtils.hasText(name)) {
      getReaderContext().error("Name must not be empty", ele);
      valid = false;
    }
    if (!StringUtils.hasText(alias)) {
      getReaderContext().error("Alias must not be empty", ele);
      valid = false;
    }
    if (valid) {
      try {
        getReaderContext().getRegistry().registerAlias(name, alias);
      }
      catch (Exception ex) {
        getReaderContext().error("Failed to register alias '" + alias +
                "' for bean with name '" + name + "'", ele, ex);
      }
      getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
    }
  }

  /**
   * Process the given bean element, parsing the bean definition
   * and registering it with the registry.
   */
  protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
    BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
    if (bdHolder != null) {
      bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
      try {
        // Register the final decorated instance.
        BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
      }
      catch (BeanDefinitionStoreException ex) {
        getReaderContext().error("Failed to register bean definition with name '" +
                bdHolder.getBeanName() + "'", ele, ex);
      }
      // Send registration event.
      getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
    }
  }

  /**
   * Allow the XML to be extensible by processing any custom element types first,
   * before we start to process the bean definitions. This method is a natural
   * extension point for any other custom pre-processing of the XML.
   * <p>The default implementation is empty. Subclasses can override this method to
   * convert custom elements into standard Framework bean definitions, for example.
   * Implementors have access to the parser's bean definition reader and the
   * underlying XML resource, through the corresponding accessors.
   *
   * @see #getReaderContext()
   */
  protected void preProcessXml(Element root) {
  }

  /**
   * Allow the XML to be extensible by processing any custom element types last,
   * after we finished processing the bean definitions. This method is a natural
   * extension point for any other custom post-processing of the XML.
   * <p>The default implementation is empty. Subclasses can override this method to
   * convert custom elements into standard Framework bean definitions, for example.
   * Implementors have access to the parser's bean definition reader and the
   * underlying XML resource, through the corresponding accessors.
   *
   * @see #getReaderContext()
   */
  protected void postProcessXml(Element root) {
  }

}
