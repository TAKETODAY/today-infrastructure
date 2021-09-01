/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.view.template.TemplateRenderer;

/**
 * @author TODAY <br>
 * 2019-07-14 17:41
 */
public class ObjectReturnValueHandler extends HandlerMethodReturnValueHandler {

  public ObjectReturnValueHandler(
          TemplateRenderer viewResolver,
          MessageConverter messageConverter, int downloadFileBuf) {

    setMessageConverter(messageConverter);
    setTemplateViewResolver(viewResolver);
    setDownloadFileBufferSize(downloadFileBuf);
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handlerMethod) {
    return handlerMethod.isReturn(Object.class);
  }

  @Override
  protected void handleInternal(RequestContext context, HandlerMethod handler, Object returnValue) throws Throwable {
    if (isResponseBody(handler)) {// @since 3.0.5 fix response body error (github #16)
      handleResponseBody(context, returnValue);
    }
    else {
      handleObject(context, returnValue);
    }
  }

  /**
   * determine this handler is write message to response body?
   *
   * @param handlerMethod
   *         target handler
   *
   * @since 3.0.3
   */
  private boolean isResponseBody(HandlerMethod handlerMethod) {
    if (handlerMethod.isMethodPresent(ResponseBody.class)) {
      return !handlerMethod.getMethodAnnotation(ResponseBody.class).value();
    }
    else if (handlerMethod.isDeclaringClassPresent(ResponseBody.class)) {
      return !handlerMethod.getDeclaringClassAnnotation(ResponseBody.class).value();
    }
    return true;
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return true;
  }

}
