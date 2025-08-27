/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.handler.result;

import java.util.List;

import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.ReturnValueHandler;
import infra.web.handler.SelectableReturnValueHandler;
import infra.web.handler.method.HandlerMethod;

/**
 * HandlerMethod return Object
 * <p>
 * Iterate handlers in runtime
 * </p>
 * <pre> {@code
 * @GET("/object")
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
 * }</pre>
 *
 * @author TODAY 2019-07-14 17:41
 */
public class ObjectHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

  private final SelectableReturnValueHandler delegate;

  public ObjectHandlerMethodReturnValueHandler(List<ReturnValueHandler> returnValueHandlers) {
    this.delegate = new SelectableReturnValueHandler(returnValueHandlers);
  }

  public ObjectHandlerMethodReturnValueHandler(SelectableReturnValueHandler delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    delegate.handleReturnValue(context, handler, returnValue);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return delegate.supportsReturnValue(returnValue);
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturn(Object.class);
  }

}
