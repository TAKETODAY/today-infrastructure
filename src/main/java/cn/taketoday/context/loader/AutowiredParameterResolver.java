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
package cn.taketoday.context.loader;

import java.lang.reflect.Parameter;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.StringUtils;

import static cn.taketoday.context.utils.ContextUtils.loadProps;
import static cn.taketoday.context.utils.ContextUtils.resolveProps;

/**
 * Resolve {@link Autowired} on {@link Parameter}
 *
 * @author TODAY <br>
 * 2019-10-28 20:27
 */
public class AutowiredParameterResolver
    extends OrderedSupport implements ExecutableParameterResolver, Ordered {

    public AutowiredParameterResolver() {
        this(LOWEST_PRECEDENCE);
    }

    public AutowiredParameterResolver(int order) {
        super(order);
    }

    @Override
    public final Object resolve(Parameter parameter, BeanFactory beanFactory) {

        final Autowired autowired = parameter.getAnnotation(Autowired.class); // @Autowired on parameter

        Object bean = resolveBean(autowired != null ? autowired.value() : null, parameter.getType(), beanFactory);

        // @Props on a bean (pojo) which has already annotated @Autowired or not
        if (parameter.isAnnotationPresent(Props.class)) {
            bean = resolvePropsInternal(parameter, parameter.getAnnotation(Props.class), bean);
        }

        if (bean == null && (autowired == null || autowired.required())) { // if it is required

            LoggerFactory.getLogger(AutowiredParameterResolver.class)//
                .error("[{}] on executable: [{}] is required and there isn't a [{}] bean",
                       parameter, parameter.getDeclaringExecutable(), parameter.getType());

            throw new NoSuchBeanDefinitionException(parameter.getType());
        }

        return bean;
    }

    protected Object resolveBean(final String name, final Class<?> type, final BeanFactory beanFactory) {

        if (StringUtils.isNotEmpty(name)) {
            // use name and bean type to get bean
            return beanFactory.getBean(name, type);
        }
        return beanFactory.getBean(type);
    }

    protected Object resolvePropsInternal(final Parameter parameter, final Props props, final Object bean) {
        if (bean != null) {
            return resolveProps(props, bean, loadProps(props, System.getProperties()));
        }
        return resolveProps(props, parameter.getType(), loadProps(props, System.getProperties()));
    }

}
