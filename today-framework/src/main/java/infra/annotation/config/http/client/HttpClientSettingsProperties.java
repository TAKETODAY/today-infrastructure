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
