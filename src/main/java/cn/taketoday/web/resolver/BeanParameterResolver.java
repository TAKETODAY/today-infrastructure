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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.reflect.BeanConstructor;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.ReflectionUtils;
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

  private final Map<Class<?>, BeanParameter> map = new HashMap<>();

  public BeanParameterResolver() {
    this(LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 100);
  }

  public BeanParameterResolver(final int order) {
    super(order);
  }

  @Override
  public boolean supports(MethodParameter parameter) {
    return true;
  }

  public Object resolve(final RequestContext context, final MethodParameter parameter) throws Throwable {
    final Class<?> parameterClass = parameter.getParameterClass();
    final BeanParameter beanParameter = map.get(parameterClass);
    final Object bean = beanParameter.newInstance();

    final Map<String, String[]> parameters = context.parameters();
    if (parameters != null) {
      final Set<Map.Entry<String, String[]>> entries = parameters.entrySet();
      if (!CollectionUtils.isEmpty(entries)) {
        // 遍历参数
        for (final Map.Entry<String, String[]> entry : entries) {
          final String[] value = entry.getValue();
          if (ObjectUtils.isNotEmpty(value)) {
            final Field field = ReflectionUtils.findField(parameterClass, entry.getKey());
            if (field != null) {
              applyParameter(field, bean, value);
            }
          }
        }
      }
    }

    return bean;
  }

  @Override
  public Object resolveParameter(final RequestContext context, final MethodParameter parameter) throws Throwable {
    final Class<?> parameterClass = parameter.getParameterClass();
    final Object bean = ClassUtils.newInstance(parameterClass);

    final Map<String, String[]> parameters = context.parameters();
    if (parameters != null) {
      final Set<Map.Entry<String, String[]>> entries = parameters.entrySet();
      if (!CollectionUtils.isEmpty(entries)) {
        // 遍历参数
        for (final Map.Entry<String, String[]> entry : entries) {
          final String[] value = entry.getValue();
          if (ObjectUtils.isNotEmpty(value)) {
            final Field field = ReflectionUtils.findField(parameterClass, entry.getKey());
            if (field != null) {
              applyParameter(field, bean, value);
            }
          }
        }
      }
    }

    return bean;
  }

  protected void applyParameter(final Field field, final Object bean, final String[] value) {
    final Class<?> type = field.getType();
    if (type.isArray()) {
      ReflectionUtils.makeAccessible(field);
      ReflectionUtils.setField(field, bean, ObjectUtils.toArrayObject(value, type));
    }
    else {
      final String parameter = value[0];
      if (parameter != null) {
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, bean, ConvertUtils.convert(parameter, type));
      }
    }
  }

  static class BeanParameter {
    MethodParameter parameter;
    BeanConstructor<?> constructor;

    Object newInstance() {
      if (constructor == null) {
        constructor = ReflectionUtils.newConstructor(parameter.getParameterClass());
      }
      return constructor.newInstance();
    }

  }

  // --

//  Scope scope;
//  ApplicationContext ctx;
//  BeanDefinitionLoader beanDefinitionLoader;
//  Map<MethodParameter, BeanDefinition> defs;
//
//  public Object resolve(final RequestContext context, final MethodParameter parameter) {
//    return ctx.getScopeBean(getBeanDefinition(parameter), scope);
//  }
//
//  protected BeanDefinition getBeanDefinition(final MethodParameter parameter) {
//    BeanDefinition def;
//    if ((def = defs.get(parameter)) == null) {
//      def = createBeanDefinition(parameter);
//      defs.put(parameter, def);
//    }
//    return def;
//  }
//
//  protected BeanDefinition createBeanDefinition(final MethodParameter parameter) {
//    final Class<?> parameterClass = parameter.getParameterClass();
//    final BeanDefinition ret = beanDefinitionLoader.createBeanDefinition(parameter.getName(), parameterClass);
//    final Collection<Field> fields = ClassUtils.getFields(parameterClass);
//    for (Field field : fields) {
//
//    }
//    return ret;
//  }
//
//  static class RequestPropertyValue extends PropertyValue {
//
//    private RequestContext context;
//
//    public RequestPropertyValue(Object value, Field field) {
//      super(value, field);
//    }
//
//    @Override
//    public Object getValue() {
//      return super.getValue();
//    }
//  }

}
