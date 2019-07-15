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

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.MethodParameter;

/**
 * @author TODAY <br>
 *         2019-07-07 23:24
 */
@FunctionalInterface
public interface ParameterResolver {

    /**
     * Whether the given parameter is supported by this resolver.
     */
    default boolean supports(final MethodParameter parameter) {
        return true;
    }

    /**
     * Resolve parameter
     * 
     * @param requestContext
     *            Current request Context
     * @param parameter
     *            parameter
     * @throws Throwable
     *             if any {@link Exception} occurred
     * @return method parameter instances
     */
    Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable;

    @FunctionalInterface
    public interface SupportsFunction {

        boolean supports(MethodParameter parameter);
    }

}
