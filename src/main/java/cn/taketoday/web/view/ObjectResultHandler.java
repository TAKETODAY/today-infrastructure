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
package cn.taketoday.web.view;

import cn.taketoday.web.MessageConverter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.view.template.TemplateViewResolver;

/**
 * @author TODAY <br>
 *         2019-07-14 17:41
 */
public class ObjectResultHandler extends HandlerMethodResultHandler {

    public ObjectResultHandler(
            TemplateViewResolver viewResolver,
            MessageConverter messageConverter, int downloadFileBuf) {

        setMessageConverter(messageConverter);
        setTemplateViewResolver(viewResolver);
        setDownloadFileBufferSize(downloadFileBuf);
    }

    @Override
    public boolean supports(HandlerMethod handlerMethod) {
        return handlerMethod.is(Object.class);
    }

    @Override
    public boolean supportsResult(Object result) {
        return true;
    }

    @Override
    public void handleResult(RequestContext requestContext, Object result) throws Throwable {
        handleObject(requestContext, result);
    }

}
