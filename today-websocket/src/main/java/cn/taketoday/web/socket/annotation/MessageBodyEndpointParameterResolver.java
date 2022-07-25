/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.socket.annotation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.bind.resolver.ParameterReadFailedException;
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

  private final ObjectMapper jacksonObjectMapper;

  public MessageBodyEndpointParameterResolver(ObjectMapper jacksonObjectMapper) {
    Assert.notNull(jacksonObjectMapper, "jacksonObjectMapper must not be null");
    this.jacksonObjectMapper = jacksonObjectMapper;
  }

  @Override
  public boolean supports(ResolvableMethodParameter parameter) {
    return parameter.hasParameterAnnotation(MessageBody.class);
  }

  @Override
  public Object resolve(
          WebSocketSession session, Message<?> message, ResolvableMethodParameter parameter) {
    if (message instanceof TextMessage) {
      String payload = (String) message.getPayload();
      try {
        return jacksonObjectMapper.readValue(payload, new TypeReference<>() {
          @Override
          public Type getType() {
            return parameter.getResolvableType().getType();
          }
        });
      }
      catch (JsonProcessingException e) {
        throw new ParameterReadFailedException(e);
      }
    }
    throw new IllegalStateException("message must be a TextMessage");
  }

}
