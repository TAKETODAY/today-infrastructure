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

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;

import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Framework's {@link KeyGenerator} implementation that either delegates to a standard JSR-107
 * {@link javax.cache.annotation.CacheKeyGenerator}, or wrap a standard {@link KeyGenerator}
 * so that only relevant parameters are handled.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.0
 */
class KeyGeneratorAdapter implements KeyGenerator {

  private final JCacheOperationSource cacheOperationSource;

  @Nullable
  private KeyGenerator keyGenerator;

  @Nullable
  private CacheKeyGenerator cacheKeyGenerator;

  /**
   * Create an instance with the given {@link KeyGenerator} so that {@link javax.cache.annotation.CacheKey}
   * and {@link javax.cache.annotation.CacheValue} are handled according to the spec.
   */
  public KeyGeneratorAdapter(JCacheOperationSource cacheOperationSource, KeyGenerator target) {
    Assert.notNull(cacheOperationSource, "JCacheOperationSource is required");
    Assert.notNull(target, "KeyGenerator is required");
    this.cacheOperationSource = cacheOperationSource;
    this.keyGenerator = target;
  }

  /**
   * Create an instance used to wrap the specified {@link javax.cache.annotation.CacheKeyGenerator}.
   */
  public KeyGeneratorAdapter(JCacheOperationSource cacheOperationSource, CacheKeyGenerator target) {
    Assert.notNull(cacheOperationSource, "JCacheOperationSource is required");
    Assert.notNull(target, "CacheKeyGenerator is required");
    this.cacheOperationSource = cacheOperationSource;
    this.cacheKeyGenerator = target;
  }

  /**
   * Return the target key generator to use in the form of either a {@link KeyGenerator}
   * or a {@link CacheKeyGenerator}.
   */
  public Object getTarget() {
    if (this.cacheKeyGenerator != null) {
      return this.cacheKeyGenerator;
    }
    Assert.state(this.keyGenerator != null, "No key generator");
    return this.keyGenerator;
  }

  @Override
  public Object generate(Object target, Method method, Object... params) {
    JCacheOperation<?> operation = this.cacheOperationSource.getCacheOperation(method, target.getClass());
    if (!(operation instanceof AbstractJCacheKeyOperation)) {
      throw new IllegalStateException("Invalid operation, should be a key-based operation " + operation);
    }
    CacheKeyInvocationContext<?> invocationContext = createCacheKeyInvocationContext(target, operation, params);

    if (this.cacheKeyGenerator != null) {
      return this.cacheKeyGenerator.generateCacheKey(invocationContext);
    }
    else {
      Assert.state(this.keyGenerator != null, "No key generator");
      return doGenerate(this.keyGenerator, invocationContext);
    }
  }

  private static Object doGenerate(KeyGenerator keyGenerator, CacheKeyInvocationContext<?> context) {
    ArrayList<Object> parameters = new ArrayList<>();
    for (CacheInvocationParameter param : context.getKeyParameters()) {
      Object value = param.getValue();
      if (param.getParameterPosition() == context.getAllParameters().length - 1 &&
              context.getMethod().isVarArgs()) {
        parameters.addAll(CollectionUtils.arrayToList(value));
      }
      else {
        parameters.add(value);
      }
    }
    return keyGenerator.generate(context.getTarget(), context.getMethod(), parameters.toArray());
  }

  @SuppressWarnings("unchecked")
  private CacheKeyInvocationContext<?> createCacheKeyInvocationContext(
          Object target, JCacheOperation<?> operation, Object[] params) {

    AbstractJCacheKeyOperation<Annotation> keyCacheOperation = (AbstractJCacheKeyOperation<Annotation>) operation;
    return new DefaultCacheKeyInvocationContext<>(keyCacheOperation, target, params);
  }

}
