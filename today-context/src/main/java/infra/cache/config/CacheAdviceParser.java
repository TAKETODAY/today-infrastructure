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

package infra.cache.config;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import infra.beans.factory.config.TypedStringValue;
import infra.beans.factory.parsing.ReaderContext;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.ManagedList;
import infra.beans.factory.support.ManagedMap;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import infra.beans.factory.xml.BeanDefinitionParser;
import infra.beans.factory.xml.ParserContext;
import infra.cache.interceptor.CacheEvictOperation;
import infra.cache.interceptor.CacheInterceptor;
import infra.cache.interceptor.CacheOperation;
import infra.cache.interceptor.CachePutOperation;
import infra.cache.interceptor.CacheableOperation;
import infra.cache.interceptor.NameMatchCacheOperationSource;
import infra.util.StringUtils;
import infra.util.xml.DomUtils;

/**
 * {@link BeanDefinitionParser
 * BeanDefinitionParser} for the {@code <tx:advice/>} tag.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class CacheAdviceParser extends AbstractSingleBeanDefinitionParser {

  private static final String CACHEABLE_ELEMENT = "cacheable";

  private static final String CACHE_EVICT_ELEMENT = "cache-evict";

  private static final String CACHE_PUT_ELEMENT = "cache-put";

  private static final String METHOD_ATTRIBUTE = "method";

  private static final String DEFS_ELEMENT = "caching";

  @Override
  protected Class<?> getBeanClass(Element element) {
    return CacheInterceptor.class;
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    builder.addPropertyReference("cacheManager", CacheNamespaceHandler.extractCacheManager(element));
    CacheNamespaceHandler.parseKeyGenerator(element, builder.getBeanDefinition());

    List<Element> cacheDefs = DomUtils.getChildElementsByTagName(element, DEFS_ELEMENT);
    if (!cacheDefs.isEmpty()) {
      // Using attributes source.
      var attributeSourceDefinitions = parseDefinitionsSources(cacheDefs, parserContext);
      builder.addPropertyValue("cacheOperationSources", attributeSourceDefinitions);
    }
    else {
      // Assume annotations source.
      RootBeanDefinition definition = new RootBeanDefinition("infra.cache.annotation.AnnotationCacheOperationSource");
      definition.setEnableDependencyInjection(false);
      builder.addPropertyValue("cacheOperationSources", definition);
    }
  }

  private List<RootBeanDefinition> parseDefinitionsSources(List<Element> definitions, ParserContext parserContext) {
    ManagedList<RootBeanDefinition> defs = new ManagedList<>(definitions.size());

    // extract default param for the definition
    for (Element element : definitions) {
      defs.add(parseDefinitionSource(element, parserContext));
    }

    return defs;
  }

  private RootBeanDefinition parseDefinitionSource(Element definition, ParserContext parserContext) {
    Props prop = new Props(definition);
    // add cacheable first

    ManagedMap<TypedStringValue, Collection<CacheOperation>> cacheOpMap = new ManagedMap<>();
    cacheOpMap.setSource(parserContext.extractSource(definition));

    List<Element> cacheableCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHEABLE_ELEMENT);

    for (Element opElement : cacheableCacheMethods) {
      String name = prop.merge(opElement, parserContext.getReaderContext());
      TypedStringValue nameHolder = new TypedStringValue(name);
      nameHolder.setSource(parserContext.extractSource(opElement));
      CacheableOperation.Builder builder = prop.merge(opElement,
              parserContext.getReaderContext(), new CacheableOperation.Builder());
      builder.setUnless(getAttributeValue(opElement, "unless", ""));
      builder.setSync(Boolean.parseBoolean(getAttributeValue(opElement, "sync", "false")));

      Collection<CacheOperation> col = cacheOpMap.computeIfAbsent(nameHolder, k -> new ArrayList<>(2));
      col.add(builder.build());
    }

    List<Element> evictCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHE_EVICT_ELEMENT);

    for (Element opElement : evictCacheMethods) {
      String name = prop.merge(opElement, parserContext.getReaderContext());
      TypedStringValue nameHolder = new TypedStringValue(name);
      nameHolder.setSource(parserContext.extractSource(opElement));
      CacheEvictOperation.Builder builder = prop.merge(opElement,
              parserContext.getReaderContext(), new CacheEvictOperation.Builder());

      String wide = opElement.getAttribute("all-entries");
      if (StringUtils.hasText(wide)) {
        builder.setCacheWide(Boolean.parseBoolean(wide.trim()));
      }

      String after = opElement.getAttribute("before-invocation");
      if (StringUtils.hasText(after)) {
        builder.setBeforeInvocation(Boolean.parseBoolean(after.trim()));
      }

      Collection<CacheOperation> col = cacheOpMap.computeIfAbsent(nameHolder, k -> new ArrayList<>(2));
      col.add(builder.build());
    }

    List<Element> putCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHE_PUT_ELEMENT);

    for (Element opElement : putCacheMethods) {
      String name = prop.merge(opElement, parserContext.getReaderContext());
      TypedStringValue nameHolder = new TypedStringValue(name);
      nameHolder.setSource(parserContext.extractSource(opElement));
      CachePutOperation.Builder builder = prop.merge(opElement,
              parserContext.getReaderContext(), new CachePutOperation.Builder());
      builder.setUnless(getAttributeValue(opElement, "unless", ""));

      Collection<CacheOperation> col = cacheOpMap.computeIfAbsent(nameHolder, k -> new ArrayList<>(2));
      col.add(builder.build());
    }

    RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(NameMatchCacheOperationSource.class);
    attributeSourceDefinition.setSource(parserContext.extractSource(definition));
    attributeSourceDefinition.getPropertyValues().add("nameMap", cacheOpMap);
    attributeSourceDefinition.setEnableDependencyInjection(false);
    return attributeSourceDefinition;
  }

  private static String getAttributeValue(Element element, String attributeName, String defaultValue) {
    String attribute = element.getAttribute(attributeName);
    if (StringUtils.hasText(attribute)) {
      return attribute.trim();
    }
    return defaultValue;
  }

  /**
   * Simple, reusable class used for overriding defaults.
   */
  private static class Props {

    private final String key;

    private final String keyGenerator;

    private final String cacheManager;

    private final String condition;

    private final String method;

    @Nullable
    private String[] caches;

    Props(Element root) {
      String defaultCache = root.getAttribute("cache");
      this.key = root.getAttribute("key");
      this.keyGenerator = root.getAttribute("key-generator");
      this.cacheManager = root.getAttribute("cache-manager");
      this.condition = root.getAttribute("condition");
      this.method = root.getAttribute(METHOD_ATTRIBUTE);

      if (StringUtils.hasText(defaultCache)) {
        this.caches = StringUtils.commaDelimitedListToStringArray(defaultCache.trim());
      }
    }

    <T extends CacheOperation.Builder> T merge(Element element, ReaderContext readerCtx, T builder) {
      String cache = element.getAttribute("cache");

      // sanity check
      String[] localCaches = this.caches;
      if (StringUtils.hasText(cache)) {
        localCaches = StringUtils.commaDelimitedListToStringArray(cache.trim());
      }
      if (localCaches != null) {
        builder.setCacheNames(localCaches);
      }
      else {
        readerCtx.error("No cache specified for " + element.getNodeName(), element);
      }

      builder.setKey(getAttributeValue(element, "key", this.key));
      builder.setKeyGenerator(getAttributeValue(element, "key-generator", this.keyGenerator));
      builder.setCacheManager(getAttributeValue(element, "cache-manager", this.cacheManager));
      builder.setCondition(getAttributeValue(element, "condition", this.condition));

      if (StringUtils.hasText(builder.getKey()) && StringUtils.hasText(builder.getKeyGenerator())) {
        throw new IllegalStateException("Invalid cache advice configuration on '" +
                element + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
                "These attributes are mutually exclusive: either set the EL expression used to" +
                "compute the key at runtime or set the name of the KeyGenerator bean to use.");
      }

      return builder;
    }

    @Nullable
    String merge(Element element, ReaderContext readerCtx) {
      String method = element.getAttribute(METHOD_ATTRIBUTE);
      if (StringUtils.hasText(method)) {
        return method.trim();
      }
      if (StringUtils.hasText(this.method)) {
        return this.method;
      }
      readerCtx.error("No method specified for " + element.getNodeName(), element);
      return null;
    }
  }

}
