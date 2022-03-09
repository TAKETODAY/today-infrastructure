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
package cn.taketoday.context;

import cn.taketoday.core.annotation.Order;

/**
 * Callback interface for initializing a {@link ConfigurableApplicationContext}
 * prior to being {@linkplain ConfigurableApplicationContext#refresh() refreshed}.
 *
 * <p>Typically used within web applications that require some programmatic initialization
 * of the application context. For example, registering property sources or activating
 * profiles against the {@linkplain ConfigurableApplicationContext#getEnvironment()
 * context's environment}.
 *
 * <p>{@code ApplicationContextInitializer} processors are encouraged to detect
 * whether {@link cn.taketoday.core.Ordered Ordered} interface has been
 * implemented or if the {@link Order @Order} annotation is
 * present and to sort instances accordingly if so prior to invocation.
 *
 * @author Chris Beams
 * @author TODAY 2021/11/5 10:29
 * @since 4.0
 */
@FunctionalInterface
public interface ApplicationContextInitializer {

  /**
   * Initialize the given application context.
   *
   * @param applicationContext the application to configure
   */
  void initialize(ConfigurableApplicationContext applicationContext);

}
