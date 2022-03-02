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
package cn.taketoday.web.bind.resolver;

import java.util.Collection;

import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author TODAY <br>
 * 2019-07-09 22:49
 */
public abstract class CollectionParameterResolver
        extends AbstractParameterResolver implements ParameterResolvingStrategy {

  @Override
  public final boolean supportsParameter(final ResolvableMethodParameter parameter) {
    return CollectionUtils.isCollection(parameter.getParameterType()) && supportsInternal(parameter);
  }

  protected boolean supportsInternal(final ResolvableMethodParameter resolvable) {
    return true;
  }

  /**
   * Resolve {@link Collection} parameter.
   */
  @Override
  protected Object resolveInternal(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    final Collection<?> collection = resolveCollection(context, resolvable);
    if (resolvable.is(collection.getClass())) {
      return collection;
    }

    final Collection<Object> ret = CollectionUtils.createCollection(resolvable.getParameterType(), collection.size());
    ret.addAll(collection);
    return ret;
  }

  protected abstract Collection<?> resolveCollection(
          final RequestContext context, final ResolvableMethodParameter parameter) throws Throwable;

}
