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
package cn.taketoday.web.framework.reactive;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.loader.AnnotationImportSelector;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Singleton;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.session.EnableWebSession;
import cn.taketoday.web.socket.WebSocketHandlerRegistry;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enable Netty, Enable {@link cn.taketoday.web.session.WebSession}
 *
 * @author TODAY 2019-11-22 00:30
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@EnableWebSession
@Import(NettyConfig.class)
public @interface EnableNettyHandling {

  /**
   * determine using which  {@link NettyDispatcher}
   *
   * @see AsyncNettyDispatcherHandler
   */
  boolean async() default true;

  /**
   * Using {@link io.netty.util.concurrent.FastThreadLocal}
   * to hold {@link cn.taketoday.web.RequestContext}
   */
  boolean fastThreadLocal() default true;

}

final class NettyConfig implements AnnotationImportSelector<EnableNettyHandling> {

  @MissingBean(type = ReactiveChannelHandler.class)
  ReactiveChannelHandler reactiveChannelHandler(
          NettyDispatcher nettyDispatcher,
          NettyRequestContextConfig contextConfig,
          @Autowired(required = false) WebSocketHandlerRegistry registry) {
    if (registry != null) {
      return new WebSocketReactiveChannelHandler(nettyDispatcher, contextConfig);
    }
    return new ReactiveChannelHandler(nettyDispatcher, contextConfig);
  }

  @Singleton
  NettyWebSocketHandlerAdapter webSocketHandlerAdapter() {
    return new NettyWebSocketHandlerAdapter();
  }

  @MissingBean(type = DispatcherHandler.class)
  DispatcherHandler dispatcherHandler() {
    return new DispatcherHandler();
  }

  /**
   * Default {@link NettyWebServer} object
   * <p>
   * framework will auto inject properties start with 'server.' or 'server.netty.'
   * </p>
   *
   * @return returns a default {@link NettyWebServer} object
   */
  @MissingBean
  @Props(prefix = { "server.", "server.netty." })
  NettyWebServer nettyWebServer() {
    return new NettyWebServer();
  }

  /**
   * Framework Channel Initializer
   *
   * @param channelHandler ChannelInboundHandler
   */
  @MissingBean
  NettyServerInitializer nettyServerInitializer(ReactiveChannelHandler channelHandler) {
    return new NettyServerInitializer(channelHandler);
  }

  @MissingBean
  NettyRequestContextConfig nettyRequestContextConfig() {
    return new NettyRequestContextConfig();
  }

  /**
   * register a {@link NettyDispatcher} bean
   */
  @Override
  public String[] selectImports(
          EnableNettyHandling target, BeanDefinition annotatedMetadata, BeanDefinitionRegistry registry) {
    // replace context holder
    if (target.fastThreadLocal()) {
      RequestContextHolder.replaceContextHolder(new FastRequestThreadLocal());
    }

    if (!registry.containsBeanDefinition(NettyDispatcher.class)) {
      if (target.async()) {
        return new String[] { AsyncNettyDispatcherHandler.class.getName() };
      }
      else {
        return new String[] { NettyDispatcher.class.getName() };
      }
    }
    return null;
  }
}
