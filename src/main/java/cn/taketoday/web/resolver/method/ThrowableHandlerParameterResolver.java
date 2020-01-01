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

import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY <br>
 *         2019-07-17 22:41
 */
public class ThrowableHandlerParameterResolver implements OrderedParameterResolver {

    @Override
    public boolean supports(MethodParameter parameter) {
        return parameter.isAssignableFrom(Throwable.class);
    }

    @Override
    public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
        return requestContext.attribute(Constant.KEY_THROWABLE);
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 60;
    }

}
