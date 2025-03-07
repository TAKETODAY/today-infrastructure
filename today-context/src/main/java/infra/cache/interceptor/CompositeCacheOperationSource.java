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

package infra.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Composite {@link CacheOperationSource} implementation that iterates
 * over a given array of {@code CacheOperationSource} instances.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class CompositeCacheOperationSource implements CacheOperationSource, Serializable {

  private final CacheOperationSource[] cacheOperationSources;

  /**
   * Create a new CompositeCacheOperationSource for the given sources.
   *
   * @param cacheOperationSources the CacheOperationSource instances to combine
   */
  public CompositeCacheOperationSource(CacheOperationSource... cacheOperationSources) {
    Assert.notEmpty(cacheOperationSources, "CacheOperationSource array must not be empty");
    this.cacheOperationSources = cacheOperationSources;
  }

  /**
   * Return the {@code CacheOperationSource} instances that this
   * {@code CompositeCacheOperationSource} combines.
   */
  public final CacheOperationSource[] getCacheOperationSources() {
    return this.cacheOperationSources;
  }

  @Override
  public boolean isCandidateClass(Class<?> targetClass) {
    for (CacheOperationSource source : this.cacheOperationSources) {
      if (source.isCandidateClass(targetClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasCacheOperations(Method method, @Nullable Class<?> targetClass) {
    for (CacheOperationSource source : this.cacheOperationSources) {
      if (source.hasCacheOperations(method, targetClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Nullable
  public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
    Collection<CacheOperation> ops = null;
    for (CacheOperationSource source : this.cacheOperationSources) {
      Collection<CacheOperation> cacheOperations = source.getCacheOperations(method, targetClass);
      if (cacheOperations != null) {
        if (ops == null) {
          ops = new ArrayList<>();
        }
        ops.addAll(cacheOperations);
      }
    }
    return ops;
  }

}
