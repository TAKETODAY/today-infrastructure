package cn.taketoday.web.framework.reactive;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.EnableDependencyInjection;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.lang.Singleton;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.socket.WebSocketHandlerRegistry;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/13 17:34
 */
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class NettyConfiguration {

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
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
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  NettyWebSocketHandlerAdapter webSocketHandlerAdapter() {
    return new NettyWebSocketHandlerAdapter();
  }

  @MissingBean(type = DispatcherHandler.class)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
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
  @EnableDependencyInjection
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
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
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  NettyServerInitializer nettyServerInitializer(ReactiveChannelHandler channelHandler) {
    return new NettyServerInitializer(channelHandler);
  }

  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  NettyRequestContextConfig nettyRequestContextConfig() {
    return new NettyRequestContextConfig();
  }

}
