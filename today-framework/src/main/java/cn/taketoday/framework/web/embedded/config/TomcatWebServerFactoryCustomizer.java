/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.embedded.config;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.UpgradeProtocol;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http2.Http2Protocol;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.cloud.CloudPlatform;
import cn.taketoday.framework.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import cn.taketoday.framework.web.error.ErrorProperties;
import cn.taketoday.framework.web.error.IncludeAttribute;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.util.DataSize;
import cn.taketoday.util.PropertyMapper;
import cn.taketoday.util.StringUtils;

/**
 * Customization for Tomcat-specific features common for both Servlet and Reactive
 * servers.
 *
 * @author Brian Clozel
 * @author Yulin Qin
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Chentao Qu
 * @author Andrew McGhie
 * @author Dirk Deyne
 * @author Rafiullah Hamedy
 * @author Victor Mandujano
 * @author Parviz Rozikov
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/19 9:35
 */
public class TomcatWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory>, Ordered {

  private final Environment environment;

  private final ServerProperties serverProperties;

  public TomcatWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
    this.environment = environment;
    this.serverProperties = serverProperties;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void customize(ConfigurableTomcatWebServerFactory factory) {
    ServerProperties properties = this.serverProperties;
    ServerProperties.Tomcat tomcatProperties = properties.getTomcat();
    PropertyMapper propertyMapper = PropertyMapper.get();
    propertyMapper.from(tomcatProperties::getBasedir).whenNonNull().to(factory::setBaseDirectory);
    propertyMapper.from(tomcatProperties::getBackgroundProcessorDelay)
            .whenNonNull().as(Duration::getSeconds).as(Long::intValue)
            .to(factory::setBackgroundProcessorDelay);
    customizeRemoteIpValve(factory);
    ServerProperties.Tomcat.Threads threadProperties = tomcatProperties.getThreads();
    propertyMapper.from(threadProperties::getMax).when(this::isPositive)
            .to((maxThreads) -> customizeMaxThreads(factory, threadProperties.getMax()));
    propertyMapper.from(threadProperties::getMinSpare).when(this::isPositive)
            .to((minSpareThreads) -> customizeMinThreads(factory, minSpareThreads));
    propertyMapper.from(this.serverProperties.getMaxHttpHeaderSize()).whenNonNull().asInt(DataSize::toBytes)
            .when(this::isPositive)
            .to((maxHttpHeaderSize) -> customizeMaxHttpHeaderSize(factory, maxHttpHeaderSize));
    propertyMapper.from(tomcatProperties::getMaxSwallowSize).whenNonNull().asInt(DataSize::toBytes)
            .to((maxSwallowSize) -> customizeMaxSwallowSize(factory, maxSwallowSize));
    propertyMapper.from(tomcatProperties::getMaxHttpFormPostSize).asInt(DataSize::toBytes)
            .when((maxHttpFormPostSize) -> maxHttpFormPostSize != 0)
            .to((maxHttpFormPostSize) -> customizeMaxHttpFormPostSize(factory, maxHttpFormPostSize));
    propertyMapper.from(tomcatProperties::getAccesslog).when(ServerProperties.Tomcat.Accesslog::isEnabled)
            .to((enabled) -> customizeAccessLog(factory));
    propertyMapper.from(tomcatProperties::getUriEncoding).whenNonNull().to(factory::setUriEncoding);
    propertyMapper.from(tomcatProperties::getConnectionTimeout).whenNonNull()
            .to((connectionTimeout) -> customizeConnectionTimeout(factory, connectionTimeout));
    propertyMapper.from(tomcatProperties::getMaxConnections).when(this::isPositive)
            .to((maxConnections) -> customizeMaxConnections(factory, maxConnections));
    propertyMapper.from(tomcatProperties::getAcceptCount).when(this::isPositive)
            .to((acceptCount) -> customizeAcceptCount(factory, acceptCount));
    propertyMapper.from(tomcatProperties::getProcessorCache).to((processorCache) -> customizeProcessorCache(factory, processorCache));
    propertyMapper.from(tomcatProperties::getKeepAliveTimeout).whenNonNull().to(keepAliveTimeout -> customizeKeepAliveTimeout(factory, keepAliveTimeout));
    propertyMapper.from(tomcatProperties::getMaxKeepAliveRequests).to(maxKeepAliveRequests -> customizeMaxKeepAliveRequests(factory, maxKeepAliveRequests));
    propertyMapper.from(tomcatProperties::getRelaxedPathChars).as(this::joinCharacters)
            .whenHasText().to(relaxedChars -> customizeRelaxedPathChars(factory, relaxedChars));
    propertyMapper.from(tomcatProperties::getRelaxedQueryChars).as(this::joinCharacters).whenHasText().to(relaxedChars -> customizeRelaxedQueryChars(factory, relaxedChars));
    propertyMapper.from(tomcatProperties::isRejectIllegalHeader).to((rejectIllegalHeader) -> customizeRejectIllegalHeader(factory, rejectIllegalHeader));
    customizeStaticResources(factory);
    customizeErrorReportValve(properties.getError(), factory);
  }

  private boolean isPositive(int value) {
    return value > 0;
  }

  private void customizeAcceptCount(ConfigurableTomcatWebServerFactory factory, int acceptCount) {
    factory.addConnectorCustomizers((connector) -> {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractProtocol<?> protocol) {
        protocol.setAcceptCount(acceptCount);
      }
    });
  }

  private void customizeProcessorCache(ConfigurableTomcatWebServerFactory factory, int processorCache) {
    factory.addConnectorCustomizers(connector -> {
      if (connector.getProtocolHandler() instanceof AbstractProtocol protocol) {
        protocol.setProcessorCache(processorCache);
      }
    });
  }

  private void customizeKeepAliveTimeout(ConfigurableTomcatWebServerFactory factory, Duration keepAliveTimeout) {
    factory.addConnectorCustomizers(connector -> {
      ProtocolHandler handler = connector.getProtocolHandler();
      for (UpgradeProtocol upgradeProtocol : handler.findUpgradeProtocols()) {
        if (upgradeProtocol instanceof Http2Protocol http2Protocol) {
          http2Protocol.setKeepAliveTimeout(keepAliveTimeout.toMillis());
        }
      }
      if (handler instanceof AbstractProtocol<?> protocol) {
        protocol.setKeepAliveTimeout((int) keepAliveTimeout.toMillis());
      }
    });
  }

  private void customizeMaxKeepAliveRequests(ConfigurableTomcatWebServerFactory factory, int maxKeepAliveRequests) {
    factory.addConnectorCustomizers(connector -> {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractHttp11Protocol<?> protocol) {
        protocol.setMaxKeepAliveRequests(maxKeepAliveRequests);
      }
    });
  }

  private void customizeMaxConnections(ConfigurableTomcatWebServerFactory factory, int maxConnections) {
    factory.addConnectorCustomizers(connector -> {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractProtocol<?> protocol) {
        protocol.setMaxConnections(maxConnections);
      }
    });
  }

  private void customizeConnectionTimeout(ConfigurableTomcatWebServerFactory factory, Duration connectionTimeout) {
    factory.addConnectorCustomizers(connector -> {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractProtocol<?> protocol) {
        protocol.setConnectionTimeout((int) connectionTimeout.toMillis());
      }
    });
  }

  private void customizeRelaxedPathChars(ConfigurableTomcatWebServerFactory factory, String relaxedChars) {
    factory.addConnectorCustomizers((connector) -> connector.setProperty("relaxedPathChars", relaxedChars));
  }

  private void customizeRelaxedQueryChars(ConfigurableTomcatWebServerFactory factory, String relaxedChars) {
    factory.addConnectorCustomizers((connector) -> connector.setProperty("relaxedQueryChars", relaxedChars));
  }

  private void customizeRejectIllegalHeader(ConfigurableTomcatWebServerFactory factory, boolean rejectIllegalHeader) {
    factory.addConnectorCustomizers(connector -> {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractHttp11Protocol<?> protocol) {
        protocol.setRejectIllegalHeader(rejectIllegalHeader);
      }
    });
  }

  private String joinCharacters(List<Character> content) {
    return content.stream().map(String::valueOf).collect(Collectors.joining());
  }

  private void customizeRemoteIpValve(ConfigurableTomcatWebServerFactory factory) {
    ServerProperties.Tomcat.Remoteip remoteIpProperties = serverProperties.getTomcat().getRemoteip();
    String protocolHeader = remoteIpProperties.getProtocolHeader();
    String remoteIpHeader = remoteIpProperties.getRemoteIpHeader();
    // For back compatibility the valve is also enabled if protocol-header is set
    if (StringUtils.hasText(protocolHeader)
            || StringUtils.hasText(remoteIpHeader) || getOrDeduceUseForwardHeaders()) {
      RemoteIpValve valve = new RemoteIpValve();
      valve.setProtocolHeader(StringUtils.isNotEmpty(protocolHeader) ? protocolHeader : "X-Forwarded-Proto");
      if (StringUtils.isNotEmpty(remoteIpHeader)) {
        valve.setRemoteIpHeader(remoteIpHeader);
      }
      // The internal proxies default to a list of "safe" internal IP addresses
      valve.setInternalProxies(remoteIpProperties.getInternalProxies());
      try {
        valve.setHostHeader(remoteIpProperties.getHostHeader());
      }
      catch (NoSuchMethodError ex) {
        // Avoid failure with war deployments to Tomcat 8.5 before 8.5.44 and
        // Tomcat 9 before 9.0.23
      }
      valve.setPortHeader(remoteIpProperties.getPortHeader());
      valve.setProtocolHeaderHttpsValue(remoteIpProperties.getProtocolHeaderHttpsValue());
      // ... so it's safe to add this valve by default.
      factory.addEngineValves(valve);
    }
  }

  private boolean getOrDeduceUseForwardHeaders() {
    if (this.serverProperties.getForwardHeadersStrategy() == null) {
      CloudPlatform platform = CloudPlatform.getActive(this.environment);
      return platform != null && platform.isUsingForwardHeaders();
    }
    return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
  }

  private void customizeMaxThreads(ConfigurableTomcatWebServerFactory factory, int maxThreads) {
    factory.addConnectorCustomizers(connector -> {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractProtocol protocol) {
        protocol.setMaxThreads(maxThreads);
      }
    });
  }

  private void customizeMinThreads(ConfigurableTomcatWebServerFactory factory, int minSpareThreads) {
    factory.addConnectorCustomizers(connector -> {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractProtocol protocol) {
        protocol.setMinSpareThreads(minSpareThreads);
      }
    });
  }

  private void customizeMaxHttpHeaderSize(ConfigurableTomcatWebServerFactory factory, int maxHttpHeaderSize) {
    factory.addConnectorCustomizers(connector -> {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractHttp11Protocol protocol) {
        protocol.setMaxHttpHeaderSize(maxHttpHeaderSize);
      }
    });
  }

  private void customizeMaxSwallowSize(ConfigurableTomcatWebServerFactory factory, int maxSwallowSize) {
    factory.addConnectorCustomizers((connector) -> {
      ProtocolHandler handler = connector.getProtocolHandler();
      if (handler instanceof AbstractHttp11Protocol<?> protocol) {
        protocol.setMaxSwallowSize(maxSwallowSize);
      }
    });
  }

  private void customizeMaxHttpFormPostSize(ConfigurableTomcatWebServerFactory factory, int maxHttpFormPostSize) {
    factory.addConnectorCustomizers(connector -> connector.setMaxPostSize(maxHttpFormPostSize));
  }

  private void customizeAccessLog(ConfigurableTomcatWebServerFactory factory) {
    ServerProperties.Tomcat tomcatProperties = this.serverProperties.getTomcat();
    AccessLogValve valve = new AccessLogValve();
    PropertyMapper map = PropertyMapper.get();
    ServerProperties.Tomcat.Accesslog accessLogConfig = tomcatProperties.getAccesslog();
    map.from(accessLogConfig.getConditionIf()).to(valve::setConditionIf);
    map.from(accessLogConfig.getConditionUnless()).to(valve::setConditionUnless);
    map.from(accessLogConfig.getPattern()).to(valve::setPattern);
    map.from(accessLogConfig.getDirectory()).to(valve::setDirectory);
    map.from(accessLogConfig.getPrefix()).to(valve::setPrefix);
    map.from(accessLogConfig.getSuffix()).to(valve::setSuffix);
    map.from(accessLogConfig.getEncoding()).whenHasText().to(valve::setEncoding);
    map.from(accessLogConfig.getLocale()).whenHasText().to(valve::setLocale);
    map.from(accessLogConfig.isCheckExists()).to(valve::setCheckExists);
    map.from(accessLogConfig.isRotate()).to(valve::setRotatable);
    map.from(accessLogConfig.isRenameOnRotate()).to(valve::setRenameOnRotate);
    map.from(accessLogConfig.getMaxDays()).to(valve::setMaxDays);
    map.from(accessLogConfig.getFileDateFormat()).to(valve::setFileDateFormat);
    map.from(accessLogConfig.isIpv6Canonical()).to(valve::setIpv6Canonical);
    map.from(accessLogConfig.isRequestAttributesEnabled()).to(valve::setRequestAttributesEnabled);
    map.from(accessLogConfig.isBuffered()).to(valve::setBuffered);
    factory.addEngineValves(valve);
  }

  private void customizeStaticResources(ConfigurableTomcatWebServerFactory factory) {
    ServerProperties.Tomcat.Resource resource = this.serverProperties.getTomcat().getResource();
    factory.addContextCustomizers(context -> context.addLifecycleListener(event -> {
      if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
        context.getResources().setCachingAllowed(resource.isAllowCaching());
        if (resource.getCacheTtl() != null) {
          long ttl = resource.getCacheTtl().toMillis();
          context.getResources().setCacheTtl(ttl);
        }
      }
    }));
  }

  private void customizeErrorReportValve(ErrorProperties error, ConfigurableTomcatWebServerFactory factory) {
    if (error.getIncludeStacktrace() == IncludeAttribute.NEVER) {
      factory.addContextCustomizers((context) -> {
        ErrorReportValve valve = new ErrorReportValve();
        valve.setShowServerInfo(false);
        valve.setShowReport(false);
        context.getParent().getPipeline().addValve(valve);
      });
    }
  }

}
