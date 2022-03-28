/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.context.config;

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.framework.env.EnvironmentPostProcessor;

/**
 * Context provided to {@link ConfigDataLocationResolver} methods.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public interface ConfigDataLocationResolverContext {

  /**
   * Provides access to a binder that can be used to obtain previously contributed
   * values.
   *
   * @return a binder instance
   */
  Binder getBinder();

  /**
   * Provides access to the parent {@link ConfigDataResource} that triggered the resolve
   * or {@code null} if there is no available parent.
   *
   * @return the parent location
   */
  ConfigDataResource getParent();

  /**
   * Provides access to the {@link ConfigurableBootstrapContext} shared across all
   * {@link EnvironmentPostProcessor EnvironmentPostProcessors}.
   *
   * @return the bootstrap context
   */
  ConfigurableBootstrapContext getBootstrapContext();

}
