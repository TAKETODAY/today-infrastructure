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

package infra.app.health.config.contributor;

import infra.app.health.contributor.HealthContributor;
import infra.app.health.contributor.PingHealthIndicator;
import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for default {@link HealthContributor
 * health contributors}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@AutoConfiguration
public final class HealthContributorAutoConfiguration {

  @Bean
  @ConditionalOnEnabledHealthIndicator("ping")
  static PingHealthIndicator pingHealthContributor() {
    return new PingHealthIndicator();
  }

}
