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

package infra.annotation.config.http.client;

import org.jspecify.annotations.Nullable;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundles;
import infra.http.client.config.HttpClientSettings;
import infra.lang.Assert;

/**
 * Utility that can be used to map {@link HttpClientSettingsProperties} to
 * {@link HttpClientSettings}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class HttpClientSettingsPropertyMapper {

  private final @Nullable SslBundles sslBundles;

  private final HttpClientSettings settings;

  public HttpClientSettingsPropertyMapper(@Nullable SslBundles sslBundles, @Nullable HttpClientSettings settings) {
    this.sslBundles = sslBundles;
    this.settings = settings != null ? settings : HttpClientSettings.defaults();
  }

  public HttpClientSettings map(@Nullable HttpClientSettingsProperties properties) {
    HttpClientSettings settings = HttpClientSettings.defaults();
    if (properties != null) {
      if (properties.redirects != null) {
        settings = settings.withRedirects(properties.redirects);
      }

      if (properties.connectTimeout != null) {
        settings = settings.withConnectTimeout(properties.connectTimeout);
      }

      if (properties.readTimeout != null) {
        settings = settings.withReadTimeout(properties.readTimeout);
      }

      if (properties.ssl.bundle != null) {
        settings = settings.withSslBundle(getSslBundle(properties.ssl.bundle));
      }
    }
    return settings.orElse(this.settings);
  }

  private SslBundle getSslBundle(String name) {
    Assert.state(this.sslBundles != null, "No 'sslBundles' available");
    return this.sslBundles.getBundle(name);
  }

}
