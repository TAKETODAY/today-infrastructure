/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import cn.taketoday.context.annotation.config.AutoConfiguration;
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
@AutoConfiguration
public class ApplicationAvailabilityAutoConfiguration {

  @Component
  @ConditionalOnMissingBean(ApplicationAvailability.class)
  public ApplicationAvailabilityBean applicationAvailability() {
    return new ApplicationAvailabilityBean();
  }

}
