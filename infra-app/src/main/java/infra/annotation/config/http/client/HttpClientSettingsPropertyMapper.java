/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
