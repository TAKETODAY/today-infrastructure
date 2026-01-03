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

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.net.ssl.SSLParameters;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslOptions;
import infra.lang.Assert;

/**
 * Builder that can be used to create a JDK {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class JdkHttpClientBuilder {

  private final Consumer<HttpClient.Builder> customizer;

  public JdkHttpClientBuilder() {
    this(Empty.consumer());
  }

  private JdkHttpClientBuilder(Consumer<HttpClient.Builder> customizer) {
    this.customizer = customizer;
  }

  /**
   * Return a new {@link JdkHttpClientBuilder} that uses the given executor with the
   * underlying {@link java.net.http.HttpClient.Builder}.
   *
   * @param executor the executor to use
   * @return a new {@link JdkHttpClientBuilder} instance
   */
  public JdkHttpClientBuilder withExecutor(Executor executor) {
    Assert.notNull(executor, "'executor' is required");
    return withCustomizer((httpClient) -> httpClient.executor(executor));
  }

  /**
   * Return a new {@link JdkHttpClientBuilder} that applies additional customization to
   * the underlying {@link java.net.http.HttpClient.Builder}.
   *
   * @param customizer the customizer to apply
   * @return a new {@link JdkHttpClientBuilder} instance
   */
  public JdkHttpClientBuilder withCustomizer(Consumer<HttpClient.Builder> customizer) {
    Assert.notNull(customizer, "'customizer' is required");
    return new JdkHttpClientBuilder(this.customizer.andThen(customizer));
  }

  /**
   * Build a new {@link HttpClient} instance with the given settings applied.
   *
   * @param settings the settings to apply
   * @return a new {@link HttpClient} instance
   */
  public HttpClient build(@Nullable HttpClientSettings settings) {
    settings = settings != null ? settings : HttpClientSettings.defaults();
    Assert.isTrue(settings.readTimeout() == null, "'settings' must not have a 'readTimeout'");
    HttpClient.Builder builder = HttpClient.newBuilder();

    builder.followRedirects(asHttpClientRedirect(settings.redirects()));

    if (settings.connectTimeout() != null) {
      builder.connectTimeout(settings.connectTimeout());
    }

    if (settings.sslBundle() != null) {
      builder.sslContext(settings.sslBundle().createSslContext());
      builder.sslParameters(asSslParameters(settings.sslBundle()));
    }

    customizer.accept(builder);
    return builder.build();
  }

  private SSLParameters asSslParameters(SslBundle sslBundle) {
    SslOptions options = sslBundle.getOptions();
    SSLParameters parameters = new SSLParameters();
    parameters.setCipherSuites(options.getCiphers());
    parameters.setProtocols(options.getEnabledProtocols());
    return parameters;
  }

  private Redirect asHttpClientRedirect(@Nullable HttpRedirects redirects) {
    if (redirects == null) {
      return Redirect.NORMAL;
    }
    return switch (redirects) {
      case FOLLOW_WHEN_POSSIBLE, FOLLOW -> Redirect.NORMAL;
      case DONT_FOLLOW -> Redirect.NEVER;
    };
  }

}
