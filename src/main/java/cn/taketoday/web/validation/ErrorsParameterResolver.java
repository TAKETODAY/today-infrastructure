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
package cn.taketoday.web.validation;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.resolver.method.OrderedParameterResolver;

/**
 * @author TODAY <br>
 *         2019-07-20 17:00
 */
@MissingBean(type = ErrorsParameterResolver.class)
public class ErrorsParameterResolver implements OrderedParameterResolver {

    private static final Errors EMPTY = new Errors() {

        @Override
        public boolean hasErrors() {
            return false;
        }

        @Override
        public int getErrorCount() {
            return 0;
        }

        @Override
        public Set<ObjectError> getAllErrors() {
            return Collections.emptySet();
        }
    };

    @Override
    public boolean supports(MethodParameter parameter) {
        return parameter.isAssignableFrom(Errors.class);
    }

    @Override
    public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
        final Object error = requestContext.attribute(Constant.VALIDATION_ERRORS);
        if (error == null) {
            return EMPTY;
        }
        return error;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
