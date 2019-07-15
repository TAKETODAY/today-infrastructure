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

import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-07-07 23:24
 */
@Singleton
public class ArrayParameterResolver implements OrderedParameterResolver {

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean supports(final MethodParameter parameter) {
        return parameter.isArray();
    }

    @Override
    public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {

        final String parameterName = parameter.getName();
        // parameter value[]
        String[] parameterValues = requestContext.parameters(parameterName);

        if (StringUtils.isArrayEmpty(parameterValues)) {
            parameterValues = StringUtils.split(requestContext.parameter(parameterName));
            if (StringUtils.isArrayEmpty(parameterValues)) {
                if (parameter.isRequired()) {
                    throw WebUtils.newBadRequest("Array", parameterName, null);
                }
                return null;
            }
        }
        return NumberUtils.toArrayObject(parameterValues, parameter.getParameterClass());
    }

}
