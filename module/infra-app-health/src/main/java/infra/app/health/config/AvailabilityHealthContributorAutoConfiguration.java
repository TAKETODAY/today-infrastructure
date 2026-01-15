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

import infra.app.availability.ApplicationAvailability;
import infra.app.config.availability.ApplicationAvailabilityAutoConfiguration;
import infra.app.health.AvailabilityStateHealthIndicator;
import infra.app.health.LivenessStateHealthIndicator;
import infra.app.health.ReadinessStateHealthIndicator;
import infra.app.health.contributor.Health;
import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBooleanProperty;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link AvailabilityStateHealthIndicator}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@AutoConfiguration(after = ApplicationAvailabilityAutoConfiguration.class)
@ConditionalOnClass(Health.class)
public final class AvailabilityHealthContributorAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = "livenessStateHealthIndicator")
  @ConditionalOnBooleanProperty("app.health.livenessstate.enabled")
  static LivenessStateHealthIndicator livenessStateHealthIndicator(ApplicationAvailability applicationAvailability) {
    return new LivenessStateHealthIndicator(applicationAvailability);
  }

  @Bean
  @ConditionalOnMissingBean(name = "readinessStateHealthIndicator")
  @ConditionalOnBooleanProperty("app.health.readinessstate.enabled")
  static ReadinessStateHealthIndicator readinessStateHealthIndicator(ApplicationAvailability applicationAvailability) {
    return new ReadinessStateHealthIndicator(applicationAvailability);
  }

}
