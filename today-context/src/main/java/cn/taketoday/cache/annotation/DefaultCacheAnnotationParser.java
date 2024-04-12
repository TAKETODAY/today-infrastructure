/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.cache.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import cn.taketoday.cache.interceptor.CacheEvictOperation;
import cn.taketoday.cache.interceptor.CacheOperation;
import cn.taketoday.cache.interceptor.CachePutOperation;
import cn.taketoday.cache.interceptor.CacheableOperation;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Strategy implementation for parsing Framework's {@link Caching}, {@link Cacheable},
 * {@link CacheEvict}, and {@link CachePut} annotations.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DefaultCacheAnnotationParser implements CacheAnnotationParser, Serializable {

  private static final Set<Class<? extends Annotation>> CACHE_OPERATION_ANNOTATIONS = Set.of(
          Cacheable.class, CacheEvict.class, CachePut.class, Caching.class
  );

  @Override
  public boolean isCandidateClass(Class<?> targetClass) {
    return AnnotationUtils.isCandidateClass(targetClass, CACHE_OPERATION_ANNOTATIONS);
  }

  @Override
  @Nullable
  public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
    DefaultCacheConfig defaultConfig = new DefaultCacheConfig(type);
    return parseCacheAnnotations(defaultConfig, type);
  }

  @Override
  @Nullable
  public Collection<CacheOperation> parseCacheAnnotations(Method method) {
    DefaultCacheConfig defaultConfig = new DefaultCacheConfig(method.getDeclaringClass());
    return parseCacheAnnotations(defaultConfig, method);
  }

  @Nullable
  private Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement ae) {
    Collection<CacheOperation> ops = parseCacheAnnotations(cachingConfig, ae, false);
    if (ops != null && ops.size() > 1) {
      // More than one operation found -> local declarations override interface-declared ones...
      Collection<CacheOperation> localOps = parseCacheAnnotations(cachingConfig, ae, true);
      if (localOps != null) {
        return localOps;
      }
    }
    return ops;
  }

  @Nullable
  private Collection<CacheOperation> parseCacheAnnotations(
          DefaultCacheConfig cachingConfig, AnnotatedElement ae, boolean localOnly) {

    Collection<? extends Annotation> anns =
            localOnly ? AnnotatedElementUtils.getAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS)
                    : AnnotatedElementUtils.findAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS);
    if (anns.isEmpty()) {
      return null;
    }

    ArrayList<CacheOperation> ops = new ArrayList<>(1);
    anns.stream()
            .filter(ann -> ann instanceof Cacheable)
            .forEach(ann -> ops.add(parseCacheableAnnotation(ae, cachingConfig, (Cacheable) ann)));
    anns.stream()
            .filter(ann -> ann instanceof CacheEvict)
            .forEach(ann -> ops.add(parseEvictAnnotation(ae, cachingConfig, (CacheEvict) ann)));
    anns.stream()
            .filter(ann -> ann instanceof CachePut)
            .forEach(ann -> ops.add(parsePutAnnotation(ae, cachingConfig, (CachePut) ann)));
    anns.stream()
            .filter(ann -> ann instanceof Caching)
            .forEach(ann -> parseCachingAnnotation(ae, cachingConfig, (Caching) ann, ops));
    return ops;
  }

  private CacheableOperation parseCacheableAnnotation(
          AnnotatedElement ae, DefaultCacheConfig defaultConfig, Cacheable cacheable) {

    CacheableOperation.Builder builder = new CacheableOperation.Builder();

    builder.setName(ae.toString());
    builder.setCacheNames(cacheable.cacheNames());
    builder.setCondition(cacheable.condition());
    builder.setUnless(cacheable.unless());
    builder.setKey(cacheable.key());
    builder.setKeyGenerator(cacheable.keyGenerator());
    builder.setCacheManager(cacheable.cacheManager());
    builder.setCacheResolver(cacheable.cacheResolver());
    builder.setSync(cacheable.sync());

    defaultConfig.applyDefault(builder);
    CacheableOperation op = builder.build();
    validateCacheOperation(ae, op);

    return op;
  }

  private CacheEvictOperation parseEvictAnnotation(
          AnnotatedElement ae, DefaultCacheConfig defaultConfig, CacheEvict cacheEvict) {

    CacheEvictOperation.Builder builder = new CacheEvictOperation.Builder();

    builder.setName(ae.toString());
    builder.setCacheNames(cacheEvict.cacheNames());
    builder.setCondition(cacheEvict.condition());
    builder.setKey(cacheEvict.key());
    builder.setKeyGenerator(cacheEvict.keyGenerator());
    builder.setCacheManager(cacheEvict.cacheManager());
    builder.setCacheResolver(cacheEvict.cacheResolver());
    builder.setCacheWide(cacheEvict.allEntries());
    builder.setBeforeInvocation(cacheEvict.beforeInvocation());

    defaultConfig.applyDefault(builder);
    CacheEvictOperation op = builder.build();
    validateCacheOperation(ae, op);

    return op;
  }

  private CacheOperation parsePutAnnotation(
          AnnotatedElement ae, DefaultCacheConfig defaultConfig, CachePut cachePut) {

    CachePutOperation.Builder builder = new CachePutOperation.Builder();

    builder.setName(ae.toString());
    builder.setCacheNames(cachePut.cacheNames());
    builder.setCondition(cachePut.condition());
    builder.setUnless(cachePut.unless());
    builder.setKey(cachePut.key());
    builder.setKeyGenerator(cachePut.keyGenerator());
    builder.setCacheManager(cachePut.cacheManager());
    builder.setCacheResolver(cachePut.cacheResolver());

    defaultConfig.applyDefault(builder);
    CachePutOperation op = builder.build();
    validateCacheOperation(ae, op);

    return op;
  }

  private void parseCachingAnnotation(
          AnnotatedElement ae, DefaultCacheConfig defaultConfig, Caching caching, Collection<CacheOperation> ops) {

    Cacheable[] cacheables = caching.cacheable();
    for (Cacheable cacheable : cacheables) {
      ops.add(parseCacheableAnnotation(ae, defaultConfig, cacheable));
    }
    CacheEvict[] cacheEvicts = caching.evict();
    for (CacheEvict cacheEvict : cacheEvicts) {
      ops.add(parseEvictAnnotation(ae, defaultConfig, cacheEvict));
    }
    CachePut[] cachePuts = caching.put();
    for (CachePut cachePut : cachePuts) {
      ops.add(parsePutAnnotation(ae, defaultConfig, cachePut));
    }
  }

  /**
   * Validates the specified {@link CacheOperation}.
   * <p>Throws an {@link IllegalStateException} if the state of the operation is
   * invalid. As there might be multiple sources for default values, this ensure
   * that the operation is in a proper state before being returned.
   *
   * @param ae the annotated element of the cache operation
   * @param operation the {@link CacheOperation} to validate
   */
  private void validateCacheOperation(AnnotatedElement ae, CacheOperation operation) {
    if (StringUtils.hasText(operation.getKey()) && StringUtils.hasText(operation.getKeyGenerator())) {
      throw new IllegalStateException("Invalid cache annotation configuration on '" +
              ae + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
              "These attributes are mutually exclusive: either set the EL expression used to" +
              "compute the key at runtime or set the name of the KeyGenerator bean to use.");
    }
    if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
      throw new IllegalStateException("Invalid cache annotation configuration on '" +
              ae + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
              "These attributes are mutually exclusive: the cache manager is used to configure a" +
              "default cache resolver if none is set. If a cache resolver is set, the cache manager" +
              "won't be used.");
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof DefaultCacheAnnotationParser);
  }

  @Override
  public int hashCode() {
    return DefaultCacheAnnotationParser.class.hashCode();
  }

  /**
   * Provides default settings for a given set of cache operations.
   */
  private static class DefaultCacheConfig {

    private final Class<?> target;

    @Nullable
    private String[] cacheNames;

    @Nullable
    private String keyGenerator;

    @Nullable
    private String cacheManager;

    @Nullable
    private String cacheResolver;

    private boolean initialized = false;

    public DefaultCacheConfig(Class<?> target) {
      this.target = target;
    }

    /**
     * Apply the defaults to the specified {@link CacheOperation.Builder}.
     *
     * @param builder the operation builder to update
     */
    public void applyDefault(CacheOperation.Builder builder) {
      if (!this.initialized) {
        CacheConfig annotation = AnnotatedElementUtils.findMergedAnnotation(this.target, CacheConfig.class);
        if (annotation != null) {
          this.cacheNames = annotation.cacheNames();
          this.keyGenerator = annotation.keyGenerator();
          this.cacheManager = annotation.cacheManager();
          this.cacheResolver = annotation.cacheResolver();
        }
        this.initialized = true;
      }

      if (builder.getCacheNames().isEmpty() && this.cacheNames != null) {
        builder.setCacheNames(this.cacheNames);
      }
      if (StringUtils.isBlank(builder.getKey()) && StringUtils.isBlank(builder.getKeyGenerator()) &&
              StringUtils.hasText(this.keyGenerator)) {
        builder.setKeyGenerator(this.keyGenerator);
      }

      if (StringUtils.hasText(builder.getCacheManager()) || StringUtils.hasText(builder.getCacheResolver())) {
        // One of these is set so we should not inherit anything
      }
      else if (StringUtils.hasText(this.cacheResolver)) {
        builder.setCacheResolver(this.cacheResolver);
      }
      else if (StringUtils.hasText(this.cacheManager)) {
        builder.setCacheManager(this.cacheManager);
      }
    }
  }

}
