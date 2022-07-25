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

package cn.taketoday.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;

import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Implementation of the {@link JCacheOperationSource} interface that reads
 * the JSR-107 {@link CacheResult}, {@link CachePut}, {@link CacheRemove} and
 * {@link CacheRemoveAll} annotations.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public abstract class AnnotationJCacheOperationSource extends AbstractFallbackJCacheOperationSource {

  @Override
  protected JCacheOperation<?> findCacheOperation(Method method, @Nullable Class<?> targetType) {
    CacheResult cacheResult = method.getAnnotation(CacheResult.class);
    CachePut cachePut = method.getAnnotation(CachePut.class);
    CacheRemove cacheRemove = method.getAnnotation(CacheRemove.class);
    CacheRemoveAll cacheRemoveAll = method.getAnnotation(CacheRemoveAll.class);

    int found = countNonNull(cacheResult, cachePut, cacheRemove, cacheRemoveAll);
    if (found == 0) {
      return null;
    }
    if (found > 1) {
      throw new IllegalStateException("More than one cache annotation found on '" + method + "'");
    }

    CacheDefaults defaults = getCacheDefaults(method, targetType);
    if (cacheResult != null) {
      return createCacheResultOperation(method, defaults, cacheResult);
    }
    else if (cachePut != null) {
      return createCachePutOperation(method, defaults, cachePut);
    }
    else if (cacheRemove != null) {
      return createCacheRemoveOperation(method, defaults, cacheRemove);
    }
    else {
      return createCacheRemoveAllOperation(method, defaults, cacheRemoveAll);
    }
  }

  @Nullable
  protected CacheDefaults getCacheDefaults(Method method, @Nullable Class<?> targetType) {
    CacheDefaults annotation = method.getDeclaringClass().getAnnotation(CacheDefaults.class);
    if (annotation != null) {
      return annotation;
    }
    return (targetType != null ? targetType.getAnnotation(CacheDefaults.class) : null);
  }

  protected CacheResultOperation createCacheResultOperation(Method method, @Nullable CacheDefaults defaults, CacheResult ann) {
    String cacheName = determineCacheName(method, defaults, ann.cacheName());
    CacheResolverFactory cacheResolverFactory =
            determineCacheResolverFactory(defaults, ann.cacheResolverFactory());
    KeyGenerator keyGenerator = determineKeyGenerator(defaults, ann.cacheKeyGenerator());

    CacheMethodDetails<CacheResult> methodDetails = createMethodDetails(method, ann, cacheName);

    CacheResolver cacheResolver = getCacheResolver(cacheResolverFactory, methodDetails);
    CacheResolver exceptionCacheResolver = null;
    final String exceptionCacheName = ann.exceptionCacheName();
    if (StringUtils.hasText(exceptionCacheName)) {
      exceptionCacheResolver = getExceptionCacheResolver(cacheResolverFactory, methodDetails);
    }

    return new CacheResultOperation(methodDetails, cacheResolver, keyGenerator, exceptionCacheResolver);
  }

  protected CachePutOperation createCachePutOperation(Method method, @Nullable CacheDefaults defaults, CachePut ann) {
    String cacheName = determineCacheName(method, defaults, ann.cacheName());
    CacheResolverFactory cacheResolverFactory =
            determineCacheResolverFactory(defaults, ann.cacheResolverFactory());
    KeyGenerator keyGenerator = determineKeyGenerator(defaults, ann.cacheKeyGenerator());

    CacheMethodDetails<CachePut> methodDetails = createMethodDetails(method, ann, cacheName);
    CacheResolver cacheResolver = getCacheResolver(cacheResolverFactory, methodDetails);
    return new CachePutOperation(methodDetails, cacheResolver, keyGenerator);
  }

  protected CacheRemoveOperation createCacheRemoveOperation(Method method, @Nullable CacheDefaults defaults, CacheRemove ann) {
    String cacheName = determineCacheName(method, defaults, ann.cacheName());
    CacheResolverFactory cacheResolverFactory =
            determineCacheResolverFactory(defaults, ann.cacheResolverFactory());
    KeyGenerator keyGenerator = determineKeyGenerator(defaults, ann.cacheKeyGenerator());

    CacheMethodDetails<CacheRemove> methodDetails = createMethodDetails(method, ann, cacheName);
    CacheResolver cacheResolver = getCacheResolver(cacheResolverFactory, methodDetails);
    return new CacheRemoveOperation(methodDetails, cacheResolver, keyGenerator);
  }

  protected CacheRemoveAllOperation createCacheRemoveAllOperation(Method method, @Nullable CacheDefaults defaults, CacheRemoveAll ann) {
    String cacheName = determineCacheName(method, defaults, ann.cacheName());
    CacheResolverFactory cacheResolverFactory =
            determineCacheResolverFactory(defaults, ann.cacheResolverFactory());

    CacheMethodDetails<CacheRemoveAll> methodDetails = createMethodDetails(method, ann, cacheName);
    CacheResolver cacheResolver = getCacheResolver(cacheResolverFactory, methodDetails);
    return new CacheRemoveAllOperation(methodDetails, cacheResolver);
  }

  private <A extends Annotation> CacheMethodDetails<A> createMethodDetails(Method method, A annotation, String cacheName) {
    return new DefaultCacheMethodDetails<>(method, annotation, cacheName);
  }

  protected CacheResolver getCacheResolver(
          @Nullable CacheResolverFactory factory, CacheMethodDetails<?> details) {

    if (factory != null) {
      javax.cache.annotation.CacheResolver cacheResolver = factory.getCacheResolver(details);
      return new CacheResolverAdapter(cacheResolver);
    }
    else {
      return getDefaultCacheResolver();
    }
  }

  protected CacheResolver getExceptionCacheResolver(
          @Nullable CacheResolverFactory factory, CacheMethodDetails<CacheResult> details) {

    if (factory != null) {
      javax.cache.annotation.CacheResolver cacheResolver = factory.getExceptionCacheResolver(details);
      return new CacheResolverAdapter(cacheResolver);
    }
    else {
      return getDefaultExceptionCacheResolver();
    }
  }

  @Nullable
  protected CacheResolverFactory determineCacheResolverFactory(
          @Nullable CacheDefaults defaults, Class<? extends CacheResolverFactory> candidate) {

    if (candidate != CacheResolverFactory.class) {
      return getBean(candidate);
    }
    else if (defaults != null && defaults.cacheResolverFactory() != CacheResolverFactory.class) {
      return getBean(defaults.cacheResolverFactory());
    }
    else {
      return null;
    }
  }

  protected KeyGenerator determineKeyGenerator(
          @Nullable CacheDefaults defaults, Class<? extends CacheKeyGenerator> candidate) {

    if (candidate != CacheKeyGenerator.class) {
      return new KeyGeneratorAdapter(this, getBean(candidate));
    }
    else if (defaults != null && CacheKeyGenerator.class != defaults.cacheKeyGenerator()) {
      return new KeyGeneratorAdapter(this, getBean(defaults.cacheKeyGenerator()));
    }
    else {
      return getDefaultKeyGenerator();
    }
  }

  protected String determineCacheName(Method method, @Nullable CacheDefaults defaults, String candidate) {
    if (StringUtils.hasText(candidate)) {
      return candidate;
    }
    if (defaults != null && StringUtils.hasText(defaults.cacheName())) {
      return defaults.cacheName();
    }
    return generateDefaultCacheName(method);
  }

  /**
   * Generate a default cache name for the specified {@link Method}.
   *
   * @param method the annotated method
   * @return the default cache name, according to JSR-107
   */
  protected String generateDefaultCacheName(Method method) {
    Class<?>[] parameterTypes = method.getParameterTypes();
    List<String> parameters = new ArrayList<>(parameterTypes.length);
    for (Class<?> parameterType : parameterTypes) {
      parameters.add(parameterType.getName());
    }

    return method.getDeclaringClass().getName()
            + '.' + method.getName()
            + '(' + StringUtils.collectionToCommaDelimitedString(parameters) + ')';
  }

  private int countNonNull(Object... instances) {
    int result = 0;
    for (Object instance : instances) {
      if (instance != null) {
        result += 1;
      }
    }
    return result;
  }

  /**
   * Locate or create an instance of the specified cache strategy {@code type}.
   *
   * @param type the type of the bean to manage
   * @return the required bean
   */
  protected abstract <T> T getBean(Class<T> type);

  /**
   * Return the default {@link CacheResolver} if none is set.
   */
  protected abstract CacheResolver getDefaultCacheResolver();

  /**
   * Return the default exception {@link CacheResolver} if none is set.
   */
  protected abstract CacheResolver getDefaultExceptionCacheResolver();

  /**
   * Return the default {@link KeyGenerator} if none is set.
   */
  protected abstract KeyGenerator getDefaultKeyGenerator();

}
