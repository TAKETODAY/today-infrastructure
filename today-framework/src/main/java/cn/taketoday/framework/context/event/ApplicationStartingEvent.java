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

package cn.taketoday.framework.context.event;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationArguments;
import cn.taketoday.framework.ConfigurableBootstrapContext;

/**
 * Event published as early as conceivably possible as soon as a {@link Application}
 * has been started - before the {@link Environment} or {@link ApplicationContext} is
 * available, but after the {@link ApplicationListener}s have been registered. The source
 * of the event is the {@link Application} itself, but beware of using its internal
 * state too much at this early stage since it might be modified later in the lifecycle.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ApplicationStartingEvent extends ApplicationStartupEvent {

  private final ConfigurableBootstrapContext bootstrapContext;

  /**
   * Create a new {@link ApplicationStartingEvent} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param application the current application
   * @param args the arguments the application is running with
   */
  public ApplicationStartingEvent(ConfigurableBootstrapContext bootstrapContext,
          Application application, ApplicationArguments args) {
    super(application, args);
    this.bootstrapContext = bootstrapContext;
  }

  /**
   * Return the bootstrap context.
   *
   * @return the bootstrap context
   */
  public ConfigurableBootstrapContext getBootstrapContext() {
    return this.bootstrapContext;
  }

}
