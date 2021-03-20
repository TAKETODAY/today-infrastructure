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

import java.util.Map;
import java.util.Set;

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.exception.NoSuchPropertyException;
import cn.taketoday.context.factory.BeanMetadata;
import cn.taketoday.context.factory.BeanPropertyAccessor;
import cn.taketoday.context.factory.InvalidPropertyValueException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;

/**
 * Resolve Bean
 *
 * @author TODAY <br>
 * 2019-07-13 01:11
 */
public class BeanParameterResolver
        extends OrderedSupport implements ParameterResolver {

  public BeanParameterResolver() {
    this(LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 100);
  }

  public BeanParameterResolver(final int order) {
    super(order);
  }

  @Override
  public boolean supports(MethodParameter parameter) {
    return !ClassUtils.isSimpleType(parameter.getParameterClass());
  }

  /**
   * @return Pojo parameter
   */
  @Override
  public Object resolveParameter(final RequestContext context, final MethodParameter parameter) {
    final Class<?> parameterClass = parameter.getParameterClass();

    BeanMetadata metadata = BeanMetadata.ofClass(parameterClass);
    final Object bean = metadata.newInstance(); // native-invoke constructor
    final BeanPropertyAccessor propertyAccessor = new BeanPropertyAccessor(metadata, bean);

    final Map<String, String[]> parameters = context.parameters();
    if (parameters != null) {
      final Set<Map.Entry<String, String[]>> entries = parameters.entrySet();
      if (!CollectionUtils.isEmpty(entries)) {
        // 遍历参数
        for (final Map.Entry<String, String[]> entry : entries) {
          final String[] value = entry.getValue();
          if (ObjectUtils.isNotEmpty(value)) {
            Object property = value[0];
            final String propertyPath = entry.getKey();
            try {
              propertyAccessor.setProperty(bean, metadata, propertyPath, property);
            }
            catch (NoSuchPropertyException ignored) { }
            catch (InvalidPropertyValueException e) {
              throw new MethodParameterException(parameter, e);
            }
          }
        }
      }
    }

    return bean;
  }

}
