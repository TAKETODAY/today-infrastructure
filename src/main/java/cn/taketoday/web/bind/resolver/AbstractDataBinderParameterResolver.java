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

import java.lang.reflect.Type;
import java.util.Map;

import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.BeanPropertyAccessor;
import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author TODAY 2021/4/8 17:33
 * @see <a href='https://taketoday.cn/articles/1616819014712'>TODAY Context 之 BeanPropertyAccessor</a>
 * @since 3.0
 */
public abstract class AbstractDataBinderParameterResolver extends AbstractNamedValueResolvingStrategy {

  public final boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return !resolvable.hasParameterAnnotation(RequestBody.class) && supportsInternal(resolvable);
  }

  /**
   * @since 3.0.3 fix request body
   */
  protected abstract boolean supportsInternal(ResolvableMethodParameter resolvable);

  @Nullable
  @Override
  protected Object resolveName(
          String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {

    final int parameterNameLength = name.length();
    // prepare property values
    final Map<String, String[]> parameters = context.getParameters();

    final DefaultMultiValueMap<String, PropertyValue> propertyValues = new DefaultMultiValueMap<>();
    for (final Map.Entry<String, String[]> entry : parameters.entrySet()) {
      final String[] paramValues = entry.getValue();
      if (ObjectUtils.isNotEmpty(paramValues)) {
        final String requestParameterName = entry.getKey();
        // users[key].userName=TODAY&users[key].age=20
        if (requestParameterName.startsWith(name)
                && requestParameterName.charAt(parameterNameLength) == '[') {
          // userList[0].name  '.' 's index
          final int separatorIndex = BeanPropertyAccessor.getNestedPropertySeparatorIndex(requestParameterName);
          final String property = requestParameterName.substring(separatorIndex + 1);
          final int closeKey = requestParameterName.indexOf(']');
          final String key = requestParameterName.substring(parameterNameLength + 1, closeKey);

          final PropertyValue propertyValue = new PropertyValue(property, paramValues[0]);

          propertyValues.add(key, propertyValue);
        }
      }
    }

    return doBind(propertyValues, resolvable);
  }

  /**
   * Bind {@code propertyValues} to object
   */
  protected abstract Object doBind(
          MultiValueMap<String, PropertyValue> propertyValues, ResolvableMethodParameter parameter);

  protected boolean supportsSetProperties(final Type valueType) {
    return !ClassUtils.primitiveTypes.contains(valueType);
  }

}
