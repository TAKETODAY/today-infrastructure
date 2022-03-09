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

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyInvocationContext;

import cn.taketoday.lang.Nullable;

/**
 * The default {@link CacheKeyInvocationContext} implementation.
 *
 * @param <A> the annotation type
 * @author Stephane Nicoll
 * @since 4.0
 */
class DefaultCacheKeyInvocationContext<A extends Annotation> extends DefaultCacheInvocationContext<A>
        implements CacheKeyInvocationContext<A> {

  private final CacheInvocationParameter[] keyParameters;

  @Nullable
  private final CacheInvocationParameter valueParameter;

  public DefaultCacheKeyInvocationContext(AbstractJCacheKeyOperation<A> operation, Object target, Object[] args) {
    super(operation, target, args);
    this.keyParameters = operation.getKeyParameters(args);
    if (operation instanceof CachePutOperation) {
      this.valueParameter = ((CachePutOperation) operation).getValueParameter(args);
    }
    else {
      this.valueParameter = null;
    }
  }

  @Override
  public CacheInvocationParameter[] getKeyParameters() {
    return this.keyParameters.clone();
  }

  @Override
  @Nullable
  public CacheInvocationParameter getValueParameter() {
    return this.valueParameter;
  }

}
