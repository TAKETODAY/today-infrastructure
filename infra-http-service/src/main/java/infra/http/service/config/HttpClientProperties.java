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

package infra.http.service.config;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.context.properties.NestedConfigurationProperty;
import infra.http.client.config.HttpClientSettingsProperties;

/**
 * Base class for configuration properties common to both imperative and reactive HTTP
 * clients.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class HttpClientProperties extends HttpClientSettingsProperties {

  /**
   * Base url to set in the underlying HTTP client group. By default, set to
   * {@code null}.
   */
  public @Nullable String baseUri;

  /**
   * Default request headers for interface client group. By default, set to empty
   * {@link Map}.
   */
  public Map<String, List<String>> defaultHeader = new LinkedHashMap<>();

  /**
   * API version properties.
   */
  @NestedConfigurationProperty
  public final ApiVersionProperties apiVersion = new ApiVersionProperties();

}
