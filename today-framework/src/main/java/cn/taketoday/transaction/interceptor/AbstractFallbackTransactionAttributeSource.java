/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.transaction.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.context.expression.EmbeddedValueResolverAware;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.MethodClassKey;

/**
 * Abstract implementation of {@link TransactionAttributeSource} that caches
 * attributes for methods and implements a fallback policy: 1. specific target
 * method; 2. target class; 3. declaring method; 4. declaring class/interface.
 *
 * <p>Defaults to using the target class's transaction attribute if none is
 * associated with the target method. Any transaction attribute associated with
 * the target method completely overrides a class transaction attribute.
 * If none found on the target class, the interface that the invoked method
 * has been called through (in case of a JDK proxy) will be checked.
 *
 * <p>This implementation caches attributes by method after they are first used.
 * If it is ever desirable to allow dynamic changing of transaction attributes
 * (which is very unlikely), caching could be made configurable. Caching is
 * desirable because of the cost of evaluating rollback rules.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractFallbackTransactionAttributeSource
        implements TransactionAttributeSource, EmbeddedValueResolverAware {
  private final static Logger logger = LoggerFactory.getLogger(AbstractFallbackTransactionAttributeSource.class);

  /**
   * Canonical value held in cache to indicate no transaction attribute was
   * found for this method, and we don't need to look again.
   */
  @SuppressWarnings("serial")
  private static final TransactionAttribute NULL_TRANSACTION_ATTRIBUTE = new DefaultTransactionAttribute() {
    @Override
    public String toString() {
      return "null";
    }
  };

  @Nullable
  private transient StringValueResolver embeddedValueResolver;

  /**
   * Cache of TransactionAttributes, keyed by method on a specific target class.
   * <p>As this base class is not marked Serializable, the cache will be recreated
   * after serialization - provided that the concrete subclass is Serializable.
   */
  private final Map<Object, TransactionAttribute> attributeCache = new ConcurrentHashMap<>(1024);

  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
  }

  /**
   * Determine the transaction attribute for this method invocation.
   * <p>Defaults to the class's transaction attribute if no method attribute is found.
   *
   * @param method the method for the current invocation (never {@code null})
   * @param targetClass the target class for this invocation (may be {@code null})
   * @return a TransactionAttribute for this method, or {@code null} if the method
   * is not transactional
   */
  @Override
  @Nullable
  public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
    if (method.getDeclaringClass() == Object.class) {
      return null;
    }

    // First, see if we have a cached value.
    Object cacheKey = getCacheKey(method, targetClass);
    TransactionAttribute cached = this.attributeCache.get(cacheKey);
    if (cached != null) {
      // Value will either be canonical value indicating there is no transaction attribute,
      // or an actual transaction attribute.
      if (cached == NULL_TRANSACTION_ATTRIBUTE) {
        return null;
      }
      else {
        return cached;
      }
    }
    else {
      // We need to work it out.
      TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
      // Put it in the cache.
      if (txAttr == null) {
        this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
      }
      else {
        String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
        if (txAttr instanceof DefaultTransactionAttribute dta) {
          dta.setDescriptor(methodIdentification);
          dta.resolveAttributeStrings(this.embeddedValueResolver);
        }
        if (logger.isTraceEnabled()) {
          logger.trace("Adding transactional method '{}' with attribute: {}", methodIdentification, txAttr);
        }
        this.attributeCache.put(cacheKey, txAttr);
      }
      return txAttr;
    }
  }

  /**
   * Determine a cache key for the given method and target class.
   * <p>Must not produce same key for overloaded methods.
   * Must produce same key for different instances of the same method.
   *
   * @param method the method (never {@code null})
   * @param targetClass the target class (may be {@code null})
   * @return the cache key (never {@code null})
   */
  protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
    return new MethodClassKey(method, targetClass);
  }

  /**
   * Same signature as {@link #getTransactionAttribute}, but doesn't cache the result.
   * {@link #getTransactionAttribute} is effectively a caching decorator for this method.
   * <p>this method can be overridden.
   *
   * @see #getTransactionAttribute
   */
  @Nullable
  protected TransactionAttribute computeTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
    // Don't allow non-public methods, as configured.
    if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
      return null;
    }

    // The method may be on an interface, but we need attributes from the target class.
    // If the target class is null, the method will be unchanged.
    Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

    // First try is the method in the target class.
    TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
    if (txAttr != null) {
      return txAttr;
    }

    // Second try is the transaction attribute on the target class.
    txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
    if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
      return txAttr;
    }

    if (specificMethod != method) {
      // Fallback is to look at the original method.
      txAttr = findTransactionAttribute(method);
      if (txAttr != null) {
        return txAttr;
      }
      // Last fallback is the class of the original method.
      txAttr = findTransactionAttribute(method.getDeclaringClass());
      if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
        return txAttr;
      }
    }

    return null;
  }

  /**
   * Subclasses need to implement this to return the transaction attribute for the
   * given class, if any.
   *
   * @param clazz the class to retrieve the attribute for
   * @return all transaction attribute associated with this class, or {@code null} if none
   */
  @Nullable
  protected abstract TransactionAttribute findTransactionAttribute(Class<?> clazz);

  /**
   * Subclasses need to implement this to return the transaction attribute for the
   * given method, if any.
   *
   * @param method the method to retrieve the attribute for
   * @return all transaction attribute associated with this method, or {@code null} if none
   */
  @Nullable
  protected abstract TransactionAttribute findTransactionAttribute(Method method);

  /**
   * Should only public methods be allowed to have transactional semantics?
   * <p>The default implementation returns {@code false}.
   */
  protected boolean allowPublicMethodsOnly() {
    return false;
  }

}
