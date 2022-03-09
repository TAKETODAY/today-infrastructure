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
import java.util.ArrayList;
import java.util.List;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;

import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;

/**
 * A base {@link JCacheOperation} that operates with a key.
 *
 * @param <A> the annotation type
 * @author Stephane Nicoll
 * @since 4.0
 */
abstract class AbstractJCacheKeyOperation<A extends Annotation> extends AbstractJCacheOperation<A> {

  private final KeyGenerator keyGenerator;

  private final List<CacheParameterDetail> keyParameterDetails;

  /**
   * Create a new instance.
   *
   * @param methodDetails the {@link CacheMethodDetails} related to the cached method
   * @param cacheResolver the cache resolver to resolve regular caches
   * @param keyGenerator the key generator to compute cache keys
   */
  protected AbstractJCacheKeyOperation(CacheMethodDetails<A> methodDetails,
          CacheResolver cacheResolver, KeyGenerator keyGenerator) {

    super(methodDetails, cacheResolver);
    this.keyGenerator = keyGenerator;
    this.keyParameterDetails = initializeKeyParameterDetails(this.allParameterDetails);
  }

  /**
   * Return the {@link KeyGenerator} to use to compute cache keys.
   */
  public KeyGenerator getKeyGenerator() {
    return this.keyGenerator;
  }

  /**
   * Return the {@link CacheInvocationParameter} for the parameters that are to be
   * used to compute the key.
   * <p>Per the spec, if some method parameters are annotated with
   * {@link javax.cache.annotation.CacheKey}, only those parameters should be part
   * of the key. If none are annotated, all parameters except the parameter annotated
   * with {@link javax.cache.annotation.CacheValue} should be part of the key.
   * <p>The method arguments must match the signature of the related method invocation
   *
   * @param values the parameters value for a particular invocation
   * @return the {@link CacheInvocationParameter} instances for the parameters to be
   * used to compute the key
   */
  public CacheInvocationParameter[] getKeyParameters(Object... values) {
    List<CacheInvocationParameter> result = new ArrayList<>();
    for (CacheParameterDetail keyParameterDetail : this.keyParameterDetails) {
      int parameterPosition = keyParameterDetail.getParameterPosition();
      if (parameterPosition >= values.length) {
        throw new IllegalStateException("Values mismatch, key parameter at position "
                + parameterPosition + " cannot be matched against " + values.length + " value(s)");
      }
      result.add(keyParameterDetail.toCacheInvocationParameter(values[parameterPosition]));
    }
    return result.toArray(new CacheInvocationParameter[0]);
  }

  private static List<CacheParameterDetail> initializeKeyParameterDetails(List<CacheParameterDetail> allParameters) {
    List<CacheParameterDetail> all = new ArrayList<>();
    List<CacheParameterDetail> annotated = new ArrayList<>();
    for (CacheParameterDetail allParameter : allParameters) {
      if (!allParameter.isValue()) {
        all.add(allParameter);
      }
      if (allParameter.isKey()) {
        annotated.add(allParameter);
      }
    }
    return (annotated.isEmpty() ? all : annotated);
  }

}
