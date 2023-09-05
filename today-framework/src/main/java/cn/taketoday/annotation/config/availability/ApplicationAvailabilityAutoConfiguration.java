/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.availability;

import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.framework.availability.ApplicationAvailability;
import cn.taketoday.framework.availability.ApplicationAvailabilityBean;
import cn.taketoday.stereotype.Component;

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
