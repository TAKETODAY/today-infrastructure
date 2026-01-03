/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.client.config;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.net.ssl.SSLException;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslManagerBundle;
import infra.core.ssl.SslOptions;
import infra.http.client.ReactorResourceFactory;
import infra.lang.Assert;
import infra.util.function.ThrowingConsumer;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider.SslContextSpec;

/**
 * Builder that can be used to create a Rector Netty {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public final class ReactorHttpClientBuilder {

  private final Supplier<HttpClient> factory;

  private final UnaryOperator<HttpClient> customizer;

  public ReactorHttpClientBuilder() {
    this(HttpClient::create, UnaryOperator.identity());
  }

  private ReactorHttpClientBuilder(Supplier<HttpClient> httpClientFactory, UnaryOperator<HttpClient> customizer) {
    this.factory = httpClientFactory;
    this.customizer = customizer;
  }

  /**
   * Return a new {@link ReactorHttpClientBuilder} that uses the given
   * {@link ReactorResourceFactory} to create the {@link HttpClient}.
   *
   * @param reactorResourceFactory the {@link ReactorResourceFactory} to use
   * @return a new {@link ReactorHttpClientBuilder} instance
   */
  public ReactorHttpClientBuilder withReactorResourceFactory(ReactorResourceFactory reactorResourceFactory) {
    Assert.notNull(reactorResourceFactory, "'reactorResourceFactory' is required");
    return new ReactorHttpClientBuilder(() -> HttpClient.create(reactorResourceFactory.getConnectionProvider()),
            httpClient -> this.customizer.apply(httpClient).runOn(reactorResourceFactory.getLoopResources()));
  }

  /**
   * Return a new {@link ReactorHttpClientBuilder} that uses the given factory to create
   * the {@link HttpClient}.
   *
   * @param factory the factory to use
   * @return a new {@link ReactorHttpClientBuilder} instance
   */
  public ReactorHttpClientBuilder withHttpClientFactory(Supplier<HttpClient> factory) {
    Assert.notNull(factory, "'factory' is required");
    return new ReactorHttpClientBuilder(factory, this.customizer);
  }

  /**
   * Return a new {@link ReactorHttpClientBuilder} that applies additional customization
   * to the underlying {@link HttpClient}.
   *
   * @param customizer the customizer to apply
   * @return a new {@link ReactorHttpClientBuilder} instance
   */
  public ReactorHttpClientBuilder withHttpClientCustomizer(UnaryOperator<HttpClient> customizer) {
    Assert.notNull(customizer, "'customizer' is required");
    return new ReactorHttpClientBuilder(this.factory,
            httpClient -> customizer.apply(this.customizer.apply(httpClient)));
  }

  /**
   * Build a new {@link HttpClient} instance with the given settings applied.
   *
   * @param settings the settings to apply
   * @return a new {@link HttpClient} instance
   */
  public HttpClient build(@Nullable HttpClientSettings settings) {
    settings = settings != null ? settings : HttpClientSettings.defaults();
    HttpClient httpClient = applyDefaults(this.factory.get());

    if (settings.connectTimeout() != null) {
      httpClient = setConnectTimeout(httpClient, settings.connectTimeout());
    }

    if (settings.readTimeout() != null) {
      httpClient = httpClient.responseTimeout(settings.readTimeout());
    }
    if (settings.sslBundle() != null) {
      httpClient = secure(httpClient, settings.sslBundle());
    }

    httpClient = httpClient.followRedirect(followRedirects(settings.redirects()));
    return this.customizer.apply(httpClient);
  }

  HttpClient applyDefaults(HttpClient httpClient) {
    // Aligns with Spring Framework defaults
    return httpClient.compress(true);
  }

  private HttpClient setConnectTimeout(HttpClient httpClient, Duration timeout) {
    return httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis());
  }

  private boolean followRedirects(@Nullable HttpRedirects redirects) {
    if (redirects == null) {
      redirects = HttpRedirects.FOLLOW_WHEN_POSSIBLE;
    }
    return switch (redirects) {
      case FOLLOW_WHEN_POSSIBLE, FOLLOW -> true;
      case DONT_FOLLOW -> false;
    };
  }

  private HttpClient secure(HttpClient httpClient, SslBundle sslBundle) {
    return httpClient.secure((ThrowingConsumer.of((spec) -> configureSsl(spec, sslBundle))));
  }

  private void configureSsl(SslContextSpec spec, SslBundle sslBundle) throws SSLException {
    SslOptions options = sslBundle.getOptions();
    SslManagerBundle managers = sslBundle.getManagers();
    SslContextBuilder builder = SslContextBuilder.forClient()
            .keyManager(managers.getKeyManagerFactory())
            .trustManager(managers.getTrustManagerFactory())
            .ciphers(SslOptions.asSet(options.getCiphers()))
            .protocols(options.getEnabledProtocols());
    spec.sslContext(builder.build());
  }

}
