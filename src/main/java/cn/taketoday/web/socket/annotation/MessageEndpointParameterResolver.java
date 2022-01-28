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

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.MatchingConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * {@link Message}
 *
 * @author TODAY 2021/5/13 21:17
 * @since 3.0.1
 */
public class MessageEndpointParameterResolver implements EndpointParameterResolver {

  private final Class<?> supportParameterType;
  private ConversionService conversionService;

  @Nullable
  private MatchingConverter converter;

  public MessageEndpointParameterResolver(Class<?> supportParameterType) {
    this(supportParameterType, (ConversionService) null);
  }

  public MessageEndpointParameterResolver(Class<?> supportParameterType, MatchingConverter converter) {
    this(supportParameterType);
    this.converter = converter;
  }

  public MessageEndpointParameterResolver(Class<?> supportParameterType, ConversionService conversionService) {
    this.supportParameterType = supportParameterType;
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(ResolvableMethodParameter parameter) {
    return parameter.hasParameterAnnotation(Message.class)
            && parameter.is(supportParameterType);
  }

  @Override
  public Object resolve(
          WebSocketSession session, cn.taketoday.web.socket.Message<?> message, ResolvableMethodParameter parameter) {
    Object payload = message.getPayload();
    if (supportParameterType.isInstance(payload)) {
      return payload;
    }

    MatchingConverter converter = getConverter();
    TypeDescriptor targetType = parameter.getTypeDescriptor();
    if (converter != null && converter.supports(targetType, payload.getClass())) {
      return converter.convert(targetType, payload);
    }
    ConversionService conversionService = getConversionService();
    Assert.state(conversionService != null, "No ConversionService");
    return conversionService.convert(payload, targetType);
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public void setConverter(@Nullable MatchingConverter converter) {
    this.converter = converter;
  }

  @Nullable
  public MatchingConverter getConverter() {
    return converter;
  }
}
