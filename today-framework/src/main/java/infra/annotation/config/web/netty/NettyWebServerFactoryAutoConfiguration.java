/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.annotation.config.web.netty;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

import infra.annotation.ConditionalOnWebApplication;
import infra.annotation.ConditionalOnWebApplication.Type;
import infra.annotation.config.task.TaskExecutionAutoConfiguration;
import infra.annotation.config.task.TaskExecutionProperties;
import infra.annotation.config.web.ErrorMvcAutoConfiguration;
import infra.annotation.config.web.WebMvcProperties;
import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.config.BeanDefinition;
import infra.context.ApplicationContext;
import infra.context.annotation.Lazy;
import infra.context.annotation.Role;
import infra.context.annotation.config.AutoConfigureOrder;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.ApplicationTemp;
import infra.core.Ordered;
import infra.core.ssl.SslBundles;
import infra.scheduling.concurrent.ThreadPoolTaskExecutor;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.StringUtils;
import infra.web.DispatcherHandler;
import infra.web.multipart.MultipartParser;
import infra.web.multipart.parsing.DefaultMultipartParser;
import infra.web.server.ServerProperties;
import infra.web.server.ServiceExecutor;
import infra.web.server.Ssl;
import infra.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import infra.web.server.error.SendErrorHandler;
import infra.web.server.support.ChannelConfigurer;
import infra.web.server.support.JUCServiceExecutor;
import infra.web.server.support.NettyChannelHandler;
import infra.web.server.support.NettyRequestConfig;
import infra.web.server.support.NettyWebServerFactory;
import infra.web.server.support.ServerBootstrapCustomizer;
import infra.web.server.support.StandardNettyWebEnvironment;
import infra.web.socket.server.support.WsNettyChannelHandler;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;

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
@ConditionalOnWebApplication(type = Type.NETTY)
@EnableConfigurationProperties(ServerProperties.class)
@DisableDIAutoConfiguration(after = { ErrorMvcAutoConfiguration.class, TaskExecutionAutoConfiguration.class })
public class NettyWebServerFactoryAutoConfiguration {

  private NettyWebServerFactoryAutoConfiguration() {
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static WebServerFactoryCustomizerBeanPostProcessor webServerFactoryCustomizerBeanPostProcessor() {
    return new WebServerFactoryCustomizerBeanPostProcessor();
  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static DispatcherHandler dispatcherHandler(ApplicationContext context, WebMvcProperties webMvcProperties) {
    DispatcherHandler handler = new DispatcherHandler(context);
    handler.setThrowExceptionIfNoHandlerFound(webMvcProperties.throwExceptionIfNoHandlerFound);
    handler.setEnableLoggingRequestDetails(webMvcProperties.logRequestDetails);
    handler.setEnvironment(new StandardNettyWebEnvironment());
    return handler;
  }

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
  public static ServiceExecutor serviceExecutor(@Qualifier(APPLICATION_TASK_EXECUTOR_BEAN_NAME) @Nullable Executor executor) {
    if (executor == null) {
      ThreadPoolTaskExecutor taskExecutor = TaskExecutorConfiguration.applicationTaskExecutor(
              threadPoolTaskExecutorBuilder(new TaskExecutionProperties(), List.of(), null));
      taskExecutor.initialize();
      taskExecutor.start();
      executor = taskExecutor;
    }
    return new JUCServiceExecutor(executor);
  }

  /**
   * Netty Server
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static NettyWebServerFactory nettyWebServerFactory(ServerProperties serverProperties,
          @Nullable ChannelConfigurer channelConfigurer, @Nullable SslBundles sslBundles,
          @Nullable List<ServerBootstrapCustomizer> customizers, @Nullable ApplicationTemp applicationTemp,
          ChannelHandler channelHandler) {
    NettyWebServerFactory factory = new NettyWebServerFactory();

    serverProperties.applyTo(factory, sslBundles, applicationTemp);

    factory.applyFrom(serverProperties.netty);
    factory.setBootstrapCustomizers(customizers);
    factory.setChannelConfigurer(channelConfigurer);
    factory.setChannelHandler(channelHandler);
    return factory;
  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static NettyRequestConfig nettyRequestConfig(ServerProperties server,
          SendErrorHandler sendErrorHandler, MultipartParser multipartParser) {

    return NettyRequestConfig.forBuilder(Ssl.isEnabled(server.ssl))
            .multipartParser(multipartParser)
            .headersFactory(DefaultHttpHeadersFactory.headersFactory()
                    .withValidation(server.netty.validateHeaders))
            .sendErrorHandler(sendErrorHandler)
            .maxContentLength(server.netty.maxContentLength.toBytes())
            .build();
  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static DefaultMultipartParser multipartParser(ServerProperties properties, @Nullable ApplicationTemp applicationTemp) {
    var config = properties.multipart;
    DefaultMultipartParser multipartParser = new DefaultMultipartParser();

    if (StringUtils.hasText(config.tempBaseDir)) {
      if (StringUtils.hasText(config.tempSubDir)) {
        multipartParser.setTempRepository(Path.of(config.tempBaseDir, config.tempSubDir));
      }
      else {
        multipartParser.setTempRepository(Path.of(config.tempBaseDir));
      }
    }
    else {
      if (applicationTemp == null) {
        applicationTemp = ApplicationTemp.instance;
      }
      multipartParser.setTempRepository(applicationTemp.getDir(Objects.requireNonNullElse(config.tempSubDir, "multipart")));
    }

    multipartParser.setMaxFields(config.maxFields);
    multipartParser.setDeleteOnExit(config.deleteOnExit);
    multipartParser.setDefaultCharset(config.defaultCharset);
    multipartParser.setThreshold(config.fieldSizeThreshold.toBytes());
    multipartParser.setMaxHeaderSize(config.maxHeaderSize.toBytesInt());
    multipartParser.setParsingBufferSize(config.parsingBufferSize.toBytesInt());
    return multipartParser;
  }

  private static ChannelHandler createChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context,
          DispatcherHandler dispatcherHandler, ServiceExecutor executor, @Nullable ClassLoader classLoader) {
    if (ClassUtils.isPresent("infra.web.socket.server.support.WsNettyChannelHandler", classLoader)) {
      return Ws.createChannelHandler(requestConfig, context, dispatcherHandler, executor);
    }

    return new NettyChannelHandler(requestConfig, context, dispatcherHandler, executor);
  }

  static class Ws {
    private static ChannelHandler createChannelHandler(NettyRequestConfig requestConfig, ApplicationContext context,
            DispatcherHandler dispatcherHandler, ServiceExecutor executor) {
      return new WsNettyChannelHandler(requestConfig, context, dispatcherHandler, executor);
    }
  }

}
