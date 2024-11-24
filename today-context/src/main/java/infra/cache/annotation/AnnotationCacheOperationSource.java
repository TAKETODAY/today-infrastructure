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

package infra.cache.annotation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import infra.cache.interceptor.AbstractFallbackCacheOperationSource;
import infra.cache.interceptor.CacheOperation;
import infra.cache.interceptor.CacheOperationSource;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Implementation of the {@link CacheOperationSource
 * CacheOperationSource} interface for working with caching metadata in annotation format.
 *
 * <p>This class reads Framework's {@link Cacheable}, {@link CachePut} and {@link CacheEvict}
 * annotations and exposes corresponding caching operation definition to Framework's cache
 * infrastructure. This class may also serve as base class for a custom
 * {@code CacheOperationSource}.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AnnotationCacheOperationSource extends AbstractFallbackCacheOperationSource implements Serializable {

  private final boolean publicMethodsOnly;

  private final Set<CacheAnnotationParser> annotationParsers;

  /**
   * Create a default AnnotationCacheOperationSource, supporting public methods
   * that carry the {@code Cacheable} and {@code CacheEvict} annotations.
   */
  public AnnotationCacheOperationSource() {
    this(true);
  }

  /**
   * Create a default {@code AnnotationCacheOperationSource}, supporting public methods
   * that carry the {@code Cacheable} and {@code CacheEvict} annotations.
   *
   * @param publicMethodsOnly whether to support only annotated public methods
   * typically for use with proxy-based AOP), or protected/private methods as well
   * (typically used with AspectJ class weaving)
   */
  public AnnotationCacheOperationSource(boolean publicMethodsOnly) {
    this.publicMethodsOnly = publicMethodsOnly;
    this.annotationParsers = Collections.singleton(new DefaultCacheAnnotationParser());
  }

  /**
   * Create a custom AnnotationCacheOperationSource.
   *
   * @param annotationParser the CacheAnnotationParser to use
   */
  public AnnotationCacheOperationSource(CacheAnnotationParser annotationParser) {
    Assert.notNull(annotationParser, "CacheAnnotationParser is required");
    this.publicMethodsOnly = true;
    this.annotationParsers = Collections.singleton(annotationParser);
  }

  /**
   * Create a custom AnnotationCacheOperationSource.
   *
   * @param annotationParsers the CacheAnnotationParser to use
   */
  public AnnotationCacheOperationSource(CacheAnnotationParser... annotationParsers) {
    Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
    this.publicMethodsOnly = true;
    this.annotationParsers = Set.of(annotationParsers);
  }

  /**
   * Create a custom AnnotationCacheOperationSource.
   *
   * @param annotationParsers the CacheAnnotationParser to use
   */
  public AnnotationCacheOperationSource(Set<CacheAnnotationParser> annotationParsers) {
    this.publicMethodsOnly = true;
    Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
    this.annotationParsers = annotationParsers;
  }

  @Override
  public boolean isCandidateClass(Class<?> targetClass) {
    for (CacheAnnotationParser parser : this.annotationParsers) {
      if (parser.isCandidateClass(targetClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Nullable
  protected Collection<CacheOperation> findCacheOperations(Class<?> clazz) {
    return determineCacheOperations(parser -> parser.parseCacheAnnotations(clazz));
  }

  @Override
  @Nullable
  protected Collection<CacheOperation> findCacheOperations(Method method) {
    return determineCacheOperations(parser -> parser.parseCacheAnnotations(method));
  }

  /**
   * Determine the cache operation(s) for the given {@link CacheOperationProvider}.
   * <p>This implementation delegates to configured
   * {@link CacheAnnotationParser CacheAnnotationParsers}
   * for parsing known annotations into Framework's metadata attribute class.
   * <p>Can be overridden to support custom annotations that carry caching metadata.
   *
   * @param provider the cache operation provider to use
   * @return the configured caching operations, or {@code null} if none found
   */
  @Nullable
  protected Collection<CacheOperation> determineCacheOperations(CacheOperationProvider provider) {
    Collection<CacheOperation> ops = null;
    for (CacheAnnotationParser parser : this.annotationParsers) {
      Collection<CacheOperation> annOps = provider.getCacheOperations(parser);
      if (annOps != null) {
        if (ops == null) {
          ops = annOps;
        }
        else {
          ArrayList<CacheOperation> combined = new ArrayList<>(ops.size() + annOps.size());
          combined.addAll(ops);
          combined.addAll(annOps);
          ops = combined;
        }
      }
    }
    return ops;
  }

  /**
   * By default, only public methods can be made cacheable.
   */
  @Override
  protected boolean allowPublicMethodsOnly() {
    return this.publicMethodsOnly;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof AnnotationCacheOperationSource otherCos)) {
      return false;
    }
    return (this.annotationParsers.equals(otherCos.annotationParsers) &&
            this.publicMethodsOnly == otherCos.publicMethodsOnly);
  }

  @Override
  public int hashCode() {
    return this.annotationParsers.hashCode();
  }

  /**
   * Callback interface providing {@link CacheOperation} instance(s) based on
   * a given {@link CacheAnnotationParser}.
   */
  @FunctionalInterface
  protected interface CacheOperationProvider {

    /**
     * Return the {@link CacheOperation} instance(s) provided by the specified parser.
     *
     * @param parser the parser to use
     * @return the cache operations, or {@code null} if none found
     */
    @Nullable
    Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser);
  }

}
