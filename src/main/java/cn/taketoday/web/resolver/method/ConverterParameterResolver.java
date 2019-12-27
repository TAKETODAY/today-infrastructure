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

import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-07-13 12:58
 */
public class ConverterParameterResolver implements OrderedParameterResolver {

    private final int order;
    private final SupportsFunction supports;
    private final Converter<String, Object> converter;

    public ConverterParameterResolver(SupportsFunction supports, Converter<String, Object> converter) {
        this(supports, converter, LOWEST_PRECEDENCE);
    }

    public ConverterParameterResolver(SupportsFunction supports, Converter<String, Object> converter, int order) {
        this.order = order;
        this.supports = supports;
        this.converter = converter;
    }

    @Override
    public boolean supports(MethodParameter parameter) {
        return supports.supports(parameter);
    }

    @Override
    public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {

        final String value = requestContext.parameter(parameter.getName());
        if (StringUtils.isEmpty(value)) {
            if (parameter.isRequired()) {
                throw WebUtils.newBadRequest(null, parameter, null);
            }
            return converter.convert(parameter.getDefaultValue());
        }
        return converter.convert(value);
    }

    @Override
    public int getOrder() {
        return order;
    }

    public static ConverterParameterResolver convert(SupportsFunction supports,
                                                     Converter<String, Object> converter) {
        return new ConverterParameterResolver(supports, converter);
    }

    public static ConverterParameterResolver convert(SupportsFunction supports,
                                                     Converter<String, Object> converter, int order) {
        return new ConverterParameterResolver(supports, converter, order);
    }

}
