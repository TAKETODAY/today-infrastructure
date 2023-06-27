/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.cache.config;

import org.w3c.dom.Element;

import cn.taketoday.aop.config.AopNamespaceUtils;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.parsing.CompositeComponentDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.xml.BeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import cn.taketoday.cache.interceptor.CacheInterceptor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link cn.taketoday.beans.factory.xml.BeanDefinitionParser}
 * implementation that allows users to easily configure all the
 * infrastructure beans required to enable annotation-driven cache
 * demarcation.
 *
 * <p>By default, all proxies are created as JDK proxies. This may cause
 * some problems if you are injecting objects as concrete classes rather
 * than interfaces. To overcome this restriction you can set the
 * '{@code proxy-target-class}' attribute to '{@code true}', which will
 * result in class-based proxies being created.
 *
 * <p>If the JSR-107 API and Framework's JCache implementation are present,
 * the necessary infrastructure beans required to handle methods annotated
 * with {@code CacheResult}, {@code CachePut}, {@code CacheRemove} or
 * {@code CacheRemoveAll} are also registered.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 4.0
 */
class AnnotationDrivenCacheBeanDefinitionParser implements BeanDefinitionParser {

  private static final String CACHE_ASPECT_CLASS_NAME =
          "cn.taketoday.cache.aspectj.AnnotationCacheAspect";

  private static final String JCACHE_ASPECT_CLASS_NAME =
          "cn.taketoday.cache.aspectj.JCacheCacheAspect";

  private static final boolean jsr107Present;

  private static final boolean jcacheImplPresent;

  static {
    ClassLoader classLoader = AnnotationDrivenCacheBeanDefinitionParser.class.getClassLoader();
    jsr107Present = ClassUtils.isPresent("javax.cache.Cache", classLoader);
    jcacheImplPresent = ClassUtils.isPresent(
            "cn.taketoday.cache.jcache.interceptor.DefaultJCacheOperationSource", classLoader);
  }

  /**
   * Parses the '{@code <cache:annotation-driven>}' tag. Will
   * {@link AopNamespaceUtils#registerAutoProxyCreatorIfNecessary
   * register an AutoProxyCreator} with the container as necessary.
   */
  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    String mode = element.getAttribute("mode");
    if ("aspectj".equals(mode)) {
      // mode="aspectj"
      registerCacheAspect(element, parserContext);
    }
    else {
      // mode="proxy"
      registerCacheAdvisor(element, parserContext);
    }

    return null;
  }

  private void registerCacheAspect(Element element, ParserContext parserContext) {
    CachingConfigurer.registerCacheAspect(element, parserContext);
    if (jsr107Present && jcacheImplPresent) {
      JCacheCachingConfigurer.registerCacheAspect(element, parserContext);
    }
  }

  private void registerCacheAdvisor(Element element, ParserContext parserContext) {
    AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);
    CachingConfigurer.registerCacheAdvisor(element, parserContext);
    if (jsr107Present && jcacheImplPresent) {
      JCacheCachingConfigurer.registerCacheAdvisor(element, parserContext);
    }
  }

  /**
   * Parse the cache resolution strategy to use. If a 'cache-resolver' attribute
   * is set, it is injected. Otherwise the 'cache-manager' is set. If {@code setBoth}
   * is {@code true}, both service are actually injected.
   */
  private static void parseCacheResolution(Element element, BeanDefinition def, boolean setBoth) {
    String name = element.getAttribute("cache-resolver");
    boolean hasText = StringUtils.hasText(name);
    if (hasText) {
      def.getPropertyValues().add("cacheResolver", new RuntimeBeanReference(name.trim()));
    }
    if (!hasText || setBoth) {
      def.getPropertyValues().add("cacheManager",
              new RuntimeBeanReference(CacheNamespaceHandler.extractCacheManager(element)));
    }
  }

  private static void parseErrorHandler(Element element, BeanDefinition def) {
    String name = element.getAttribute("error-handler");
    if (StringUtils.hasText(name)) {
      def.getPropertyValues().add("errorHandler", new RuntimeBeanReference(name.trim()));
    }
  }

  /**
   * Configure the necessary infrastructure to support the Framework's caching annotations.
   */
  private static class CachingConfigurer {

    private static void registerCacheAdvisor(Element element, ParserContext parserContext) {
      if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME)) {
        Object eleSource = parserContext.extractSource(element);

        // Create the CacheOperationSource definition.
        RootBeanDefinition sourceDef = new RootBeanDefinition("cn.taketoday.cache.annotation.AnnotationCacheOperationSource");
        sourceDef.setSource(eleSource);
        sourceDef.setEnableDependencyInjection(false);
        sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

        // Create the CacheInterceptor definition.
        RootBeanDefinition interceptorDef = new RootBeanDefinition(CacheInterceptor.class);
        interceptorDef.setSource(eleSource);
        interceptorDef.setEnableDependencyInjection(false);
        interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        parseCacheResolution(element, interceptorDef, false);
        parseErrorHandler(element, interceptorDef);
        CacheNamespaceHandler.parseKeyGenerator(element, interceptorDef);
        interceptorDef.getPropertyValues().add("cacheOperationSources", new RuntimeBeanReference(sourceName));
        String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

        // Create the CacheAdvisor definition.
        RootBeanDefinition advisorDef = new RootBeanDefinition(BeanFactoryCacheOperationSourceAdvisor.class);
        advisorDef.setSource(eleSource);
        advisorDef.setEnableDependencyInjection(false);
        advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        advisorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
        advisorDef.getPropertyValues().add("adviceBeanName", interceptorName);
        if (element.hasAttribute("order")) {
          advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
        }
        parserContext.getRegistry().registerBeanDefinition(CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME, advisorDef);

        CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), eleSource);
        compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
        compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
        compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME));
        parserContext.registerComponent(compositeDef);
      }
    }

    /**
     * Registers a cache aspect.
     * <pre class="code">
     * &lt;bean id="cacheAspect" class="cn.taketoday.cache.aspectj.AnnotationCacheAspect" factory-method="aspectOf"&gt;
     *   &lt;property name="cacheManager" ref="cacheManager"/&gt;
     *   &lt;property name="keyGenerator" ref="keyGenerator"/&gt;
     * &lt;/bean&gt;
     * </pre>
     */
    private static void registerCacheAspect(Element element, ParserContext parserContext) {
      if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.CACHE_ASPECT_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClassName(CACHE_ASPECT_CLASS_NAME);
        def.setFactoryMethodName("aspectOf");
        def.setEnableDependencyInjection(false);
        parseCacheResolution(element, def, false);
        CacheNamespaceHandler.parseKeyGenerator(element, def);
        parserContext.registerBeanComponent(new BeanComponentDefinition(def, CacheManagementConfigUtils.CACHE_ASPECT_BEAN_NAME));
      }
    }
  }

  /**
   * Configure the necessary infrastructure to support the standard JSR-107 caching annotations.
   */
  private static class JCacheCachingConfigurer {

    private static void registerCacheAdvisor(Element element, ParserContext parserContext) {
      if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME)) {
        Object source = parserContext.extractSource(element);

        // Create the CacheOperationSource definition.
        BeanDefinition sourceDef = createJCacheOperationSourceBeanDefinition(element, source);
        String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

        // Create the CacheInterceptor definition.
        RootBeanDefinition interceptorDef =
                new RootBeanDefinition("cn.taketoday.cache.jcache.interceptor.JCacheInterceptor");
        interceptorDef.setSource(source);
        interceptorDef.setEnableDependencyInjection(false);
        interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        interceptorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
        parseErrorHandler(element, interceptorDef);
        String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

        // Create the CacheAdvisor definition.
        RootBeanDefinition advisorDef = new RootBeanDefinition(
                "cn.taketoday.cache.jcache.interceptor.BeanFactoryJCacheOperationSourceAdvisor");
        advisorDef.setSource(source);
        advisorDef.setEnableDependencyInjection(false);
        advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        advisorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
        advisorDef.getPropertyValues().add("adviceBeanName", interceptorName);
        if (element.hasAttribute("order")) {
          advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
        }
        parserContext.getRegistry().registerBeanDefinition(CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME, advisorDef);

        CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), source);
        compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
        compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
        compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME));
        parserContext.registerComponent(compositeDef);
      }
    }

    private static void registerCacheAspect(Element element, ParserContext parserContext) {
      if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME)) {
        Object source = parserContext.extractSource(element);

        BeanDefinition cacheOperationSourceDef = createJCacheOperationSourceBeanDefinition(element, source);
        String cacheOperationSourceName = parserContext.getReaderContext().registerWithGeneratedName(cacheOperationSourceDef);

        RootBeanDefinition jcacheAspectDef = new RootBeanDefinition();
        jcacheAspectDef.setBeanClassName(JCACHE_ASPECT_CLASS_NAME);
        jcacheAspectDef.setFactoryMethodName("aspectOf");
        jcacheAspectDef.setEnableDependencyInjection(false);
        jcacheAspectDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(cacheOperationSourceName));
        parserContext.getRegistry().registerBeanDefinition(CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME, jcacheAspectDef);

        CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), source);
        compositeDef.addNestedComponent(new BeanComponentDefinition(cacheOperationSourceDef, cacheOperationSourceName));
        compositeDef.addNestedComponent(new BeanComponentDefinition(jcacheAspectDef, CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME));
        parserContext.registerComponent(compositeDef);
      }
    }

    private static RootBeanDefinition createJCacheOperationSourceBeanDefinition(Element element, @Nullable Object eleSource) {
      RootBeanDefinition sourceDef =
              new RootBeanDefinition("cn.taketoday.cache.jcache.interceptor.DefaultJCacheOperationSource");
      sourceDef.setSource(eleSource);
      sourceDef.setEnableDependencyInjection(false);
      sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
      // JSR-107 support should create an exception cache resolver with the cache manager
      // and there is no way to set that exception cache resolver from the namespace
      parseCacheResolution(element, sourceDef, true);
      CacheNamespaceHandler.parseKeyGenerator(element, sourceDef);
      return sourceDef;
    }
  }

}
