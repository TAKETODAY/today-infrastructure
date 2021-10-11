/*
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

package cn.taketoday.web.socket.annotation;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.MessageBodyConverter;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.resolver.ParameterReadFailedException;
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
  private final MessageBodyConverter messageBodyConverter;

  public MessageBodyEndpointParameterResolver(MessageBodyConverter messageBodyConverter) {
    Assert.notNull(messageBodyConverter, "MessageBodyConverter must not be null");
    this.messageBodyConverter = messageBodyConverter;
  }

  @Override
  public boolean supports(MethodParameter parameter) {
    return parameter.isAnnotationPresent(MessageBody.class);
  }

  @Override
  public Object resolve(
          WebSocketSession session, Message<?> message, MethodParameter parameter) {
    if (message instanceof TextMessage) {
      final String payload = (String) message.getPayload();
      try {
        return messageBodyConverter.read(payload, parameter);
      }
      catch (Exception e) {
        throw new ParameterReadFailedException(e);
      }
    }
    throw new IllegalStateException("message must be a TextMessage");
  }

}
