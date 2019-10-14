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
package cn.taketoday.web.resolver.method;

import cn.taketoday.context.Ordered;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.MethodParameter;

/**
 * @author TODAY <br>
 *         2019-07-15 13:01
 */
public final class DelegatingParameterResolver implements ParameterResolver, Ordered {

    private final int order;
    private final SupportsFunction function;
    private final ParameterResolver resolver;

    public DelegatingParameterResolver(SupportsFunction function, ParameterResolver resolver) {
        this(function, resolver, LOWEST_PRECEDENCE);
    }

    public DelegatingParameterResolver(SupportsFunction function, ParameterResolver resolver, int order) {
        this.function = function;
        this.resolver = resolver;
        this.order = order;
    }

    @Override
    public boolean supports(MethodParameter parameter) {
        return function.supports(parameter);
    }

    @Override
    public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
        return resolver.resolveParameter(requestContext, parameter);
    }

    @Override
    public int getOrder() {
        return order;
    }

    public static DelegatingParameterResolver delegate(SupportsFunction supports, ParameterResolver resolver) {
        return new DelegatingParameterResolver(supports, resolver);
    }

    public static DelegatingParameterResolver delegate(SupportsFunction supports, ParameterResolver resolver, int order) {
        return new DelegatingParameterResolver(supports, resolver, order);
    }
}
