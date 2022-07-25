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

package cn.taketoday.framework.context.event;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ConfigurableBootstrapContext;

/**
 * Event published when a {@link Application} is starting up and the
 * {@link Environment} is first available for inspection and modification.
 *
 * @author Dave Syer
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ApplicationEnvironmentPreparedEvent extends ApplicationStartupEvent {

  private final ConfigurableBootstrapContext bootstrapContext;

  private final ConfigurableEnvironment environment;

  /**
   * Create a new {@link ApplicationEnvironmentPreparedEvent} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param application the current application
   * @param args the arguments the application is running with
   * @param environment the environment that was just created
   */
  public ApplicationEnvironmentPreparedEvent(ConfigurableBootstrapContext bootstrapContext,
          Application application, String[] args, ConfigurableEnvironment environment) {
    super(application, args);
    this.bootstrapContext = bootstrapContext;
    this.environment = environment;
  }

  /**
   * Return the bootstrap context.
   *
   * @return the bootstrap context
   */
  public ConfigurableBootstrapContext getBootstrapContext() {
    return this.bootstrapContext;
  }

  /**
   * Return the environment.
   *
   * @return the environment
   */
  public ConfigurableEnvironment getEnvironment() {
    return this.environment;
  }

}
