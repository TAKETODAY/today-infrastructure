/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.function.Consumer;
import java.util.regex.Pattern;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.parsing.CompositeComponentDefinition;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.xml.BeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.beans.factory.xml.XmlReaderContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.AspectJTypeFilter;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.core.type.filter.RegexPatternTypeFilter;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Parser for the {@code <context:component-scan/>} element.
 *
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 18:04
 */
public class ComponentScanBeanDefinitionParser implements BeanDefinitionParser {

  private static final String BASE_PACKAGE_ATTRIBUTE = "base-package";

  private static final String RESOURCE_PATTERN_ATTRIBUTE = "resource-pattern";

  private static final String USE_DEFAULT_FILTERS_ATTRIBUTE = "use-default-filters";

  private static final String ANNOTATION_CONFIG_ATTRIBUTE = "annotation-config";

  private static final String NAME_GENERATOR_ATTRIBUTE = "name-generator";

  private static final String SCOPE_RESOLVER_ATTRIBUTE = "scope-resolver";

  private static final String SCOPED_PROXY_ATTRIBUTE = "scoped-proxy";

  private static final String EXCLUDE_FILTER_ELEMENT = "exclude-filter";

  private static final String INCLUDE_FILTER_ELEMENT = "include-filter";

  private static final String FILTER_TYPE_ATTRIBUTE = "type";

  private static final String FILTER_EXPRESSION_ATTRIBUTE = "expression";

  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    String basePackage = element.getAttribute(BASE_PACKAGE_ATTRIBUTE);
    basePackage = parserContext.getReaderContext().getEnvironment().resolvePlaceholders(basePackage);
    String[] basePackages = StringUtils.tokenizeToStringArray(basePackage,
            ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);

    // Actually scan for bean definitions and register them.
    ClassPathBeanDefinitionScanner scanner = configureScanner(parserContext, element);
    registerComponents(parserContext.getReaderContext(), scanner, basePackages, element);

    return null;
  }

  protected ClassPathBeanDefinitionScanner configureScanner(ParserContext parserContext, Element element) {
    boolean useDefaultFilters = true;
    if (element.hasAttribute(USE_DEFAULT_FILTERS_ATTRIBUTE)) {
      useDefaultFilters = Boolean.parseBoolean(element.getAttribute(USE_DEFAULT_FILTERS_ATTRIBUTE));
    }

    // Delegate bean definition registration to scanner class.
    ClassPathBeanDefinitionScanner scanner = createScanner(parserContext.getReaderContext(), useDefaultFilters);
    scanner.setBeanDefinitionDefaults(parserContext.getDelegate().getBeanDefinitionDefaults());
    scanner.setAutowireCandidatePatterns(parserContext.getDelegate().getAutowireCandidatePatterns());

    if (element.hasAttribute(RESOURCE_PATTERN_ATTRIBUTE)) {
      scanner.setResourcePattern(element.getAttribute(RESOURCE_PATTERN_ATTRIBUTE));
    }

    try {
      parseBeanNameGenerator(element, scanner);
    }
    catch (Exception ex) {
      parserContext.getReaderContext().error(ex.getMessage(), parserContext.extractSource(element), ex.getCause());
    }

    try {
      parseScope(element, scanner);
    }
    catch (Exception ex) {
      parserContext.getReaderContext().error(ex.getMessage(), parserContext.extractSource(element), ex.getCause());
    }

    parseTypeFilters(element, scanner, parserContext);

    return scanner;
  }

  protected ClassPathBeanDefinitionScanner createScanner(XmlReaderContext readerContext, boolean useDefaultFilters) {
    return new ClassPathBeanDefinitionScanner(readerContext.getRegistry(), useDefaultFilters,
            readerContext.getEnvironment(), readerContext.getResourceLoader());
  }

  protected void registerComponents(
          XmlReaderContext readerContext, ClassPathBeanDefinitionScanner scanner, String[] basePackages, Element element) {

    Object source = readerContext.extractSource(element);
    CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), source);

    scanner.scan(new Consumer<BeanDefinitionHolder>() {
      @Override
      public void accept(BeanDefinitionHolder holder) {
        compositeDef.addNestedComponent(new BeanComponentDefinition(holder));
      }
    }, basePackages);

    // Register annotation config processors, if necessary.
    boolean annotationConfig = true;
    if (element.hasAttribute(ANNOTATION_CONFIG_ATTRIBUTE)) {
      annotationConfig = Boolean.parseBoolean(element.getAttribute(ANNOTATION_CONFIG_ATTRIBUTE));
    }
    if (annotationConfig) {
      AnnotationConfigUtils.registerAnnotationConfigProcessors(readerContext.getRegistry(), definition -> {
        definition.setSource(source);
        compositeDef.addNestedComponent(new BeanComponentDefinition(definition));
      });
    }

    readerContext.fireComponentRegistered(compositeDef);
  }

  protected void parseBeanNameGenerator(Element element, ClassPathBeanDefinitionScanner scanner) {
    if (element.hasAttribute(NAME_GENERATOR_ATTRIBUTE)) {
      BeanNameGenerator beanNameGenerator = instantiateUserDefinedStrategy(
              element.getAttribute(NAME_GENERATOR_ATTRIBUTE), BeanNameGenerator.class,
              scanner.getResourceLoader().getClassLoader());
      scanner.setBeanNameGenerator(beanNameGenerator);
    }
  }

  protected void parseScope(Element element, ClassPathBeanDefinitionScanner scanner) {
    // Register ScopeMetadataResolver if class name provided.
    if (element.hasAttribute(SCOPE_RESOLVER_ATTRIBUTE)) {
      if (element.hasAttribute(SCOPED_PROXY_ATTRIBUTE)) {
        throw new IllegalArgumentException(
                "Cannot define both 'scope-resolver' and 'scoped-proxy' on <component-scan> tag");
      }
      ScopeMetadataResolver scopeMetadataResolver = instantiateUserDefinedStrategy(
              element.getAttribute(SCOPE_RESOLVER_ATTRIBUTE), ScopeMetadataResolver.class,
              scanner.getResourceLoader().getClassLoader());
      scanner.setScopeMetadataResolver(scopeMetadataResolver);
    }

    if (element.hasAttribute(SCOPED_PROXY_ATTRIBUTE)) {
      String mode = element.getAttribute(SCOPED_PROXY_ATTRIBUTE);
      switch (mode) {
        case "no" -> scanner.setScopedProxyMode(ScopedProxyMode.NO);
        case "interfaces" -> scanner.setScopedProxyMode(ScopedProxyMode.INTERFACES);
        case "targetClass" -> scanner.setScopedProxyMode(ScopedProxyMode.TARGET_CLASS);
        default -> throw new IllegalArgumentException("scoped-proxy only supports 'no', 'interfaces' and 'targetClass'");
      }
    }
  }

  protected void parseTypeFilters(Element element, ClassPathBeanDefinitionScanner scanner, ParserContext parserContext) {
    // Parse exclude and include filter elements.
    ClassLoader classLoader = scanner.getResourceLoader().getClassLoader();
    NodeList nodeList = element.getChildNodes();
    int length = nodeList.getLength();
    for (int i = 0; i < length; i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        String localName = parserContext.getDelegate().getLocalName(node);
        try {
          if (INCLUDE_FILTER_ELEMENT.equals(localName)) {
            TypeFilter typeFilter = createTypeFilter((Element) node, classLoader, parserContext);
            scanner.addIncludeFilter(typeFilter);
          }
          else if (EXCLUDE_FILTER_ELEMENT.equals(localName)) {
            TypeFilter typeFilter = createTypeFilter((Element) node, classLoader, parserContext);
            scanner.addExcludeFilter(typeFilter);
          }
        }
        catch (ClassNotFoundException ex) {
          parserContext.getReaderContext().warning(
                  "Ignoring non-present type filter class: " + ex, parserContext.extractSource(element));
        }
        catch (Exception ex) {
          parserContext.getReaderContext().error(
                  ex.getMessage(), parserContext.extractSource(element), ex.getCause());
        }
      }
    }
  }

  protected TypeFilter createTypeFilter(Element element, @Nullable ClassLoader classLoader,
          ParserContext parserContext) throws ClassNotFoundException {

    String filterType = element.getAttribute(FILTER_TYPE_ATTRIBUTE);
    String expression = element.getAttribute(FILTER_EXPRESSION_ATTRIBUTE);
    expression = parserContext.getReaderContext().getEnvironment().resolvePlaceholders(expression);
    switch (filterType) {
      case "annotation":
        return new AnnotationTypeFilter(ClassUtils.forName(expression, classLoader));
      case "assignable":
        return new AssignableTypeFilter(ClassUtils.forName(expression, classLoader));
      case "aspectj":
        return new AspectJTypeFilter(expression, classLoader);
      case "regex":
        return new RegexPatternTypeFilter(Pattern.compile(expression));
      case "custom":
        Class<?> filterClass = ClassUtils.forName(expression, classLoader);
        if (!TypeFilter.class.isAssignableFrom(filterClass)) {
          throw new IllegalArgumentException(
                  "Class is not assignable to [" + TypeFilter.class.getName() + "]: " + expression);
        }
        return (TypeFilter) BeanUtils.newInstance(filterClass);
      default:
        throw new IllegalArgumentException("Unsupported filter type: " + filterType);
    }
  }

  private <T> T instantiateUserDefinedStrategy(
          String className, Class<T> strategyType, @Nullable ClassLoader classLoader) {

    T result;
    try {
      result = ReflectionUtils.accessibleConstructor(ClassUtils.<T>forName(className, classLoader)).newInstance();
    }
    catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException("Class [" + className + "] for strategy [" +
              strategyType.getName() + "] not found", ex);
    }
    catch (Throwable ex) {
      throw new IllegalArgumentException("Unable to instantiate class [" + className + "] for strategy [" +
              strategyType.getName() + "]: a zero-argument constructor is required", ex);
    }

    if (!strategyType.isAssignableFrom(result.getClass())) {
      throw new IllegalArgumentException("Provided class name must be an implementation of " + strategyType);
    }
    return result;
  }

}
