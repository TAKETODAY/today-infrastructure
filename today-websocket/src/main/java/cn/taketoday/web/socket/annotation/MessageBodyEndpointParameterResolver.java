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

package cn.taketoday.web.socket.annotation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.converter.GenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * read websocket message as a java object
 *
 * @author TODAY 2021/5/17 12:27
 * @since 3.0.1
 */
public class MessageBodyEndpointParameterResolver implements EndpointParameterResolver {

  private final List<HttpMessageConverter<?>> messageConverters;

  public MessageBodyEndpointParameterResolver(List<HttpMessageConverter<?>> messageConverters) {
    this.messageConverters = messageConverters;
  }

  @Override
  public boolean supports(ResolvableMethodParameter parameter) {
    return parameter.hasParameterAnnotation(MessageBody.class);
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Object resolve(
          WebSocketSession session, Message<?> message, ResolvableMethodParameter parameter) {
    if (message instanceof TextMessage) {
      ResolvableType resolvableType = parameter.getResolvableType();
      Class<?> contextClass = parameter.getParameter().getContainingClass();
      Type targetType = resolvableType.getType();
      Class targetClass = targetType instanceof Class ? (Class) targetType : null;
      if (targetClass == null) {
        targetClass = resolvableType.resolve();
      }
      String payload = (String) message.getPayload();

      ByteArrayInputStream inputStream = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
      HttpInputMessage msgToUse = new HttpInputMessage() {
        @Override
        public InputStream getBody() throws IOException {
          return inputStream;
        }

        @Override
        public HttpHeaders getHeaders() {
          return session.getHandshakeHeaders();
        }
      };
      try {
        for (HttpMessageConverter converter : messageConverters) {
          if (converter instanceof GenericHttpMessageConverter genericConverter) {
            if (genericConverter.canRead(targetType, contextClass, null)) {
              return genericConverter.read(targetType, contextClass, msgToUse);
            }
          }
          else if (targetClass != null && converter.canRead(targetClass, null)) {
            return converter.read(targetClass, msgToUse);
          }
        }
      }
      catch (IOException e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    }
    throw new IllegalStateException("message must be a TextMessage");
  }

}
