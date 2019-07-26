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

import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-07-13 11:21
 */
public abstract class TypeConverterParameterResolver implements ParameterResolver {

    @Override
    public abstract boolean supports(MethodParameter parameter);

    @Override
    public final Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
        Object source = resolveSource(requestContext, parameter);
        if (source == null) {
            if (parameter.isRequired()) {
                throw WebUtils.newBadRequest("Cookie", parameter, null);
            }
            source = parameter.getDefaultValue();
        }
        return ConvertUtils.convert(source, resolveTargetClass(parameter));
    }

    protected Object resolveSource(final RequestContext requestContext, final MethodParameter parameter) {
        return requestContext.parameter(parameter.getName());
    }

    protected Class<?> resolveTargetClass(final MethodParameter parameter) {
        return parameter.getParameterClass();
    }

}
