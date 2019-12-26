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
package cn.taketoday.web.handler;

import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.ViewController;

/**
 * @author TODAY <br>
 *         2019-12-08 21:54
 */
public class ViewControllerHandlerAdapter extends AbstractHandlerAdapter {

    @Override
    public boolean supports(Object handler) {
        return handler instanceof ViewController;
    }

    @Override
    public Object handle(final RequestContext context, final Object handler) throws Throwable {

        final ViewController view = (ViewController) handler;

        applyHttpStatus(context, view);
        applyContentType(context, view);

        if (view.hasAction()) {
            return invokeHandlerMethod(context, view);
        }
        return view.getAssetsPath();
    }

    protected Object invokeHandlerMethod(final RequestContext context, final ViewController view) throws Throwable {
        final HandlerMethod handlerMethod = view.getHandlerMethod();
        final Object result = handlerMethod.invokeHandler(context);
        if (handlerMethod.is(void.class)) {
            return view.getAssetsPath();
        }
        handlerMethod.handleResult(context, result);
        return null;
    }

    protected void applyContentType(final RequestContext context, final ViewController mapping) {
        final String contentType = mapping.getContentType();
        if (StringUtils.isNotEmpty(contentType)) {
            context.contentType(contentType);
        }
    }

    protected void applyHttpStatus(final RequestContext context, final ViewController mapping) {

        if (mapping.getStatus() != null) {
            context.status(mapping.getStatus());
        }
    }

}
