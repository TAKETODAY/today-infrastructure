/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import static cn.taketoday.context.utils.ObjectUtils.toArrayObject;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Scope;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY <br>
 *         2019-07-13 01:11
 */
public class BeanParameterResolver implements OrderedParameterResolver {

    @Override
    public boolean supports(MethodParameter parameter) {
        return true;
    }

    @Override
    public Object resolveParameter(final RequestContext context, final MethodParameter parameter) throws Throwable {

        final Class<?> parameterClass = parameter.getParameterClass();

        final Object bean = ClassUtils.newInstance(parameterClass);

        final Enumeration<String> parameterNames = context.parameterNames();

        while (parameterNames.hasMoreElements()) {
            // 遍历参数
            final String parameterName = parameterNames.nextElement();
            // 寻找参数
            try {
                resolvePojoParameter(context, parameterName, bean, parameterClass.getDeclaredField(parameterName));
            }
            catch (NoSuchFieldException e) {}
        }

        return bean;
    }

    protected void resolvePojoParameter(RequestContext request,
                                        String parameterName, Object bean, Field field) throws Throwable {

        final Class<?> type = field.getType();
        if (type.isArray()) {
            ClassUtils.makeAccessible(field)
                    .set(bean, toArrayObject(request.parameters(parameterName), type));
        }
        else {
            final String parameter = request.parameter(parameterName);
            if (parameter != null) {
                ClassUtils.makeAccessible(field)
                        .set(bean, ConvertUtils.convert(parameter, type));
            }
        }
    }

    Scope scope;
    ApplicationContext ctx;
    Map<MethodParameter, BeanDefinition> defs;

    public Object resolve(final RequestContext context, final MethodParameter parameter) {
        return ctx.getScopeBean(getBeanDefinition(parameter), scope);
    }

    protected BeanDefinition getBeanDefinition(final MethodParameter parameter) {
        BeanDefinition def;
        if ((def = defs.get(parameter)) == null) {
            def = createBeanDefinition(parameter);
            defs.put(parameter, def);
        }
        return def;
    }

    protected BeanDefinition createBeanDefinition(final MethodParameter parameter) {
        final Class<?> parameterClass = parameter.getParameterClass();
        final BeanDefinition ret = ContextUtils.createBeanDefinition(parameter.getName(), parameterClass, ctx);
        final Collection<Field> fields = ClassUtils.getFields(parameterClass);
        for (Field field : fields) {
            
        }
        return ret;
    }

    static class RequestPropertyValue extends PropertyValue {
        
        private RequestContext context;

        public RequestPropertyValue(Object value, Field field) {
            super(value, field);
        }

        @Override
        public Object getValue() {
            return super.getValue();
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 100;
    }
}
