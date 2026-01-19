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

import infra.app.health.SslHealthIndicator;
import infra.app.health.config.contributor.ConditionalOnEnabledHealthIndicator;
import infra.app.health.config.contributor.HealthContributorAutoConfiguration;
import infra.app.health.contributor.Health;
import infra.app.info.SslInfo;
import infra.core.ssl.SslBundles;
import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link SslHealthIndicator}.
 *
 * @author Jonatan Ivanov
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@AutoConfiguration(before = HealthContributorAutoConfiguration.class)
@ConditionalOnClass(Health.class)
@ConditionalOnEnabledHealthIndicator("ssl")
@EnableConfigurationProperties(SslHealthIndicatorProperties.class)
public final class SslHealthContributorAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = "sslHealthIndicator")
  static SslHealthIndicator sslHealthIndicator(SslInfo sslInfo, SslHealthIndicatorProperties properties) {
    return new SslHealthIndicator(sslInfo, properties.certificateValidityWarningThreshold);
  }

  @Bean
  @ConditionalOnMissingBean
  static SslInfo sslInfo(SslBundles sslBundles) {
    return new SslInfo(sslBundles);
  }

}
