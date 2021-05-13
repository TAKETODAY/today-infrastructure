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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.socket.WebSocketHandler;

/**
 * @author TODAY 2021/5/12 23:30
 * @since 3.0.1
 */
public class AnnotationWebSocketHandlerBuilder {
  protected static boolean isJettyPresent = ClassUtils.isPresent("org.eclipse.jetty.websocket.api.Session");
  protected final LinkedList<EndpointParameterResolver> resolvers = new LinkedList<>();
  protected boolean supportPartialMessage;

  /**
   * register default resolvers
   */
  public void registerDefaultResolvers() {
    if (isJettyPresent) {
      resolvers.add(new JettySessionEndpointParameterResolver());
    }
    resolvers.add(new PathVariableEndpointParameterResolver());
    resolvers.add(new WebSocketSessionEndpointParameterResolver());
  }

  public void addResolvers(EndpointParameterResolver... resolvers) {
    Assert.notNull(resolvers, "EndpointParameterResolvers must not be null");
    Collections.addAll(this.resolvers, resolvers);
  }

  public void setResolvers(EndpointParameterResolver... resolvers) {
    Assert.notNull(resolvers, "EndpointParameterResolvers must not be null");
    this.resolvers.clear();
    Collections.addAll(this.resolvers, resolvers);
  }

  public void setResolvers(List<EndpointParameterResolver> resolvers) {
    Assert.notNull(resolvers, "EndpointParameterResolvers must not be null");
    this.resolvers.clear();
    this.resolvers.addAll(resolvers);
  }

  public WebSocketHandler build(
          BeanDefinition definition, WebApplicationContext context, AnnotationHandlerDelegate annotationHandler) {
    return new AnnotationWebSocketDispatcher(annotationHandler, resolvers, supportPartialMessage);
  }

  public void setSupportPartialMessage(boolean supportPartialMessage) {
    this.supportPartialMessage = supportPartialMessage;
  }

  public boolean isSupportPartialMessage() {
    return supportPartialMessage;
  }
}
