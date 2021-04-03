/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.resolver;

import java.util.Collection;
import java.util.Enumeration;

import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.utils.ParamList;

/**
 * @author TODAY <br>
 * 2019-07-09 22:49
 */
public class CollectionParameterResolver implements ParameterResolver {

  @Override
  public final boolean supports(final MethodParameter parameter) {
    return parameter.isCollection() && supportsInternal(parameter);
  }

  protected boolean supportsInternal(final MethodParameter parameter) {
    return true;
  }

  /**
   * Resolve {@link Collection} parameter.
   */
  @Override
  public Object resolveParameter(final RequestContext context, final MethodParameter parameter) throws Throwable {
    final Collection<?> collection = resolveCollection(context, parameter);
    if (parameter.is(collection.getClass())) {
      return collection;
    }

    final Collection<Object> ret = CollectionUtils.createCollection(parameter.getParameterClass(), collection.size());
    ret.addAll(collection);
    return ret;
  }

  protected Collection<?> resolveCollection(final RequestContext context, final MethodParameter parameter) throws Throwable {

    final String parameterName = parameter.getName();
    final ParamList<Object> list = new ParamList<>();
    final Class<?> clazz = (Class<?>) parameter.getGenerics()[0];
    final Enumeration<String> parameterNames = context.getParameterNames();// all request parameter name
    final String collectionParamRegexp = Constant.COLLECTION_PARAM_REGEXP;

    while (parameterNames.hasMoreElements()) {
      final String requestParameter = parameterNames.nextElement();

      if (requestParameter.startsWith(parameterName)) {// users[0].userName=TODAY&users[0].age=20
        final String[] split = requestParameter.split(collectionParamRegexp);// [users, 1,, userName]
        final int index = Integer.parseInt(split[1]);// get index
        //TODO index < 0
        Object newInstance = list.get(index);

        if (newInstance == null) {
          newInstance = clazz.getConstructor().newInstance();
        }

        // if (!resolvePojoParameter(request, requestParameter, //
        //      newInstance, clazz.getDeclaredField(split[3]))) {// 得到Field准备注入
        //      return list;
        // }
        list.set(index, newInstance);
      }
    }
    return list;
  }
}
