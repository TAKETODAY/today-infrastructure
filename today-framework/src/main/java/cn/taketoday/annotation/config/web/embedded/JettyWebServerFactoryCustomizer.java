/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.annotation.config.web.embedded;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.cloud.CloudPlatform;
import cn.taketoday.framework.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import cn.taketoday.framework.web.embedded.jetty.JettyServerCustomizer;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.ServerProperties.Jetty.Accesslog;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.DataSize;
import cn.taketoday.util.PropertyMapper;

/**
 * Customization for Jetty-specific features common for both Servlet and Reactive servers.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author HaiTao Zhang
 * @author Rafiullah Hamedy
 * @author Florian Storz
 * @author Michael Weidmann
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JettyWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory>, Ordered {

  static final int ORDER = 0;

  private final Environment environment;

  private final ServerProperties serverProperties;

  public JettyWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
    this.environment = environment;
    this.serverProperties = serverProperties;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void customize(ConfigurableJettyWebServerFactory factory) {
    ServerProperties properties = this.serverProperties;
    ServerProperties.Jetty jettyProperties = properties.jetty;
    factory.setUseForwardHeaders(getOrDeduceUseForwardHeaders());

    ServerProperties.Jetty.Threads threadProperties = jettyProperties.getThreads();

    factory.setThreadPool(JettyThreadPool.create(threadProperties));

    PropertyMapper map = PropertyMapper.get();
    map.from(threadProperties::getAcceptors).whenNonNull().to(factory::setAcceptors);
    map.from(threadProperties::getSelectors).whenNonNull().to(factory::setSelectors);

    map.from(properties::getMaxHttpRequestHeaderSize)
            .whenNonNull()
            .asInt(DataSize::toBytes)
            .when(this::isPositive)
            .to(size -> factory.addServerCustomizers(new MaxHttpRequestHeaderSizeCustomizer(size)));

    map.from(jettyProperties::getMaxConnections).to(factory::setMaxConnections);
    map.from(jettyProperties::getMaxHttpResponseHeaderSize)
            .whenNonNull()
            .asInt(DataSize::toBytes)
            .when(this::isPositive)
            .to(size -> factory.addServerCustomizers(new MaxHttpResponseHeaderSizeCustomizer(size)));

    map.from(jettyProperties::getMaxHttpFormPostSize)
            .asInt(DataSize::toBytes)
            .when(this::isPositive)
            .to(maxHttpFormPostSize -> customizeMaxHttpFormPostSize(factory, maxHttpFormPostSize));

    map.from(jettyProperties::getConnectionIdleTimeout)
            .whenNonNull()
            .to(idleTimeout -> customizeIdleTimeout(factory, idleTimeout));
    map.from(jettyProperties::getAccesslog)
            .when(Accesslog::isEnabled)
            .to(accesslog -> customizeAccessLog(factory, accesslog));
  }

  private boolean isPositive(Integer value) {
    return value > 0;
  }

  private boolean getOrDeduceUseForwardHeaders() {
    if (this.serverProperties.getForwardHeadersStrategy() == null) {
      CloudPlatform platform = CloudPlatform.getActive(this.environment);
      return platform != null && platform.isUsingForwardHeaders();
    }
    return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
  }

  private void customizeIdleTimeout(ConfigurableJettyWebServerFactory factory, Duration connectionTimeout) {
    factory.addServerCustomizers((server) -> {
      for (org.eclipse.jetty.server.Connector connector : server.getConnectors()) {
        if (connector instanceof AbstractConnector) {
          ((AbstractConnector) connector).setIdleTimeout(connectionTimeout.toMillis());
        }
      }
    });
  }

  private void customizeMaxHttpFormPostSize(ConfigurableJettyWebServerFactory factory, int maxHttpFormPostSize) {
    factory.addServerCustomizers(new JettyServerCustomizer() {

      @Override
      public void customize(Server server) {
        setHandlerMaxHttpFormPostSize(server.getHandlers());
      }

      private void setHandlerMaxHttpFormPostSize(List<Handler> handlers) {
        for (Handler handler : handlers) {
          setHandlerMaxHttpFormPostSize(handler);
        }
      }

      private void setHandlerMaxHttpFormPostSize(Handler handler) {
        if (handler instanceof ServletContextHandler servletContextHandler) {
          servletContextHandler.setMaxFormContentSize(maxHttpFormPostSize);
        }
        else if (handler instanceof Handler.Wrapper wrapper) {
          setHandlerMaxHttpFormPostSize(wrapper.getHandler());
        }
        else if (handler instanceof Handler.Collection handlerColl) {
          setHandlerMaxHttpFormPostSize(handlerColl.getHandlers());
        }
      }

    });
  }

  private void customizeAccessLog(ConfigurableJettyWebServerFactory factory, Accesslog properties) {
    factory.addServerCustomizers(server -> {
      RequestLogWriter logWriter = new RequestLogWriter();
      String format = getLogFormat(properties);
      CustomRequestLog log = new CustomRequestLog(logWriter, format);
      if (CollectionUtils.isNotEmpty(properties.getIgnorePaths())) {
        log.setIgnorePaths(properties.getIgnorePaths().toArray(new String[0]));
      }
      if (properties.getFilename() != null) {
        logWriter.setFilename(properties.getFilename());
      }
      if (properties.getFileDateFormat() != null) {
        logWriter.setFilenameDateFormat(properties.getFileDateFormat());
      }
      logWriter.setRetainDays(properties.getRetentionPeriod());
      logWriter.setAppend(properties.isAppend());
      server.setRequestLog(log);
    });
  }

  private String getLogFormat(Accesslog properties) {
    if (properties.getCustomFormat() != null) {
      return properties.getCustomFormat();
    }
    else if (Accesslog.FORMAT.EXTENDED_NCSA.equals(properties.getFormat())) {
      return CustomRequestLog.EXTENDED_NCSA_FORMAT;
    }
    return CustomRequestLog.NCSA_FORMAT;
  }

  private record MaxHttpRequestHeaderSizeCustomizer(int maxHttpHeaderSize) implements JettyServerCustomizer {

    @Override
    public void customize(Server server) {
      Arrays.stream(server.getConnectors()).forEach(this::customize);
    }

    private void customize(org.eclipse.jetty.server.Connector connector) {
      connector.getConnectionFactories().forEach(this::customize);
    }

    private void customize(ConnectionFactory factory) {
      if (factory instanceof HttpConfiguration.ConnectionFactory connectionFactory) {
        connectionFactory.getHttpConfiguration()
                .setRequestHeaderSize(this.maxHttpHeaderSize);
      }
    }

  }

  private static class MaxHttpResponseHeaderSizeCustomizer implements JettyServerCustomizer {

    private final int maxResponseHeaderSize;

    MaxHttpResponseHeaderSizeCustomizer(int maxResponseHeaderSize) {
      this.maxResponseHeaderSize = maxResponseHeaderSize;
    }

    @Override
    public void customize(Server server) {
      Arrays.stream(server.getConnectors()).forEach(this::customize);
    }

    private void customize(org.eclipse.jetty.server.Connector connector) {
      connector.getConnectionFactories().forEach(this::customize);
    }

    private void customize(ConnectionFactory factory) {
      if (factory instanceof HttpConfiguration.ConnectionFactory) {
        ((HttpConfiguration.ConnectionFactory) factory).getHttpConfiguration()
                .setResponseHeaderSize(this.maxResponseHeaderSize);
      }
    }

  }

}
