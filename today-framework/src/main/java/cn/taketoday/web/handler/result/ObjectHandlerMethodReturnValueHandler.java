/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.web.handler.result;

import java.util.List;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.handler.SelectableReturnValueHandler;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;

/**
 * HandlerMethod return Object
 * <p>
 * Iterate handlers in runtime
 * </p>
 * <pre>
 * &#64GET("/object")
 * public Object object(boolean key1, boolean key2, boolean key3, RequestContext context) throws IOException {
 *   if (key1) {
 *     return new Body("key1", 1);
 *   }
 *   if (key2) {
 *     Resource resource = new ClassPathResource("error/404.png");
 *     context.setContentType(MediaType.IMAGE_JPEG_VALUE);
 *     return ImageIO.read(resource.getInputStream());
 *   }
 *   if (key3) {
 *     return ResourceUtils.getResource("classpath:application.yaml");
 *   }
 *   return "body:Hello";
 * }
 * </pre>
 *
 * @author TODAY 2019-07-14 17:41
 */
public class ObjectHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

  private final SelectableReturnValueHandler returnValueHandlers;

  public ObjectHandlerMethodReturnValueHandler(List<ReturnValueHandler> returnValueHandlers) {
    this.returnValueHandlers = new SelectableReturnValueHandler(returnValueHandlers);
  }

  public ObjectHandlerMethodReturnValueHandler(SelectableReturnValueHandler returnValueHandlers) {
    this.returnValueHandlers = returnValueHandlers;
  }

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, Object returnValue) throws Exception {
    returnValueHandlers.handleReturnValue(context, handler, returnValue);
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return returnValueHandlers.supportsReturnValue(returnValue);
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturn(Object.class);
  }

}
