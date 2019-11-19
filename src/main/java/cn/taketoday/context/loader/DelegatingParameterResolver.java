/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
import cn.taketoday.context.factory.BeanFactory;

/**
 * @author TODAY <br>
 *         2019-10-28 20:27
 */
public final class DelegatingParameterResolver implements ExecutableParameterResolver, Ordered {

    private final int order;
    private final SupportsFunction supports;
    private final ExecutableParameterResolver resolver;

    public DelegatingParameterResolver(SupportsFunction supports, ExecutableParameterResolver resolver) {
        this(supports, resolver, Ordered.HIGHEST_PRECEDENCE);
    }

    public DelegatingParameterResolver(SupportsFunction supports, ExecutableParameterResolver resolver, int order) {
        this.order = order;
        this.resolver = resolver;
        this.supports = supports;
    }

    @Override
    public boolean supports(Parameter parameter) {
        return supports.supports(parameter);
    }

    @Override
    public Object resolve(Parameter parameter, BeanFactory beanFactory) {
        return resolver.resolve(parameter, beanFactory);
    }

    @Override
    public int getOrder() {
        return order;
    }

    public static DelegatingParameterResolver delegate(SupportsFunction supports, ExecutableParameterResolver resolver) {
        return new DelegatingParameterResolver(supports, resolver);
    }

    public static DelegatingParameterResolver delegate(SupportsFunction supports,
                                                       ExecutableParameterResolver resolver, int order) {
        return new DelegatingParameterResolver(supports, resolver, order);
    }

}
