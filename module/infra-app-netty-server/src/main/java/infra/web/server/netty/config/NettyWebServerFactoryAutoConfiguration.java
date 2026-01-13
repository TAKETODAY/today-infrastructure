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

package infra.web.server.netty.config;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Executor;

import infra.annotation.ConditionalOnWebApplication;
import infra.annotation.ConditionalOnWebApplication.Type;
import infra.annotation.config.task.TaskExecutionAutoConfiguration;
import infra.annotation.config.task.TaskExecutionProperties;
import infra.beans.BeanUtils;
import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.config.BeanDefinition;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.Lazy;
import infra.context.annotation.Role;
import infra.context.annotation.config.AutoConfigureOrder;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.ApplicationTemp;
import infra.core.Ordered;
import infra.scheduling.concurrent.ThreadPoolTaskExecutor;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.DataSize;
import infra.web.DispatcherHandler;
import infra.web.multipart.MultipartParser;
import infra.web.multipart.parsing.DefaultMultipartParser;
import infra.web.multipart.parsing.ProgressListener;
import infra.web.server.ServerProperties;
import infra.web.server.ServiceExecutor;
import infra.web.server.SimpleServiceExecutor;
import infra.web.server.Ssl;
import infra.web.server.config.WebServerConfiguration;
import infra.web.server.error.SendErrorHandler;
import infra.web.server.netty.ChannelConfigurer;
import infra.web.server.netty.NettyChannelHandler;
import infra.web.server.netty.NettyRequestConfig;
import infra.web.server.netty.NettyRequestUpgradeStrategy;
import infra.web.server.netty.NettyServerProperties;
import infra.web.server.netty.NettyWebServerFactory;
import infra.web.server.netty.ServerBootstrapCustomizer;
import infra.web.server.netty.WsNettyChannelHandler;
import infra.web.socket.server.RequestUpgradeStrategy;
import infra.webmvc.config.ErrorMvcAutoConfiguration;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;

import static infra.annotation.config.task.TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME;
import static infra.annotation.config.task.TaskExecutionAutoConfiguration.TaskExecutorConfiguration;
import static infra.annotation.config.task.TaskExecutionAutoConfiguration.threadPoolTaskExecutorBuilder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a netty web server.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/5 17:39
 */
@Lazy
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = Type.MVC)
@Import(WebServerConfiguration.class)
@EnableConfigurationProperties({ ServerProperties.class, NettyServerProperties.class })
@DisableDIAutoConfiguration(after = {
        ErrorMvcAutoConfiguration.class,
        TaskExecutionAutoConfiguration.class
})
public final class NettyWebServerFactoryAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static ChannelHandler nettyChannelHandler(ApplicationContext context,
          NettyRequestConfig requestConfig, DispatcherHandler dispatcherHandler, ServiceExecutor executor) {
    return createChannelHandler(requestConfig, context, dispatcherHandler, executor, context.getClassLoader());
  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static ServiceExecutor serviceExecutor(ServerProperties serverProperties,
          @Qualifier(APPLICATION_TASK_EXECUTOR_BEAN_NAME) @Nullable Executor executor) {
    if (serverProperties.useVirtualThreadServiceExecutor) {
      return BeanUtils.newInstance("infra.web.server.support.VirtualThreadServiceExecutor",
              ClassUtils.getDefaultClassLoader());
    }
    if (executor == null) {
      ThreadPoolTaskExecutor taskExecutor = TaskExecutorConfiguration.applicationTaskExecutor(
              threadPoolTaskExecutorBuilder(new TaskExecutionProperties(), List.of(), null));
      taskExecutor.initialize();
      taskExecutor.start();
      executor = taskExecutor;
    }
    return new SimpleServiceExecutor(executor);
  }

  /**
   * Netty Server
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static NettyWebServerFactory nettyWebServerFactory(
          NettyServerProperties nettyServerProperties, @Nullable ChannelConfigurer channelConfigurer,
          @Nullable List<ServerBootstrapCustomizer> customizers, ChannelHandler channelHandler) {

    NettyWebServerFactory factory = new NettyWebServerFactory();
    factory.applyFrom(nettyServerProperties);
    factory.setBootstrapCustomizers(customizers);
    factory.setChannelConfigurer(channelConfigurer);
    factory.setChannelHandler(channelHandler);
    return factory;
  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static NettyRequestConfig nettyRequestConfig(ServerProperties server,
          NettyServerProperties netty, SendErrorHandler sendErrorHandler, MultipartParser multipartParser) {

    return NettyRequestConfig.forBuilder(Ssl.isEnabled(server.ssl))
            .multipartParser(multipartParser)
            .writerAutoFlush(netty.writerAutoFlush)
            .headersFactory(DefaultHttpHeadersFactory.headersFactory()
                    .withValidation(netty.validateHeaders))
            .sendErrorHandler(sendErrorHandler)
            .maxContentLength(netty.maxContentLength.toBytes())
            .dataReceivedQueueCapacity(netty.dataReceivedQueueCapacity)
            .autoRead(netty.autoRead)
            .build();
  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static MultipartParser multipartParser(ServerProperties properties,
          @Nullable ApplicationTemp applicationTemp, @Nullable ProgressListener progressListener) {
    var config = properties.multipart;
    DefaultMultipartParser multipartParser = new DefaultMultipartParser();

    multipartParser.setTempRepository(config.computeTempRepository(applicationTemp));
    multipartParser.setMaxFields(config.maxFields);
    multipartParser.setDeleteOnExit(config.deleteOnExit);
    multipartParser.setProgressListener(progressListener);
    multipartParser.setDefaultCharset(config.defaultCharset);
    multipartParser.setThreshold(config.fieldSizeThreshold.toBytes());
    multipartParser.setMaxHeaderSize(config.maxHeaderSize.toBytesInt());
    multipartParser.setParsingBufferSize(config.parsingBufferSize.toBytesInt());
    return multipartParser;
  }

  private static ChannelHandler createChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context,
          DispatcherHandler dispatcherHandler, ServiceExecutor executor, @Nullable ClassLoader classLoader) {
    if (ClassUtils.isPresent("infra.web.socket.server.RequestUpgradeStrategy", classLoader)) {
      return Ws.createChannelHandler(requestConfig, context, dispatcherHandler, executor);
    }

    return new NettyChannelHandler(requestConfig, context, dispatcherHandler, executor);
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(RequestUpgradeStrategy.class)
  public static class NettyWebSocket {

    @Component
    @ConditionalOnMissingBean
    public static RequestUpgradeStrategy nettyRequestUpgradeStrategy(WebSocketDecoderConfig decoderConfig) {
      return new NettyRequestUpgradeStrategy(decoderConfig);
    }

    @Component
    @ConditionalOnMissingBean
    public static WebSocketDecoderConfig webSocketDecoderConfig() {
      return WebSocketDecoderConfig.newBuilder()
              .maxFramePayloadLength(DataSize.ofKilobytes(512).bytes().intValue())
              .expectMaskedFrames(true)
              .allowMaskMismatch(false)
              .allowExtensions(false)
              .closeOnProtocolViolation(true)
              .withUTF8Validator(true)
              .build();
    }

  }

  static class Ws {
    private static ChannelHandler createChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context,
            DispatcherHandler dispatcherHandler, ServiceExecutor executor) {
      return new WsNettyChannelHandler(requestConfig, context, dispatcherHandler, executor);
    }
  }

}
