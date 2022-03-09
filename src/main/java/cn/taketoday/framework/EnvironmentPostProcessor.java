/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.framework;

import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;

/**
 * Allows for customization of the application's {@link Environment} prior to the
 * application context being refreshed.
 * <p>
 * EnvironmentPostProcessor implementations have to be registered in
 * {@code META-INF/today.strategies.properties}, using the fully qualified name of this class as the
 * key. Implementations may implement the {@link cn.taketoday.core.Ordered Ordered}
 * interface or use an {@link Order @Order} annotation
 * if they wish to be invoked in specific order.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author TODAY 2021/10/5 23:44
 * @since 4.0
 */
@FunctionalInterface
public interface EnvironmentPostProcessor {

  /**
   * Post-process the given {@code environment}.
   *
   * @param environment the environment to post-process
   * @param application the application to which the environment belongs
   */
  void postProcessEnvironment(
          ConfigurableEnvironment environment, Application application);

}
