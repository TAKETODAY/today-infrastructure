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

package infra.app.context.event;

import infra.app.Application;
import infra.app.ApplicationArguments;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;

/**
 * Event published when a {@link Application} is starting up and the
 * {@link ApplicationContext} is prepared and ApplicationContextInitializers have been
 * called but before any bean definitions are loaded.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Artsiom Yudovin
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ApplicationContextInitializedEvent extends ApplicationStartupEvent {

  private final ConfigurableApplicationContext context;

  /**
   * Create a new {@link ApplicationContextInitializedEvent} instance.
   *
   * @param application the current application
   * @param args the arguments the application is running with
   * @param context the context that has been initialized
   */
  public ApplicationContextInitializedEvent(Application application, ApplicationArguments args,
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
