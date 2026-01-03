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

import infra.annotation.config.ssl.SslAutoConfiguration;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.ssl.SslBundles;
import infra.http.client.config.HttpClientSettings;
import infra.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpClientSettings}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@DisableDIAutoConfiguration(after = SslAutoConfiguration.class)
@EnableConfigurationProperties(HttpClientsProperties.class)
public class HttpClientAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  public static HttpClientSettings httpClientSettings(@Nullable SslBundles sslBundles, HttpClientsProperties properties) {
    HttpClientSettingsPropertyMapper propertyMapper = new HttpClientSettingsPropertyMapper(sslBundles, null);
    return propertyMapper.map(properties);
  }

}
