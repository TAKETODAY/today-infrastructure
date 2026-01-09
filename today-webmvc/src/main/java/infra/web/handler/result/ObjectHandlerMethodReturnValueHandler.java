/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.handler.result;

import org.jspecify.annotations.Nullable;

import java.util.List;

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
