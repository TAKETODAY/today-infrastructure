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
import infra.context.ConfigurableApplicationContext;

/**
 * Event published by a {@link Application} when it fails to start.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationReadyEvent
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ApplicationFailedEvent extends ApplicationStartupEvent {

  private final ConfigurableApplicationContext context;

  private final Throwable exception;

  /**
   * Create a new {@link ApplicationFailedEvent} instance.
   *
   * @param application the current application
   * @param args the arguments the application was running with
   * @param context the context that was being created (maybe null)
   * @param exception the exception that caused the error
   */
  public ApplicationFailedEvent(Application application,
          ApplicationArguments args, ConfigurableApplicationContext context, Throwable exception) {
    super(application, args);
    this.context = context;
    this.exception = exception;
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
   * Return the exception that caused the failure.
   *
   * @return the exception
   */
  public Throwable getException() {
    return this.exception;
  }

}
