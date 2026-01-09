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

package infra.annotation.config.availability;

import infra.app.availability.ApplicationAvailability;
import infra.app.availability.ApplicationAvailabilityBean;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.stereotype.Component;

/**
 * {@link EnableAutoConfiguration} for {@link ApplicationAvailabilityBean}.
 *
 * @author Brian Clozel
 * @author Taeik Lim
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDIAutoConfiguration
public class ApplicationAvailabilityAutoConfiguration {

  private ApplicationAvailabilityAutoConfiguration() {
  }

  @Component
  @ConditionalOnMissingBean(ApplicationAvailability.class)
  public static ApplicationAvailabilityBean applicationAvailability() {
    return new ApplicationAvailabilityBean();
  }

}
