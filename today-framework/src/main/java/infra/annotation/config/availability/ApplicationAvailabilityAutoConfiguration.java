/*
 * Copyright 2017 - 2024 the original author or authors.
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

  @Component
  @ConditionalOnMissingBean(ApplicationAvailability.class)
  static ApplicationAvailabilityBean applicationAvailability() {
    return new ApplicationAvailabilityBean();
  }

}
