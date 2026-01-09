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

import java.time.Duration;

import infra.context.properties.ConfigurationPropertiesSource;
import infra.http.client.config.HttpClientSettings;
import infra.http.client.config.HttpRedirects;

/**
 * Base class for configuration properties configure {@link HttpClientSettings}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see HttpClientSettings
 * @since 5.0
 */
@ConfigurationPropertiesSource
public abstract class HttpClientSettingsProperties {

  /**
   * Handling for HTTP redirects.
   */
  public @Nullable HttpRedirects redirects;

  /**
   * Default connect timeout for a client HTTP request.
   */
  public @Nullable Duration connectTimeout;

  /**
   * Default read timeout for a client HTTP request.
   */
  public @Nullable Duration readTimeout;

  /**
   * Default SSL configuration for a client HTTP request.
   */
  public final Ssl ssl = new Ssl();

  /**
   * SSL configuration.
   */
  @ConfigurationPropertiesSource
  public static class Ssl {

    /**
     * SSL bundle to use.
     */
    public @Nullable String bundle;

  }

}
