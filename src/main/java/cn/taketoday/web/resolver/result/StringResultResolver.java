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

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.utils.ResultUtils;
import cn.taketoday.web.view.ViewResolver;

/**
 * @author TODAY <br>
 *         2019-07-14 11:32
 */
@Singleton
public class StringResultResolver implements ResultResolver {

    /** view resolver **/
    private final ViewResolver viewResolver;

    @Autowired
    public StringResultResolver(ViewResolver viewResolver) {
        this.viewResolver = viewResolver;
    }

    @Override
    public boolean supports(HandlerMethod handlerMethod) {

        if (handlerMethod.is(String.class)) {

            if (handlerMethod.isMethodPresent(ResponseBody.class)) {
                return !handlerMethod.getMethodAnnotation(ResponseBody.class).value();
            }
            else if (handlerMethod.isDeclaringClassPresent(ResponseBody.class)) {
                return !handlerMethod.getDeclaringClassAnnotation(ResponseBody.class).value();
            }
            return true;
        }
        return false;
    }

    @Override
    public void resolveResult(RequestContext requestContext, Object result) throws Throwable {
        ResultUtils.resolveView((String) result, viewResolver, requestContext);
    }

}
