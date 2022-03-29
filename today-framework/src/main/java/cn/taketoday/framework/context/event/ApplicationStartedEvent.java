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

import java.time.Duration;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationRunner;
import cn.taketoday.framework.CommandLineRunner;

/**
 * Event published once the application context has been refreshed but before any
 * {@link ApplicationRunner application} and {@link CommandLineRunner command line}
 * runners have been called.
 *
 * @author Andy Wilkinson
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ApplicationStartedEvent extends SpringApplicationEvent {

  private final ConfigurableApplicationContext context;

  private final Duration timeTaken;

  /**
   * Create a new {@link ApplicationStartedEvent} instance.
   *
   * @param application the current application
   * @param args the arguments the application is running with
   * @param context the context that was being created
   * @param timeTaken the time taken to start the application
   */
  public ApplicationStartedEvent(Application application, String[] args, ConfigurableApplicationContext context,
          Duration timeTaken) {
    super(application, args);
    this.context = context;
    this.timeTaken = timeTaken;
  }

  /**
   * Return the application context.
   *
   * @return the context
   */
  public ConfigurableApplicationContext getApplicationContext() {
    return this.context;
  }

  /**
   * Return the time taken to start the application, or {@code null} if unknown.
   *
   * @return the startup time
   */
  public Duration getTimeTaken() {
    return this.timeTaken;
  }

}
