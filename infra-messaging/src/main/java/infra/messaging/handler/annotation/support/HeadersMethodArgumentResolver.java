/*
 * Copyright 2002-present the original author or authors.
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

package infra.messaging.handler.annotation.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;

import infra.core.MethodParameter;
import infra.messaging.Message;
import infra.messaging.MessageHeaders;
import infra.messaging.handler.annotation.Headers;
import infra.messaging.handler.invocation.HandlerMethodArgumentResolver;
import infra.messaging.support.MessageHeaderAccessor;
import infra.util.ReflectionUtils;

/**
 * Argument resolver for headers. Resolves the following method parameters:
 * <ul>
 * <li>{@link Headers @Headers} {@link Map}
 * <li>{@link MessageHeaders}
 * <li>{@link MessageHeaderAccessor}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HeadersMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    Class<?> paramType = parameter.getParameterType();
    return ((parameter.hasParameterAnnotation(Headers.class) && Map.class.isAssignableFrom(paramType)) ||
            MessageHeaders.class == paramType || MessageHeaderAccessor.class.isAssignableFrom(paramType));
  }

  @Override
  public @Nullable Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
    Class<?> paramType = parameter.getParameterType();
    if (Map.class.isAssignableFrom(paramType)) {
      return message.getHeaders();
    }
    else if (MessageHeaderAccessor.class == paramType) {
      MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
      return accessor != null ? accessor : new MessageHeaderAccessor(message);
    }
    else if (MessageHeaderAccessor.class.isAssignableFrom(paramType)) {
      MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
      if (accessor != null && paramType.isAssignableFrom(accessor.getClass())) {
        return accessor;
      }
      else {
        Method method = ReflectionUtils.findMethod(paramType, "wrap", Message.class);
        if (method == null) {
          throw new IllegalStateException(
                  "Cannot create accessor of type " + paramType + " for message " + message);
        }
        return ReflectionUtils.invokeMethod(method, null, message);
      }
    }
    else {
      throw new IllegalStateException("Unexpected parameter of type " + paramType +
              " in method " + parameter.getMethod() + ". ");
    }
  }

}
