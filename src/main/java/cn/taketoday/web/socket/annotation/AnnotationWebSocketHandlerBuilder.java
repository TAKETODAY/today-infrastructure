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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.conversion.support.StringToBytesConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.WebSocketHandler;

/**
 * @author TODAY 2021/5/12 23:30
 * @since 3.0.1
 */
public class AnnotationWebSocketHandlerBuilder implements ArraySizeTrimmer {
  protected static boolean isJettyPresent = ClassUtils.isPresent("org.eclipse.jetty.websocket.api.Session");
  protected final ArrayList<EndpointParameterResolver> resolvers = new ArrayList<>(16);
  protected boolean supportPartialMessage;

  /**
   * register default resolvers
   */
  public void registerDefaultResolvers() {
    if (isJettyPresent) {
      resolvers.add(new JettySessionEndpointParameterResolver());
    }
    resolvers.add(new IsLastEndpointParameterResolver());
    resolvers.add(new MessageEndpointParameterResolver(String.class));
    resolvers.add(new MessageEndpointParameterResolver(byte[].class, new StringToBytesConverter()));
    resolvers.add(new MessageEndpointParameterResolver(ByteBuffer.class));
    resolvers.add(new PathVariableEndpointParameterResolver());
    resolvers.add(new WebSocketSessionEndpointParameterResolver());
  }

  public void addResolvers(EndpointParameterResolver... resolvers) {
    Assert.notNull(resolvers, "EndpointParameterResolvers must not be null");
    Collections.addAll(this.resolvers, resolvers);
  }

  public void addResolvers(List<EndpointParameterResolver> resolvers) {
    Assert.notNull(resolvers, "EndpointParameterResolvers must not be null");
    this.resolvers.addAll(resolvers);
  }

  public void setResolvers(@Nullable EndpointParameterResolver... resolvers) {
    this.resolvers.clear();
    CollectionUtils.addAll(this.resolvers, resolvers);
  }

  public void setResolvers(@Nullable List<EndpointParameterResolver> resolvers) {
    this.resolvers.clear();
    CollectionUtils.addAll(this.resolvers, resolvers);
  }

  /**
   * @since 4.0
   */
  @Override
  public void trimToSize() {
    CollectionUtils.trimToSize(resolvers);
  }

  public WebSocketHandler build(
          BeanDefinition definition, WebApplicationContext context, AnnotationHandlerDelegate annotationHandler) {
    return new AnnotationWebSocketDispatcher(annotationHandler, resolvers, supportPartialMessage);
  }

  /**
   * set support Partial Message
   *
   * @param supportPartialMessage
   *         supportPartialMessage?
   *
   * @see Message#isLast()
   * @see WebSocketHandler#supportPartialMessage()
   */
  public void setSupportPartialMessage(boolean supportPartialMessage) {
    this.supportPartialMessage = supportPartialMessage;
  }

  public boolean isSupportPartialMessage() {
    return supportPartialMessage;
  }

}
