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
package cn.taketoday.web.resolver.method;

import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.utils.WebUtils;

/**
 * @author TODAY <br>
 *         2019-07-07 23:24
 */
public class ArrayParameterResolver implements OrderedParameterResolver {

    @Override
    public boolean supports(final MethodParameter parameter) {
        return parameter.isArray();
    }

    @Override
    public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {

        final String name = parameter.getName();
        // parameter value[]
        String[] values = requestContext.parameters(name);

        if (ObjectUtils.isEmpty(values)) {
            values = StringUtils.split(requestContext.parameter(name));
            if (ObjectUtils.isEmpty(values)) {
                if (parameter.isRequired()) {
                    throw WebUtils.newBadRequest("Array", name, null);
                }
                return null;
            }
        }
        return ObjectUtils.toArrayObject(values, parameter.getParameterClass());
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 70;
    }
}
