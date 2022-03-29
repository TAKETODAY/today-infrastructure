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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.Application;

/**
 * Event published as when a {@link Application} is starting up and the
 * {@link ApplicationContext} is fully prepared but not refreshed. The bean definitions
 * will be loaded and the {@link Environment} is ready for use at this stage.
 *
 * @author Dave Syer
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ApplicationPreparedEvent extends SpringApplicationEvent {

  private final ConfigurableApplicationContext context;

  /**
   * Create a new {@link ApplicationPreparedEvent} instance.
   *
   * @param application the current application
   * @param args the arguments the application is running with
   * @param context the ApplicationContext about to be refreshed
   */
  public ApplicationPreparedEvent(Application application, String[] args,
          ConfigurableApplicationContext context) {
    super(application, args);
    this.context = context;
  }

  /**
   * Return the application context.
   *
   * @return the context
   */
  public ConfigurableApplicationContext getApplicationContext() {
    return this.context;
  }

}
