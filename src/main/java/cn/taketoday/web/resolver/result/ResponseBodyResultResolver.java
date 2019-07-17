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
package cn.taketoday.web.resolver.result;

import cn.taketoday.web.MessageConverter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.HandlerMethod;

/**
 * @author TODAY <br>
 *         2019-07-14 01:19
 */
public class ResponseBodyResultResolver implements OrderedResultResolver {

    private final MessageConverter messageConverter;

    public ResponseBodyResultResolver(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    @Override
    public boolean supports(HandlerMethod handlerMethod) {
        return true;
    }

    @Override
    public void resolveResult(RequestContext requestContext, Object result) throws Throwable {
        messageConverter.write(requestContext, result);
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 100;
    }

}
