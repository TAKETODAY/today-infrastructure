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

package infra.app.health.config;

import java.time.Duration;

import infra.app.health.SslHealthIndicator;
import infra.context.properties.ConfigurationProperties;

/**
 * External configuration properties for {@link SslHealthIndicator}.
 *
 * @author Jonatan Ivanov
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@ConfigurationProperties("app.health.ssl")
public class SslHealthIndicatorProperties {

  /**
   * If an SSL Certificate will be invalid within the time span defined by this
   * threshold, it should trigger a warning.
   */
  public Duration certificateValidityWarningThreshold = Duration.ofDays(14);

}
