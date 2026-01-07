/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.time.Duration;

import infra.app.Application;
import infra.app.ApplicationArguments;
import infra.context.ConfigurableApplicationContext;

/**
 * Event published as late as conceivably possible to indicate that the application is
 * ready to service requests. The source of the event is the {@link Application}
 * itself, but beware of modifying its internal state since all initialization steps will
 * have been completed by then.
 *
 * @author Stephane Nicoll
 * @author Chris Bono
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationFailedEvent
 * @since 4.0
 */
public class ApplicationReadyEvent extends ApplicationStartupEvent {

  @Serial
  private static final long serialVersionUID = 1L;

  private final ConfigurableApplicationContext context;

  @Nullable
  private final Duration timeTaken;

  /**
   * Create a new {@link ApplicationReadyEvent} instance.
   *
   * @param application the current application
   * @param args the arguments the application is running with
   * @param context the context that was being created
   * @param timeTaken the time taken to get the application ready to service requests
   */
  public ApplicationReadyEvent(Application application,
          ApplicationArguments args, ConfigurableApplicationContext context, @Nullable Duration timeTaken) {
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
   * Return the time taken for the application to be ready to service requests, or
   * {@code null} if unknown.
   *
   * @return the time taken to be ready to service requests
   */
  @Nullable
  public Duration getTimeTaken() {
    return this.timeTaken;
  }

}
