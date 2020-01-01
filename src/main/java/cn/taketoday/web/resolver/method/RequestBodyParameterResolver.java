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

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.web.MessageConverter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY <br>
 *         2019-07-12 22:23
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestBodyParameterResolver implements ParameterResolver {

    private final MessageConverter messageConverter;

    @Autowired
    public RequestBodyParameterResolver(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    @Override
    public boolean supports(final MethodParameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object resolveParameter(final RequestContext requestContext, final MethodParameter parameter) throws Throwable {
        return messageConverter.read(requestContext, parameter);
    }

}
