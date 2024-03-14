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

import org.xnio.Option;
import org.xnio.Options;

import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.cloud.CloudPlatform;
import cn.taketoday.framework.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.DataSize;
import cn.taketoday.util.PropertyMapper;
import cn.taketoday.util.ReflectionUtils;
import io.undertow.UndertowOptions;

/**
 * Customization for Undertow-specific features common for both Servlet and Reactive
 * servers.
 *
 * @author Brian Clozel
 * @author Yulin Qin
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Arstiom Yudovin
 * @author Rafiullah Hamedy
 * @author HaiTao Zhang
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UndertowWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableUndertowWebServerFactory>, Ordered {

  private final Environment environment;

  private final ServerProperties serverProperties;

  public UndertowWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
    this.environment = environment;
    this.serverProperties = serverProperties;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void customize(ConfigurableUndertowWebServerFactory factory) {
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    ServerOptions options = new ServerOptions(factory);
    map.from(serverProperties.maxHttpRequestHeaderSize).asInt(DataSize::toBytes).when(this::isPositive).to(options.option(UndertowOptions.MAX_HEADER_SIZE));
    mapUndertowProperties(factory, options);
    mapAccessLogProperties(factory);
    map.from(this::getOrDeduceUseForwardHeaders).to(factory::setUseForwardHeaders);
  }

  private void mapUndertowProperties(ConfigurableUndertowWebServerFactory factory, ServerOptions serverOptions) {
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    ServerProperties.Undertow properties = serverProperties.undertow;
    map.from(properties.bufferSize).whenNonNull().asInt(DataSize::toBytes).to(factory::setBufferSize);
    ServerProperties.Undertow.Threads threadProperties = properties.threads;

    map.from(threadProperties.io).to(factory::setIoThreads);
    map.from(threadProperties.worker).to(factory::setWorkerThreads);
    map.from(properties.directBuffers).to(factory::setUseDirectBuffers);
    map.from(properties.maxHttpPostSize).as(DataSize::toBytes).when(this::isPositive).to(serverOptions.option(UndertowOptions.MAX_ENTITY_SIZE));
    map.from(properties.maxParameters).to(serverOptions.option(UndertowOptions.MAX_PARAMETERS));
    map.from(properties.maxHeaders).to(serverOptions.option(UndertowOptions.MAX_HEADERS));
    map.from(properties.maxCookies).to(serverOptions.option(UndertowOptions.MAX_COOKIES));

    mapSlashProperties(properties, serverOptions);

    map.from(properties.decodeUrl).to(serverOptions.option(UndertowOptions.DECODE_URL));
    map.from(properties.urlCharset).as(Charset::name).to(serverOptions.option(UndertowOptions.URL_CHARSET));
    map.from(properties.alwaysSetKeepAlive).to(serverOptions.option(UndertowOptions.ALWAYS_SET_KEEP_ALIVE));
    map.from(properties.noRequestTimeout).asInt(Duration::toMillis).to(serverOptions.option(UndertowOptions.NO_REQUEST_TIMEOUT));
    map.from(properties.options.server).to(serverOptions.forEach(serverOptions::option));

    SocketOptions socketOptions = new SocketOptions(factory);
    map.from(properties.options.socket).to(socketOptions.forEach(socketOptions::option));
  }

  private void mapSlashProperties(ServerProperties.Undertow properties, ServerOptions serverOptions) {
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(properties.decodeSlash).to(serverOptions.option(UndertowOptions.DECODE_SLASH));
  }

  private boolean isPositive(Number value) {
    return value.longValue() > 0;
  }

  private void mapAccessLogProperties(ConfigurableUndertowWebServerFactory factory) {
    ServerProperties.Undertow.Accesslog properties = this.serverProperties.undertow.accesslog;
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(properties.enabled).to(factory::setAccessLogEnabled);
    map.from(properties.dir).to(factory::setAccessLogDirectory);
    map.from(properties.pattern).to(factory::setAccessLogPattern);
    map.from(properties.prefix).to(factory::setAccessLogPrefix);
    map.from(properties.suffix).to(factory::setAccessLogSuffix);
    map.from(properties.rotate).to(factory::setAccessLogRotate);
  }

  private boolean getOrDeduceUseForwardHeaders() {
    if (this.serverProperties.forwardHeadersStrategy == null) {
      CloudPlatform platform = CloudPlatform.getActive(this.environment);
      return platform != null && platform.isUsingForwardHeaders();
    }
    return this.serverProperties.forwardHeadersStrategy.equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
  }

  private abstract static class AbstractOptions {

    private final Class<?> source;

    private final Map<String, Option<?>> nameLookup;

    private final ConfigurableUndertowWebServerFactory factory;

    AbstractOptions(Class<?> source, ConfigurableUndertowWebServerFactory factory) {
      Map<String, Option<?>> lookup = new HashMap<>();
      ReflectionUtils.doWithLocalFields(source, field -> {
        int modifiers = field.getModifiers();
        if (Modifier.isPublic(modifiers)
                && Modifier.isStatic(modifiers)
                && Option.class.isAssignableFrom(field.getType())) {
          try {
            Option<?> option = (Option<?>) field.get(null);
            lookup.put(getCanonicalName(field.getName()), option);
          }
          catch (IllegalAccessException ignored) { }
        }
      });
      this.source = source;
      this.nameLookup = Collections.unmodifiableMap(lookup);
      this.factory = factory;
    }

    protected ConfigurableUndertowWebServerFactory getFactory() {
      return this.factory;
    }

    @SuppressWarnings("unchecked")
    <T> Consumer<Map<String, String>> forEach(Function<Option<T>, Consumer<T>> function) {
      return map -> map.forEach((key, value) -> {
        Option<T> option = (Option<T>) this.nameLookup.get(getCanonicalName(key));
        if (option == null) {
          throw new IllegalStateException("Unable to find '" + key + "' in " + ClassUtils.getShortName(this.source));
        }
        T parsed = option.parseValue(value, getClass().getClassLoader());
        function.apply(option).accept(parsed);
      });
    }

    private static String getCanonicalName(String name) {
      StringBuilder canonicalName = new StringBuilder(name.length());
      name.chars().filter(Character::isLetterOrDigit)
              .map(Character::toLowerCase)
              .forEach((c) -> canonicalName.append((char) c));
      return canonicalName.toString();
    }

  }

  /**
   * {@link ConfigurableUndertowWebServerFactory} wrapper that makes it easier to apply
   * {@link UndertowOptions server options}.
   */
  private static class ServerOptions extends AbstractOptions {

    ServerOptions(ConfigurableUndertowWebServerFactory factory) {
      super(UndertowOptions.class, factory);
    }

    <T> Consumer<T> option(Option<T> option) {
      return (value) -> getFactory().addBuilderCustomizers((builder) -> builder.setServerOption(option, value));
    }

  }

  /**
   * {@link ConfigurableUndertowWebServerFactory} wrapper that makes it easier to apply
   * {@link Options socket options}.
   */
  private static class SocketOptions extends AbstractOptions {

    SocketOptions(ConfigurableUndertowWebServerFactory factory) {
      super(Options.class, factory);
    }

    <T> Consumer<T> option(Option<T> option) {
      return (value) -> getFactory().addBuilderCustomizers((builder) -> builder.setSocketOption(option, value));
    }

  }

}
