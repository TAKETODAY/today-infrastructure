/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.context.event;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.time.Duration;

import infra.app.Application;
import infra.app.ApplicationArguments;
import infra.app.ApplicationRunner;
import infra.app.CommandLineRunner;
import infra.context.ConfigurableApplicationContext;

/**
 * Event published once the application context has been refreshed but before any
 * {@link ApplicationRunner application} and {@link CommandLineRunner command line}
 * runners have been called.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ApplicationStartedEvent extends ApplicationStartupEvent {

  @Serial
  private static final long serialVersionUID = 1L;

  private final ConfigurableApplicationContext context;

  @Nullable
  private final Duration timeTaken;

  /**
   * Create a new {@link ApplicationStartedEvent} instance.
   *
   * @param application the current application
   * @param args the arguments the application is running with
   * @param context the context that was being created
   * @param timeTaken the time taken to start the application
   */
  public ApplicationStartedEvent(Application application,
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
   * Return the time taken to start the application, or {@code null} if unknown.
   *
   * @return the startup time
   */
  @Nullable
  public Duration getTimeTaken() {
    return this.timeTaken;
  }

}
